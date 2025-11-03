package com.rossPalletScanApp.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rossPalletScanApp.dto.OrganizedInferenceResponse;
import com.rossPalletScanApp.dto.RossLabelExtractResponse;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class ImageService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final RestTemplate restTemplate;
	private final ObjectMapper mapper = new ObjectMapper();

	private final String bucketName = "ross-label-extraction";
	private static final String INFERENCE_URL = "http://agenticaisearchservice-public.westus2.azurecontainer.io:8002/inference";

	public ImageService(S3Client s3Client, S3Presigner s3Presigner) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;

		SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
		rf.setConnectTimeout(10_000);
		rf.setReadTimeout(30_000);
		this.restTemplate = new RestTemplate(rf);
	}

	/** Upload → presign → call inference → map into DTOs */
	public OrganizedInferenceResponse uploadAndInfer(MultipartFile file) throws IOException {
		// Upload (private, no ACLs)
		String key = uploadToS3AndReturnKey(file);
		// Presigned URL (e.g., 30 minutes)
		// String presignedUrl = generatePresignedGetUrl(key, Duration.ofMinutes(30));
		String presignedUrl = "https://ross-label-extraction.s3.amazonaws.com/" + key;
		// Build inference payload
		Map<String, Object> payload = buildInferencePayload(presignedUrl);

		// Call inference API
		String raw = restTemplate.postForObject(INFERENCE_URL, payload, String.class);

		// Parse root inference node
		JsonNode root = mapper.readTree(raw);
		JsonNode inferenceNode = root; // some services return directly, others wrap—yours returns {"success": "..."}
		// If the response is like {"inference": {...}}, then:
		if (root.has("inference")) {
			inferenceNode = root.get("inference");
		}

		// Map to your DTO
		RossLabelExtractResponse dto = mapInferenceToRossDto(inferenceNode, key);

		// Wrap with S3 meta
		return new OrganizedInferenceResponse(key, presignedUrl, dto);
	}

	private String uploadToS3AndReturnKey(MultipartFile file) throws IOException {
		String sanitizedName = sanitize(file.getOriginalFilename());
		String key = "uploads/" + Instant.now().toEpochMilli() + "_" + sanitizedName;

		s3Client.putObject(
				PutObjectRequest.builder().bucket(bucketName).key(key).contentType(file.getContentType()).build(),
				RequestBody.fromBytes(file.getBytes()));

		return key;
	}

	private String generatePresignedGetUrl(String key, Duration ttl) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(ttl)
				.getObjectRequest(getObjectRequest).build();

		PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
		return presigned.url().toString();
	}

	private String sanitize(String filename) {
		String base = (filename == null || filename.isBlank()) ? "file" : filename;

		// Keep safe characters: letters, digits, dot, dash, underscore, space,
		// parentheses
		// Replace anything else with underscore
		String cleaned = base.replaceAll("[^a-zA-Z0-9._\\- ()]", "_");

		// Normalize spaces
		cleaned = cleaned.replaceAll(" +", " ").trim();

		// IMPORTANT: do NOT URL-encode here.
		return cleaned;
	}

	/** Build the exact payload your inference service expects */
	private Map<String, Object> buildInferencePayload(String imageUrl) {
		Map<String, Object> payload = new LinkedHashMap<>();

		// Generate GUIDs
		String guid = UUID.randomUUID().toString();

		payload.put("user_msg", imageUrl);
		payload.put("user_id", guid);
		payload.put("session_id", guid);
		payload.put("request_id", guid);
		payload.put("intent", "label_extraction:inference");
		payload.put("version_id", "v1");
		payload.put("default", true);
		payload.put("latest", true);
		payload.put("entity", Map.of());
		payload.put("image_input", List.of(Map.of("type", "image_url", "url", imageUrl)));
		payload.put("agent_code", "ross_label_extraction");
		payload.put("context_list", List.of(Map.of("context_type", "parentcontext", "context_name",
				"ross_image_label_extraction", "context_value", imageUrl)));
		payload.put("top", 5);
		payload.put("validation_clue", "Check for missing attributes");
		payload.put("model", "gpt-4o");
		payload.put("context_code", "ross_label_extraction");
		return payload;
	}

	/** Parse the inference JSON (with ```json fenced block) into your DTO */
	private RossLabelExtractResponse mapInferenceToRossDto(JsonNode inferenceNode, String s3Key) {
		try {
			// 1) Extract the markdown-fenced JSON string
			String successBlock = textOrNull(inferenceNode.get("success"));
			if (successBlock == null || successBlock.isBlank()) {
				return errorDto("Inference 'success' content missing or empty", s3Key);
			}

			// 2) Strip ``` fences
			String jsonText = stripCodeFence(successBlock);

			// 3) Parse the JSON block
			JsonNode successJson = mapper.readTree(jsonText);
			JsonNode firstImage = successJson.path("images").path(0);
			if (firstImage.isMissingNode() || firstImage.isNull()) {
				return errorDto("No images[] in inference result", s3Key);
			}

			JsonNode attrs = firstImage.path("attributes");
			if (attrs.isMissingNode() || !attrs.isObject()) {
				return errorDto("No attributes object in first image", s3Key);
			}

			// 4) Map attributes → your fields
			String rossPo = getAttr(attrs, "ROSS PO#", "ROSS PO", "ROSSPO#", "ROSS PO Number");
			String rossStyle = getAttr(attrs, "ROSS Style#", "ROSS STYLE#", "ROSS Style", "ROSS STYLE");
			String itemDescription = getAttr(attrs, "Item Description", "ITEM DESCRIPTION");
			String color = getAttr(attrs, "Color", "COLOR");
			String rossSkuNumber = getAttr(attrs, "ROSS SKU Number", "ROSS SKU#", "ROSS SKU", "ROSS SKU No");
			String quantity = getAttr(attrs, "Quantity", "QUANTITY");
			String netWeightKg = getAttr(attrs, "Net Weight (N.W.)", "N.W.", "Net Weight");
			String grossWeightKg = getAttr(attrs, "Gross Weight (G.W.)", "G.W.", "Gross Weight");
			String measurement = getAttr(attrs, "Measurement", "MEASUREMENT");
			String consignedTo = getAttr(attrs, "Consigned To", "CONSIGNED TO");
			String deliverTo = getAttr(attrs, "Deliver To", "DELIVER TO");
			String countryOfOrigin = getAttr(attrs, "Country of Origin", "COUNTRY OF ORIGIN");
			String cartonNo = getAttr(attrs, "Carton No.", "Carton No", "CARTON NO.");

			// Friendly filename from key
			String imageFilename = deriveFilenameFromS3Key(s3Key);

			// 5) VALIDATION: require rossPo, color, rossSkuNumber
			boolean hasRequired = notNull(rossPo) && notNull(color) && notNull(rossSkuNumber);

			if (!hasRequired) {
				// Build FAILURE DTO with a clear message
				return RossLabelExtractResponse.of("ERROR",
						"Missing required attributes: ROSS PO#, Color, or ROSS SKU Number", null, imageFilename, null,
						rossPo, rossStyle, itemDescription, color, rossSkuNumber, quantity, netWeightKg, grossWeightKg,
						measurement, consignedTo, deliverTo, countryOfOrigin, cartonNo);
			}

			// 6) SUCCESS DTO
			return RossLabelExtractResponse.of("SUCCESS", "Label extracted successfully", null, // ocrConfidence
																								// (unknown)
					imageFilename, null, // rawText (not stored in class, keep null)
					rossPo, rossStyle, itemDescription, color, rossSkuNumber, quantity, netWeightKg, grossWeightKg,
					measurement, consignedTo, deliverTo, countryOfOrigin, cartonNo);

		} catch (Exception ex) {
			return errorDto("Failed to extract the label: " + ex.getMessage(), s3Key);
		}
	}

	private boolean notNull(String s) {
		return s != null && !s.isBlank();
	}

	private RossLabelExtractResponse errorDto(String message, String s3Key) {
		String imageFilename = deriveFilenameFromS3Key(s3Key);
		return RossLabelExtractResponse.of("ERROR", message, null, imageFilename, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null);
	}

	private String textOrNull(JsonNode n) {
		return (n == null || n.isNull()) ? null : n.asText();
	}

	private String stripCodeFence(String fenced) {
		String s = fenced.trim();
		if (s.startsWith("```")) {
			// Remove leading ```lang (if any)
			int firstNewline = s.indexOf('\n');
			if (firstNewline > 0) {
				s = s.substring(firstNewline + 1);
			} else {
				s = s.substring(3); // just remove ```
			}
		}
		if (s.endsWith("```")) {
			s = s.substring(0, s.length() - 3);
		}
		return s.trim();
	}

	private String getAttr(JsonNode attrs, String... candidates) {
		for (String key : candidates) {
			JsonNode v = attrs.get(key);
			if (v != null && !v.isNull()) {
				String val = v.asText();
				if (val != null && !val.isBlank() && !"Not specified".equalsIgnoreCase(val)) {
					return val;
				}
			}
		}
		return null;
	}

	private String deriveFilenameFromS3Key(String key) {
		// key like: uploads/1761899527113_Carton+sample+5+Black.jpg
		String last = key.substring(key.lastIndexOf('/') + 1);
		String afterTs = last.contains("_") ? last.substring(last.indexOf('_') + 1) : last;
		// Convert URL-encoded + and %XX to readable
		String decoded = URLDecoder.decode(afterTs, StandardCharsets.UTF_8);
		return decoded;
	}
}