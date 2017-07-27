package com.lightrail.model.stripe;

import com.lightrail.helpers.*;

import java.util.*;
import java.util.Currency;

public class PaymentSummary {

    class PaymentSummaryLine {
        String title;
        int amount;
    }

    Map<String, PaymentSummaryLine> summaryLineItems = new HashMap<>();
    String currency;

    public PaymentSummary(String currency, int lightrailAmount, int creditCardAmount) {
        this.currency = currency;
        addLightrailAmount(lightrailAmount, "");
        addStripeAmount(creditCardAmount, "");

    }

    public PaymentSummary(String currency) {
        this.currency = currency;
    }

    public void addLightrailAmount(int lightrailAmount, String comment) {
        PaymentSummaryLine paymentSummaryLine = new PaymentSummaryLine();

        paymentSummaryLine.title = comment;
        paymentSummaryLine.amount = lightrailAmount;
        summaryLineItems.put(LightrailEcommerceConstants.PaymentSummary.LIGHTRAIL_SHARE, paymentSummaryLine);
    }

    public void addStripeAmount(int creditCardAmount, String comment) {
        PaymentSummaryLine paymentSummaryLine = new PaymentSummaryLine();

        paymentSummaryLine.title = comment;
        paymentSummaryLine.amount = creditCardAmount;
        summaryLineItems.put(LightrailEcommerceConstants.PaymentSummary.CREDIT_CARD_SHARE, paymentSummaryLine);
    }

    public int getLightrailAmount() {
        return summaryLineItems.get(LightrailEcommerceConstants.PaymentSummary.LIGHTRAIL_SHARE).amount;
    }

    public int getStripeAmount() {
        return summaryLineItems.get(LightrailEcommerceConstants.PaymentSummary.CREDIT_CARD_SHARE).amount;
    }

    public String toString() {
        StringBuffer orderSummaryOutputBuffer = new StringBuffer();
        for (String summaryItemKey : summaryLineItems.keySet()) {
            orderSummaryOutputBuffer.append(summaryLineItems.get(summaryItemKey).title).append("\t:")
                    .append(Currency.getInstance(currency).getSymbol())
                    .append(String.valueOf(summaryLineItems.get(summaryItemKey).amount)).append("\n");
        }
        return orderSummaryOutputBuffer.toString();
    }
}
