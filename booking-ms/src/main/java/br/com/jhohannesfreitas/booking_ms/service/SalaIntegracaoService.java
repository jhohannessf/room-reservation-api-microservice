package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;


// Classe separada de propósito: o Circuit Breaker do Resilience4j só funciona
// quando o método anotado é chamado a partir de OUTRO bean (via proxy do Spring).
// Se essa chamada estivesse dentro do próprio ReservaService e fosse invocada
// como "this.marcarSalaOcupada(...)", a anotação seria silenciosamente ignorada.
@Service
public class SalaIntegracaoService {

    private final SalaClient salaClient;

    public SalaIntegracaoService(SalaClient salaClient) {
        this.salaClient = salaClient;
    }

    // Só esta chamada específica tem Circuit Breaker + fallback.
    // As chamadas de validação (buscarPorId de sala e usuário) continuam sem
    // proteção, propagando o erro real (404, 403, etc.) ao invés de mascará-lo.
    @CircuitBreaker(name = "atualizaSala", fallbackMethod = "marcarSalaOcupadaFallback")
    public boolean marcarSalaOcupada(Long salaId) {
        salaClient.alterarStatusSala(salaId, new StatusSalaRequest(StatusSala.OCUPADA));
        return true; // integração com room-ms bem-sucedida
    }

    // Fallback: room-ms estava fora do ar — retorna false para sinalizar
    // que a integração ficou pendente e o ReservaService deve reagir.
    // Mesma assinatura do método original + Throwable no final (obrigatório).
    public boolean marcarSalaOcupadaFallback(Long salaId, Throwable t) {
        System.out.println("Circuit Breaker acionado: room-ms fora do ar ao marcar sala "
                + salaId + " como OCUPADA. Motivo: " + t.getMessage());
        return false; // integração pendente
    }
}
