package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;

import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class IntegrationSample {

    public void CheckoutClassSample() throws IOException, CurrencyMismatchException, BadParameterException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException, ThirdPartyException {
        //this is your order
        int orderTotal = 7505;
        String orderCurrency = "USD";

        //set up your API keys
        Stripe.apiKey = "...";
        Lightrail.apiKey = "...";

        //get the stripe token and the gift code
        String stripeToken = "...";
        String giftCode = "...";
        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        checkoutWithGiftCode.useLightrailGiftCode(giftCode);
        //or: checkoutWithGiftCode.useLightrailCardId("...");
        //or: checkoutWithGiftCode.useLightrailContact("...");
        checkoutWithGiftCode.useStripeToken(stripeToken);
        //or: checkoutWithGiftCode.useStripeCustomer("...");

        SimulatedStripeLightrailSplitTenderCharge simulatedCharge = checkoutWithGiftCode.simulate();
        int creditCardShare = simulatedCharge.getStripeShare();
        int giftCodeShare = simulatedCharge.getLightrailShare();
        System.out.println(String.format("Will charge %s%s on your gift card and %s%s on your credit card..", orderCurrency, giftCodeShare, orderCurrency, creditCardShare));

        StripeLightrailSplitTenderCharge committedCharge = simulatedCharge.commit();
    }

    public void SplitTenderCheckoutSample() throws IOException, CurrencyMismatchException, BadParameterException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException, ThirdPartyException {

        //this is your order
        int orderTotal = 7505;
        String orderCurrency = "USD";

        //set up your API keys
        Stripe.apiKey = "...";
        Lightrail.apiKey = "...";

        //get the stripe token and the gift code
        String stripeToken = "...";
        String giftCode = "...";

        Map<String, Object> chargeParams = new HashMap<String, Object>();
        chargeParams.put("amount", orderTotal);
        chargeParams.put("currency", orderCurrency);
        chargeParams.put("source", stripeToken);
        chargeParams.put("code", giftCode);

        SimulatedStripeLightrailSplitTenderCharge simulatedCharge = StripeLightrailSplitTenderCharge.simulate(chargeParams);
        int creditCardShare = simulatedCharge.getStripeShare();
        int giftCodeShare = simulatedCharge.getLightrailShare();
        System.out.println(String.format("Will charge %s%s on your gift card and %s%s on your credit card..", orderCurrency, giftCodeShare, orderCurrency, creditCardShare));

        StripeLightrailSplitTenderCharge committedCharge = simulatedCharge.commit();
    }

    public void ManualCheckoutWalkThroughSample() throws IOException, CurrencyMismatchException, BadParameterException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Properties properties = TestParams.getProperties();

        //this is your order
        int orderTotal = 7505;
        String orderCurrency = "USD";

        //set up your API keys
        Stripe.apiKey = "...";
        Lightrail.apiKey = "...";

        //get the stripe token and the gift code
        String stripeToken = "...";
        String giftCode = "...";

        //check how much the gift code can contribute to the checkout
        LightrailCharge simulatedCharge = LightrailCharge.simulateByCode(giftCode, orderTotal, orderCurrency);

        int giftCodeShare = simulatedCharge.getAmount();
        int creditCardShare = orderTotal - giftCodeShare;

        System.out.println(String.format("Will charge %s%s on your gift card and %s%s on your credit card..", orderCurrency, giftCodeShare, orderCurrency, creditCardShare));


        if (creditCardShare == 0) { // the gift code can pay for the full order
            System.out.println(String.format("charging gift code for the entire order total, %s%s.", orderCurrency, giftCodeShare));
            LightrailCharge lightrailCharge = LightrailCharge.createByCode(giftCode, giftCodeShare, orderCurrency);
        } else if (giftCodeShare > 0) { //the gift code can pay some and the remainder goes on the credit card
            //pending charge on gift code
            System.out.println(String.format("charging gift code %s%s.", orderCurrency, giftCodeShare));
            LightrailCharge lightrailCharge = LightrailCharge.createPendingByCode(giftCode, giftCodeShare, orderCurrency);

            // Charge the remainder on the credit card:
            System.out.println(String.format("charging credit card %s%s.", orderCurrency, creditCardShare));

            Map<String, Object> stripeParam = new HashMap<String, Object>();
            stripeParam.put("amount", creditCardShare);
            stripeParam.put("currency", orderCurrency);
            stripeParam.put("source", stripeToken);
            try {
                Charge charge = Charge.create(stripeParam);
                //capture gift code charge once the credit card transaction went through
                lightrailCharge.capture();
            } catch (IOException e) {
                lightrailCharge.doVoid();
                throw new IOException(e);
            }
        } else { //entire order charged on credit card
            System.out.println(String.format("charging credit card for the entire order total, %s%s.", orderCurrency, creditCardShare));

            Map<String, Object> stripeParam = new HashMap<String, Object>();
            stripeParam.put("amount", creditCardShare);
            stripeParam.put("currency", orderCurrency);
            stripeParam.put("source", stripeToken);
            Charge charge = Charge.create(stripeParam);
        }
    }
}
