package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.api.objects.Metadata;
import com.lightrail.model.api.objects.RequestParameters;
import com.lightrail.model.business.LightrailTransaction;

import java.io.IOException;
import java.util.*;

public class LightrailCharge extends LightrailBaseTransaction {

    LightrailCharge(LightrailTransaction transactionObject) {
        this.transactionObject = transactionObject;
    }

    public int getAmount() {
        return 0 - transactionObject.getValue();
    }

    public void setAmount (int amount) {
        transactionObject.setValue(0-amount);
    }

    static RequestParameters translateChargeParamsToLightrail(Map<String, Object> chargeParams) {
        chargeParams = LightrailBaseTransaction.translateToLightrail(chargeParams);
        if (!chargeParams.containsKey(StripeConstants.Parameters.CAPTURE))
            chargeParams.put(StripeConstants.Parameters.CAPTURE, true);

        RequestParameters translatedParams = new RequestParameters();
        translatedParams.putAll(chargeParams);

        //capture --> pending
        Boolean capture = (Boolean) translatedParams.remove(StripeConstants.Parameters.CAPTURE);
        if (capture != null)
            translatedParams.put(LightrailConstants.Parameters.PENDING, !capture);
        else
            translatedParams.put(LightrailConstants.Parameters.PENDING, false);


        //amount --> value
        Integer chargeAmount = (Integer) translatedParams.remove(StripeConstants.Parameters.AMOUNT);

        if (chargeAmount == null || chargeAmount <= 0)
            throw new BadParameterException("Charge 'amount' must be a positive integer in the smallest unit of currency.");
        Integer lightrailTransactionValue = 0 - chargeAmount;
        translatedParams.put(LightrailConstants.Parameters.VALUE, lightrailTransactionValue);

        return translatedParams;
    }

    public LightrailFund refund(String userSuppliedId, Metadata metadata) throws IOException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException {
        return new LightrailFund(transactionObject.refund(userSuppliedId, metadata));
    }

    public LightrailCharge capture(String userSuppliedId, Metadata metadata) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailCharge(transactionObject.capture(userSuppliedId, metadata));
    }

    public LightrailFund doVoid(String userSuppliedId, Metadata metadata) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailFund(transactionObject.doVoid(userSuppliedId, metadata));
    }

    public LightrailFund refund(Metadata metadata) throws IOException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException {
        return new LightrailFund(transactionObject.refund(metadata));
    }

    public LightrailCharge capture(Metadata metadata) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailCharge(transactionObject.capture(metadata));
    }

    public LightrailFund doVoid(Metadata metadata) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailFund(transactionObject.doVoid(metadata));
    }

    public LightrailFund refund() throws IOException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException {
        return new LightrailFund(transactionObject.refund());
    }

    public LightrailCharge capture() throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailCharge(transactionObject.capture());
    }

    public LightrailFund doVoid() throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return new LightrailFund(transactionObject.doVoid());
    }

    public static LightrailCharge createPendingByContact(String customerAccountId, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByContact(customerAccountId, amount, currency, false);
    }

    public static LightrailCharge createByContact(String contactId, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByContact(contactId, amount, currency, true);
    }

    private static LightrailCharge createByContact(String contactId, int amount, String currency, boolean capture) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put(LightrailConstants.Parameters.CONTACT, contactId);
        chargeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        chargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        chargeParams.put(StripeConstants.Parameters.CAPTURE, capture);
        return create(chargeParams);
    }

    public static LightrailCharge createPendingByCardId(String cardId, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByCardId(cardId, amount, currency, false);
    }

    public static LightrailCharge createByCardId(String cardId, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByCardId(cardId, amount, currency, true);
    }

    private static LightrailCharge createByCardId(String cardId, int amount, String currency, boolean capture) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        Map<String, Object> giftChargeParams = new HashMap<>();
        giftChargeParams.put(LightrailConstants.Parameters.CARD_ID, cardId);
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        giftChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        giftChargeParams.put(StripeConstants.Parameters.CAPTURE, capture);
        return create(giftChargeParams);
    }

    public static LightrailCharge createPendingByCode(String code, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByCode(code, amount, currency, false);
    }

    public static LightrailCharge createByCode(String code, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return createByCode(code, amount, currency, true);
    }

    private static LightrailCharge createByCode(String code, int amount, String currency, boolean capture) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        Map<String, Object> giftChargeParams = new HashMap<>();
        giftChargeParams.put(LightrailConstants.Parameters.CODE, code);
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        giftChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        giftChargeParams.put(StripeConstants.Parameters.CAPTURE, capture);
        return create(giftChargeParams);
    }

    public static LightrailCharge simulateByCardId(String cardId, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        Map<String, Object> giftChargeParams = new HashMap<>();
        giftChargeParams.put(LightrailConstants.Parameters.CARD_ID, cardId);
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        giftChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        return simulate(giftChargeParams);
    }
    public static LightrailCharge simulateByCode(String code, int amount, String currency) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        Map<String, Object> giftChargeParams = new HashMap<>();
        giftChargeParams.put(LightrailConstants.Parameters.CODE, code);
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, amount);
        giftChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        return simulate(giftChargeParams);
    }

    public static LightrailCharge simulate(Map<String, Object> giftChargeParams) throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        return create(giftChargeParams, true);
    }

    public static LightrailCharge create(Map<String, Object> giftChargeParams) throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        return create(giftChargeParams, false);
    }

    private static LightrailCharge create(Map<String, Object> giftChargeParams, boolean simulate) throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), giftChargeParams);

        if (simulate)
            return new LightrailCharge(LightrailTransaction.Simulate.simulate(translateChargeParamsToLightrail(giftChargeParams)));
        else
            return new LightrailCharge(LightrailTransaction.Create.create(translateChargeParamsToLightrail(giftChargeParams)));
    }

    public static LightrailCharge retrieve(Map<String, Object> chargeParams) throws AuthorizationException, IOException, CouldNotFindObjectException {
        RequestParameters requestParameters = new RequestParameters();
        requestParameters.putAll(chargeParams);
        LightrailTransaction retrievedTransaction = LightrailTransaction.Retrieve.retrieve(requestParameters);

        return new LightrailCharge(retrievedTransaction);
    }
}