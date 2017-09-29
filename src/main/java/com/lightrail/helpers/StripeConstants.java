package com.lightrail.helpers;

public class StripeConstants {
    public static final int STRIPE_MINIMUM_TRANSACTION_VALUE = 50;
    public static final class Parameters {
        public static final String AMOUNT = "amount";
        public static final String CURRENCY = "currency";
        public static final String TOKEN = "source";
        public static final String CUSTOMER = "customer";
        public static final String CAPTURE = "capture";
        public static final String METADATA = "metadata";
    }
}
