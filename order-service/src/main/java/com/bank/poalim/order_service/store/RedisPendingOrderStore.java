package com.bank.poalim.order_service.store;

import com.bank.poalim.order_service.model.OrderRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPendingOrderStore implements PendingOrderStore {
    
    private final ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate;
    
    @Override
    public void savePending(OrderRecord orderRecord, long ttlSeconds) {
        String key = key(orderRecord.getOrderId());
        orderReactiveRedisTemplate.opsForValue()
                .set(key, orderRecord, Duration.ofSeconds(ttlSeconds))
                .doOnSuccess(saved -> log.info("Saved pending order {} with TTL {}s", orderRecord.getOrderId(), ttlSeconds))
                .doOnError(err -> log.error("Failed to save pending order {}", orderRecord.getOrderId(), err))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
    
    private String key(String orderId) {
        return "order:pending:" + orderId;
    }
}
