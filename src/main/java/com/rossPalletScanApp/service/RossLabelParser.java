package com.rossPalletScanApp.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
@Service
/** Parses ROSS carton label text into structured fields. Java 8 compatible. */
public class RossLabelParser {

	// Accept both ':' and '：' (full-width)
	private static final String SEP = "\\s*[：:]\\s*";


	// Accept both ':' and '：' already normalized to ':', hash already normalized to '#'
	private static final Pattern PO_PATTERN = Pattern.compile(
	    "ROSSPO#\\s*:\\s*([A-Z0-9\\-_/\\. ]+)",
	    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
	);


//	// Patterns for single-line fields
//	private static final Pattern PO_PATTERN = Pattern.compile(
//			"(?:\\bROSS\\s*PO\\s*#\\b|\\bROSSPO#\\b)" + SEP + "([A-Za-z0-9\\-_/\\.]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STYLE_PATTERN = Pattern.compile("\\bROSS\\s*STYLE\\w*\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ITEM_DESC_PATTERN = Pattern.compile("\\bITEM\\s*DESCRIPTION\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern COLOR_PATTERN = Pattern.compile("\\bCOLOR\\b" + SEP + "([A-Za-z0-9 \\-/&()]+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ROSS_SKU_PATTERN = Pattern.compile("\\bROSS\\s*SKU\\s*NUMBER\\b" + SEP
			+ "([A-Za-z0-9\\-_/\\.]+)|\\bROSS\\s*SKUNUMBER\\b" + SEP + "([A-Za-z0-9\\-_/\\.]+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern QTY_PATTERN = Pattern.compile("\\bQUANTITY\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NW_PATTERN = Pattern
			.compile("\\bN\\.?\\s*W\\.?\\b" + SEP + "([0-9]+(?:\\.[0-9]+)?)\\s*KGS?", Pattern.CASE_INSENSITIVE);

	private static final Pattern GW_PATTERN = Pattern
			.compile("\\bG\\.?\\s*W\\.?\\b" + SEP + "([0-9]+(?:\\.[0-9]+)?)\\s*KGS?", Pattern.CASE_INSENSITIVE);

	private static final Pattern MEAS_PATTERN = Pattern.compile("\\bMEASUREMENT\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern CONSIGNED_TO_PATTERN = Pattern.compile("\\bCONSIGNED\\s*TO\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DELIVER_TO_PATTERN = Pattern.compile("\\bDELIVER\\s*TO\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern ORIGIN_PATTERN = Pattern.compile("\\bCOUNTRY\\s*OF\\s*ORIGIN\\b" + SEP + "(.+)",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern CARTON_NO_PATTERN = Pattern.compile("\\bCARTON\\s*NO\\b" + SEP + "(.*)",
			Pattern.CASE_INSENSITIVE);

	public Parsed parse(String rawText) {
		if (rawText == null) {
			throw new IllegalArgumentException("OCR text is null");
		}

		// Normalize line endings and also keep lines array for multi-line lookahead
		String norm = rawText.replace("\r\n", "\n").replace('\r', '\n');
		List<String> lines = Arrays.asList(norm.split("\n"));

		String rossPo = findOne(norm, PO_PATTERN, 1);
		String rossStyle = findOne(norm, STYLE_PATTERN, 1);
		String itemDesc = findOne(norm, ITEM_DESC_PATTERN, 1);
		String color = trimToNull(findOne(norm, COLOR_PATTERN, 1));

		// ROSS SKU NUMBER (supports "ROSS SKU NUMBER:" or "ROSS SKUNUMBER:")
		Matcher skuMatcher = ROSS_SKU_PATTERN.matcher(norm);
		String rossSku = null;
		if (skuMatcher.find()) {
			// Only one of group(1) or group(2) will be populated
			String g1 = skuMatcher.group(1);
			String g2 = skuMatcher.group(2);
			if (g1 != null && !g1.trim().isEmpty()) {
				rossSku = g1.trim();
			} else if (g2 != null && !g2.trim().isEmpty()) {
				rossSku = g2.trim();
			}
		}

		String qty = trimToNull(findOne(norm, QTY_PATTERN, 1));
		String nwKg = trimToNull(findOne(norm, NW_PATTERN, 1));
		String gwKg = trimToNull(findOne(norm, GW_PATTERN, 1));
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

		return new Parsed(rossPo, rossStyle, itemDesc, color, rossSku, qty, nwKg, gwKg, meas, consignedTo, deliverTo,
				deliverToAddress, origin, cartonNo);
	}

	// --- helpers ---

	private static String findOne(String text, Pattern p, int group) {
		Matcher m = p.matcher(text);
		return m.find() ? trimToNull(m.group(group)) : null;
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
		String u = s.toUpperCase(Locale.ENGLISH);
		return u.contains("ROSSPO") || u.contains("ROSS PO") || u.contains("ROSS STYLE")
				|| u.contains("ITEM DESCRIPTION") || u.startsWith("COLOR") || u.contains("ROSS SKU")
				|| u.contains("QUANTITY") || u.startsWith("N.W") || u.startsWith("NW") || u.startsWith("G.W")
				|| u.startsWith("GW") || u.contains("MEASUREMENT") || u.contains("CONSIGNED TO")
				|| u.contains("DELIVER TO") || u.contains("COUNTRY OF ORIGIN") || u.contains("CARTON NO");
	}

	private static String trimToNull(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static String firstNonNull(String a, String b) {
		return a != null ? a : b;
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
		private final String deliverToAddress;
		private final String countryOfOrigin;
		private final String cartonNo;

		public Parsed(String rossPo, String rossStyle, String itemDescription, String color, String rossSkuNumber,
				String quantity, String netWeightKg, String grossWeightKg, String measurement, String consignedTo,
				String deliverTo, String deliverToAddress, String countryOfOrigin, String cartonNo) {
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
			this.deliverToAddress = deliverToAddress;
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

		public String getDeliverToAddress() {
			return deliverToAddress;
		}

		public String getCountryOfOrigin() {
			return countryOfOrigin;
		}

		public String getCartonNo() {
			return cartonNo;
		}
	}
}