package com.sellics.search.services.impl;

import com.sellics.search.model.EstimationScore;
import com.sellics.search.restclients.EstimationRestClient;
import com.sellics.search.services.SearchScoreEstimationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class SearchScoreEstimationServiceImpl implements SearchScoreEstimationService {

    private static final int AMAZON_MAX_PHRASES_IN_RESPONSE_COUNT = 10;
    private EstimationRestClient restClient;

    @Autowired
    public SearchScoreEstimationServiceImpl(EstimationRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * This is the main estimation method that returns score of the keyword.
     * It performs call to Amazon API to get initial matched strings set.
     * Based on the unique words from initial set new keywords set is formed.
     * {@link CompletableFuture} list is prepared to perform calls to Amazon API for each keyword from the new word set.
     * After completion of all Amazon API calls within {@link CompletableFuture} new unique matched strings are added to
     * initial matched strings set, and the keyword score is counted by the formula:
     * number_of_all_matched_strings/number_of_all_calls_to_amazon*AMAZON_MAX_PHRASES_IN_RESPONSE_COUNT
     * @param keyword
     * @return the {@link EstimationScore} in the {@link CompletableFuture} entity that contains input keyword and keyword score
     * @throws ExecutionException, InterruptedException
     */
    @Override
    public CompletableFuture<EstimationScore> estimate(String keyword) throws ExecutionException, InterruptedException {
        Set<String> initialMatchedStrings = getMatchedStringsFromAmazon(keyword);
        Set<String> nextSearchKeywords = getNextSearchKeywords(initialMatchedStrings, keyword);

        List<CompletableFuture<Set<String>>> listFutures = nextSearchKeywords.stream()
                .map(str -> CompletableFuture.supplyAsync(() -> getMatchedStringsFromAmazon(str)))
                .collect(Collectors.toList());

        CompletableFuture<Void> waitForAll = CompletableFuture.allOf(listFutures.toArray(new CompletableFuture[0]));
        CompletableFuture<Set<String>> result = waitForAll.thenApply(v ->
                listFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet()));
        return result.thenApply(resultPhrases->{
            initialMatchedStrings.addAll(resultPhrases);
            int score = (int) ((double) initialMatchedStrings.size() / (nextSearchKeywords.size() + 1) * AMAZON_MAX_PHRASES_IN_RESPONSE_COUNT);
            return new EstimationScore(keyword, score);
        });

    }

    /**
     * The method forms new set of keywords for additional calls to Amazon API.
     * Unique words are extracted from the set of strings, except keyword.
     * New key strings are build buy combining unique words with keyword.
     * @param initialMatchedStrings, keyword
     * @return the {@link Set<String>} that contains unique words from the initial Amazon API call, except a keyword
     */
    private Set<String> getNextSearchKeywords(Set<String> initialMatchedStrings, String keyword) {
        Set<String> additionalKeys = initialMatchedStrings.stream()
                .flatMap(str -> extractNewKeywords(str, keyword).stream())
                .collect(Collectors.toSet());
        Set<String> newKeywords = new HashSet<>();
        additionalKeys.forEach(str -> {
            newKeywords.add(str + " " + keyword);
            newKeywords.add(keyword + " " + str);
        });
        return newKeywords;
    }

    /**
     * The method makes call to {@link EstimationRestClient}
     * Returned list of matched strings is filtered.
     * Only strings that contain exact keyword are included to the result Set.
     * @param keyword
     * @return the {@link Set<String>} that contains only strings where exact keyword is included
     */
    private Set<String> getMatchedStringsFromAmazon(String keyword) {
        List<String> initialSearchResult = restClient.searchResult(keyword);
        return initialSearchResult.stream()
                .filter(str -> ifStringContainExactKeyword(str, keyword))
                .collect(Collectors.toSet());
    }

    /**
     * The method returns true if string contains exact keyword string.
     * @param input, keyword
     * @return the boolean
     */
    private boolean ifStringContainExactKeyword(String input, String keyword) {
        return input.contains(keyword + " ") || input.contains(" " + keyword) || input.equals(keyword);
    }

    /**
     * The method splits input string by whitespaces and returns list of words that not equal to keyword.
     * @param input, keyword
     * @return the {@link List<String>} that contains words except keyword
     */
    private List<String> extractNewKeywords(String input, String keyword) {
        return Arrays.stream(input.split(" "))
                .filter(str -> !str.equals(keyword)).collect(Collectors.toList());
    }

}
