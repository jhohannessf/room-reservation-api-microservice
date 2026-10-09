package br.com.jhohannesfreitas.user_ms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Padrão singleton: sem @Container e sem @Testcontainers.
    // Os containers sobem UMA vez por JVM e são compartilhados por todas as classes de teste.
    // O Ryuk (container de limpeza do Testcontainers) os remove quando a JVM termina.
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("room_ms_test")
            .withUsername("root")
            .withPassword("mysql");

    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3-management");

    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    static {
        // Sobe os três uma única vez; só retorna quando todos estiverem prontos
        MYSQL.start();
        RABBITMQ.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Banco de dados
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

    }

    // Teste simples só para garantir que tudo sobe corretamente

    @Test
    void contextLoads() {
        System.out.println("Teste funcionou!");
        // Se chegar aqui, significa que o Spring conseguiu se conectar no banco, rabbit e kafka!
    }
}


