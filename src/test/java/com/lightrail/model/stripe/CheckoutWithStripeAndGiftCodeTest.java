package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.Constants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.GiftFund;
import com.lightrail.model.business.GiftValue;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class CheckoutWithStripeAndGiftCodeTest {

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



    private CheckoutWithStripeAndGiftCode createCheckoutObject(int orderTotal, boolean addGiftCode, boolean addStripe)
            throws IOException, AuthorizationException, GiftCodeNotActiveException, CurrencyMismatchException, InsufficientValueException, ThirdPartyPaymentException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = new CheckoutWithStripeAndGiftCode(orderTotal, orderCurrency);
        if (addGiftCode) {
            checkoutWithGiftCode.useGiftCode(properties.getProperty("happyPath.code"));
        }
        if (addStripe) {
            int randomNum = ThreadLocalRandom.current().nextInt(0, 2);
            //System.out.println("rand: " + randomNum);
            if (randomNum > 0)
                checkoutWithGiftCode.useStripeToken(properties.getProperty("stripe.demoToken"));
            else
                checkoutWithGiftCode.useStripeCustomer(properties.getProperty("stripe.demoCustomer"));

        }
        return checkoutWithGiftCode;
    }

    @Test
    public void checkoutWithGiftCodeHappyPathWalkThroughTest() throws IOException, GiftCodeNotActiveException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = createCheckoutObject(7645, true, true);
        int giftCodeShare = checkoutWithGiftCode.checkout().getGiftCodeAmount();
        returnFundsToCode(giftCodeShare);

        checkoutWithGiftCode = createCheckoutObject(7645, true, true);

        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        int newGiftCodeShare = paymentSummary.getGiftCodeAmount();
        assertEquals(giftCodeShare, newGiftCodeShare);
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void checkoutWithGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        int giftCodeValue = getGiftCodeValue();
        int orderTotal = giftCodeValue - 1;
        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        assertEquals(0, paymentSummary.getCreditCardAmount());

        returnFundsToCode(paymentSummary.getGiftCodeAmount());
    }

    @Test
    public void checkoutWithoutCreditCardInfoWhenNeededTest() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
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
    public void checkoutWithGiftCodeNeedsCreditCardTest() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int giftCodeValue = getGiftCodeValue();

        int orderTotal = giftCodeValue - 1;

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        assert (!checkoutWithGiftCode.needsCreditCardPayment());

        int newOrderTotal = giftCodeValue + 1;
        checkoutWithGiftCode = createCheckoutObject(newOrderTotal, true, false);
        assert (checkoutWithGiftCode.needsCreditCardPayment());
    }

    @Test
    public void checkoutWithoutGiftCode() throws IOException, AuthorizationException, CurrencyMismatchException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = new CheckoutWithStripeAndGiftCode(100, orderCurrency);
        try {
            checkoutWithGiftCode.checkout();
        } catch (Exception e) {
            assertEquals(e.getCause().getClass().getName(), BadParameterException.class.getName());
        }
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
        checkoutWithGiftCode = createCheckoutObject(100, false, true);
        PaymentSummary paymentSummary = checkoutWithGiftCode.checkout();
        assertEquals(0, paymentSummary.getGiftCodeAmount());
    }

    @Test
    public void checkoutWithZeroedGiftCode() throws IOException, CurrencyMismatchException, AuthorizationException, GiftCodeNotActiveException, InsufficientValueException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int originalGiftValue = getGiftCodeValue();

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = createCheckoutObject(originalGiftValue, true, false);
        checkoutWithGiftCode.checkout();

        int newGiftValue = getGiftCodeValue();
        assertEquals(0, newGiftValue);

        checkoutWithGiftCode = createCheckoutObject(100, true, false);

        try {
            checkoutWithGiftCode.checkout();
        } catch (Exception e) {
            assertEquals(e.getClass().getName(), InsufficientValueException.class.getName());
        }

        returnFundsToCode(originalGiftValue);
    }

    public void checkoutWithGiftCodeSample() throws IOException, GiftCodeNotActiveException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, ThirdPartyPaymentException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        //set up your api keys
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        CheckoutWithStripeAndGiftCode checkoutWithGiftCode = new CheckoutWithStripeAndGiftCode(7645, "USD")
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
