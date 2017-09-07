package com.lightrail.model.stripe;

import com.lightrail.exceptions.AuthorizationException;
import com.lightrail.exceptions.CouldNotFindObjectException;
import com.lightrail.exceptions.InsufficientValueException;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class LightrailFundTest {

    @Test
    public void GiftFundHappyPathTest () throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int fundAmount = 500;

        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put("amount", fundAmount);

        LightrailFund giftCharge = LightrailFund.create(giftFundParams);
        assertEquals(giftCharge.getAmount(), fundAmount);
    }

    @Test
    public void GiftFundHappyPathTestWithoutParams () throws IOException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int fundAmount = 500;

        LightrailFund giftCharge = LightrailFund.createByCardId(
                properties.getProperty("happyPath.code.cardId"),
                fundAmount,
                properties.getProperty("happyPath.code.currency")
                );
        assertEquals(giftCharge.getAmount(), fundAmount);
    }
}
