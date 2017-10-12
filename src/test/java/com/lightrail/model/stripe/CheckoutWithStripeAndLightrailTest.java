package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.lightrail.helpers.StripeConstants;
import com.lightrail.helpers.TestParams;
import com.lightrail.model.Lightrail;
import com.lightrail.model.business.LightrailContact;
import com.stripe.Stripe;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static com.lightrail.helpers.TestParams.getGiftCodeValue;
import static org.junit.Assert.assertEquals;

public class CheckoutWithStripeAndLightrailTest {

    private LightrailFund returnFundsToCode(int amount) throws IOException, AuthorizationException, CouldNotFindObjectException {
        Map<String, Object> giftFundParams = TestParams.readCardParamsFromProperties();
        giftFundParams.put(StripeConstants.Parameters.AMOUNT, amount);

        return LightrailFund.create(giftFundParams);
    }


    private CheckoutWithStripeAndLightrail createCheckoutObject(int orderTotal, boolean addGiftCode, boolean addStripe)
            throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        if (addGiftCode) {
            checkoutWithGiftCode.useLightrailGiftCode(properties.getProperty("happyPath.code"));
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
    public void checkoutWithGiftCodeHappyPathWalkThroughTest() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, ThirdPartyException {

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(7645, true, true);
        int giftCodeShare = checkoutWithGiftCode.checkout().getLightrailShare();
        returnFundsToCode(giftCodeShare);

        checkoutWithGiftCode = createCheckoutObject(7645, true, true);

        StripeLightrailSplitTenderCharge charge = checkoutWithGiftCode.checkout();
        int newGiftCodeShare = charge.getLightrailShare();
        assertEquals(giftCodeShare, newGiftCodeShare);
        returnFundsToCode(giftCodeShare);
    }

    @Test
    public void checkoutWithGiftCodeOnlyTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        int giftCodeValue = getGiftCodeValue();
        int orderTotal = giftCodeValue - 1;
        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        StripeLightrailSplitTenderCharge charge = checkoutWithGiftCode.checkout();
        assertEquals(0, charge.getStripeShare());

        returnFundsToCode(charge.getLightrailShare());
    }

    @Test
    public void checkoutWithoutCreditCardInfoWhenNeededTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int giftCodeValue = getGiftCodeValue();

        int orderTotal = giftCodeValue + 1;
        try {
            createCheckoutObject(orderTotal, true, false).checkout();
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getClass().getName());
        }
    }

    @Test
    public void checkoutWithGiftCodeNeedsCreditCardTest() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int giftCodeValue = getGiftCodeValue();

        if (giftCodeValue == 0)
            throw new BadParameterException("Gift card value too small for tests.");

        int orderTotal = giftCodeValue - 1;

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(orderTotal, true, false);

        assert (!checkoutWithGiftCode.needsCreditCardPayment());

        int newOrderTotal = giftCodeValue + 1;
        checkoutWithGiftCode = createCheckoutObject(newOrderTotal, true, false);
        assert (checkoutWithGiftCode.needsCreditCardPayment());
    }

    @Test
    public void checkoutWithoutGiftCode() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        String orderCurrency = properties.getProperty("happyPath.code.currency");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(100, orderCurrency);
        try {
            checkoutWithGiftCode.checkout();
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getClass().getName());
        }
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");
        checkoutWithGiftCode = createCheckoutObject(100, false, true);
        StripeLightrailSplitTenderCharge charge = checkoutWithGiftCode.checkout();
        assertEquals(0, charge.getLightrailShare());
    }

    @Test
    public void checkoutWithZeroedGiftCode() throws IOException, CurrencyMismatchException, AuthorizationException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");

        int originalGiftValue = getGiftCodeValue();

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = createCheckoutObject(originalGiftValue, true, false);
        checkoutWithGiftCode.checkout();

        int newGiftValue = getGiftCodeValue();
        assertEquals(0, newGiftValue);

        checkoutWithGiftCode = createCheckoutObject(100, true, false);

        try {

            StripeLightrailSplitTenderCharge checkout = checkoutWithGiftCode.checkout();
        } catch (Exception e) {
            assertEquals(BadParameterException.class.getName(), e.getClass().getName());
        } finally {
            returnFundsToCode(originalGiftValue);
        }
    }

    @Test
    public void checkoutWithContactIdTest() throws IOException, AuthorizationException, CurrencyMismatchException, InsufficientValueException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        int orderTotal = 400;
        String orderCurrency = "USD";

        int customerCreditValue = 300;
        LightrailContact customerAccount = LightrailContact.create("test@test.ca",
                "Test",
                "McTest",
                orderCurrency,
                customerCreditValue);

        CheckoutWithStripeAndLightrail checkout = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        checkout.useLightrailContact(customerAccount.getContactId());
        checkout.useStripeToken(properties.getProperty("stripe.demoToken"));

        StripeLightrailSplitTenderCharge simulatedTx = checkout.simulate();
        int lightrailShare = simulatedTx.getLightrailShare();
        assertEquals(customerCreditValue, lightrailShare);

        StripeLightrailSplitTenderCharge charge = checkout.checkout();
        assertEquals(customerCreditValue, lightrailShare);

        int creditCardShare = charge.getStripeShare();

        assertEquals(orderTotal, lightrailShare + creditCardShare);

        customerAccount.createTransaction(lightrailShare);

        checkout = new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
        checkout.useLightrailContact(customerAccount.getContactId());
        checkout.useStripeCustomer(properties.getProperty("stripe.demoCustomer"));

        simulatedTx = checkout.simulate();
        lightrailShare = simulatedTx.getLightrailShare();
        assertEquals(customerCreditValue, lightrailShare);

        charge = checkout.checkout();
        creditCardShare = charge.getStripeShare();

        assertEquals(orderTotal, lightrailShare + creditCardShare);
    }

    public void checkoutWithGiftCodeSample() throws IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException, CouldNotFindObjectException, ThirdPartyException {
        Properties properties = TestParams.getProperties();

        //set up your api keys
        Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
        Stripe.apiKey = properties.getProperty("stripe.testApiKey");

        CheckoutWithStripeAndLightrail checkoutWithGiftCode = new CheckoutWithStripeAndLightrail(7645, "USD")
                .useLightrailGiftCode(properties.getProperty("happyPath.code"))
                .useStripeToken(properties.getProperty("stripe.demoToken"));
        StripeLightrailSplitTenderCharge simulatedCharge = checkoutWithGiftCode.simulate();

        //show this summary to the user and get them to confirm
        System.out.println(simulatedCharge.getSummary());
        if (simulatedCharge instanceof SimulatedStripeLightrailSplitTenderCharge) {
            StripeLightrailSplitTenderCharge charge = ((SimulatedStripeLightrailSplitTenderCharge) simulatedCharge).commit();
            //show final summary to the user
            System.out.println(charge.getSummary());
        }
    }
}
