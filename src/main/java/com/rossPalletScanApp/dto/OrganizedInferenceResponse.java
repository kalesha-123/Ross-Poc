package com.rossPalletScanApp.dto;

public class OrganizedInferenceResponse {

    private String s3Key;
    private String presignedUrl;
    private RossLabelExtractResponse data;

    public OrganizedInferenceResponse() {}

    public OrganizedInferenceResponse(String s3Key, String presignedUrl, RossLabelExtractResponse data) {
        this.s3Key = s3Key;
        this.presignedUrl = presignedUrl;
        this.data = data;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public void setPresignedUrl(String presignedUrl) {
        this.presignedUrl = presignedUrl;
    }

    public RossLabelExtractResponse getData() {
        return data;
    }

    public void setData(RossLabelExtractResponse data) {
        this.data = data;
    }
}
