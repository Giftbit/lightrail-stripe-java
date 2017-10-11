# Lightrail Stripe Integration Library

Lightrail is a modern platform for digital account credits, gift cards, promotions, and points —to learn more, visit [Lightrail](https://www.lightrail.com/). The Lightrail Stripe integration provides a client library for developers to easily use Lightrail alongside [Stripe](https://stripe.com/). This library is based on the [Lightrail Java Client Library](https://github.com/Giftbit/lightrail-client-java).

If you are looking for specific use-cases or other languages, check out the *Integrations* section of the [Lightrail API documentation](https://www.lightrail.com/docs/).

## Features ##
- Simple order checkout which supports Lightrail gift card redemption or account credits alongside a Stripe payment.

## Usage ##

### Order Checkout Using `StripeLightrailSplitTenderCharge`

The`StripeLightrailSplitTenderCharge` class resembles the interface of a Stripe `Charge` class and transparently splits the transaction between Lightrail and Stripe. The Lightrail parameter could be one of the following:

- `code`, specifying a Gift Card by its `fullcode`, 

- `cardId`, specifying a Gift Card by its `cardId`, or

- `contact`, specifying a Contact by its `contactId`. This will eventually be translated as the `cardId` of the corresponding Account Card for the transaction currency. 

The Stripe parameter could be:

- `source`, indicating a Stripe token, or 
- `customer`, indicating a Stripe customer ID. 

Here is a simple example:

```java
//this is your order
int orderTotal = 7505;
String orderCurrency = "USD";

//set up your API keys
Stripe.apiKey = "...";
Lightrail.apiKey = "...";

//get the stripe token and the gift code
String stripeToken = "...";
String giftCode = "...";

Map<String, Object> params = new HashMap<String, Object>();
params.put("amount", orderTotal);
params.put("currency", orderCurrency);
params.put("source", stripeToken);
params.put("code", giftCode);

SimulatedStripeLightrailSplitTenderCharge simulatedCharge = StripeLightrailSplitTenderCharge.simulate(params);

int creditCardShare = simulatedCharge.getStripeShare();
int giftCodeShare = simulatedCharge.getLightrailShare();
System.out.println(
  String.format("Will charge %s%s on your gift card and %s%s on your credit card..",
                orderCurrency, 
                giftCodeShare, 
                orderCurrency, 
                creditCardShare));

StripeLightrailSplitTenderCharge committedCharge = simulatedCharge.commit();
```

If you do not provide any Lightrail parameters, the entire transaction will be charged to Stripe. Similarly, if you do not provide any Stripe parameters, the library will attempt to charge the entire transaction to Lightrail. In that case, if the value of the Card is not enough to cover the entire transaction amount, you will receive a `BadParameterException` asking you to provide a Stripe parameter.

When both of Lightrail and Stripe parameters are provided, the library will try to split the payment, in such a way that Lightrail contributes to the payment to the maximum possible extent. This usually means:

- If the Lightrail value is sufficient, the entire transaction will be charged to the Lightrail Card.


- If the transaction amount is larger than the available Lightrail value, the remainder will be charged to Stripe (except when the remainder is too small for a Stripe transaction, in which case the split point is shifted just enough for the Stripe share of the transaction to meet the minimum requirements).

The `simulate()` method returns a `SimulatedStripeLightrailSplitTenderCharge` object which demonstrates the intended plan for splitting the transaction between Lightrail and Stripe. You can use this object for showing the summary of the payment to the user to confirm. After the user confirms the payment breakdown, call the `commit` method to finalize the transaction.

### Order Checkout Using `CheckoutWithStripeAndLightrail`

This class provides a wrapper around `StripeLightrailSplitTenderCharge` with a more straightforward interface for developers who are not familiar with Stripe's Java library. 

```java
//this is your order
int orderTotal = 7505;
String orderCurrency = "USD";

//set up your API keys
Stripe.apiKey = "...";
Lightrail.apiKey = "...";

//get the stripe token and the gift code
String stripeToken = "...";
String giftCode = "...";
CheckoutWithStripeAndLightrail checkoutWithGiftCode = 
                               new CheckoutWithStripeAndLightrail(orderTotal, orderCurrency);
checkoutWithGiftCode.useLightrailGiftCode(giftCode);
//or: checkoutWithGiftCode.useLightrailCardId("...");
//or: checkoutWithGiftCode.useLightrailContact("...");
checkoutWithGiftCode.useStripeToken(stripeToken);
//or: checkoutWithGiftCode.useStripeCustomer("...");

SimulatedStripeLightrailSplitTenderCharge simulatedCharge = checkoutWithGiftCode.simulate();
int creditCardShare = simulatedCharge.getStripeShare();
int giftCodeShare = simulatedCharge.getLightrailShare();

System.out.println(
  String.format("Will charge %s%s on your gift card and %s%s on your credit card..", 
                orderCurrency,
                giftCodeShare,
                orderCurrency,
                creditCardShare));

StripeLightrailSplitTenderCharge committedCharge = simulatedCharge.commit();
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
  <version>2.0.0</version>
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

The only dependency of this library are `lightrail-client` and `stripe-java`. 

```xml
<dependency>
  <groupId>com.lightrail</groupId>
  <artifactId>lightrail-client</artifactId>
  <version>2.0.0</version>
</dependency>
<dependency>
  <groupId>com.stripe</groupId>
  <artifactId>stripe-java</artifactId>
  <version>5.6.0</version>
</dependency>
```
If your project already depends on a different version of the Stripe library, make sure the versions are compatible. We will ensure to periodically update the dependencies to the latest version. 

The following dependency is also necessary if you want to run the unit tests.

```xml
<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
  <version>4.12</version>
  <scope>test</scope>
</dependency>
```

## Changelog ## 

### 2.0.0

- Fully upgraded based on the new Lightrail Java library 2.0.0 and supports simulated split-tender transactions.

### 1.1.0

- Support for using account credits in hybrid charges and checkout.
- Renamed the main classes to reflect that they are more general now and support account credits as well.

### 1.0.1 ###

- `StripeGiftHybridCharge` and `CheckoutWithGiftCode` class for easy order checkout alongside `Stripe`.

