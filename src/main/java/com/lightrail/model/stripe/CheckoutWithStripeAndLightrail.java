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
    private String cardId = null;
    private String lightrailShopperId = null;
    private String lightrailContact = null;
    private String stripeToken = null;
    private String stripeCustomer = null;

    StripeLightrailSplitTenderCharge stripeLightrailSplitTenderChargeObject = null;

    public CheckoutWithStripeAndLightrail(int orderTotal, String orderCurrency) {
        setOrderTotal(orderTotal, orderCurrency);
    }

    public CheckoutWithStripeAndLightrail useLightrailGiftCode(String giftCode) {
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

    public CheckoutWithStripeAndLightrail useLightrailContact(String lightrailCustomerId) {
        this.lightrailContact = lightrailCustomerId;
        return this;
    }

    public CheckoutWithStripeAndLightrail useLightrailShopperId(String lightrailShopperId) {
        this.lightrailShopperId = lightrailShopperId;
        return this;
    }

    public CheckoutWithStripeAndLightrail useLightrailCardId(String giftCardId) {
        this.cardId = giftCardId;
        return this;
    }

    public CheckoutWithStripeAndLightrail setOrderTotal(int orderTotal, String orderCurrency) {
        this.orderTotal = orderTotal;
        this.orderCurrency = orderCurrency;
        return this;
    }

    public boolean needsCreditCardPayment() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        return (StripeLightrailSplitTenderCharge.simulate(getChargeParams()).getStripeCharge() != null);

    }

    public SimulatedStripeLightrailSplitTenderCharge simulate() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, ThirdPartyException {
        return StripeLightrailSplitTenderCharge.simulate(getChargeParams());
    }

    public StripeLightrailSplitTenderCharge checkout() throws IOException, AuthorizationException, ThirdPartyException, CurrencyMismatchException, CouldNotFindObjectException, InsufficientValueException {
        SimulatedStripeLightrailSplitTenderCharge simulatedTx = simulate();
        return simulatedTx.commit();
    }

    private Map<String, Object> getChargeParams() {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put(StripeConstants.Parameters.AMOUNT, orderTotal);
        chargeParams.put(LightrailConstants.Parameters.CURRENCY, orderCurrency);
        if (giftCode != null) {
            chargeParams.put(LightrailConstants.Parameters.CODE, giftCode);
        }
        if (cardId != null) {
            chargeParams.put(LightrailConstants.Parameters.CARD_ID, cardId);
        }
        if (lightrailContact != null) {
            chargeParams.put(LightrailConstants.Parameters.CONTACT, lightrailContact);
        }
        if (lightrailShopperId != null) {
            chargeParams.put(LightrailConstants.Parameters.SHOPPER_ID, lightrailShopperId);
        }
        if (stripeToken != null) {
            chargeParams.put(StripeConstants.Parameters.TOKEN, stripeToken);
        }
        if (stripeCustomer != null) {
            chargeParams.put(StripeConstants.Parameters.CUSTOMER, stripeCustomer);
        }

        return chargeParams;
    }

    public String getOrderCurrency() {
        return orderCurrency;
    }

    public int getOrderTotal() {
        return orderTotal;
    }
}
