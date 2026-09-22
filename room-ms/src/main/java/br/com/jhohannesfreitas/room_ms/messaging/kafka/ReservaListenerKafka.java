package br.com.jhohannesfreitas.room_ms.messaging.kafka;

import br.com.jhohannesfreitas.room_ms.dto.ReservaRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservaListenerKafka {

    @KafkaListener(topics = "booking-created", groupId = "room-ms-group")
    public void recebeMensagem(ReservaRequest reservaRequest){
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
