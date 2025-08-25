compose_up:
	docker compose up -d

compose_down:
	docker compose down -v

compose_logs:
	docker compose logs -f --tail=200

run_order_service:
	SPRING_PROFILES_ACTIVE=docker ./order-service/mvnw spring-boot:run -f ./order-service/pom.xml

seed_topics:
	docker exec -it kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic order-created --partitions 3 --replication-factor 1
