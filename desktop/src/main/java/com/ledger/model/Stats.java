package com.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Stats {
    public double income;
    public double expense;
    public double balance;
    public int count;
    public List<CategoryStat> by_category;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryStat {
        public int category_id;
        public String category_name;
        public String kind;
        public double total;
        public int count;
    }
}
