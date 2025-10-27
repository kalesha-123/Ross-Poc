package com.rossPalletScanApp.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
/** Parses ROSS carton label text into structured fields. Java 8 compatible. */
public class RossLabelParser {

	// Accept both ':' and '：' (full-width). In some OCR cases colon may be missing;
	// we'll allow optional colon in critical patterns.
	private static final String SEP = "\\s*[:：]?\\s*";

	// Stop at next header or end-of-string (prevents bleeding across headers on
	// noisy lines)
	private static final String HEADER_STOP = "(?=\\s*(?:"
			+ "ROSS\\s*PO\\b|ROSSPO\\b|ROSS\\s*STYLE\\b|ROSSSTYLE\\b|ITEM\\s*DESCRIPTION\\b|COLOR\\b|"
			+ "ROSS\\s*SKU\\s*NUMBER\\b|ROSS\\s*SKUNUMBER\\b|QUANTITY\\b|N\\.?\\s*W\\.?\\b|G\\.?\\s*W\\.?\\b|"
			+ "MEASUREMENT\\b|CONSIGNED\\s*TO\\b|DELIVER\\s*TO\\b|COUNTRY\\s*OF\\s*ORIGIN\\b|CARTON\\s*NO\\b" + ")|$)";

	// ---------- OCR-tolerant Patterns ----------

	// PO: tolerate "ROSS PO" or "ROSSPO", optional space before '#', optional
	// colon, capture up to next header.
	// Also, PO values often are numeric; we'll post-clean to take the first long
	// digit run.
	private static final Pattern PO_PATTERN = Pattern.compile(
			"(?i)(?:ROSS\\s*PO|ROSSPO)\\s*#?\\s*[:：]?\\s*([A-Za-z0-9\\-_/\\. ]+?)" + HEADER_STOP, Pattern.MULTILINE);

	// STYLE: allow "ROSS STYLE" / "ROSSSTYLE" plus possible trailing noise chars
	// like 't' from OCR, optional '#', optional colon.
	private static final Pattern STYLE_PATTERN = Pattern.compile(
			"(?i)(?:ROSS\\s*STYLE\\w*|ROSSSTYLE\\w*)\\s*#?\\s*[:：]?\\s*(.+?)" + HEADER_STOP, Pattern.MULTILINE);

	// ITEM DESCRIPTION: straightforward with lookahead stop.
	private static final Pattern ITEM_DESC_PATTERN = Pattern
			.compile("(?i)\\bITEM\\s*DESCRIPTION\\b" + SEP + "(.+?)" + HEADER_STOP, Pattern.MULTILINE);

	// COLOR: OCR often leaves dots or adjacent text. Accept simple characters and
	// stop at next header.
	// Also provide a fallback for lines that look like "COLORRED" without colon.
	private static final Pattern COLOR_PATTERN = Pattern
			.compile("(?i)\\bCOLOR\\b" + SEP + "([A-Za-z0-9 \\-/&()\\.]+?)" + HEADER_STOP, Pattern.MULTILINE);

	// SKU: support both "ROSS SKU NUMBER:" and "ROSSSKUNUMBER:" variants
	private static final Pattern ROSS_SKU_PATTERN = Pattern
			.compile("(?i)(?:\\bROSS\\s*SKU\\s*NUMBER\\b|\\bROSS\\s*SKUNUMBER\\b)" + SEP + "([A-Za-z0-9\\-_/\\.]+?)"
					+ HEADER_STOP, Pattern.MULTILINE);

	private static final Pattern QTY_PATTERN = Pattern.compile("(?i)\\bQUANTITY\\b" + SEP + "(.+?)" + HEADER_STOP,
			Pattern.MULTILINE);

	// Net Weight: allow "NW" or "N.W", optional colon, optional +/- sign,
	// decimal/comma, "KG"/"KGS" with/without space
	private static final Pattern NW_PATTERN = Pattern.compile(
			"(?i)\\bN\\s*\\.?\\s*W\\s*\\.?\\b" + SEP + "\\s*([-+]?\\d+(?:[\\.,]\\d+)?)\\s*KGS?\\b", Pattern.MULTILINE);

	// Gross Weight: allow "GW" or "G.W", same tolerance as above
	private static final Pattern GW_PATTERN = Pattern.compile(
			"(?i)\\bG\\s*\\.?\\s*W\\s*\\.?\\b" + SEP + "\\s*([-+]?\\d+(?:[\\.,]\\d+)?)\\s*KGS?\\b", Pattern.MULTILINE);

	private static final Pattern MEAS_PATTERN = Pattern.compile("(?i)\\bMEASUREMENT\\b" + SEP + "(.+?)" + HEADER_STOP,
			Pattern.MULTILINE);

	private static final Pattern CONSIGNED_TO_PATTERN = Pattern
			.compile("(?i)\\bCONSIGNED\\s*TO\\b" + SEP + "(.+?)" + HEADER_STOP, Pattern.MULTILINE);

	private static final Pattern DELIVER_TO_PATTERN = Pattern
			.compile("(?i)\\bDELIVER\\s*TO\\b" + SEP + "(.+?)" + HEADER_STOP, Pattern.MULTILINE);

	private static final Pattern ORIGIN_PATTERN = Pattern
			.compile("(?i)\\bCOUNTRY\\s*OF\\s*ORIGIN\\b" + SEP + "(.+?)" + HEADER_STOP, Pattern.MULTILINE);

	private static final Pattern CARTON_NO_PATTERN = Pattern.compile("(?i)\\bCARTON\\s*NO\\b" + SEP + "(.*)",
			Pattern.MULTILINE);

	public Parsed parse(String rawText) {
		if (rawText == null) {
			throw new IllegalArgumentException("OCR text is null");
		}

		// Normalize first so patterns match reliably
		String norm = normalize(rawText);
		List<String> lines = Arrays.asList(norm.split("\n"));

		// --- Primary extraction via regex ---
		String rossPo = cleanPo(trimToNull(findOne(norm, PO_PATTERN, 1)));
		String rossStyle = cleanStyle(trimToNull(findOne(norm, STYLE_PATTERN, 1)));
		String itemDesc = trimToNull(findOne(norm, ITEM_DESC_PATTERN, 1));
		String color = cleanColor(trimToNull(findOne(norm, COLOR_PATTERN, 1)));

		// --- Fallbacks if still null ---
		if (rossPo == null) {
			rossPo = fallbackPo(lines);
		}
		if (rossStyle == null) {
			rossStyle = fallbackStyle(lines);
		}
		if (color == null) {
			color = fallbackColor(lines);
		}

		// ROSS SKU NUMBER or ROSSSKUNUMBER:
		String rossSku = trimToNull(findOne(norm, ROSS_SKU_PATTERN, 1));
		String qty = trimToNull(findOne(norm, QTY_PATTERN, 1));
		String nwKg = cleanWeight(trimToNull(findOne(norm, NW_PATTERN, 1)));
		String gwKg = cleanWeight(trimToNull(findOne(norm, GW_PATTERN, 1)));

		// Fallbacks if primary regex missed due to weird OCR
		if (nwKg == null)
			nwKg = fallbackWeight(lines, true); // true => NW
		if (gwKg == null)
			gwKg = fallbackWeight(lines, false); // false => GW
		String meas = trimToNull(findOne(norm, MEAS_PATTERN, 1));
		String consignedTo = trimToNull(findOne(norm, CONSIGNED_TO_PATTERN, 1));

		// Deliver-to may be followed by an address line(s). Capture the first following
		// non-empty line
		String deliverTo = null;
		String deliverToAddress = null;
		{
			int idx = indexOfLineMatching(lines, DELIVER_TO_PATTERN);
			if (idx >= 0) {
				// The name on the same line:
				deliverTo = trimToNull(extractFromLine(lines.get(idx), DELIVER_TO_PATTERN, 1));
				// Address likely on next non-empty line (until next known header)
				String next = nextNonEmpty(lines, idx + 1);
				// Stop if next looks like a header
				if (next != null && !looksLikeHeader(next)) {
					deliverToAddress = next.trim();
				}
			} else {
				// Fallback (single-line match)
				deliverTo = trimToNull(findOne(norm, DELIVER_TO_PATTERN, 1));
			}
		}

		String origin = trimToNull(findOne(norm, ORIGIN_PATTERN, 1));

		// Carton No may be blank on line, with value on next line (e.g. "70")
		String cartonNo = null;
		{
			int idx = indexOfLineMatching(lines, CARTON_NO_PATTERN);
			if (idx >= 0) {
				String sameLine = trimToNull(extractFromLine(lines.get(idx), CARTON_NO_PATTERN, 1));
				if (sameLine != null && !sameLine.isEmpty()) {
					cartonNo = sameLine;
				} else {
					String next = nextNonEmpty(lines, idx + 1);
					if (next != null && !looksLikeHeader(next)) {
						cartonNo = next.trim();
					}
				}
			} else {
				cartonNo = trimToNull(findOne(norm, CARTON_NO_PATTERN, 1));
			}
		}

		return new Parsed(rossPo, rossStyle, itemDesc, color, rossSku, qty, nwKg, gwKg, meas, consignedTo, deliverTo, origin, cartonNo);
	}

	// ---------- normalization ----------

	private static String normalize(String text) {
		String s = text.replace("\r\n", "\n").replace('\r', '\n');

		// Normalize full-width colon to ASCII and strip common OCR noise
		s = s.replace('：', ':').replace("|", " ").replace("¥", " ").replace("，", " ").replace("…", " ")
				.replace("—", "-").replace("_", " ").replace("=", " ").replace("‘", "'").replace("“", "\"")
				.replace("”", "\"");

		// Collapse whitespace
		s = s.replaceAll("[\\t\\u00A0]+", " ");
		s = s.replaceAll(" +", " ");

		// Canonicalize headers so spacing around '#' and ':' won't break regex
		s = s.replaceAll("(?i)\\bROSS\\s*PO\\b\\s*#\\s*:", "ROSS PO #:")
				.replaceAll("(?i)\\bROSSPO\\b\\s*#\\s*:", "ROSSPO #:")
				.replaceAll("(?i)\\bROSS\\s*STYLE\\b\\s*#\\s*:", "ROSS STYLE #:")
				.replaceAll("(?i)\\bROSSSTYLE\\b\\s*#\\s*:", "ROSSSTYLE #:")
				.replaceAll("(?i)\\bROSS\\s*SKU\\s*NUMBER\\b\\s*:", "ROSS SKU NUMBER:")
				.replaceAll("(?i)\\bROSS\\s*SKUNUMBER\\b\\s*:", "ROSS SKUNUMBER:");

		// Also fix common OCR header typos from sample 2
		s = s.replaceAll("(?i)ROSS\\s*STYLEt\\s*:", "ROSS STYLE:").replaceAll("(?i)COLORBLUE", "COLOR: BLUE"); // recover
																												// missing
																												// colon
																												// cases
		s = s.replaceAll("(?i)ROSS\\s*SKUNUMBER\\s*;", "ROSS SKUNUMBER:"); // semicolon OCR

		return s;
	}

	// ---------- helpers ----------

	private static String findOne(String text, Pattern p, int group) {
		Matcher m = p.matcher(text);
		return m.find() ? m.group(group) : null;
	}

	private static String extractFromLine(String line, Pattern p, int group) {
		Matcher m = p.matcher(line);
		return m.find() ? m.group(group) : null;
	}

	private static String nextNonEmpty(List<String> lines, int start) {
		for (int i = start; i < lines.size(); i++) {
			String s = lines.get(i);
			if (s != null && !s.trim().isEmpty())
				return s;
		}
		return null;
	}

	private static int indexOfLineMatching(List<String> lines, Pattern p) {
		for (int i = 0; i < lines.size(); i++) {
			Matcher m = p.matcher(lines.get(i));
			if (m.find())
				return i;
		}
		return -1;
	}

	private static boolean looksLikeHeader(String s) {
		String u = s == null ? "" : s.toUpperCase(Locale.ENGLISH);
		return u.matches(
				".*\\b(ROSS\\s*PO|ROSSPO|ROSS\\s*STYLE|ROSSSTYLE|ITEM\\s*DESCRIPTION|COLOR|ROSS\\s*SKU\\s*NUMBER|ROSS\\s*SKUNUMBER|QUANTITY|N\\.?\\s*W\\.?|G\\.?\\s*W\\.?|MEASUREMENT|CONSIGNED\\s*TO|DELIVER\\s*TO|COUNTRY\\s*OF\\s*ORIGIN|CARTON\\s*NO)\\b.*");
	}

	private static String trimToNull(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static String cleanWeight(String s) {
		if (s == null)
			return null;
		// Remove leading +/- (minus is usually OCR noise)
		String t = s.trim().replaceFirst("^[+\\-]\\s*", "");
		// Normalize decimal comma to dot
		t = t.replace(',', '.');
		// Keep only the numeric part
		Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(t);
		return m.find() ? m.group(1) : null;
	}

	private static String fallbackWeight(List<String> lines, boolean net) {
		Pattern header = net ? Pattern.compile("(?i)\\bN\\s*\\.?\\s*W\\s*\\.?\\b")
				: Pattern.compile("(?i)\\bG\\s*\\.?\\s*W\\s*\\.?\\b");
		Pattern num = Pattern.compile("([-+]?\\d+(?:[\\.,]\\d+)?)");

		for (String ln : lines) {
			if (ln == null)
				continue;
			if (header.matcher(ln).find()) {
				Matcher m = num.matcher(ln);
				if (m.find()) {
					return cleanWeight(m.group(1));
				}
			}
		}
		return null;
	}
	// --- small post-cleaners tailored for fields ---

	private static String cleanPo(String s) {
		if (s == null)
			return null;
		// Extract first long digit run (covers cases like "60238987: oe")
		Matcher m = Pattern.compile("(\\d{6,})").matcher(s);
		if (m.find())
			return m.group(1);
		return s.replaceAll("[^A-Za-z0-9\\-_/\\. ]", "").trim();
	}

	private static String cleanStyle(String s) {
		if (s == null)
			return null;
		// Remove obvious noise, keep typical style chars, collapse spaces
		String cleaned = s.replaceAll("[^A-Za-z0-9\\-_/\\. ]", " ").replaceAll(" +", " ").trim();
		return cleaned;
	}

	private static String cleanColor(String s) {
		if (s == null)
			return null;
		// Drop trailing dots/spaces; normalize 'BLUE' etc.
		String cleaned = s.replaceAll("\\s*\\.+\\s*$", "").trim();
		return cleaned;
	}

	// --- fallbacks when regex fails due to unusual OCR ---
	private static String fallbackPo(List<String> lines) {
		for (String ln : lines) {
			String u = ln.toUpperCase(Locale.ENGLISH);
			if (u.contains("ROSSPO")) {
				// Take digits from the line
				Matcher m = Pattern.compile("(\\d{6,})").matcher(ln);
				if (m.find())
					return m.group(1);
				// else take tokens after marker
				int idx = u.indexOf("ROSSPO");
				String tail = ln.substring(Math.min(idx + "ROSSPO".length(), ln.length()));
				tail = tail.replaceAll("[^A-Za-z0-9\\-_/\\. ]", " ").trim();
				if (!tail.isEmpty())
					return tail.split("\\s+")[0];
			}
			if (u.contains("ROSS PO")) {
				Matcher m = Pattern.compile("(\\d{6,})").matcher(ln);
				if (m.find())
					return m.group(1);
			}
		}
		return null;
	}

	private static String fallbackStyle(List<String> lines) {
		for (String ln : lines) {
			String u = ln.toUpperCase(Locale.ENGLISH);
			if (u.contains("ROSSSTYLE") || u.contains("ROSS STYLE")) {
				// Extract after header up to end-of-line
				String val = ln.replaceFirst("(?i).*ROSS\\s*STYLE\\w*\\s*#?\\s*[:：]?", "").trim();
				val = val.replaceAll("[|¥]+", " ").replaceAll(" +", " ");
				val = val.replaceAll("[^A-Za-z0-9\\-_/\\. ]", " ").replaceAll(" +", " ").trim();
				if (!val.isEmpty())
					return val;
			}
		}
		return null;
	}

	private static String fallbackColor(List<String> lines) {
		for (String ln : lines) {
			String u = ln.toUpperCase(Locale.ENGLISH);
			if (u.contains("COLOR")) {
				// Handle "COLOR: RED ." and "COLORBLUE"
				String val = ln.replaceFirst("(?i).*COLOR\\s*[:：]?", "");
				val = val.replaceAll("[^A-Za-z0-9\\-_/&()\\. ]", " ").replaceAll(" +", " ").trim();
				// pick the first token that looks colorish (letters only)
				Matcher m = Pattern.compile("([A-Za-z]{3,})").matcher(val);
				if (m.find())
					return m.group(1).toUpperCase(Locale.ENGLISH);
				if (!val.isEmpty())
					return val;
			}
		}
		return null;
	}

	// Java 8 POJO
	public static class Parsed {
		private final String rossPo;
		private final String rossStyle;
		private final String itemDescription;
		private final String color;
		private final String rossSkuNumber;
		private final String quantity;
		private final String netWeightKg;
		private final String grossWeightKg;
		private final String measurement;
		private final String consignedTo;
		private final String deliverTo;
		private final String countryOfOrigin;
		private final String cartonNo;

		public Parsed(String rossPo, String rossStyle, String itemDescription, String color, String rossSkuNumber,
				String quantity, String netWeightKg, String grossWeightKg, String measurement, String consignedTo,
				String deliverTo, String countryOfOrigin, String cartonNo) {
			this.rossPo = rossPo;
			this.rossStyle = rossStyle;
			this.itemDescription = itemDescription;
			this.color = color;
			this.rossSkuNumber = rossSkuNumber;
			this.quantity = quantity;
			this.netWeightKg = netWeightKg;
			this.grossWeightKg = grossWeightKg;
			this.measurement = measurement;
			this.consignedTo = consignedTo;
			this.deliverTo = deliverTo;
			this.countryOfOrigin = countryOfOrigin;
			this.cartonNo = cartonNo;
		}

		public String getRossPo() {
			return rossPo;
		}

		public String getRossStyle() {
			return rossStyle;
		}

		public String getItemDescription() {
			return itemDescription;
		}

		public String getColor() {
			return color;
		}

		public String getRossSkuNumber() {
			return rossSkuNumber;
		}

		public String getQuantity() {
			return quantity;
		}

		public String getNetWeightKg() {
			return netWeightKg;
		}

		public String getGrossWeightKg() {
			return grossWeightKg;
		}

		public String getMeasurement() {
			return measurement;
		}

		public String getConsignedTo() {
			return consignedTo;
		}

		public String getDeliverTo() {
			return deliverTo;
		}

		public String getCountryOfOrigin() {
			return countryOfOrigin;
		}

		public String getCartonNo() {
			return cartonNo;
		}
	}
}