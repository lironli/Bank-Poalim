package com.bank.poalim.order_service.config;

import com.bank.poalim.order_service.model.OrderRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<OrderRecord> valueSerializer = new Jackson2JsonRedisSerializer<>(OrderRecord.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, OrderRecord> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, OrderRecord> context = builder
                .value(valueSerializer)
                .build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
