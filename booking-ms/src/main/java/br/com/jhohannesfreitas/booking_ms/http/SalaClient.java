package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "room-ms")
public interface SalaClient {

    @GetMapping("/api/v1/salas/{id}")
    SalaRequest buscarPorId(@PathVariable Long id);

    @PutMapping("api/v1/salas/{id}")
    SalaRequest alterarStatusSala(@PathVariable Long id, @RequestBody StatusSala statusSala);
}
