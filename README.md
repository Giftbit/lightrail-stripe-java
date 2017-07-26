# Lightrail Stripe Integration Library

Lightrail is a modern platform for digital account credits, gift cards, promotions, and points.
(To learn more, visit [Lightrail](https://www.lightrail.com/)). The Lightrail Stripe integration provides a client library for developers to easily use Lightrail's features alongside [Stripe](https://stripe.com/). This library is based on [Lightrail Java Client Library](https://github.com/Giftbit/lightrail-client-java).

If you are looking for specific use cases or other languages, check out [related projects](https://github.com/Giftbit/lightrail-client-java#related-projects). For a complete list of all Lightrail libraries and integrations, check out the [Lightrail Integration page](https://github.com/Giftbit/Lightrail-API-Docs/blob/usecases/Integrations.md).

## Features ##
- Simple order checkout which supports Lightreail gift card redemption or account credits alongside a Stripe payment.

## Usage ##

### Order Checkout Using `StripeLightrailHybridCharge`

`StripeLightrailHybridCharge` is a class designed to resemble the interface of a Stripe `Charge` class which transparently splits the transaction between Lightrail and Stripe. The Lightrail parameter could be one of the following:

- `code`, specifying a gift card by its code, 

- `cardId`, specifying a gift card by its card ID, or

- `lightrailCustomer`, specifying a customer account by its customer account ID. 

The Stripe parameter could be:

- `token`, indicating a Stripe token, or 
- `customer`, indicating a Stripe customer ID. 

Here is a simple example:

```java
Lightrail.apiKey = "<your lightrail API key>";
Stripe.apiKey = "<your stripe API key>";

Map<String, Object> hybridChargeParams = new HashMap<>();
  hybridChargeParams.put("code", "<GIFT CODE>");
  hybridChargeParams.put("currency","USD"));
  hybridChargeParams.put("amount", 375);
  hybridChargeParams.put("token", "<STRIPE TOKEN>");

PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
//show summary to the user and confirm
//...
StripeGiftHybridCharge charge = StripeLightrailHybridCharge.create(hybridChargeParams);
```

If you don't pass any Lightrail  parameter, the entire transaction will be charged to Stripe. Similarly, if you don't provide any Stripe parameter, the library will attempt to charge the entire transaction to Lightrail. If the value of the gift card or account credit is not enough to cover the entire transaction amount, you will receive a `BadParameterException` asking you to provide a Stripe parameter.

When both a Lightrail and Stripe credit card parameters are provided, the library will try to split the payment, in such a way that Lightrail contributes to the payment as much as possible. This usually means:

- If the Lightrail value is sufficient, the entire transaction will be charged on the gift card or account credit.


- If the transaction amount is larger than the Lightrail value, the remainder will be charged to Stripe — unless the remainder is too small for a Stripe transaction in which case the split point is shifted just enough for the Stripe share of the transaction to meet the minimum requirements.

The `simulate()` method returns a `PaymentSummary` object which demonstrates the intended plan for splitting the transaction between Lightrail and Stripe. You can use this for showing the summary of the payment to the user to confirm. You can also use this for checking whether the Lightrail value is enough to cover the entire transaction and determine whether you need to provide a Stripe payment parameter:

```java
//set up hybridChargeParams
PaymentSummary paymentSummary = StripeLightrailHybridCharge.simulate(hybridChargeParams);
int stripeShare = paymentSummary.getStripeAmount();
if (stripeShare >0) {
  //obtain a Stripe token
  hybridChargeParams.put("token", "<STRIPE TOKEN>");
}
StripeLightrailHybridCharge charge = StripeLightrailHybridCharge.create(hybridChargeParams);
```

### Order Checkout Using `CheckoutWithStripeAndLightrail`

This class provides a wrapper around `StripeLightrailHybridCharge` with a more straightforward interface for developers who are not familiar with Stripe's Java library. 

```java
Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
Stripe.apiKey = properties.getProperty("stripe.testApiKey");

CheckoutWithStripeAndLightrail checkout = new CheckoutWithStripeAndLightrail (375, "USD")
      .useGiftCode("<GIFT CODE>") 
      //or .useLightrailCustomer("<LIGHTRAIL CUSTOMER ACC ID>")
      .useStripeToken("<STRIPE TOKEN>"); 
      //or .useStripeCustomer("<STRIPE CUSTOMER IS>"") 
PaymentSummary paymentSummary = checkout.getPaymentSummary();
//show summary to the user and confirm
//...
paymentSummary = checkout.checkout();
```

## Related Projects

- [Lightrail Java Client](https://github.com/Giftbit/lightrail-client-java)


## Installation ##
### Maven
You can add this library as a dependency in your `maven` `POM` file as:
```xml
<dependency>
  <groupId>com.lightrail</groupId>
  <artifactId>lightrail-stripe-client</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Build And Test ##
You can build this library from source using `maven`. Assuming that `maven` is installed and configured in your environment, you can simply download or clone the source and invoke:
```sh
$ mvn clean package -DskipTests
```
Note that this will skip the unit tests. In order to run the tests, you will need to set the 
following parameters in the property file `_test-config.property`. A template 
is provided in `test-config-template.properties`:
- `lightrail.testApiKey`: the API key for Lightrail. You can find your test API key in your account at 
  [lightrail.com](lightrail.com). 
- `happyPath.code`: a gift code with at least $5 value.
- `happyPath.code.cardId`: the card ID corresponding to the above gift code.
- `happyPath.code.currency`: the currency for this code, preferably `USD`.
- `stripe.testApiKey`: your test API key for Stripe.
- `stripe.demoToken`: a sample test token for Stripe, e.g. `tok_visa`.
- `stripe.demoCustomer`: a sample Stripe customer ID. To learn how to create a demo customer using your API key and a demo token, check out Stripe documentation.  

## Requirements ## 
This library requires `Java 1.7` or later.

## Dependencies ##

The only dependecies of this library are `lightrail-client` and `stripe-java`. 

```xml
<dependency>
  <groupId>com.lightrail</groupId>
  <artifactId>lightrail-client</artifactId>
  <version>1.1.0</version>
</dependency>
<dependency>
  <groupId>com.stripe</groupId>
  <artifactId>stripe-java</artifactId>
  <version>5.6.0</version>
</dependency>
```
If your project already depends on a different version of the Stripe library, make sure the versions are compatible. We will ensure to periodically update the dependencies to the latest version. 

The following dependecy is also necessary if you want to run the unit tests.

```xml
<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
  <version>4.12</version>
  <scope>test</scope>
</dependency>
```

## Changelog ## 

### 1.1.0

- Support for using account credits in hybrid charges and checkout.
- Renamed the main classes to reflect that they are more general now and support account credits as well.

### 1.0.1 ###

- `StripeGiftHybridCharge` and `CheckoutWithGiftCode` class for easy order checkout alongside `Stripe`.

