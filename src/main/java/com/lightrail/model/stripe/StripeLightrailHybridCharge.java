package com.lightrail.model.stripe;

import com.google.gson.Gson;
import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.LightrailEcommerceConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.api.objects.Transaction;
import com.lightrail.model.business.ContactHandler;
import com.lightrail.model.business.LightrailTransaction;
import com.lightrail.model.business.LightrailValue;
import com.stripe.model.Charge;
import com.stripe.net.RequestOptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StripeLightrailHybridCharge {

    LightrailCharge lightrailCharge = null;
    Charge stripeCharge = null;

    PaymentSummary paymentSummary = null;


    public LightrailCharge getLightrailCharge() {
        return lightrailCharge;
    }

    public Charge getStripeCharge() {
        return stripeCharge;
    }

    public PaymentSummary getPaymentSummary() {
        return paymentSummary;
    }

    StripeLightrailHybridCharge(LightrailCharge lightrailCharge, Charge stripeCharge, PaymentSummary paymentSummary) {
        this.lightrailCharge = lightrailCharge;
        this.stripeCharge = stripeCharge;
        this.paymentSummary = paymentSummary;
    }

    public String getIdempotencyKey() {
        return lightrailCharge.getIdempotencyKey();
    }

    private static Map<String, Object> getStripeParams(int amount, Map<String, Object> chargeParams) {
        Object stripeToken = chargeParams.get(StripeConstants.Parameters.TOKEN);
        Object stripeCustomerId = chargeParams.get(StripeConstants.Parameters.CUSTOMER);

        Map<String, Object> stripeParam = new HashMap<>();

        if (stripeToken != null) {
            stripeParam.put(StripeConstants.Parameters.TOKEN, stripeToken);
        } else if (stripeCustomerId != null) {
            stripeParam.put(StripeConstants.Parameters.CUSTOMER, stripeCustomerId);
        } else {
            throw new BadParameterException("Need credit card payment information to handle this order.");
        }
        stripeParam.put(StripeConstants.Parameters.AMOUNT, amount);
        stripeParam.put(StripeConstants.Parameters.CURRENCY, chargeParams.get(StripeConstants.Parameters.CURRENCY));
        return stripeParam;
    }

    private static int adjustForMinimumStripeTransactionValue(int transactionAmount, int currentLightrailShare) throws InsufficientValueException {
        int newLightrailShare = currentLightrailShare;
        int stripeShare = transactionAmount - currentLightrailShare;
        if (stripeShare > 0 && stripeShare < StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE) {
            int differential = StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE - stripeShare;
            newLightrailShare = currentLightrailShare - differential;
            if (newLightrailShare < 0)
                throw new InsufficientValueException("The balance of this gift card or account credit is too small for this transaction.");
        }

        return newLightrailShare;
    }

    private static int determineLightrailShare(Map<String, Object> chargeParams) throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        int transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);
        int lightrailShare = 0;
        Transaction lightrailCharge;
        try {
            lightrailCharge = retrieveLightrailCharge(chargeParams);
        } catch (BadParameterException e) {
            lightrailCharge = null;
        } catch (CouldNotFindObjectException e) {
            lightrailCharge = null;
        }

        if (lightrailCharge == null) { //it's not a replay
            Object giftCodeObject = chargeParams.get(LightrailConstants.Parameters.CODE);
            Object giftCardIdObject = chargeParams.get(LightrailConstants.Parameters.CARD_ID);
            if (giftCardIdObject == null && giftCodeObject == null) {
                return 0; //if no lightrail parameters are provided then lightrail's share will be zero
            }
            Map<String, Object> lightrailValueParams = new HashMap<>();
            lightrailValueParams.put(LightrailConstants.Parameters.CURRENCY, chargeParams.get(LightrailConstants.Parameters.CURRENCY));

            if (giftCodeObject != null) {
                lightrailValueParams.put(LightrailConstants.Parameters.CODE, giftCodeObject);
            } else if (giftCardIdObject != null) {
                lightrailValueParams.put(LightrailConstants.Parameters.CARD_ID, giftCardIdObject);
            }
            int lightrailValue = LightrailValue.retrieve(lightrailValueParams).getCurrentValue();
            if (lightrailValue == 0)
                return 0;
            lightrailShare = Math.min(transactionAmount, lightrailValue);
            lightrailShare = adjustForMinimumStripeTransactionValue(transactionAmount, lightrailShare);

        } else { //it's a replay
            lightrailShare = 0 - lightrailCharge.getValue();
            Integer originaltransactionAmount = ((Double) lightrailCharge.getMetadata().get(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL)).intValue();
            if (transactionAmount != originaltransactionAmount)
                throw new BadParameterException("Idempotency Error. The parameters do not match the original transaction.");
        }
        return lightrailShare;
    }

    public static PaymentSummary simulate(Map<String, Object> chargeParams) throws AuthorizationException, CurrencyMismatchException, InsufficientValueException, IOException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), chargeParams);

        chargeParams = ContactHandler.handleContact(chargeParams);

        int transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);

        int lightrailShare = determineLightrailShare(chargeParams);
        int stripeShare = transactionAmount - lightrailShare;
        return new PaymentSummary((String) chargeParams.get(LightrailConstants.Parameters.CURRENCY),
                lightrailShare,
                stripeShare);
    }

    //todo: this needs to be improved to return a LightrailCharge
    private static Transaction retrieveLightrailCharge(Map<String, Object> chargeParams) throws AuthorizationException, IOException, CouldNotFindObjectException {
        return LightrailTransaction.retrieve(chargeParams);
    }

    public static StripeLightrailHybridCharge create(Map<String, Object> chargeParams) throws InsufficientValueException, AuthorizationException, CurrencyMismatchException, IOException, ThirdPartyPaymentException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), chargeParams);

        chargeParams = ContactHandler.handleContact(chargeParams);

        String idempotencyKey = (String) chargeParams.get(LightrailConstants.Parameters.USER_SUPPLIED_ID);
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
            chargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, idempotencyKey);
        }

        LightrailCharge lightrailCharge = null;
        LightrailTransaction lightrailCaptureTransaction = null;
        Charge stripeCharge = null;

        int transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);
        String transactionCurrency = (String) chargeParams.get(LightrailConstants.Parameters.CURRENCY);

        int lightrailShare = determineLightrailShare(chargeParams);
        int creditCardShare = transactionAmount - lightrailShare;

        if (lightrailShare != 0) {
            Map<String, Object> lightrailChargeParams = new HashMap<>();
            String code = (String) chargeParams.get(LightrailConstants.Parameters.CODE);
            String cardId = (String) chargeParams.get(LightrailConstants.Parameters.CARD_ID);
            if (code != null) {
                lightrailChargeParams.put(LightrailConstants.Parameters.CODE, code);
            } else if (cardId != null) {
                lightrailChargeParams.put(LightrailConstants.Parameters.CARD_ID, cardId);
            }
            lightrailChargeParams.put(StripeConstants.Parameters.AMOUNT, lightrailShare);
            lightrailChargeParams.put(LightrailConstants.Parameters.CURRENCY, transactionCurrency);
            lightrailChargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, idempotencyKey);

            Map<String, Object> lightrailChargeMetadata = new HashMap<>();
            lightrailChargeMetadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL, transactionAmount);
            lightrailChargeMetadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER, "STRIPE");
            lightrailChargeParams.put(LightrailConstants.Parameters.METADATA, lightrailChargeMetadata);

            if (creditCardShare == 0) { //everything on giftcode
                lightrailCharge = LightrailCharge.create(lightrailChargeParams);
            } else { //split between giftcode and credit card
                lightrailChargeParams.put(StripeConstants.Parameters.CAPTURE, false);
                lightrailCharge = LightrailCharge.create(lightrailChargeParams);
                try {
                    RequestOptions stripeRequestOptions = RequestOptions.builder()
                            .setIdempotencyKey(idempotencyKey)
                            .build();
                    stripeCharge = Charge.create(getStripeParams(creditCardShare, chargeParams), stripeRequestOptions);
                } catch (Exception e) {
                    lightrailCharge.doVoid();
                    throw new ThirdPartyPaymentException(e);
                }
                lightrailCaptureTransaction = lightrailCharge.capture();
            }
        } else { //all on credit card
            try {
                stripeCharge = Charge.create(getStripeParams(creditCardShare, chargeParams));
            } catch (Exception e) {
                throw new ThirdPartyPaymentException(e);
            }
        }

        //PaymentSummary paymentSummary = new PaymentSummary(transactionCurrency, lightrailShare, creditCardShare);
        PaymentSummary paymentSummary = new PaymentSummary(transactionCurrency)
                .addLightrailAmount(lightrailShare, getMetadata(lightrailShare, lightrailCaptureTransaction))
                .addStripeAmount(creditCardShare, getMetadata(creditCardShare, stripeCharge));
        return new StripeLightrailHybridCharge(lightrailCharge, stripeCharge, paymentSummary);
    }

    static Map<String, Object> getMetadata (int amount, Object charge) {
        if (amount > 0) {
            String jsonString = new Gson().toJson(charge);
            return (Map<String, Object>) new Gson().fromJson(jsonString, Map.class);
        } else {
            return null;
        }
    }

    public String getLightrailTransactionId() {
        String lightrailTransactionId = null;
        if (lightrailCharge != null)
            lightrailTransactionId = lightrailCharge.getId();
        return lightrailTransactionId;
    }

    public String getStripeTransactionId() {
        String stripeTransactionId = null;
        if (stripeCharge != null)
            stripeTransactionId = stripeCharge.getId();
        return stripeTransactionId;
    }
}
