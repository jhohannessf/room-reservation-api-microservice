package br.com.jhohannesfreitas.booking_ms.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    // Não é necessário criar um Producer, basta inserir no application.properties
//    @Bean
//    public ProducerFactory<String, String> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }

    // Se você tem spring.kafka.bootstrap-servers configurado no application.properties,
    // o Spring Boot já cria ProducerFactory e KafkaTemplate automaticamente via auto-configuration
//    @Bean
//    public KafkaTemplate<String, String> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name("booking-created")
                .partitions(2)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // Define por quanto tempo (em milissegundos) o Kafka mantém as mensagens no tópico antes de poder descartá-las.
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE) // Define a estratégia usada para limpar dados antigos
                .build();
    }


}
