package br.com.jhohannesfreitas.user_ms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    protected JavaMailSender javaMailSender;

    @MockitoBean
    protected ClientRegistrationRepository clientRegistrationRepository;


    // Padrão singleton: sem @Container e sem @Testcontainers.
    // Os containers sobem UMA vez por JVM e são compartilhados por todas as classes de teste.
    // O Ryuk (container de limpeza do Testcontainers) os remove quando a JVM termina.
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("room_ms_test")
            .withUsername("root")
            .withPassword("mysql");

    static {
        // Sobe uma única vez e retorna quando o serviço estiver pronto
        MYSQL.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // Banco de dados
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    // Teste simples só para garantir que tudo sobe corretamente

    @Test
    void contextLoads() {
        System.out.println("Teste funcionou!");
        // Se chegar aqui, significa que o Spring conseguiu se conectar no banco.
    }
}


