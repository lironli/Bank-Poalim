package com.bank.poalim.notification_service.store;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.bank.poalim.notification_service.model.OrderRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOrderStore implements OrderStore {
    
    private final ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate;

	@Override
	public Mono<OrderRecord> getOrderById(String orderId) {
		return orderReactiveRedisTemplate.opsForValue().get(key(orderId));
		
	}
	
	@Override
	public Mono<OrderRecord> retrieveAndDeleteOrder(String orderId) {
	    String redisKey = key(orderId);

	    return orderReactiveRedisTemplate.opsForValue()
	            .get(redisKey)
	            .flatMap(order -> {
	                log.info("Retrieved order from Redis: {}", order);
	                // delete the order after logging
	                return orderReactiveRedisTemplate.opsForValue()
	                        .delete(redisKey)
	                        .doOnNext(deleted -> log.info("Order {} deleted from Redis: {}", orderId, deleted))
	                        .thenReturn(order); // return the retrieved order
	            });
	}
	
	private String key(String orderId) {
        return "order:pending:" + orderId;
    }
}
