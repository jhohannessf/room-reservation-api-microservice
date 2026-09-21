package br.com.jhohannesfreitas.room_ms.amqp;

import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaListener {

    // Método que recebe/consume as mensagens de Reserva via RabbitMQ
    @RabbitListener(queues = "reserva.detalhes-sala")
    public void recebeMensagem(ReservaRequest reservaRequest) {
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
}
