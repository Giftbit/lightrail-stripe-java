package com.lightrail.helpers;

import com.lightrail.exceptions.AuthorizationException;
import com.lightrail.exceptions.CouldNotFindObjectException;
import com.lightrail.exceptions.CurrencyMismatchException;
import com.lightrail.exceptions.InsufficientValueException;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.GiftCard;
import com.lightrail.model.stripe.LightrailCharge;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestParams {

    public static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("_test-config.properties"));
        return properties;
    }


    public static Map<String, Object> readCodeParamsFromProperties () throws IOException {
        Properties properties = getProperties();

        Map<String, Object> giftChargeParams = new HashMap<>();
        giftChargeParams.put("code", properties.getProperty("happyPath.code"));
        giftChargeParams.put("currency", properties.getProperty("happyPath.code.currency"));

        return giftChargeParams;
    }

    public static Map<String, Object> readCardParamsFromProperties () throws IOException {
        Properties properties = getProperties();

        Map<String, Object> giftChargeParams = new HashMap<String, Object>();
        giftChargeParams.put("cardId", properties.getProperty("happyPath.code.cardId"));
        giftChargeParams.put("currency", properties.getProperty("happyPath.code.currency"));

        return giftChargeParams;
    }

    public static int getGiftCodeValue() throws IOException, AuthorizationException, CouldNotFindObjectException, CurrencyMismatchException, InsufficientValueException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        String cardId = GiftCard.retrieveCardDetailsByCode(properties.getProperty("happyPath.code")).getCardId();
        GiftCard giftCard = GiftCard.retrieve(cardId);
        return LightrailCharge.simulateByCardId(cardId, giftCard.retrieveMaximumValue(), giftCard.getCurrency()).getAmount();
    }

}
