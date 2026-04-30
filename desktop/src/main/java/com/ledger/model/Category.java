package com.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {
    public int id;
    public String name;
    public String kind;
    public String icon;

    public Category() {}

    public Category(int id, String name, String kind, String icon) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.icon = icon;
    }

    @Override
    public String toString() {
        String prefix = (icon == null || icon.isBlank()) ? "" : icon + " ";
        return prefix + name;
    }
}
