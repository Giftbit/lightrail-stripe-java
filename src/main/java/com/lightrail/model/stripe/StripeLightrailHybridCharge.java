package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.LightrailEcommerceConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.business.LightrailCharge;
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

    public StripeLightrailHybridCharge(LightrailCharge lightrailCharge, Charge stripeCharge, PaymentSummary paymentSummary) {
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

    private static int adjustForMinimumStripeTransactionValue(int transactionAmount, int currentGiftCodeShare) throws InsufficientValueException {
        int newGiftCodeShare = currentGiftCodeShare;
        int stripeShare = transactionAmount - currentGiftCodeShare;
        if (stripeShare > 0 && stripeShare < StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE) {
            int differential = StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE - stripeShare;
            newGiftCodeShare = currentGiftCodeShare - differential;
            if (newGiftCodeShare < 0)
                throw new InsufficientValueException("The balance of this gift card or account credit is too small for this transaction.");
        }

        return newGiftCodeShare;
    }

    private static int determineLightrailShare(Map<String, Object> chargeParams) throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        int transactionAmount = (Integer) chargeParams.get(LightrailConstants.Parameters.AMOUNT);
        int lightrailShare = 0;
        LightrailCharge lightrailCharge = null;
        try {
            lightrailCharge = retrieveGiftCharge(chargeParams);
        } catch (BadParameterException e) {
        } catch (CouldNotFindObjectException e) {
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
            lightrailShare = lightrailCharge.getAmount();
            Integer originaltransactionAmount = ((Double) lightrailCharge.getMetadata().get(LightrailEcommerceConstants.HYBRID_TRANSACTION_TOTAL_METADATA_KEY)).intValue();
            if (transactionAmount != originaltransactionAmount)
                throw new BadParameterException("Idempotency Error. The parameters do not match the original transaction.");
        }
        return lightrailShare;
    }

    public static PaymentSummary simulate(Map<String, Object> chargeParams) throws AuthorizationException, CurrencyMismatchException, InsufficientValueException, IOException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                LightrailConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), chargeParams);

        chargeParams = LightrailTransaction.handleCustomer(chargeParams);

        int transactionAmount = (Integer) chargeParams.get(LightrailConstants.Parameters.AMOUNT);

        int giftCodeShare = determineLightrailShare(chargeParams);
        int creditCardShare = transactionAmount - giftCodeShare;
        return new PaymentSummary((String) chargeParams.get(LightrailConstants.Parameters.CURRENCY),
                giftCodeShare,
                creditCardShare);
    }

    private static LightrailCharge retrieveGiftCharge(Map<String, Object> chargeParams) throws AuthorizationException, IOException, CouldNotFindObjectException {
        return LightrailCharge.retrieve(chargeParams);
    }

    public static StripeLightrailHybridCharge create(Map<String, Object> chargeParams) throws InsufficientValueException, AuthorizationException, CurrencyMismatchException, IOException, ThirdPartyPaymentException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                LightrailConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), chargeParams);

        chargeParams = LightrailTransaction.handleCustomer(chargeParams);

        String idempotencyKey = (String) chargeParams.get(LightrailConstants.Parameters.USER_SUPPLIED_ID);
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
            chargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, idempotencyKey);
        }

        LightrailCharge giftCharge = null;
        Charge stripeCharge = null;

        int transactionAmount = (Integer) chargeParams.get(LightrailConstants.Parameters.AMOUNT);
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
            lightrailChargeParams.put(LightrailConstants.Parameters.AMOUNT, lightrailShare);
            lightrailChargeParams.put(LightrailConstants.Parameters.CURRENCY, transactionCurrency);
            lightrailChargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, idempotencyKey);

            Map<String, Object> giftChargeMetadata = new HashMap<>();
            giftChargeMetadata.put(LightrailEcommerceConstants.HYBRID_TRANSACTION_TOTAL_METADATA_KEY, transactionAmount);
            lightrailChargeParams.put(LightrailConstants.Parameters.METADATA, giftChargeMetadata);

            if (creditCardShare == 0) { //everything on giftcode
                giftCharge = LightrailCharge.create(lightrailChargeParams);
            } else { //split between giftcode and credit card
                lightrailChargeParams.put(LightrailConstants.Parameters.CAPTURE, false);
                giftCharge = LightrailCharge.create(lightrailChargeParams);
                try {
                    RequestOptions stripeRequestOptions = RequestOptions.builder()
                            .setIdempotencyKey(idempotencyKey)
                            .build();
                    stripeCharge = Charge.create(getStripeParams(creditCardShare, chargeParams), stripeRequestOptions);
                } catch (Exception e) {
                    giftCharge.cancel();
                    throw new ThirdPartyPaymentException(e);
                }
                giftCharge.capture();
            }
        } else { //all on credit card
            try {
                stripeCharge = Charge.create(getStripeParams(creditCardShare, chargeParams));
            } catch (Exception e) {
                throw new ThirdPartyPaymentException(e);
            }
        }
        PaymentSummary paymentSummary = new PaymentSummary(transactionCurrency, lightrailShare, creditCardShare);
        return new StripeLightrailHybridCharge(giftCharge, stripeCharge, paymentSummary);
    }

    public String getGiftTransactionId() {
        String giftCodeTransactionId = null;
        if (lightrailCharge != null)
            giftCodeTransactionId = lightrailCharge.getTransactionId();
        return giftCodeTransactionId;
    }

    public String getStripeTransactionId() {
        String stripeTransactionId = null;
        if (stripeCharge != null)
            stripeTransactionId = stripeCharge.getId();
        return stripeTransactionId;
    }
}
