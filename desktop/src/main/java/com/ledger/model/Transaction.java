package com.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
    public int id;
    public double amount;
    public String kind;
    public String note;
    public LocalDateTime occurred_at;
    public LocalDateTime created_at;
    public Category category;

    public Transaction() {}
}
