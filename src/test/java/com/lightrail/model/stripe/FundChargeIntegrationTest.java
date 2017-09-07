package com.lightrail.model.stripe;

import com.lightrail.exceptions.AuthorizationException;
import com.lightrail.exceptions.CouldNotFindObjectException;
import com.lightrail.exceptions.CurrencyMismatchException;
import com.lightrail.exceptions.InsufficientValueException;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.LightrailValue;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class FundChargeIntegrationTest {
    @Test
    public void GiftValueChargeCancelTest () throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        int giftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        int chargeAmount = 101;

        Map<String, Object> giftChargeParams = TestParams.readCodeParamsFromProperties();
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);
        giftChargeParams.put(StripeConstants.Parameters.CAPTURE, false);

        LightrailCharge giftCharge = LightrailCharge.create(giftChargeParams);
        int newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        assertEquals(giftCodeValue - chargeAmount, newGiftCodeValue);

        giftCharge.doVoid();
        newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();
        assertEquals(giftCodeValue, newGiftCodeValue);
    }

    @Test
    public void GiftValueChargeRefundTest () throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        int giftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        int chargeAmount = 101;

        Map<String, Object> giftChargeParams = TestParams.readCodeParamsFromProperties();
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);

        LightrailCharge giftCharge = LightrailCharge.create(giftChargeParams);
        int newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        assertEquals(giftCodeValue - chargeAmount, newGiftCodeValue);

        giftCharge.refund();
        newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();
        assertEquals(giftCodeValue, newGiftCodeValue);
    }

    @Test
    public void GiftValueFundTest () throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        int giftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        int valueAdded = 101;

        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, valueAdded);
        LightrailFund giftFund = LightrailFund.create(giftFundParams);

        int newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        assertEquals(giftCodeValue + valueAdded, newGiftCodeValue);
    }

    @Test
    public void GiftValueChargeCaptureFundTest () throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        int giftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();

        int chargeAmount = 101;

        Map<String, Object> giftChargeParams = TestParams.readCodeParamsFromProperties();
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);
        giftChargeParams.put(StripeConstants.Parameters.CAPTURE, false);
        LightrailCharge giftCharge = LightrailCharge.create(giftChargeParams);

        int newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();
        assertEquals(giftCodeValue - chargeAmount, newGiftCodeValue);
        giftCharge.capture();
        newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();
        assertEquals(giftCodeValue - chargeAmount, newGiftCodeValue);

        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);

        LightrailFund giftFund = LightrailFund.create(giftFundParams);
        newGiftCodeValue = LightrailValue.retrieve(giftValueParams).getCurrentValue();
        assertEquals(giftCodeValue, newGiftCodeValue);

    }

    @Test
    public void GiftValueChargeFundTest () throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        LightrailValue giftValue = LightrailValue.retrieve(giftValueParams);
        int giftCodeValue = giftValue.getCurrentValue();

        int chargeAmount = 101;

        Map<String, Object> giftChargeParams = TestParams.readCodeParamsFromProperties();
        giftChargeParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);
        LightrailCharge giftCharge = LightrailCharge.create(giftChargeParams);

        LightrailValue newGiftValue = LightrailValue.retrieve(giftValueParams);
        int newGiftCodeValue = newGiftValue.getCurrentValue();

        assertEquals(giftCodeValue - chargeAmount, newGiftCodeValue);

        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, chargeAmount);

        LightrailFund giftFund = LightrailFund.create(giftFundParams);

        newGiftValue = LightrailValue.retrieve(giftValueParams);
        newGiftCodeValue = newGiftValue.getCurrentValue();
        assertEquals(giftCodeValue, newGiftCodeValue);
    }

}
