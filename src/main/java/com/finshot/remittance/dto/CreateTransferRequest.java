package com.finshot.remittance.dto;

public class CreateTransferRequest {

    private String customerId;
    private String sendCurrency;
    private String sendAmount;
    private String receiveCurrency;
    private String recipientName;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSendCurrency() {
        return sendCurrency;
    }

    public void setSendCurrency(String sendCurrency) {
        this.sendCurrency = sendCurrency;
    }

    public String getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(String sendAmount) {
        this.sendAmount = sendAmount;
    }

    public String getReceiveCurrency() {
        return receiveCurrency;
    }

    public void setReceiveCurrency(String receiveCurrency) {
        this.receiveCurrency = receiveCurrency;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
}