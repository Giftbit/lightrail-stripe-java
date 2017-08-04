package com.lightrail.model.stripe;


import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.LightrailEcommerceConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.CustomerAccount;
import com.lightrail.model.business.LightrailFund;
import com.lightrail.model.business.LightrailValue;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class StripeLightrailHybridChargeTest {

    private LightrailFund returnFundsToCode(int amount) throws IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, amount);

        return LightrailFund.create(giftFundParams);
    }
    private int getGiftCodeValue() throws IOException, AuthorizationException, CouldNotFindObjectException, CurrencyMismatchException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Map<String, Object> giftValueParams = TestParams.readCodeParamsFromProperties();
        LightrailValue giftValue = LightrailValue.retrieve(giftValueParams);
        return giftValue.getCurrentValue();
    }

    @Test
    public void hybridChargeHappyPathTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 400;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        int giftCodeShare = paymentSummary.getLightrailPayment().getAmount();
        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);

        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        assertEquals(giftCodeShare, paymentSummary.getLightrailPayment().getAmount());

        int creditCardShare = paymentSummary.getStripePayment().getAmount();

        assertEquals(transactionAmount, giftCodeShare + creditCardShare);

        returnFundsToCode(giftCodeShare);
    }


    @Test
    public void hybridChargeHappyPathWithCustomerIdTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 400;
        String currency = "USD";

        int customerCreditValue = 300;
        CustomerAccount customerAccount = CustomerAccount.create("test@test.ca", "Test", "McTest");
        customerAccount.addCurrency(currency, customerCreditValue);

        Map<String, Object> hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(LightrailConstants.Parameters.CUSTOMER, customerAccount.getId());
        hybridChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        int lightrailShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        assertEquals(customerCreditValue, lightrailShare);

        int creditCardShare = paymentSummary.getStripePayment().getAmount();

        assertEquals(transactionAmount, lightrailShare + creditCardShare);

        customerAccount.fund(lightrailShare);

        hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(LightrailConstants.Parameters.CUSTOMER, customerAccount.getId());
        hybridChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.CUSTOMER, properties.getProperty("stripe.demoCustomer"));

        paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        lightrailShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        assertEquals(customerCreditValue, lightrailShare);
    }

    @Test
    public void hybridChargeGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int transactionAmount = getGiftCodeValue() - 1;
        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);

        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        assertEquals(0, paymentSummary.getStripePayment().getAmount());
        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        int giftCodeShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(0, paymentSummary.getStripePayment().getAmount());
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void hybridChargeCreditCardOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 500;
        Map<String, Object> hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(StripeConstants.Parameters.CURRENCY, properties.getProperty("happyPath.code.currency"));
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        assertEquals(0, paymentSummary.getLightrailPayment().getAmount());
        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        assertEquals(0, paymentSummary.getLightrailPayment().getAmount());
    }

    @Test
    public void hybridChargeGiftCodeOnlyButNotEnough () throws IOException, CurrencyMismatchException, AuthorizationException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int amountToGoOnCreditCard = 400;
        int transactionAmount = getGiftCodeValue() + amountToGoOnCreditCard;
        try {
            Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
            hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
            PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
            assertEquals( amountToGoOnCreditCard, paymentSummary.getStripePayment().getAmount());

            StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getCause().getClass().getName());
        }
    }

    @Test
    public void splitTransactionValueWithStripeMinimumTransactionValueInMindTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = getGiftCodeValue()+ 1;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
        int giftCodeShare = paymentSummary.getLightrailPayment().getAmount();
        int creditCardShare = paymentSummary.getStripePayment().getAmount();
        assertEquals(StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE, creditCardShare);
        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);

        paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        assertEquals(StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE, paymentSummary.getStripePayment().getAmount());

        returnFundsToCode(stripeLightrailHybridCharge.getPaymentSummary().getLightrailPayment().getAmount());
    }

    @Test
    public void giftCodeValueTooSmall() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int drainGiftCodeTransactionAmount = getGiftCodeValue() - 3;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, drainGiftCodeTransactionAmount);
        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);

        assertEquals(3, getGiftCodeValue());

        int impossibleForGiftCodeTransaction = StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE - 1;

        hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, impossibleForGiftCodeTransaction);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));
        try {
            stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(InsufficientValueException.class.getName(), e.getClass().getName());
        }

        returnFundsToCode(drainGiftCodeTransactionAmount + 3);
    }

    @Test
    public void hybridChargeMetadataTest () throws IOException, CouldNotFindObjectException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, ThirdPartyPaymentException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        Integer transactionAmount = 563;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailHybridCharge stripeLightrailHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);

        PaymentSummary paymentSummary = stripeLightrailHybridCharge.getPaymentSummary();
        int giftCodeShare = paymentSummary.getLightrailPayment().getAmount();

        Integer total = ((Double) stripeLightrailHybridCharge.getLightrailCharge().getMetadata().get(LightrailEcommerceConstants.HYBRID_TRANSACTION_TOTAL_METADATA_KEY)).intValue();
        assertEquals(transactionAmount, total);

        returnFundsToCode(giftCodeShare);
    }

    //todo: uncomment when the API side support is complete
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
//        StripeLightrailHybridCharge stripeGiftHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
//
//        String idempotencyKey = stripeGiftHybridCharge.getIdempotencyKey();
//
//        String firstGiftTransactionId = stripeGiftHybridCharge.getLightrailTransactionId();
//        String firstStripeTransactionId = stripeGiftHybridCharge.getStripeTransactionId();
//
//        hybridChargeParams.put(Constants.LightrailParameters.USER_SUPPLIED_ID, idempotencyKey);
//
//        stripeGiftHybridCharge = StripeLightrailHybridCharge.create(hybridChargeParams);
//
//        String secondGiftTransactionId = stripeGiftHybridCharge.getLightrailTransactionId();
//        String secondStripeTransactionId = stripeGiftHybridCharge.getStripeTransactionId();
//
//        assertEquals(firstGiftTransactionId, secondGiftTransactionId);
//        assertEquals(firstStripeTransactionId, secondStripeTransactionId);
//
//        returnFundsToCode(stripeGiftHybridCharge.getLightrailCharge().getAmount());
//    }

}
