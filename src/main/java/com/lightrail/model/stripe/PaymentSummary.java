package com.lightrail.model.stripe;

import com.lightrail.helpers.*;

import java.util.*;
import java.util.Currency;

public class PaymentSummary {

    public class PaymentSummaryLine {
        //String title;
        int amount;
        Map<String , Object> metadata;

        public int getAmount() {
            return amount;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    Map<String, PaymentSummaryLine> summaryLineItems = new HashMap<>();
    String currency;

    public PaymentSummary(String currency, int lightrailAmount, int creditCardAmount) {
        this.currency = currency;
        addLightrailAmount(lightrailAmount, null);
        addStripeAmount(creditCardAmount, null);

    }

    public PaymentSummary(String currency) {
        this.currency = currency;
    }

    public PaymentSummary addLightrailAmount(int lightrailAmount, Map<String , Object> metadata) {
        PaymentSummaryLine paymentSummaryLine = new PaymentSummaryLine();

        paymentSummaryLine.metadata = metadata;
        paymentSummaryLine.amount = lightrailAmount;
        summaryLineItems.put(LightrailEcommerceConstants.PaymentSummary.LIGHTRAIL_SHARE, paymentSummaryLine);
        return this;
    }

    public PaymentSummary addStripeAmount(int stripeAmount, Map<String , Object> metadata) {
        PaymentSummaryLine paymentSummaryLine = new PaymentSummaryLine();

        paymentSummaryLine.metadata = metadata;
        paymentSummaryLine.amount = stripeAmount;
        summaryLineItems.put(LightrailEcommerceConstants.PaymentSummary.CREDIT_CARD_SHARE, paymentSummaryLine);
        return this;
    }

    public PaymentSummaryLine getLightrailPayment() {
        return summaryLineItems.get(LightrailEcommerceConstants.PaymentSummary.LIGHTRAIL_SHARE);
    }

    public PaymentSummaryLine getStripePayment() {
        return summaryLineItems.get(LightrailEcommerceConstants.PaymentSummary.CREDIT_CARD_SHARE);
    }

    public String toHtml(String elementId) {
        StringBuffer orderSummaryOutputBuffer = new StringBuffer();
        orderSummaryOutputBuffer.append(String.format("<table id='%s'>", elementId));
        for (String summaryItemKey : summaryLineItems.keySet()) {
            orderSummaryOutputBuffer.append("<tr>");
            orderSummaryOutputBuffer.append("<td>").append(summaryItemKey).append("</td>");

            orderSummaryOutputBuffer.append("<td>")
//            orderSummaryOutputBuffer.append(Currency.getInstance(currency).getSymbol())
                    .append(String.valueOf(summaryLineItems.get(summaryItemKey).amount));
            orderSummaryOutputBuffer.append("</td>");

            orderSummaryOutputBuffer.append("<td>");
            Map <String, Object> metadata =  summaryLineItems.get(summaryItemKey).metadata;
            if (summaryLineItems.get(summaryItemKey).metadata != null) {
                for (String key: metadata.keySet()) {
                    orderSummaryOutputBuffer.append("<p>").append(key).append(":");
                    orderSummaryOutputBuffer.append(metadata.get(key)).append("</p>");
                }
            }
            orderSummaryOutputBuffer.append("</td>");

            orderSummaryOutputBuffer.append("</tr>");
        }
        orderSummaryOutputBuffer.append("</table>");
        return orderSummaryOutputBuffer.toString();
    }

//    public String toString() {
//        StringBuffer orderSummaryOutputBuffer = new StringBuffer();
//        for (String summaryItemKey : summaryLineItems.keySet()) {
//            orderSummaryOutputBuffer.append(summaryLineItems.get(summaryItemKey).title).append("\t:")
//                    .append(Currency.getInstance(currency).getSymbol())
//                    .append(String.valueOf(summaryLineItems.get(summaryItemKey).amount)).append("\n");
//        }
//        return orderSummaryOutputBuffer.toString();
//    }
}
