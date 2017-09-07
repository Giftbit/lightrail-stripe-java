package com.lightrail.model.stripe;

import com.lightrail.exceptions.AuthorizationException;
import com.lightrail.exceptions.BadParameterException;
import com.lightrail.exceptions.CouldNotFindObjectException;
import com.lightrail.exceptions.InsufficientValueException;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.business.LightrailTransaction;

import java.io.IOException;
import java.util.*;

public class LightrailCharge extends LightrailBaseTransaction {

    private LightrailCharge(LightrailTransaction transactionObject) {
        this.transactionObject = transactionObject;
    }

    public int getAmount() {
        return 0 - transactionObject.getValue();
    }

    static Map<String, Object> translateToLightrail(Map<String, Object> chargeParams) {
        chargeParams = LightrailBaseTransaction.translateToLightrail(chargeParams);
        if (!chargeParams.containsKey(StripeConstants.Parameters.CAPTURE))
            chargeParams.put(StripeConstants.Parameters.CAPTURE, true);

        Map<String, Object> translatedParams = new HashMap<>(chargeParams);

        //capture --> pending
        Boolean capture = (Boolean) translatedParams.remove(StripeConstants.Parameters.CAPTURE);
        if (capture != null)
            translatedParams.put(LightrailConstants.Parameters.PENDING, !capture);
        else
            translatedParams.put(LightrailConstants.Parameters.PENDING, false);


        //amount --> value
        Integer chargeAmount = (Integer) translatedParams.remove(StripeConstants.Parameters.AMOUNT);
        if (chargeAmount <= 0)
            throw new BadParameterException("Charge 'amount' must be a positive integer in the smallest unit of currency.");
        Integer lightrailTransactionValue = 0 - chargeAmount;
        translatedParams.put(LightrailConstants.Parameters.VALUE, lightrailTransactionValue);

        return translatedParams;
    }

    public LightrailTransaction refund () throws IOException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException {
        return transactionObject.refund();
    }

    public LightrailTransaction capture() throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return transactionObject.capture();
    }

    public LightrailTransaction doVoid() throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return transactionObject.doVoid();
    }

    LightrailTransaction refund(Map<String, Object> transactionParams) throws AuthorizationException, CouldNotFindObjectException, InsufficientValueException, IOException {
        return transactionObject.refund(translateToLightrail(transactionParams));
    }

    LightrailTransaction doVoid(Map<String, Object> transactionParams) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return transactionObject.doVoid(translateToLightrail(transactionParams));
    }

    LightrailTransaction capture(Map<String, Object> transactionParams) throws IOException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        return transactionObject.capture(translateToLightrail(transactionParams));
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

    public static LightrailCharge create(Map<String, Object> giftChargeParams) throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), giftChargeParams);

        return new LightrailCharge(LightrailTransaction.create(translateToLightrail(giftChargeParams)));
    }
}