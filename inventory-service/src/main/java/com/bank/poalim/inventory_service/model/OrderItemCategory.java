package com.bank.poalim.inventory_service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderItemCategory {
    STANDARD,
    PERISHABLE,
    DIGITAL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static OrderItemCategory fromJson(String value) {
        if (value == null) {
            return null;
        }
        return OrderItemCategory.valueOf(value.trim().toUpperCase());
    }
}
