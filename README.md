# Lightrail Stripe Integration Library

Lightrail is a modern platform for digital account credits, gift cards, promotions, and points.
(To learn more, visit [Lightrail](https://www.lightrail.com/)). Lightrail Stripe integration provides a client library for developers to easily use the Lightrail capabilities alongside [Stripe](https://stripe.com/). 

If you are looking for other specific use cases or other languages, check out [related projects](#related-projects). 

## Features ##
- Simple order checkout which supports a gift code redemption alongside a Stripe payment.

## Usage ##

### Order Checkout Using `StripeGiftHybridCharge`

`StripeGiftHybridCharge` is a class closely designed to resemble the behaviour of a Stripe `Charge` and transparently splits the transaction between a gift code and a stripe credit card. Here is a simple example:

```java
Lightrail.apiKey = "<your lightrail API key>";
Stripe.apiKey = "<your stripe API key>";

Map<String, Object> hybridChargeParams = new HashMap<>();
  hybridChargeParams.put("code", "<GIFT CODE>");
  hybridChargeParams.put("currency","USD"));
  hybridChargeParams.put("amount", 375);
  hybridChargeParams.put("token", "<STRIPE TOKEN>");

PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
//show summary to the user and confirm
//...
StripeGiftHybridCharge charge = StripeGiftHybridCharge.create(hybridChargeParams);
```

If you don't pass a gift code parameter, the entire transaction will be charged on the credit card via Stripe. Similarly, if you don't pass a Stripe parameter, the library will attempt at charging the entire transaction on the gift code. The transaction will still go through if the value of the gift code is enough to cover paying for the entire transaction, otherwise you will receive a `BadParameterException` with a message asking for providing credit card payment parameters. 

Instead of a Stripe token, you may also pass a Stripe customer ID in which case the charge will be posted to that customer: 

```Java
hybridChargeParams.put("customer", "<STRIPE CUSTOMER ID>");
```

When both a code and credit card parameters are provided, the library will try to split the payment between the two, in a way that the gift code value contributes to the payment as much as possible. This usually means:

- If the gift code value is sufficient, the entire transaction will be charged on the gift code.


- If the transaction amount is larger than the value of the gift code, entire value of the gift code will be  redeemed and the remainder goes on the credit card —unless the remainder is too small for a Stripe transaction in which case the split point is shifted just enough for the credit card's share of the transaction to meet the minimum requirements.

The `simulate()` method returns a `PaymentSummary` object which demonstrates the intended plan for splitting the transaction between the gift code and the credit card. This provides a good way of showing the summary of the payment to the user to confirm. It can also be used for checking whether the gift code value will cover the entire transaction and determine whether you need to provide a Stripe payment parameter:

```java
//set up hybridChargeParams
PaymentSummary paymentSummary = StripeGiftHybridCharge.simulate(hybridChargeParams);
int creditCardShare = paymentSummary.getCreditCardAmount();
if (creditCardShare >0) {
  //obtain a Stripe token
  hybridChargeParams.put("token", "<STRIPE TOKEN>");
}
StripeGiftHybridCharge charge = StripeGiftHybridCharge.create(hybridChargeParams);
```

### Order Checkout Using `CheckoutWithStripeAndGiftCode`

This class provides a wrapper around `StripeGiftHybridCharge` with a more straightforward interface for developers who are not used to Stripe's Java library. 

```java
Lightrail.apiKey = properties.getProperty("lightrail.testApiKey");
Stripe.apiKey = properties.getProperty("stripe.testApiKey");

CheckoutWithStripeAndGiftCode checkoutWithGiftCode = new CheckoutWithStripeAndGiftCode(375, "USD")
                .useGiftCode("<GIFT CODE>")
                .useStripeToken("<STRIPE TOKEN>");
PaymentSummary paymentSummary = checkoutWithGiftCode.getPaymentSummary();
//show summary to the user and confirm
//...
paymentSummary = checkoutWithGiftCode.checkout();
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
  <version>1.0.0</version>
</dependency>
```

## Build And Test ##
You can build  this library from source using `maven`. Assuming that `maven` is installed and configured in your environment, you can simply download or clone the source and invoke:
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
  <version>1.0.0</version>
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

### 1.0.0 ###
- `StripeGiftHybridCharge` and `CheckoutWithGiftCode` class for easy order checkout alongside `Stripe`.

