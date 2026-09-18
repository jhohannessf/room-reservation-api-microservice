package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.StatusSalaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "room-ms", fallback = SalaClientFallback.class)
public interface SalaClient {

    @GetMapping("/api/v1/salas/{id}")
    SalaRequest buscarPorId(@PathVariable Long id);

    @PatchMapping("/api/v1/salas/alterar-status/{id}")
    SalaRequest alterarStatusSala(@PathVariable Long id, @RequestBody StatusSalaRequest statusSalaRequest);
}
