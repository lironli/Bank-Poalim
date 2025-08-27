package com.bank.poalim.notification_service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bank.poalim.notification_service.dto.OrderItemDto;
import com.bank.poalim.notification_service.model.InventoryCheckResult;
import com.bank.poalim.notification_service.model.MissingItem;
import com.bank.poalim.notification_service.model.OrderItemCategory;
import com.bank.poalim.notification_service.model.OrderRecord;
import com.bank.poalim.notification_service.model.OrderStatus;
import com.bank.poalim.notification_service.service.NotificationService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"inventory-check-result"})
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReactiveRedisTemplate<String, OrderRecord> redisTemplate;

    private OrderRecord testOrder;
    private InventoryCheckResult approvedResult;
    private InventoryCheckResult rejectedResult;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        redisTemplate.delete(redisTemplate.keys("order:*")).block();

        // Create test order
        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setProductId("P1001");
        orderItem.setQuantity(2);
        orderItem.setCategory(OrderItemCategory.STANDARD);
        
        testOrder = OrderRecord.builder()
                .orderId("integration-test-order-123")
                .customerName("Integration Test User")
                .items(Arrays.asList(orderItem))
                .requestedAt(Instant.now().minusSeconds(300))
                .createdAt(Instant.now().minusSeconds(200))
                .status(OrderStatus.PENDING)
                .build();

        // Create approved inventory check result
        approvedResult = new InventoryCheckResult(
                "integration-test-order-123",
                null, // no missing items
                true  // approved
        );

        // Create rejected inventory check result
        MissingItem missingItem = new MissingItem();
        missingItem.setProductId("P1001");
        missingItem.setReason("Insufficient quantity");
        
        List<MissingItem> missingItems = Arrays.asList(missingItem);
        rejectedResult = new InventoryCheckResult(
                "integration-test-order-456",
                missingItems,
                false  // not approved
        );
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndApproved_ShouldUpdateStatusToCompleted() {
        // Arrange - Save order to Redis
        String redisKey = "order:integration-test-order-123";
        redisTemplate.opsForValue().set(redisKey, testOrder).block();

        // Verify order exists in Redis
        OrderRecord savedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert - Check that order status was updated to COMPLETED
        OrderRecord updatedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(updatedOrder.getOrderId()).isEqualTo("integration-test-order-123");
        assertThat(updatedOrder.getCustomerName()).isEqualTo("Integration Test User");
    }

    @Test
    void processInventoryCheckResult_WhenOrderExistsAndRejected_ShouldUpdateStatusToRejected() {
        // Arrange - Save order to Redis
        String redisKey = "order:integration-test-order-456";
        OrderRecord orderToReject = OrderRecord.builder()
                .orderId("integration-test-order-456")
                .customerName("Rejection Test User")
                .items(testOrder.getItems())
                .requestedAt(testOrder.getRequestedAt())
                .createdAt(testOrder.getCreatedAt())
                .status(OrderStatus.PENDING)
                .build();

        redisTemplate.opsForValue().set(redisKey, orderToReject).block();

        // Verify order exists in Redis
        OrderRecord savedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        // Act
        notificationService.processInventoryCheckResult(rejectedResult);

        // Assert - Check that order status was updated to REJECTED
        OrderRecord updatedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(updatedOrder.getOrderId()).isEqualTo("integration-test-order-456");
        assertThat(updatedOrder.getCustomerName()).isEqualTo("Rejection Test User");
    }

    @Test
    void processInventoryCheckResult_WhenOrderNotFound_ShouldNotUpdateAnything() {
        // Arrange - Don't save any order to Redis
        String redisKey = "order:integration-test-order-123";
        
        // Verify order doesn't exist in Redis
        OrderRecord savedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(savedOrder).isNull();

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert - Order should still not exist in Redis
        OrderRecord updatedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(updatedOrder).isNull();
    }

    @Test
    void processInventoryCheckResult_ShouldPreserveAllOrderDataExceptStatus() {
        // Arrange - Save order to Redis
        String redisKey = "order:integration-test-order-123";
        redisTemplate.opsForValue().set(redisKey, testOrder).block();

        // Act
        notificationService.processInventoryCheckResult(approvedResult);

        // Assert - Check that all data is preserved except status
        OrderRecord updatedOrder = redisTemplate.opsForValue().get(redisKey).block();
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getOrderId()).isEqualTo(testOrder.getOrderId());
        assertThat(updatedOrder.getCustomerName()).isEqualTo(testOrder.getCustomerName());
        assertThat(updatedOrder.getItems()).isEqualTo(testOrder.getItems());
        assertThat(updatedOrder.getRequestedAt()).isEqualTo(testOrder.getRequestedAt());
        assertThat(updatedOrder.getCreatedAt()).isEqualTo(testOrder.getCreatedAt());
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED); // Only status should change
    }

    @Test
    void redisOperations_ShouldWorkCorrectly() {
        // Test basic Redis operations
        String testKey = "test:key";
        OrderRecord testData = OrderRecord.builder()
                .orderId("test-redis-order")
                .customerName("Redis Test User")
                .items(Arrays.asList())
                .requestedAt(Instant.now())
                .createdAt(Instant.now())
                .status(OrderStatus.PENDING)
                .build();

        // Test set operation
        StepVerifier.create(redisTemplate.opsForValue().set(testKey, testData))
                .expectNext(true)
                .verifyComplete();

        // Test get operation
        StepVerifier.create(redisTemplate.opsForValue().get(testKey))
                .expectNext(testData)
                .verifyComplete();

        // Test delete operation
        StepVerifier.create(redisTemplate.opsForValue().delete(testKey))
                .expectNext(true)
                .verifyComplete();

        // Verify deletion
        StepVerifier.create(redisTemplate.opsForValue().get(testKey))
                .verifyComplete();
    }
}
