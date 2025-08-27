package com.bank.poalim.notification_service.event;

import java.time.Instant;
import java.util.List;

import com.bank.poalim.notification_service.model.MissingItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCheckResultEvent {
    
    private String orderId;
    private List<MissingItem> missingItems;
    private Boolean approved;
    @Builder.Default
    private String eventType = "INVENTORY_CHECK_RESULT";
    @Builder.Default
    private Instant eventTimestamp = Instant.now();
    
}


