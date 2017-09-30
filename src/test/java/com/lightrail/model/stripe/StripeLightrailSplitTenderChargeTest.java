package com.lightrail.model.stripe;


import com.lightrail.exceptions.*;
import com.lightrail.helpers.LightrailConstants;
import com.lightrail.helpers.LightrailEcommerceConstants;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.LightrailContact;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.lightrail.helpers.TestParams.getGiftCodeValue;
import static org.junit.Assert.assertEquals;

public class StripeLightrailSplitTenderChargeTest {

    private LightrailFund returnFundsToCode(int amount) throws IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, amount);

        return LightrailFund.create(giftFundParams);
    }

    @Test
    public void hybridChargeHappyPathTest() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 400;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        assert simulatedTx instanceof SimulatedStripeLightrailSplitTenderCharge;

        int giftCodeShare = simulatedTx.getLightrailCharge().getAmount();

        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = ((SimulatedStripeLightrailSplitTenderCharge)simulatedTx).commit();

        assertEquals(giftCodeShare, stripeLightrailSplitTenderCharge.getLightrailCharge().getAmount());

        int creditCardShare = stripeLightrailSplitTenderCharge.getStripeShare();

        assertEquals(transactionAmount, giftCodeShare + creditCardShare);

        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void hybridChargeHappyPathWithCustomerIdTest() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 400;
        String currency = "USD";

        int customerCreditValue = 300;
        LightrailContact customerAccount = LightrailContact.create("test@test.ca", "Test", "McTest");
        customerAccount.addCurrency(currency, customerCreditValue);

        Map<String, Object> hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(LightrailConstants.Parameters.CONTACT, customerAccount.getContactId());
        hybridChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge simulatedTx  = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        int lightrailShare = simulatedTx.getLightrailCharge().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams, transactionAmount - lightrailShare, lightrailShare);
        assertEquals(customerCreditValue, lightrailShare);
        int creditCardShare = stripeLightrailSplitTenderCharge.getStripeCharge().getAmount().intValue();
        assertEquals(transactionAmount, lightrailShare + creditCardShare);

        customerAccount.createTransaction(lightrailShare);

        hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(LightrailConstants.Parameters.CONTACT, customerAccount.getContactId());
        hybridChargeParams.put(LightrailConstants.Parameters.CURRENCY, currency);
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.CUSTOMER, properties.getProperty("stripe.demoCustomer"));

        simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        lightrailShare = simulatedTx.getLightrailCharge().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
        lightrailShare = stripeLightrailSplitTenderCharge.getLightrailCharge().getAmount();
        assertEquals(customerCreditValue, lightrailShare);
    }

    @Test
    public void hybridChargeGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int transactionAmount = getGiftCodeValue() - 1;
        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);

        StripeLightrailSplitTenderCharge simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        assertEquals(0, simulatedTx.getStripeShare());
        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
        int giftCodeShare = stripeLightrailSplitTenderCharge.getLightrailShare();
        assertEquals(0, stripeLightrailSplitTenderCharge.getStripeShare());
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void hybridChargeCreditCardOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = 500;
        Map<String, Object> hybridChargeParams = new HashMap<>();
        hybridChargeParams.put(StripeConstants.Parameters.CURRENCY, properties.getProperty("happyPath.code.currency"));
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        assertEquals(0, simulatedTx.getLightrailShare());
        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
        assertEquals(0, stripeLightrailSplitTenderCharge.getLightrailShare());
    }

    @Test
    public void hybridChargeGiftCodeOnlyButNotEnough () throws IOException, CurrencyMismatchException, AuthorizationException, CouldNotFindObjectException, InsufficientValueException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int amountToGoOnCreditCard = 400;
        int transactionAmount = getGiftCodeValue() + amountToGoOnCreditCard;
        try {
            Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
            hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
            StripeLightrailSplitTenderCharge simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
            assertEquals( amountToGoOnCreditCard, simulatedTx.getStripeShare());

            StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getClass().getName());
        }
    }

    @Test
    public void splitTransactionValueWithStripeMinimumTransactionValueInMindTest() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int transactionAmount = getGiftCodeValue()+ 1;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge simulatedTx = StripeLightrailSplitTenderCharge.simulate(hybridChargeParams);
        int giftCodeShare = simulatedTx.getLightrailShare();
        int creditCardShare = simulatedTx.getStripeShare();
        assertEquals(StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE, creditCardShare);
        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);

        assertEquals(StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE, stripeLightrailSplitTenderCharge.getStripeShare());

        returnFundsToCode(stripeLightrailSplitTenderCharge.getLightrailCharge().getAmount());
    }

    @Test
    public void giftCodeValueTooSmall() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int drainGiftCodeTransactionAmount = getGiftCodeValue() - 3;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, drainGiftCodeTransactionAmount);
        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);

        assertEquals(3, getGiftCodeValue());

        int impossibleForGiftCodeTransaction = StripeConstants.STRIPE_MINIMUM_TRANSACTION_VALUE - 1;

        hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, impossibleForGiftCodeTransaction);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));
        try {
            stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
        } catch (Exception e) {
            assertEquals(InsufficientValueException.class.getName(), e.getClass().getName());
        }

        returnFundsToCode(drainGiftCodeTransactionAmount + 3);
    }

    @Test
    public void hybridChargeMetadataTest () throws IOException, CouldNotFindObjectException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        Integer transactionAmount = 563;

        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
        hybridChargeParams.put(StripeConstants.Parameters.TOKEN, properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge stripeLightrailSplitTenderCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);

        int giftCodeShare = stripeLightrailSplitTenderCharge.getLightrailShare();

        assert stripeLightrailSplitTenderCharge.getLightrailCharge() != null;

        Integer total = ((Double) stripeLightrailSplitTenderCharge.getLightrailCharge().getMetadata().get(LightrailEcommerceConstants.HybridTransactionMetadata.SPLIT_TENDER_TOTAL)).intValue();
        assertEquals(transactionAmount, total);

        returnFundsToCode(giftCodeShare);
    }

//    @Test
//    public void hybridChargeIdempotencyTest () throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException, ThirdPartyException {
//        Properties properties = TestParams.getProperties();
//        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
//        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
//
//        int amountToGoOnCreditCard = 400;
//        int transactionAmount = getGiftCodeValue() + amountToGoOnCreditCard;
//
//        Map<String, Object> hybridChargeParams = TestParams.readCodeParamsFromProperties();
//        hybridChargeParams.put(StripeConstants.Parameters.CUSTOMER, properties.getProperty("stripe.demoCustomer"));
//        hybridChargeParams.put(StripeConstants.Parameters.AMOUNT, transactionAmount);
//
//        StripeLightrailSplitTenderCharge stripeGiftHybridCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
//
//        String idempotencyKey = stripeGiftHybridCharge.getIdempotencyKey();
//
//        String firstGiftTransactionId = stripeGiftHybridCharge.getLightrailTransactionId();
//        String firstStripeTransactionId = stripeGiftHybridCharge.getStripeTransactionId();
//
//        hybridChargeParams.put(LightrailConstants.Parameters.USER_SUPPLIED_ID, idempotencyKey);
//
//        stripeGiftHybridCharge = StripeLightrailSplitTenderCharge.create(hybridChargeParams);
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
