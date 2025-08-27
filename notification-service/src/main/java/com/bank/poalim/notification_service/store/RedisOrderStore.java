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
	public Mono<Boolean> deleteOrder(String orderId) {
	    String redisKey = key(orderId);

	    return orderReactiveRedisTemplate.opsForValue().delete(redisKey);
	}
	
	@Override
	public Mono<Boolean> updateOrderStatus(OrderRecord orderRecord) {
	    String redisKey = key(orderRecord.getOrderId());
	    
	    return orderReactiveRedisTemplate.opsForValue()
	            .set(redisKey, orderRecord)
	            .map(result -> Boolean.TRUE)
	            .onErrorReturn(Boolean.FALSE);
	}
	
	private String key(String orderId) {
        return "order:" + orderId;
    }
}
