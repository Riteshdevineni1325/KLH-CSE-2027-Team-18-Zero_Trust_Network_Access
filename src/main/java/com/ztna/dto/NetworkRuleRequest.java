package com.ztna.dto;

public class NetworkRuleRequest {

    private String ipOrCidr;
    private String classification; // OFFICE | HOME | BLOCKED | UNKNOWN
    private String description;

    public NetworkRuleRequest() {}

    public NetworkRuleRequest(String ipOrCidr, String classification, String description) {
        this.ipOrCidr = ipOrCidr;
        this.classification = classification;
        this.description = description;
    }

    public String getIpOrCidr() { return ipOrCidr; }
    public void setIpOrCidr(String ipOrCidr) { this.ipOrCidr = ipOrCidr; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
