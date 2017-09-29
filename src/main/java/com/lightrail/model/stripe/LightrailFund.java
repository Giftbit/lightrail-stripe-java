package com.lightrail.model.stripe;

import com.lightrail.exceptions.AuthorizationException;
import com.lightrail.exceptions.BadParameterException;
import com.lightrail.exceptions.CouldNotFindObjectException;
import com.lightrail.exceptions.InsufficientValueException;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.model.api.objects.RequestParameters;
import com.lightrail.model.business.LightrailTransaction;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LightrailFund extends LightrailBaseTransaction {

    LightrailFund(LightrailTransaction cardTransactionResponse) {
        this.transactionObject = cardTransactionResponse;
    }

    public int getAmount() {
        return transactionObject.getValue();
    }

    public static LightrailFund createByCardId(String cardId, int amount, String currency) throws BadParameterException, IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> fundParams = new HashMap<>();
        fundParams.put(LightrailConstants.Parameters.CARD_ID, cardId);
        fundParams.put(StripeConstants.Parameters.AMOUNT, amount);
        fundParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        return create(fundParams);
    }

    public static LightrailFund createByContact(String contactId, int amount, String currency) throws BadParameterException, IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = new HashMap<>();
        giftFundParams.put(LightrailConstants.Parameters.CONTACT, contactId);
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, amount);
        giftFundParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        return create(giftFundParams);
    }

    public static LightrailFund create(Map<String, Object> fundParams) throws BadParameterException, IOException, AuthorizationException, CouldNotFindObjectException {
        LightrailConstants.Parameters.requireParameters(Arrays.asList(
                StripeConstants.Parameters.AMOUNT,
                LightrailConstants.Parameters.CURRENCY
        ), fundParams);

        LightrailTransaction fundTransaction;

        try {
            fundTransaction = LightrailTransaction.Create.create(translateToLightrail(fundParams));
        } catch (InsufficientValueException e) {
            throw new RuntimeException(e); //never happens since we are funding
        }

        return new LightrailFund(fundTransaction);
    }

    static RequestParameters translateToLightrail(Map<String, Object> fundParams) {
        fundParams = LightrailBaseTransaction.translateToLightrail(fundParams);
        RequestParameters translatedParams = new RequestParameters();
        translatedParams.putAll(fundParams);
        //amount --> value
        Integer fundAmount = (Integer) translatedParams.remove(StripeConstants.Parameters.AMOUNT);
        if (fundAmount <= 0)
            throw new BadParameterException("Fund 'amount' must be a positive integer in the smallest unit of currency.");
        Integer lightrailTransactionValue = fundAmount;
        translatedParams.put(LightrailConstants.Parameters.VALUE, lightrailTransactionValue);


        return translatedParams;
    }
}

