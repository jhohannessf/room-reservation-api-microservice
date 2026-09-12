package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.repository.CodigoA2fRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CodigoA2fService {

    private final CodigoA2fRepository codigoA2fRepository;

    public CodigoA2fService(CodigoA2fRepository codigoA2fRepository) {
        this.codigoA2fRepository = codigoA2fRepository;
    }

    public CodigoA2f gerarCodigo(Long usuarioId) {

        String codigo = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(5);

        CodigoA2f codigoA2f = new CodigoA2f(
                usuarioId,
                codigo,
                expiracao
        );

        return codigoA2fRepository.save(codigoA2f);
    }
}
