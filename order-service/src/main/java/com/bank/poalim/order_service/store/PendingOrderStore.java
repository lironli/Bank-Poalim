package com.bank.poalim.order_service.store;

import com.bank.poalim.order_service.model.OrderRecord;

public interface PendingOrderStore {
    void savePending(OrderRecord orderRecord);
}
