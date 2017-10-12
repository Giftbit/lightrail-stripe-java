package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.LightrailEcommerceConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.api.objects.Metadata;
import com.lightrail.model.api.objects.RequestParameters;
import com.lightrail.model.business.LightrailContact;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import com.stripe.net.RequestOptions;

import java.io.IOException;
import java.util.*;

public class StripeLightrailSplitTenderCharge {

    LightrailCharge lightrailCharge = null;
    Charge stripeCharge = null;


    public LightrailCharge getLightrailCharge() {
        return lightrailCharge;
    }

    public Charge getStripeCharge() {
        return stripeCharge;
    }


    StripeLightrailSplitTenderCharge(LightrailCharge lightrailCharge, Charge stripeCharge) {
        this.lightrailCharge = lightrailCharge;
        this.stripeCharge = stripeCharge;
    }

    public String getIdempotencyKey() {
        return (lightrailCharge != null) ? lightrailCharge.getIdempotencyKey() : null;
    }

    private static Map<String, Object> getStripeParams(int amount, Map<String, Object> chargeParams, String lightrailTxFullId) {
        Object stripeToken = chargeParams.get(StripeConstants.Parameters.TOKEN);
        Object stripeCustomerId = chargeParams.get(StripeConstants.Parameters.CUSTOMER);
        Integer splitTenderTotal = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);

        Map<String, Object> stripeParams = new HashMap<>();

        if (stripeToken != null) {
            stripeParams.put(StripeConstants.Parameters.TOKEN, stripeToken);
        } else if (stripeCustomerId != null) {
            stripeParams.put(StripeConstants.Parameters.CUSTOMER, stripeCustomerId);
        } else {
            throw new BadParameterException("Need credit card payment information to handle this order.");
        }
        stripeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        stripeParams.put(StripeConstants.Parameters.CURRENCY, chargeParams.get(StripeConstants.Parameters.CURRENCY));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL, splitTenderTotal);
        metadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER, "LIGHTRAIL");
        if (lightrailTxFullId != null)
            metadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER_TRANSACTION_ID, lightrailTxFullId);

        stripeParams.put(StripeConstants.Parameters.METADATA, metadata);

        return stripeParams;
    }

    private static int adjustForMinimumStripeTransactionValue(int transactionAmount, int currentLightrailShare) throws InsufficientValueException {
        int newLightrailShare = currentLightrailShare;
        int stripeShare = transactionAmount - currentLightrailShare;
        if (stripeShare > 0 && stripeShare < StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE) {
            int differential = StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE - stripeShare;
            newLightrailShare = currentLightrailShare - differential;
            if (newLightrailShare < 0)
                throw new InsufficientValueException("The balance of this Lightrail Card is too small for this transaction.");
        }

        return newLightrailShare;
    }

    private static void removeStripeParams(Map<String, Object> params) {
        params.remove(StripeConstants.Parameters.CUSTOMER);
        params.remove(StripeConstants.Parameters.TOKEN);
    }

    public static StripeLightrailSplitTenderCharge retrieve(Map<String, Object> chargeParams) throws AuthorizationException, CurrencyMismatchException, InsufficientValueException, IOException, CouldNotFindObjectException, ThirdPartyException {
        Map<String, Object> lightrailRetrieveParams = new HashMap<>(chargeParams);
        removeStripeParams(lightrailRetrieveParams);
        LightrailCharge lightrailCharge = LightrailCharge.retrieve(lightrailRetrieveParams);
        int originalTransactionAmount = ((Double) lightrailCharge.getMetadata().get(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL)).intValue();
        int transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);
        if (transactionAmount != originalTransactionAmount)
            throw new BadParameterException("Idempotency Error. The parameters do not match the original transaction.");

        String stripeTxId = ((String) lightrailCharge.getMetadata().get(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER_TRANSACTION_ID));

        Charge stripeTransaction = null;
        if (originalTransactionAmount - lightrailCharge.getAmount() != 0) { //then there was no stripe
            try {
                stripeTransaction = Charge.retrieve(stripeTxId);
            } catch (StripeException e) {
                throw new ThirdPartyException(e);
            }
        }
        return new StripeLightrailSplitTenderCharge(lightrailCharge, stripeTransaction);
    }

    public static SimulatedStripeLightrailSplitTenderCharge simulate(Map<String, Object> chargeParams) throws AuthorizationException, CurrencyMismatchException, InsufficientValueException, IOException, CouldNotFindObjectException, ThirdPartyException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), chargeParams);

        RequestParameters requestParameters = new RequestParameters();
        requestParameters.putAll(chargeParams);
        chargeParams = LightrailContact.handleContact(requestParameters);
        Integer transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);
        String currency = (String) chargeParams.get(LightrailConstants.Parameters.CURRENCY);

        LightrailCharge lightrailCharge = null;
        Charge stripeCharge = null;
        Integer lightrailShare = 0;
        Integer stripeShare = 0;

        Object lightrailCodeObject = chargeParams.get(LightrailConstants.Parameters.CODE);
        Object lightrailCardIdObject = chargeParams.get(LightrailConstants.Parameters.CARD_ID);
        String idempotencyKey = (String) chargeParams.get(LightrailConstants.Parameters.USER_SUPPLIED_ID);
        if (idempotencyKey == null) {
            chargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, UUID.randomUUID().toString());
        }

        if (lightrailCardIdObject != null || lightrailCodeObject != null) {
            RequestParameters simulateParameters = new RequestParameters();
            simulateParameters.putAll(chargeParams);
            removeStripeParams(simulateParameters);
            lightrailCharge = LightrailCharge.simulate(simulateParameters);
            lightrailShare = lightrailCharge.getAmount();

            if (lightrailCharge.getId() != null) { //this means this is not a simulation; make sure the amount is the same. todo: check more request params
                int originalTransactionAmount = ((Double) lightrailCharge.getMetadata().get(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL)).intValue();
                if (transactionAmount != originalTransactionAmount)
                    throw new BadParameterException("Idempotency Error. The parameters do not match the original transaction.");

            } else {
                lightrailShare = adjustForMinimumStripeTransactionValue(transactionAmount, lightrailShare);
            }
            lightrailCharge.setAmount(lightrailShare);
            if (lightrailShare == 0)
                lightrailCharge = null;
        }
        stripeShare = transactionAmount - lightrailShare;
        if (stripeShare != 0) {
            //getStripeParams(stripeShare, chargeParams, null);

            stripeCharge = new Charge();
            stripeCharge.setCurrency(currency);
            stripeCharge.setAmount(stripeShare.longValue());
        }

        SimulatedStripeLightrailSplitTenderCharge splitTenderCharge = new SimulatedStripeLightrailSplitTenderCharge(lightrailCharge, stripeCharge, chargeParams);
        return splitTenderCharge;
    }

    private static Metadata getLightrailMetadata(int transactionAmount, String stripeTxId) {
        Metadata lightrailChargeMetadata = new Metadata();
        lightrailChargeMetadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL, transactionAmount);
        lightrailChargeMetadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER, "STRIPE");
        if (stripeTxId != null)
            lightrailChargeMetadata.put(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_PARTNER_TRANSACTION_ID, stripeTxId);
        return lightrailChargeMetadata;
    }

    public static StripeLightrailSplitTenderCharge create(Map<String, Object> chargeParams, int stripeShare, int lightrailShare) throws ThirdPartyException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException, CurrencyMismatchException, ThirdPartyException {
        int transactionAmount = (Integer) chargeParams.get(StripeConstants.Parameters.AMOUNT);
        if (transactionAmount != stripeShare + lightrailShare)
            throw new BadParameterException("Transaction amount does not match the sum of the given Stripe and Lightrail shares.");

        String idempotencyKey = (String) chargeParams.get(LightrailConstants.Parameters.USER_SUPPLIED_ID);
        if (idempotencyKey == null) {
            idempotencyKey = (String) chargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, UUID.randomUUID().toString());
        }

        LightrailCharge lightrailCapturedCharge = null;
        Charge stripeCharge = null;

        if (lightrailShare != 0) {
            Map<String, Object> lightrailChargeParams = new HashMap<>(chargeParams);
            removeStripeParams(lightrailChargeParams);
            lightrailChargeParams.put(StripeConstants.Parameters.AMOUNT, lightrailShare);

            lightrailChargeParams.put(LightrailConstants.Parameters.METADATA, getLightrailMetadata(transactionAmount, null));

            if (stripeShare == 0) { //everything on card
                lightrailCapturedCharge = LightrailCharge.create(lightrailChargeParams);
            } else { //split between car and credit card
                lightrailChargeParams.put(StripeConstants.Parameters.CAPTURE, false);
                LightrailCharge lightrailCharge = LightrailCharge.create(lightrailChargeParams);
                try {
                    RequestOptions stripeRequestOptions = RequestOptions.builder()
                            .setIdempotencyKey(idempotencyKey)
                            .build();
                    stripeCharge = Charge.create(getStripeParams(stripeShare, chargeParams, lightrailCharge.getFullId()), stripeRequestOptions);
                } catch (Exception e) {
                    lightrailCharge.doVoid();
                    if (e instanceof BadParameterException)
                        throw (BadParameterException) e;
                    else
                        throw new ThirdPartyException(e);
                }
                lightrailCapturedCharge = lightrailCharge.capture(getLightrailMetadata(transactionAmount, stripeCharge.getId()));
            }
        } else { //all on credit card
            try {
                stripeCharge = Charge.create(getStripeParams(stripeShare, chargeParams, null));
            } catch (Exception e) {
                if (e instanceof BadParameterException)
                    throw (BadParameterException) e;
                else
                    throw new ThirdPartyException(e);
            }
        }

        return new StripeLightrailSplitTenderCharge(lightrailCapturedCharge, stripeCharge);
    }

    public static StripeLightrailSplitTenderCharge create(Map<String, Object> chargeParams) throws InsufficientValueException, AuthorizationException, CurrencyMismatchException, IOException, CouldNotFindObjectException, ThirdPartyException {
        StripeLightrailSplitTenderCharge charge = simulate(chargeParams);
        if (charge instanceof SimulatedStripeLightrailSplitTenderCharge)
            return ((SimulatedStripeLightrailSplitTenderCharge) charge).commit();
        else
            return charge;
    }

    public int getLightrailShare() {
        return (lightrailCharge == null) ? 0 : lightrailCharge.getAmount();
    }

    public int getStripeShare() {
        return (stripeCharge == null) ? 0 : stripeCharge.getAmount().intValue();
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

    public String getSummary() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Lightrail: ");
        if (lightrailCharge != null)
            buffer.append(lightrailCharge.getAmount());
        else
            buffer.append("0");

        buffer.append('\n');

        if (stripeCharge != null)
            buffer.append(stripeCharge.getAmount());
        else
            buffer.append("0");

        return buffer.toString();
    }
}
