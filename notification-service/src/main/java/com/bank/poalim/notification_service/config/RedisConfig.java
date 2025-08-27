package com.bank.poalim.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.bank.poalim.notification_service.model.OrderRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {
	
	@Bean
	public ReactiveRedisTemplate<String, OrderRecord> orderReactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
		
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		Jackson2JsonRedisSerializer<OrderRecord> valueSerializer = new Jackson2JsonRedisSerializer<>(mapper, OrderRecord.class);
		
		RedisSerializationContext.RedisSerializationContextBuilder<String, OrderRecord> builder =
				RedisSerializationContext.newSerializationContext(keySerializer);
		
		
		RedisSerializationContext<String, OrderRecord> context = builder
				.value(valueSerializer)
				.build();
		return new ReactiveRedisTemplate<>(connectionFactory, context);
	  }
    

}
