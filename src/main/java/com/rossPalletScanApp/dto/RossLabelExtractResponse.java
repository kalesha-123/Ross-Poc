package com.rossPalletScanApp.dto;

public class RossLabelExtractResponse {

    private String status;          // SUCCESS / ERROR
    private String message;         // human-readable message
    private Integer ocrConfidence;  // 0..100
    private String imageFilename;

    // Parsed fields
    private String rossPo;          // ROSSPO#
    private String rossStyle;       // ROSS STYLE...
    private String itemDescription; // ITEM DESCRIPTION
    private String color;           // COLOR
    private String rossSkuNumber;   // ROSS SKUNUMBER
    private String quantity;        // QUANTITY (keep full text like "4 SET/CARTON")
    private String netWeightKg;     // N.W (in KG)
    private String grossWeightKg;   // G.W (in KG)
    private String measurement;     // MEASUREMENT
    private String consignedTo;     // CONSIGNED TO
    private String deliverTo;       // DELIVER TO (name line)
    private String deliverToAddress;// following address line(s) if present
    private String countryOfOrigin; // COUNTRY OF ORIGIN
    private String cartonNo;        // CARTON NO

    public RossLabelExtractResponse() {}

    public static RossLabelExtractResponse of(String status, String message, Integer ocrConfidence,
                                              String imageFilename, String rawText,
                                              String rossPo, String rossStyle, String itemDescription, String color,
                                              String rossSkuNumber, String quantity, String netWeightKg, String grossWeightKg,
                                              String measurement, String consignedTo, String deliverTo, String deliverToAddress,
                                              String countryOfOrigin, String cartonNo) {
        RossLabelExtractResponse r = new RossLabelExtractResponse();
        r.status = status;
        r.message = message;
        r.ocrConfidence = ocrConfidence;
        r.imageFilename = imageFilename;
        r.rossPo = rossPo;
        r.rossStyle = rossStyle;
        r.itemDescription = itemDescription;
        r.color = color;
        r.rossSkuNumber = rossSkuNumber;
        r.quantity = quantity;
        r.netWeightKg = netWeightKg;
        r.grossWeightKg = grossWeightKg;
        r.measurement = measurement;
        r.consignedTo = consignedTo;
        r.deliverTo = deliverTo;
        r.deliverToAddress = deliverToAddress;
        r.countryOfOrigin = countryOfOrigin;
        r.cartonNo = cartonNo;
        return r;
    }

    // getters (Java 8)
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Integer getOcrConfidence() { return ocrConfidence; }
    public String getImageFilename() { return imageFilename; }
    public String getRossPo() { return rossPo; }
    public String getRossStyle() { return rossStyle; }
    public String getItemDescription() { return itemDescription; }
    public String getColor() { return color; }
    public String getRossSkuNumber() { return rossSkuNumber; }
    public String getQuantity() { return quantity; }
    public String getNetWeightKg() { return netWeightKg; }
    public String getGrossWeightKg() { return grossWeightKg; }
    public String getMeasurement() { return measurement; }
    public String getConsignedTo() { return consignedTo; }
    public String getDeliverTo() { return deliverTo; }
    public String getDeliverToAddress() { return deliverToAddress; }
    public String getCountryOfOrigin() { return countryOfOrigin; }
    public String getCartonNo() { return cartonNo; }
}