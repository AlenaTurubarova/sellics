# Sellics Coding Challenge

This is a REST API for a keyword search rate estimation on the Amazon platform. 
The API counts the search rate based on the Amazon Completion API. 
The keyword score represents only estimated rate, it does not guaranty exactly the same rate in realtime.

## Requirements

For building and running the application you need:

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)

## Running the application locally

There are several ways to run a Spring Boot application on your local machine. One way is to execute the `main` method in the `com.sellics.search.SearchApplication` class from your IDE.

Alternatively you can use the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html) like so:

```shell
mvn spring-boot:run
```

By default application is running locally on http://localhost:8080

## REST Endpoint

The application is accessible through REST API on the endpoint http://localhost:8080/estimate?keyword=<your_keyword>
In the GET request keyword parameter must be specified. The keyword can be single word or a list of words concatenated by +.
For example:
```shell
http://localhost:8080/estimate?keyword=iphone
http://localhost:8080/estimate?keyword=iphone+charger
```

API returns a JSON in the format:

```shell
{
    "keyword":"iphone charger",
    "score":63
}
```

## Backend Alghorithm

First of all Amazon API is called once with the input keyword parameter.
It returns 10 or less exactly matched strings.
All unique words are parsed from the set, except the keyword.
New keywords for the next Amazon API calls are formed.
The forming logic is as follows:
```shell
  uniqueString + " " + keyword
  keyword + " " + uniqueString
```
(uniqueString + " " + keyword) - this is the main keywords formula, because Amazon starts search by the beggining of the phrase. And the possibility to get new unique results is higer.

(keyword + " " + uniqueString) - this formula was added to get wider sample set.
The more popular input keyword is, the more new keywords will be formed.
It depends on the number of firstly returned set of strings and the number of different words in the searched requests.
After this parallel calls are made to the Amazon API for all newly formed key phrases.
All unique results are bind to one set. 
And the final score is computed by the formula:
```shell
number_of_all_matched_strings/number_of_all_calls_to_amazon*AMAZON_MAX_PHRASES_IN_RESPONSE_COUNT
```
All parameters dependent from the keyword are included into the formula. So this makes it normilazed for the general case.
This is why result score should be pretty accurate.

## Assumptions
1. API takes into account only result strings with the exact match to the keyword.
For example, for the keywrod = 'camera' and the result strings set 'iphone camera, photocamera, camera for photos' only the following strings will be included in the result estimation scope:
'iphone camera, camera for photos'
2. It is assumed that maximum number of calls to the Amazon in parallel should be around 100. 
On the tested localhost SLA 10 sek was pretty enough to make 100 calls.
That is why no timeouts were specified for the integration calls.
3. There was a hint: the order of the 10 returned keywords is comparatively insignificant!
It is assumed that the hint is correct and the order of the Amazon returned strings was not included to the alghorithm.
