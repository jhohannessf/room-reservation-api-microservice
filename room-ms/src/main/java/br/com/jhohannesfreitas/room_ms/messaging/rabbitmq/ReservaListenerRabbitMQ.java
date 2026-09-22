package br.com.jhohannesfreitas.room_ms.messaging.rabbitmq;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaListenerRabbitMQ {

    // Método que recebe/consume as mensagens de Reserva via RabbitMQ
    @RabbitListener(queues = "reserva.detalhes-sala")
    public void recebeMensagem(ReservaRequest reservaRequest) {
        // Apenas pra simular a Dead Letter Queue
//        if (reservaRequest.quantidadePessoas() <= 1)
//            throw new RegraNegocioException("Não faz sentido a quantidade de pessoas ser menor ou igual a 1.",
//                    HttpStatus.BAD_REQUEST);

        String mensagem = """
                Número da sala: %s
                Data: %s
                Hora inicial: %s
                Hora final: %s
                Quantidade de pessoas: %s
                """.formatted(reservaRequest.salaId(),
                reservaRequest.data(),
                reservaRequest.horaInicial(),
                reservaRequest.horaFinal(),
                reservaRequest.quantidadePessoas());

        System.out.println("Recebendo Mensagem: \n" + mensagem);
    }

    @RabbitListener(queues = "reserva.detalhes-status-sala")
    public void recebeMensagemAlteracaoStatus(StatusSala statusSala) {
        String  mensagem = """
                Status da sala: %s
        """.formatted(statusSala);
        System.out.println("Recebendo Mensagem: \n" + mensagem);
    }
}
