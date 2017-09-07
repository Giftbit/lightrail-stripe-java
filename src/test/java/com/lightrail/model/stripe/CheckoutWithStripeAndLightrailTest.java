package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.CustomerAccount;
import com.lightrail.model.business.LightrailValue;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class CheckoutWithStripeAndLightrailTest {

    private LightrailFund returnFundsToCode(int amount) throws IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, amount);

        return LightrailFund.create(giftFundParams);
    }

    private int getGiftCodeValue() throws IOException, AuthorizationException, CouldNotFindObjectException, CurrencyMismatchException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        LightrailValue giftValue = LightrailValue.retrieveByCode(properties.getProperty("happyPath.code"));
        return giftValue.getCurrentValue();
    }

    private CheckoutWithStripeAndLightrail createCheckoutObject(int orderTotal, boolean addGiftCode, boolean addStripe)
            throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, ThirdPartyPaymentException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        if (addGiftCode) {
            checkoutWithGiftCode.useGiftCode(properties.getProperty("happyPath.code"));
        }
        if (addStripe) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, 2);
            if (randomNum > 0)
                checkoutWithGiftCode.useStripeToken(properties.getProperty("stripe.demoToken"));
            else
                checkoutWithGiftCode.useStripeCustomer(properties.getProperty("stripe.demoCustomer"));
        }
        return checkoutWithGiftCode;
    }

    @Test
    public void checkoutWithGiftCodeHappyPathWalkThroughTest() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(7645, true, true);
        int giftCodeShare = checkoutWithGiftCode.checkout().getLightrailPayment().getAmount();
        returnFundsToCode(giftCodeShare);

        checkoutWithGiftCode = createCheckoutObject(7645, true, true);

        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        int newGiftCodeShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(giftCodeShare, newGiftCodeShare);
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void checkoutWithGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        int giftCodeValue = getGiftCodeValue();
        int orderTotal = giftCodeValue - 1;
        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        assertEquals(0, paymentSummary.getStripePayment().getAmount());

        returnFundsToCode(paymentSummary.getLightrailPayment().getAmount());
    }

    @Test
    public void checkoutWithoutCreditCardInfoWhenNeededTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int giftCodeValue = getGiftCodeValue();

        int orderTotal = giftCodeValue + 1;
        try {
            createCheckoutObject(orderTotal, true, false).checkout();
        } catch (Exception e) {
            assertEquals(e.getCause().getClass().getName(), BadParameterException.class.getName());
        }
    }

    @Test
    public void checkoutWithGiftCodeNeedsCreditCardTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int giftCodeValue = getGiftCodeValue();

        int orderTotal = giftCodeValue - 1;

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        assert (!checkoutWithGiftCode.needsCreditCardPayment());

        int newOrderTotal = giftCodeValue + 1;
        checkoutWithGiftCode = createCheckoutObject(newOrderTotal, true, false);
        assert (checkoutWithGiftCode.needsCreditCardPayment());
    }

    @Test
    public void checkoutWithoutGiftCode() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(100, orderCurrency);
        try {
            checkoutWithGiftCode.checkout();
        } catch (Exception e) {
            assertEquals(e.getCause().getClass().getName(), BadParameterException.class.getName());
        }
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
        checkoutWithGiftCode = createCheckoutObject(100, false, true);
        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        assertEquals(0, paymentSummary.getLightrailPayment().getAmount());
    }

//    @Test
//    public void checkoutWithZeroedGiftCode() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
//        Properties properties = TestParams.getProperties();
//
//        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
//
//        int originalGiftValue = getGiftCodeValue();
//
//        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(originalGiftValue, true, false);
//        checkoutWithGiftCode.checkout();
//
//        int newGiftValue = getGiftCodeValue();
//        assertEquals(0, newGiftValue);
//
//        checkoutWithGiftCode = createCheckoutObject(100, true, false);
//
//        try {
//            checkoutWithGiftCode.checkout();
//        } catch (Exception e) {
//            assertEquals(e.getClass().getName(), InsufficientValueException.class.getName());
//        }
//
//        returnFundsToCode(originalGiftValue);
//    }

    @Test
    public void checkoutWithContactIdTest() throws IOException, ThirdPartyPaymentException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int orderTotal = 400;
        String orderCurrency = "USD";

        int customerCreditValue = 300;
        CustomerAccount customerAccount = CustomerAccount.create("test@test.ca",
                "Test",
                "McTest",
                orderCurrency,
                customerCreditValue);

        CheckoutWithStripeAndLightrail checkout = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        checkout.useLightrailCustomer(customerAccount.getId());
        checkout.useStripeToken(properties.getProperty("stripe.demoToken"));

        PaymentSummary paymentSummary = checkout.getPaymentSummary();
        int lightrailShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        paymentSummary = checkout.checkout();
        assertEquals(customerCreditValue, lightrailShare);

        int creditCardShare = paymentSummary.getStripePayment().getAmount();

        assertEquals(orderTotal, lightrailShare + creditCardShare);

        customerAccount.transact(lightrailShare);

        checkout = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        checkout.useLightrailCustomer(customerAccount.getId());
        checkout.useStripeCustomer(properties.getProperty("stripe.demoCustomer"));

        paymentSummary = checkout.getPaymentSummary();
        lightrailShare = paymentSummary.getLightrailPayment().getAmount();
        assertEquals(customerCreditValue, lightrailShare);

        paymentSummary = checkout.checkout();
        assertEquals(customerCreditValue, lightrailShare);

        creditCardShare = paymentSummary.getStripePayment().getAmount();

        assertEquals(orderTotal, lightrailShare + creditCardShare);
    }

    public void checkoutWithGiftCodeSample() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        //set up your api keys
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(7645, "USD")
                .useGiftCode(properties.getProperty("happyPath.code"))
                .useStripeToken(properties.getProperty("stripe.demoToken"));
        PaymentSummary paymentSummary = checkoutWithGiftCode.getPaymentSummary();

        //show this summary to the user and get them to confirm
        System.out.println(paymentSummary);

        paymentSummary = checkoutWithGiftCode.checkout();
        //show final summary to the user
        System.out.println(paymentSummary);
    }
}
