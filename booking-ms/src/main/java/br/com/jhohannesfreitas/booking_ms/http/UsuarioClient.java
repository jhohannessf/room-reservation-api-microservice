package br.com.jhohannesfreitas.booking_ms.http;

import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-ms")
public interface UsuarioClient {

    @GetMapping("/api/v1/usuarios/{id}")
    UsuarioRequest buscarPorId(@PathVariable Long id);
}
