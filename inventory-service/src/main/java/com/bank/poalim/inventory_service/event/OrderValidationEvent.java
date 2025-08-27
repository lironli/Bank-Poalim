package com.bank.poalim.inventory_service.event;

import java.time.Instant;
import java.util.List;

import com.bank.poalim.inventory_service.model.ValidationMissingItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationEvent {
    
    private String orderId;
    private List<ValidationMissingItem> missingItems;
    private Boolean approved;
    @Builder.Default
    private String eventType = "ORDER_VALIDATION";
    @Builder.Default
    private Instant eventTimestamp = Instant.now();
    
}


