package br.com.jhohannesfreitas.booking_ms.amqp;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Não é necessário criar fila quando se tem a exchange
    @Bean
    public Queue criarFila() {
        return new Queue("reserva.concluida", false);
        // Outra forma de fazer
        //return QueueBuilder.nonDurable("reserva.concluida").build();
    }

    // Criando Exchange
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("reserva.fanout.ex");
    }

    // Necessário para conseguir fazer alterações no painel admin
    @Bean
    public RabbitAdmin criaRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // Inicializar o rabbit Admin ao subir a aplicação
    @Bean
    public ApplicationListener<ApplicationReadyEvent> inicializaRabbitAdmin(RabbitAdmin rabbitAdmin) {
        return event -> rabbitAdmin.initialize();
    }

    // Serialização e Desserialização
    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // Conversor de mensagens
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter jacksonJsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonJsonMessageConverter);
        return rabbitTemplate;
    }


}
