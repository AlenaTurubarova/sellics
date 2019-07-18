package com.sellics.search.restclients.impl;

import com.sellics.search.restclients.EstimationRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AmazonEstimationRestClient implements EstimationRestClient {

    private final WebClient webClient;
    private static final String BASE_URL = "https://completion.amazon.com";
    private static final String COMPLETION_URI = "/search/complete";
    private static final Pattern AMAZON_RESPONSE_REGEX = Pattern.compile(",\\[([^]]+)\\],");

    @Autowired
    public AmazonEstimationRestClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * The method calls Amazon completion API method and parses returned string.
     * @param keyword
     * @return the {@link List<String>} that contains strings matched to keyword
     */
    @Override
    public List<String> searchResult(String keyword) {
        String amazonResponse = callAmazonCompletionAPI(keyword);
        return parseResponse(amazonResponse);
    }

    /**
     * The method calls Amazon completion API.
     * URI Builder is configured to call "https://completion.amazon.com/search/complete" with parameters:
     * search-alias = aps, represents the Departure on Amazon. 'aps' means serach in all departments.
     * client = amazon-search-ui
     * mkt = 1 - this parameter is not documented. Do not delete.
     * q = {@code keyword}
     *
     * @param keyword
     * @return the {@link String} Amazon completion API response
     */
    private String callAmazonCompletionAPI(String keyword){
        return webClient.get().uri(
                uriBuilder -> uriBuilder
                        .path(COMPLETION_URI)
                        .queryParam("search-alias", "aps")
                        .queryParam("client", "amazon-search-ui")
                        .queryParam("mkt", "1")
                        .queryParam("q", keyword)
                        .build()
        ).retrieve().bodyToMono(String.class).block();
    }

    /**
     * The method parses Amazon completion API response to get only 10 matched strings.
     *
     * @param response
     * @return the {@link List<String>} Amazon completion API response split to matched strings
     */
    private List<String> parseResponse(String response){
        Matcher matcher = AMAZON_RESPONSE_REGEX.matcher(response);
        List<String> resultExpressions = new ArrayList<>();
        if(matcher.find()){
            String replaced = matcher.group(1);
            replaced = replaced.substring(1,replaced.length()-1);
            resultExpressions = Arrays.asList(replaced.split("\",\""));
        }
        return resultExpressions;
    }

}
