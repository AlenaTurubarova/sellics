package com.sellics.search.services;

import com.sellics.search.model.EstimationScore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface SearchScoreEstimationService {

    CompletableFuture<EstimationScore> estimate(String keyword) throws ExecutionException, InterruptedException;
}
