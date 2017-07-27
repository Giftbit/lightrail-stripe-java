package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.StripeConstants;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CheckoutWithStripeAndLightrail {
    private int orderTotal;
    private String orderCurrency;
    private String giftCode = null;
    private String giftCardId = null;
    private String lightrailCustomer = null;
    private String stripeToken = null;
    private String stripeCustomer = null;

    StripeLightrailHybridCharge stripeLightrailHybridChargeObject = null;

    public CheckoutWithStripeAndLightrail(int orderTotal, String orderCurrency) {
        setOrderTotal(orderTotal, orderCurrency);
    }

    public CheckoutWithStripeAndLightrail useGiftCode(String giftCode) {
        this.giftCode = giftCode;
        return this;
    }

    public CheckoutWithStripeAndLightrail useStripeToken(String stripeToken) {
        this.stripeToken = stripeToken;
        return this;
    }

    public CheckoutWithStripeAndLightrail useStripeCustomer(String stripeCustomer) {
        this.stripeCustomer = stripeCustomer;
        return this;
    }

    public CheckoutWithStripeAndLightrail useLightrailCustomer(String lightrailCustomerId) {
        this.lightrailCustomer = lightrailCustomerId;
        return this;
    }

    public CheckoutWithStripeAndLightrail useGiftCardId(String giftCardId) {
        this.giftCardId = giftCardId;
        return this;
    }

    public CheckoutWithStripeAndLightrail setOrderTotal(int orderTotal, String orderCurrency) {
        this.orderTotal = orderTotal;
        this.orderCurrency = orderCurrency;
        return this;
    }

    public boolean needsCreditCardPayment() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(getChargeParams());
        return paymentSummary.getStripeAmount() > 0;
    }

    public PaymentSummary getPaymentSummary() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        if (stripeLightrailHybridChargeObject != null) {
            return stripeLightrailHybridChargeObject.getPaymentSummary();
        } else {
            return StripeLightrailHybridCharge.simulate(getChargeParams());
        }
    }

    private Map<String, Object> getChargeParams() {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put(LightrailConstants.Parameters.AMOUNT, orderTotal);
        chargeParams.put(LightrailConstants.Parameters.CURRENCY, orderCurrency);
        if (giftCode != null) {
            chargeParams.put(LightrailConstants.Parameters.CODE, giftCode);
        }
        if (giftCardId != null) {
            chargeParams.put(LightrailConstants.Parameters.CARD_ID, giftCardId);
        }
        if (lightrailCustomer != null) {
            chargeParams.put(LightrailConstants.Parameters.CUSTOMER, lightrailCustomer);
        }
        if (stripeToken != null) {
            chargeParams.put(StripeConstants.Parameters.TOKEN, stripeToken);
        }
        if (stripeCustomer != null) {
            chargeParams.put(StripeConstants.Parameters.CUSTOMER, stripeCustomer);
        }

        return chargeParams;
    }

    public PaymentSummary checkout() throws ThirdPartyPaymentException, InsufficientValueException, CurrencyMismatchException, AuthorizationException, IOException, CouldNotFindObjectException {
        stripeLightrailHybridChargeObject = StripeLightrailHybridCharge.create(getChargeParams());
        return stripeLightrailHybridChargeObject.getPaymentSummary();
    }

    public String getOrderCurrency() {
        return orderCurrency;
    }

    public int getOrderTotal() {
        return orderTotal;
    }
}
