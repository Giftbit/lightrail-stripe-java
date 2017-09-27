package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;

import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;


import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.lightrail.helpers.TestParams.getGiftCodeValue;
import static org.junit.Assert.assertEquals;

public class IntegrationSample {



    //todo: rewrite
    public void CheckoutWalkThroughSample () throws IOException, CurrencyMismatchException, BadParameterException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Properties properties = TestParams.getProperties();

        //this is your order
        int orderTotal = 7505;
        String orderCurrency = "USD";

        //set up your API keys
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        //get the stripe token and the gift code
        String stripeToken = properties.getProperty("stripe.demoToken");
        String giftCode = properties.getProperty("happyPath.code");

        //check how much the gift code can contribute to the checkout
        int giftCodeValue = getGiftCodeValue();

        int giftCodeShare = Math.min (orderTotal , giftCodeValue);
        int creditCardShare = orderTotal - giftCodeShare;

        if (creditCardShare == 0) { // the gift code can pay for the full order
            System.out.println(String.format("charging gift code for the entire order total, %s%s.", orderCurrency, giftCodeShare));
            LightrailCharge giftCharge = LightrailCharge.createByCode(giftCode, giftCodeShare, orderCurrency);
        } else if (giftCodeShare > 0){ //the gift code can pay some and the remainder goes on the credit card
            //pending charge on gift code
            System.out.println(String.format("charging gift code %s%s.", orderCurrency, giftCodeValue));

            LightrailCharge giftCharge = LightrailCharge.createPendingByCode(giftCode, giftCodeShare,orderCurrency);

            // Charge the remainder on the credit card:
            System.out.println(String.format("charging credit card %s%s.", orderCurrency, creditCardShare));

            Map<String, Object> stripeParam = new HashMap<String, Object>();
            stripeParam.put("amount", creditCardShare);
            stripeParam.put("currency", orderCurrency);
            stripeParam.put("source", stripeToken);
            try {
                Charge charge = Charge.create(stripeParam);
                //capture gift code charge once the credit card transaction went through
                giftCharge.capture();
            } catch (IOException e) {
                giftCharge.doVoid();
                throw new IOException(e);
            }
        }
        else { //entire order charged on credit card
            System.out.println(String.format("charging credit card for the entire order total, %s%s.", orderCurrency, creditCardShare));

            Map<String, Object> stripeParam = new HashMap<String, Object>();
            stripeParam.put("amount", creditCardShare);
            stripeParam.put("currency", orderCurrency);
            stripeParam.put("source", stripeToken);
            Charge charge = Charge.create(stripeParam);
        }
    }

}
