package com.bank.poalim.order_service.store;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.bank.poalim.order_service.model.OrderRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPendingOrderStore implements PendingOrderStore {
    
    private final ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate;
    
    @Override
    public void savePending(OrderRecord orderRecord) {
        String key = key(orderRecord.getOrderId());
        orderReactiveRedisTemplate.opsForValue()
                .set(key, orderRecord)
                .doOnSuccess(saved -> log.info("Saved pending order {}", orderRecord.getOrderId()))
                .doOnError(err -> log.error("Failed to save pending order {}", orderRecord.getOrderId(), err))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
        
    }
    
    private String key(String orderId) {
        return "order:" + orderId;
    }
}
