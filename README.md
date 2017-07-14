# Lightrail E-Commerce Library (java)

Lightrail is a modern platform for digital account credits, gift cards, promotions, and points.
(To learn more, visit [Lightrail](https://www.lightrail.com/)).
Lightrail E-Commerce Library provides a client library for developers to easily integrate their 
e-commerce applications with the Lightrail API. 

## Features ##
- Gift code balance check, charge on gift code, and fund a gift card.
- Easy order checkout using a gift code and possibly a credit card.
- Easy and intuitive integration with [Stripe](https://stripe.com/) including a 
consistent interface familiar to Stripe developers.

## Usage ##


## Installation ##
### Maven
You can add this library as a dependency in your `maven` `POM` file as:
```xml
<dependency>
  <groupId>com.lightrail</groupId>
  <artifactId>lightrail-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Build And Test ##
You can build  this library from source using `maven`. Assuming that `maven` is installed and configured in your 
environment, you can simply download or clone the source and invoke:
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
- `stripe.demoCustomer`: a sample Stripe customer ID. To learn how to create a demo customer using your 
API key and a demo token, check out Stripe documentation.  

## Requirements ## 
This library requires `Java 1.7` or later.

## Dependencies ##

This library has two dependencies as the following. If your project already depends on a different version of any of these libraries, 
make sure the versions are compatible. We will be committed to updating these dependencies to the latest version at each 
release.
```xml
<dependency>
  <groupId>com.stripe</groupId>
  <artifactId>stripe-java</artifactId>
  <version>5.6.0</version>
</dependency>
```
Note that the Stripe library in turn depends on 
The following dependecy is also necessary for running the unit tests but is not needed otherwise.
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
- Basic API functions: Gift code balance check, charge on gift code, and fund a gift card.
- `CheckoutWithGiftCode` class for easy order checkout alongside `Stripe`.

