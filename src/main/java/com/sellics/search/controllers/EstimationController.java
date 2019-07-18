package com.sellics.search.controllers;

import com.sellics.search.model.EstimationScore;
import com.sellics.search.services.SearchScoreEstimationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/estimate")
public class EstimationController {

    private SearchScoreEstimationService service;

    @Autowired
    public EstimationController(SearchScoreEstimationService service) {
        this.service = service;
    }

    @GetMapping
    public CompletableFuture<EstimationScore> estimateScore(@RequestParam String keyword) throws ExecutionException, InterruptedException {
        return service.estimate(keyword);
    }

    @ExceptionHandler({ExecutionException.class, InterruptedException.class})
    public ResponseEntity<Object> handleError(Exception ex) {
        return new ResponseEntity<>(
                ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
