package br.com.jhohannesfreitas.room_ms.messaging.rabbitmq;

import org.springframework.amqp.core.*;
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

    // Fila
    @Bean
    public Queue filaDetalhesReserva() {
        return QueueBuilder
                .nonDurable("reserva.detalhes-sala")
                .deadLetterExchange("reserva.dlx")
                .build();
        //return new Queue("reserva.detalhes-sala", false);
    }

    // Fila sala
    @Bean
    public Queue filaDetalhesSalaStatus() {
        return QueueBuilder
                .nonDurable("reserva.detalhes-status-sala")
                .deadLetterExchange("reserva.dlx")
                .build();
    }

    // Dead Letter Queue
    @Bean
    public Queue filaDlqDetalhesReserva() {
        return new Queue("reserva.detalhes-sala-dlq", false);
    }

    // Exchange
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("reserva.fanout.ex");
        //return ExchangeBuilder.fanoutExchange("reserva.ex").build();
    }

    // Dead Letter Exchange
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange("reserva.dlx");
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("reserva.direct.ex");
    }

    // Binding
    @Bean
    public Binding bindingReserva() {
        return BindingBuilder
                .bind(filaDetalhesReserva())
                .to(fanoutExchange());
    }

    // Binding sala
    @Bean
    public Binding bindingSala() {
        return BindingBuilder
                .bind(filaDetalhesSalaStatus())
                .to(directExchange())
                .with("reserva.detalhes-status-sala");
    }

    // Binding Dead Letter
    @Bean
    public Binding bindingDlxReserva() {
        return BindingBuilder.bind(filaDlqDetalhesReserva()).to(deadLetterExchange());
    }

    // Necessário para conseguir criar/alterar no painel admin
    @Bean
    public RabbitAdmin criaRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    // Inicializar o rabbit Admin ao subir a aplicação
    @Bean
    public ApplicationListener<ApplicationReadyEvent> inicializaRabbitAdmin(RabbitAdmin rabbitAdmin) {
        return event -> rabbitAdmin.initialize();
    }
}
