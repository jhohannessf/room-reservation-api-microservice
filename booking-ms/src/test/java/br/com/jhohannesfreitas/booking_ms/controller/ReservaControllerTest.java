package br.com.jhohannesfreitas.booking_ms.controller;

import br.com.jhohannesfreitas.booking_ms.domain.entity.UserPrincipal;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.infra.config.JwtAuthenticationFilter;
import br.com.jhohannesfreitas.booking_ms.infra.config.SecurityConfiguration;
import br.com.jhohannesfreitas.booking_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.service.ReservaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ReservaController.class) // sobe apenas a camada web, sem banco, sem Kafka, sem Rabbit
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TokenProvider.class})
class ReservaControllerTest {

    @MockitoBean
    private ReservaService reservaService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deveria retornar 200 OK quando listar todas as reservas")
    void deveriaRetornar200AoListarTodasAsReservas() throws Exception {
        //ARRANGE
        ReservaResponse reservaResponse1 = new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        ReservaResponse reservaResponse2 = new ReservaResponse(
                2L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        given(reservaService.listar()).willReturn(List.of(reservaResponse1, reservaResponse2));

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas")
                        .with(authentication(createAuthentication()))) // simula usuário authenticado
                .andExpect(status().isOk()) // retorno do status esperado
                .andExpect(jsonPath("$.length()").value(2)) // tamanho do json
                .andExpect(jsonPath("$[0].id").value(1L)) // esperado json com id = 1
                .andExpect(jsonPath("$[1].id").value(2L)); // esperado json com id = 2
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar todas as reservas sem autenticação")
    void deveriaRetornar401QuandoListarTodasAsReservasSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando lista reservas por páginas")
    void deveriaRetornar200AoListarReservasPorPaginas() throws Exception {
        //ARRANGE
        ReservaResponse reservaResponse1 = new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        ReservaResponse reservaResponse2 = new ReservaResponse(
                2L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        // Paginação
        Page<ReservaResponse> page = new PageImpl<>(List.of(reservaResponse1, reservaResponse2), // conteúdo
                PageRequest.of(0, 10), // paginação
                2); // total de elementos

        given(reservaService.listarPaginado(any(Pageable.class))).willReturn(page);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/listar-paginado")
                        .param("page", "0") // número da página
                        .param("size", "10") // tamanho de elementos por página
                        .with(authentication(createAuthentication()))) // simula usuário authenticado
                .andExpect(status().isOk()) // retorno do status esperado
                .andExpect(jsonPath("$.content.length()").value(2)) // tamanho do json
                .andExpect(jsonPath("$.content[0].id").value(1L)) // esperado json com id = 1
                .andExpect(jsonPath("$.content[1].id").value(2L)) // esperado json com id = 2
                .andExpect(jsonPath("$.totalElements").value(2)); // total de elementos esperado
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar todas as reservas por página sem autenticação")
    void deveriaRetornar401QuandoListarTodasAsReservasPorPaginaSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/listar-paginado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK ao listar reservas paginadas por sala e intervalo")
    void deveriaRetornar200AoListarReservaPaginadasPorSalaEIntervalo() throws Exception {
        //ARRANGE
        ReservaResponse reservaResponse1 = new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        ReservaResponse reservaResponse2 = new ReservaResponse(
                2L, 1L, 10L, LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA
        );

        // Paginação
        Page<ReservaResponse> page = new PageImpl<>(List.of(reservaResponse1, reservaResponse2), // conteúdo
                PageRequest.of(0, 10), // paginação
                2); // total de elementos

        given(reservaService.listarPorSalaEIntervalo(eq(1L), any(LocalDate.class), any(LocalDate.class), any(Pageable.class))).willReturn(page);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/sala/{id}", 1L)
                        .param("page", "0") // número da página
                        .param("size", "10") // tamanho de elementos por página
                        .param("inicio", "2026-10-01")
                        .param("fim", "2026-10-31")
                        .with(authentication(createAuthentication()))) // simula usuário authenticado
                .andExpect(status().isOk()) // retorno do status esperado
                .andExpect(jsonPath("$.content.length()").value(2)) // 2 é o tamanho do json esperado
                .andExpect(jsonPath("$.content[0].id").value(1L)) // json com id = 1 é esperado
                .andExpect(jsonPath("$.totalElements").value(2)); // 2 é total de elementos esperado
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar reservas paginadas por sala e intervalo sem autenticação")
    void deveriaRetornar401QuandoListarReservasPaginadasPorSalaEIntervaloSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/sala/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK ao buscar por id reserva")
    void deveriaRetornar200AoBuscarPorIdReserva() throws Exception {
        // ARRANGE
        given(reservaService.buscarPorId(1L)).willReturn(new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA));

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/{id}", 1L)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando buscar reserva por id sem autenticação")
    void deveriaRetornar401QuandoBuscarReservaPorIdSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/reservas/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 404 NOT FOUND quando reserva não for encontrada por id")
    void deveriaRetornar404QuandoReservaNaoForEncontradaPorId() throws Exception {
        //ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );
        Long id = 1L;

        // ACT + ASSERT
        when(reservaService.buscarPorId(anyLong()))
                .thenThrow(new RegraNegocioException("Reserva com id " + id + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/reservas/{id}", 1L)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());

        verify(reservaService).buscarPorId(1L);
    }

    @Test
    @DisplayName("Deveria retornar status 201 CREATED ao cadastrar nova reserva quando dados forem válidos")
    void deveriaRetornar201AoCadastrarReservaQuandoDadosForemValidos() throws Exception {
        // ARRANGE
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );

        given(reservaService.cadastrar(reservaRequest, usuarioId)).willReturn(new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA));

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest)) // Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.usuarioId").value(1L))
                .andExpect(jsonPath("$.salaId").value(10L))
                .andExpect(jsonPath("$.status").value("ATIVA"));
    }

    @Test
    @DisplayName("Deveria retornar status 400 Bad Request ao cadastrar nova reserva quando corpo da requisição for inválido")
    void deveriaRetornar400AoCadastrarReservaQuandoCorpoRequisicaoForInvalido() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                null,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                -5
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest)) // Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'salaId')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar status 401 Unauthorized ao cadastrar nova reserva sem autenticação")
    void deveriaRetornar401AoCadastrarReservaQuandoNaoAutenticado() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest))) // Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found quando o service lançar exceção de sala não encontrada")
    void deveriaRetornar404AoCadastrarReservaQuandoSalaNaoEncontrada() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                999L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );

        //ACT + ASSERT
        when(reservaService.cadastrar(any(), any()))
                .thenThrow(new RegraNegocioException("Sala com id " + reservaRequest.salaId() + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao cadastrar reserva quando o service lançar exceção de horário conflitante")
    void deveriaRetornar409AoCadastrarReservaQuandoHorarioConflitante() throws Exception {
        //ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );

        // ACT + ASSERT
        when(reservaService.cadastrar(any(), any()))
                .thenThrow(new RegraNegocioException("Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar status 200 OK ao atualizar uma reserva quando dados forem válidos")
    void deveriaRetornar200AoAtualizarReservaQuandoDadosForemValidos() throws Exception {
        // ARRANGE
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );
        Long id = 1L;

        given(reservaService.atualizar(id, reservaRequest, usuarioId)).willReturn(new ReservaResponse(
                1L, 1L, 10L, LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20,
                StatusReserva.ATIVA));

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest)) // Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.usuarioId").value(1L))
                .andExpect(jsonPath("$.salaId").value(10L))
                .andExpect(jsonPath("$.status").value("ATIVA"));
    }

    @Test
    @DisplayName("Deveria retornar status 400 Bad Request ao atualizar reserva com corpo da requisição inválido")
    void deveriaRetornar400AoAtualizarReservaQuandoCorpoRequisicaoForInvalido() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                null,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                -5
        );
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest)) // Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'salaId')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar status 401 Unauthorized ao atualizar reserva sem autenticação")
    void deveriaRetornar401AoAtualizarReservaQuandoNaoAutenticado() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest))) // Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden quando o service lançar exceção de permissão ao atualizar reserva")
    void deveriaRetornar403AoAtualizarReservaQuandoServiceLancarExcecaoDePermissao() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );
        Long id = 2L;

        // ACT + ASSERT
        doThrow(new RegraNegocioException(
                "Você não tem permissão para alterar ou deletar a reserva de outro usuário.",
                HttpStatus.FORBIDDEN
        )).when(reservaService).atualizar(anyLong(), any(), anyLong());

        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON) // informando que o body é json
                        .content(objectMapper.writeValueAsString(reservaRequest)) // Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found ao atualizar reserva quando não encontrada")
    void deveriaRetornar404AoAtualizarReservaQuandoNaoEncontrada() throws Exception {
        ReservaRequest reservaRequest = new ReservaRequest(
                999L,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );
        Long id = 1L;

        when(reservaService.atualizar(anyLong(), any(), anyLong()))
                .thenThrow(new RegraNegocioException("Reserva com id " + id + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao atualizar reserva quando o service lançar exceção de horário conflitante")
    void deveriaRetornar409AoAtualizarReservaQuandoHorarioConflitante() throws Exception {
        // ARRANGE
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                30
        );
        Long id = 1L;

        // ACT + ASSERT
        when(reservaService.atualizar(anyLong(), any(), anyLong()))
                .thenThrow(new RegraNegocioException("Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT));

        mockMvc.perform(put("/api/v1/reservas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar status 204 No Content ao deletar uma reserva com sucesso quando Id existir")
    void deveriaRetornar204AoDeletarReservaQuandoIdExistir() throws Exception {
        // ARRANGE
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/reservas/{id}", id)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNoContent());

        // Prova que o service foi chamado com argumentos certos. Não somente status.
        then(reservaService).should().deletar(1L, 1L);
    }

    @Test
    @DisplayName("Deveria retornar status 401 Unauthorized ao deletar reserva sem autenticação")
    void deveriaRetornar401AoDeletarReservaQuandoNaoAutenticado() throws Exception {
        // ARRANGE
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/reservas/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden quando o service lançar exceção de permissão ao deletar reserva")
    void deveriaRetornar403AoDeletarReservaQuandoServiceLancarExcecaoDePermissao() throws Exception {
        // ARRANGE
        Long id = 2L;

        // ACT + ASSERT
        doThrow(new RegraNegocioException(
                "Você não tem permissão para alterar ou deletar a reserva de outro usuário.",
                HttpStatus.FORBIDDEN
        )).when(reservaService).deletar(anyLong(), anyLong());

        mockMvc.perform(delete("/api/v1/reservas/{id}", id)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found ao deletar reserva quando não encontrada")
    void deveriaRetornar404AoDeletarReservaQuandoNaoEncontrada() throws Exception {
        //ARRANGE
        Long id = 1L;

        // ACT + ASSERT
        doThrow(new RegraNegocioException(
                "Reserva com id " + id + " não encontrada.",
                HttpStatus.NOT_FOUND
        )).when(reservaService).deletar(anyLong(), anyLong());

        mockMvc.perform(delete("/api/v1/reservas/{id}", id)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());
    }

    // Para usar o .with(authentication(...)) no MockMvc
    private Authentication createAuthentication() {
        UserPrincipal userPrincipal = new UserPrincipal(1L, "teste@email.com");
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }
}