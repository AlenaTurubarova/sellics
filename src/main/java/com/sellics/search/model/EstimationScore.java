package com.sellics.search.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EstimationScore {

    private String keyword;
    private int score;
}
