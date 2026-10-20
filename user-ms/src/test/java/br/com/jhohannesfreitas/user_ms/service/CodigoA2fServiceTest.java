package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.CodigoA2f;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.repository.CodigoA2fRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CodigoA2fServiceTest {

    @Mock
    private CodigoA2fRepository codigoA2fRepository;

    @InjectMocks
    private CodigoA2fService codigoA2fService;

    @Test
    @DisplayName("Deveria gerar código quando usuarioId for válido")
    void deveriaGerarCodigoQuandoUsuarioIdForValido() {
        // ARRANGE
        Long usuarioId = 1L;

        // configura o mock do repositório para retornar o mesmo objeto CodigoA2f que recebeu como argumento.
        given(codigoA2fRepository.save(any(CodigoA2f.class)))
                .willAnswer(invocation -> invocation.getArgument(0)); // retorna o primeiro argumento recebido pelo método save()

        // ACT
        CodigoA2f respostaPersistida = codigoA2fService.gerarCodigo(usuarioId);

        // ASSERT
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(codigoA2fRepository).should().save(any(CodigoA2f.class));

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(usuarioId, respostaPersistida.getUsuarioId()),
                () -> assertNotNull(respostaPersistida.getCodigo()),
                () -> assertTrue(respostaPersistida.getCodigo().matches("\\d{6}")),
                () -> assertNotNull(respostaPersistida.getExpiracao()),
                () -> assertTrue(respostaPersistida.getExpiracao()
                        .isAfter(LocalDateTime.now().plusMinutes(4)))
        );

    }

    @Test
    @DisplayName("Deveria definir expiração para cinco minutos após gerar o código")
    void deveriaDefinirExpiracaoParaCincoMinutosAposGerarCodigo() {
        // ARRANGE
        Long usuarioId = 1L;

        given(codigoA2fRepository.save(any(CodigoA2f.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antesDaGeracao = LocalDateTime.now();

        // ACT
        CodigoA2f resposta = codigoA2fService.gerarCodigo(usuarioId);

        LocalDateTime depoisDaGeracao = LocalDateTime.now();

        // ASSERT
        assertAll(
                () -> assertFalse(
                        resposta.getExpiracao().isBefore(antesDaGeracao.plusMinutes(5))
                ),
                () -> assertFalse(
                        resposta.getExpiracao().isAfter(depoisDaGeracao.plusMinutes(5))
                )
        );
    }
}