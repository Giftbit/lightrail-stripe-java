package com.lightrail.model.stripe;


import com.lightrail.exceptions.*;
import com.lightrail.helpers.Constants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.GiftCharge;
import com.lightrail.model.business.GiftFund;
import com.lightrail.model.business.GiftValue;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class StripeGiftHybridChargeTest {

    private GiftFund returnFundsToCode(int amount) throws IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(Constants.StripeParameters.AMOUNT, amount);

        return GiftFund.create(giftFundParams);
    }
    private int getGiftCodeValue() throws IOException, AuthorizationException, CouldNotFindObjectException, CurrencyMismatchException, GiftCodeNotActiveException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        GiftValue giftValue = GiftValue.retrieve(giftValueParams);
        return giftValue.getCurrentValue();
    }

    @Test
    public void hybridChargeHappyPathTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, GiftCodeNotActiveException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 400;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(Constants.StripeParameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
        int giftCodeShare = paymentSummary.getGiftCodeAmount();
        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);

         paymentSummary = stripeGiftHybridCharge.getPaymentSummary();
         assertEquals(giftCodeShare, paymentSummary.getGiftCodeAmount());

        int creditCardShare = paymentSummary.getCreditCardAmount();

        assertEquals(transactionAmount, giftCodeShare + creditCardShare);

        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void hybridChargeGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int transactionAmount = getGiftCodeValue() - 1;
        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);

        PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
        assertEquals(0, paymentSummary.getCreditCardAmount());
        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeGiftHybridCharge.getPaymentSummary();
        int giftCodeShare = paymentSummary.getGiftCodeAmount();
        assertEquals(0, paymentSummary.getCreditCardAmount());
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void hybridChargeCreditCardOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 500;
        Map<String, Object> hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(Constants.StripeParameters.CURRENCY, properties.getProperty("happyPath.code.currency"));
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(Constants.StripeParameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
        assertEquals(0, paymentSummary.getGiftCodeAmount());
        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeGiftHybridCharge.getPaymentSummary();
        assertEquals(0, paymentSummary.getGiftCodeAmount());
    }

    @Test
    public void hybridChargeGiftCodeOnlyButNotEnough () throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int amountToGoOnCreditCard = 400;
        int transactionAmount = getGiftCodeValue() + amountToGoOnCreditCard;
        try {
            Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
            hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
            PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
            assertEquals( amountToGoOnCreditCard, paymentSummary.getCreditCardAmount());

            StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getCause().getClass().getName());
        }
    }

    @Test
    public void splitTransactionValueWithStripeMinimumTransactionValueInMindTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, GiftCodeNotActiveException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = getGiftCodeValue()+ 1;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(Constants.StripeParameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
        int giftCodeShare = paymentSummary.getGiftCodeAmount();
        int creditCardShare = paymentSummary.getCreditCardAmount();
        assertEquals(Constants.LightrailEcommerce.STRIPE_MINIMUM_TRANSACTION_VALUE, creditCardShare);
        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);

        paymentSummary = stripeGiftHybridCharge.getPaymentSummary();
        assertEquals(Constants.LightrailEcommerce.STRIPE_MINIMUM_TRANSACTION_VALUE, paymentSummary.getCreditCardAmount());

        returnFundsToCode(stripeGiftHybridCharge.getPaymentSummary().getGiftCodeAmount());
    }

    @Test
    public void giftCodeValueTooSmall() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, GiftCodeNotActiveException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int drainGiftCodeTransactionAmount = getGiftCodeValue() - 3;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, drainGiftCodeTransactionAmount);
        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);

        assertEquals(3, getGiftCodeValue());

        int impossibleForGiftCodeTransaction = Constants.LightrailEcommerce.STRIPE_MINIMUM_TRANSACTION_VALUE - 1;

        hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, impossibleForGiftCodeTransaction);
        hybridChargeParams.put(Constants.StripeParameters.TOKEN, properties.getProperty("stripe.demoToken"));
        try {
            stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(InsufficientValueException.class.getName(), e.getClass().getName());
        }

        returnFundsToCode(drainGiftCodeTransactionAmount + 3);
    }

    @Test
    public void hybridChargeMetadataTest () throws IOException, CouldNotFindObjectException, AuthorizationException, CurrencyMismatchException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        Integer transactionAmount = 563;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(Constants.StripeParameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);

        PaymentSummary paymentSummary = stripeGiftHybridCharge.getPaymentSummary();
        int giftCodeShare = paymentSummary.getGiftCodeAmount();

        Integer total = ((Double)stripeGiftHybridCharge.getGiftCharge().getMetadata().get(Constants.LightrailEcommerce.HYBRID_TRANSACTION_TOTAL_METADATA_KEY)).intValue();
        assertEquals(transactionAmount, total);

        returnFundsToCode(giftCodeShare);
    }

//    @Test
//    public void hybridChargeIdempotencyTest () throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
//        Properties properties = TestParams.getProperties();
//        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
//        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
//
//        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
//        GiftValue giftValue = GiftValue.retrieve(giftValueParams);
//
//        int amountToGoOnCreditCard = 400;
//        int transactionAmount = giftValue.getCurrentValue() + amountToGoOnCreditCard;
//
//        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
//        hybridChargeParams.put(Constants.StripeParameters.CUSTOMER, properties.getProperty("stripe.demoCustomer"));
//        hybridChargeParams.put(Constants.StripeParameters.AMOUNT, transactionAmount);
//
//        StripeGiftHybridCharge stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
//
//        String idempotencyKey = stripeGiftHybridCharge.getIdempotencyKey();
//
//        String firstGiftTransactionId = stripeGiftHybridCharge.getGiftTransactionId();
//        String firstStripeTransactionId = stripeGiftHybridCharge.getStripeTransactionId();
//
//        hybridChargeParams.put(Constants.LightrailParameters.USER_SUPPLIED_ID, idempotencyKey);
//
//        stripeGiftHybridCharge = StripeGiftHybridCharge.create(hybridChargeParams);
//
//        String secondGiftTransactionId = stripeGiftHybridCharge.getGiftTransactionId();
//        String secondStripeTransactionId = stripeGiftHybridCharge.getStripeTransactionId();
//
//        assertEquals(firstGiftTransactionId, secondGiftTransactionId);
//        assertEquals(firstStripeTransactionId, secondStripeTransactionId);
//
//        returnFundsToCode(stripeGiftHybridCharge.getGiftCharge().getAmount());
//    }

}
