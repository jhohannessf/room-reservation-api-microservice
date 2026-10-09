package br.com.jhohannesfreitas.room_ms.controller;

import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import br.com.jhohannesfreitas.room_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.room_ms.infra.config.JwtAuthenticationFilter;
import br.com.jhohannesfreitas.room_ms.infra.config.SecurityConfiguration;
import br.com.jhohannesfreitas.room_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.room_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.room_ms.service.SalaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.security.auth.UserPrincipal;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalaController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TokenProvider.class})
class SalaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalaService salaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deveria retornar 200 OK quando listar todas as salas")
    void deveriaRetornar200QuandoListarTodasAsSalas() throws Exception {
        //ARRANGE
        SalaResponse salaResponse = createSalaResponse(1L, 1);

        SalaResponse salaResponse2 = createSalaResponse(2L, 2);

        given(salaService.listar()).willReturn(List.of(salaResponse, salaResponse2));

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas")
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // tamanho do json esperado
                .andExpect(jsonPath("$[0].id").value(1L)) // id esperado para [0]
                .andExpect(jsonPath("$[1].id").value(2L)); // id esperado para [1]
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar todas as salas sem autenticação")
    void deveriaRetornar401QuandoListarTodasAsSalasSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando listar salas por página")
    void deveriaRetornar200QuandoListarSalasPorPaginas() throws Exception {
        //ARRANGE
        SalaResponse salaResponse = createSalaResponse(1L, 1);

        SalaResponse salaResponse2 = createSalaResponse(2L, 2);

        //Paginação
        Page<SalaResponse> page = new PageImpl<>(List.of(salaResponse, salaResponse2), // conteúdo
                PageRequest.of(0, 10), //paginação
                2); // total de elementos

        given(salaService.listarPaginado(any(Pageable.class))).willReturn(page);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas/listar-paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2)) // tamanho do json esperado
                .andExpect(jsonPath("$.content[0].id").value(1L)) // id esperado para [0]
                .andExpect(jsonPath("$.content[1].id").value(2L)) // id esperado para [1]
                .andExpect(jsonPath("$.totalElements").value(2)); // total de elementos esperado
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar salas por página sem autenticação")
    void deveriaRetornar401QuandoListarSalasPorPaginaSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas/listar-paginado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando buscar sala por id")
    void deveriaRetornar200QuandoBuscarSalaPorId() throws Exception {
        // ARRANGE
        SalaResponse salaResponse = createSalaResponse(1L, 1);

        given(salaService.buscarSalaPorId(1L))
                .willReturn(salaResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas/{id}", 1L)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(1));
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando buscar sala por id sem autenticação")
    void deveriaRetornar401QuandoBuscarSalaPorIdSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/salas/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 404 NOT FOUND quando sala não for encontrada por id")
    void deveriaRetornar404QuandoSalaNaoForEncontradaPorId() throws Exception {
        //ARRANGE
        SalaResponse salaResponse = createSalaResponse(1L, 1);

        // ACT + ASSERT
        when(salaService.buscarSalaPorId(anyLong()))
                .thenThrow(new RegraNegocioException("Sala com id " + salaResponse.id() + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/salas/{id}", 1L)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());

        verify(salaService).buscarSalaPorId(1L);
    }

    @Test
    @DisplayName("Deveria retornar 201 CREATED ao cadastrar nova sala quando dados forem válidos")
    void deveriaRetornar201AoCadastrarSalaQuandoDadosForemVida() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );
        SalaResponse salaResponse = createSalaResponse(1L, 1);

        given(salaService.cadastrar(salaRequest)).willReturn(salaResponse);

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/salas")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                .with(authentication(authenticationAdmin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.capacidade").value(20));
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao cadastrar nova sala quando corpo da requisição for inválido")
    void deveriaRetornar400AoCadastrarSalaQuandoCorpoRequisicaoForInvalido() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                null,
                -20
        );

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'numero')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao cadastrar nova sala quando não autenticado")
    void deveriaRetornar401AoCadastrarSalaQuandoNaoAutenticado() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden ao cadastrar nova sala quando não autorizado (Perfil Administrador)")
    void deveriaRetornar403AoCadastrarSalaQuandoNaoAutorizado() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication()))) // ROLE_ESTUDANTE
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao cadastrar nova sala quando o service lançar exceção de sala com este número já existe")
    void deveriaRetornar409AoCadastrarSalaQuandoNumeroJaExiste() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        when(salaService.cadastrar(any(SalaRequest.class)))
                .thenThrow(new RegraNegocioException("Sala já cadastrada com este número.",
                        HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/salas")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar status 200 OK ao atualizar uma sala quando dados forem válidos")
    void deveriaRetornar200AoAtualizarSalaQuandoDadosForemValidos() throws Exception {
        // ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                25
        );
        Long id = 1L;

        SalaResponse salaResponse = createSalaResponse(id, 1);

        given(salaService.atualizar(id, salaRequest))
                .willReturn(salaResponse);

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/salas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(salaRequest))
                .with(authentication(authenticationAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.capacidade").value(20));

    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao atualizar sala quando corpo da requisição for inválido")
    void deveriaRetornar400AoAtualizarSalaQuandoCorpoRequisicaoForInvalido() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                null,
                -20
        );
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/salas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'numero')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao atualizar sala quando não autenticado")
    void deveriaRetornar401AoAtualizarSalaQuandoNaoAutenticado() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/salas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest))) //Transforma em json o objeto que foi enviando no body (serializar)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden ao atualizar sala quando não autorizado (Perfil Administrador)")
    void deveriaRetornar403AoAtualizarSalaQuandoNaoAutorizado() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/salas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication()))) // ROLE_ESTUDANTE
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found ao atualizar sala quando número não for encontrado")
    void deveriaRetornar404AoAtualizarSalaQuandoNumeroNaoForEncontrado() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        when(salaService.atualizar(any(Long.class), any(SalaRequest.class)))
                .thenThrow(new RegraNegocioException("Sala com id " + id + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/v1/salas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao atualizar sala quando o service lançar exceção de sala com este número já existe")
    void deveriaRetornar409AoAtualizarSalaQuandoNumeroJaExiste() throws Exception {
        //ARRANGE
        SalaRequest salaRequest = new SalaRequest(
                1,
                20
        );
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        when(salaService.atualizar(any(Long.class), any(SalaRequest.class)))
                .thenThrow(new RegraNegocioException("Sala já cadastrada com este número.",
                        HttpStatus.CONFLICT));

        mockMvc.perform(put("/api/v1/salas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(salaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar status 204 No Content ao deletar uma sala com sucesso quando Id existir")
    void deveriaRetornar204AoDeletarSalaQuandoIdExistir() throws Exception {
        // ARRANGE
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/salas/{id}", id)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isNoContent());

        // Prova que o service foi chamado com argumentos certos. Não somente status.
        then(salaService).should().deletar(id);
    }

    @Test
    @DisplayName("Deveria retornar status 401 Unauthorized ao deletar sala sem autenticação")
    void deveriaRetornar401AoDeletarSalaQuandoNaoAutenticado() throws Exception {
        // ARRANGE
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/salas/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden ao deletar sala quando não autorizado (Perfil Administrador)")
    void deveriaRetornar403AoDeletarSalaQuandoNaoAutorizado() throws Exception {
        // ARRANGE
        Long id = 2L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/salas/{id}", id)
                        .with(authentication(createAuthentication()))) // ROLE_ESTUDANTE
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found ao deletar sala quando não encontrada")
    void deveriaRetornar404AoDeletarSalaQuandoNaoEncontrada() throws Exception {
        //ARRANGE
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        doThrow(new RegraNegocioException(
                "Sala com id " + id + " não encontrada.",
                HttpStatus.NOT_FOUND
        )).when(salaService).deletar(anyLong());

        mockMvc.perform(delete("/api/v1/salas/{id}", id)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK ao alterar status da sala quando dados forem válidos")
    void deveriaRetornar200AoAlterarStatusQuandoDadosForemValidos() throws Exception {
        // ARRANGE
        Long id = 1L;

        StatusSalaRequest statusSalaRequest = new StatusSalaRequest(
                StatusSala.OCUPADA
        );

        SalaResponse salaResponse = createSalaResponse(id, 1);

        given(salaService.alterarStatus(id, statusSalaRequest.status()))
                .willReturn(salaResponse);

        // ACT + ASSERT
        mockMvc.perform(patch("/api/v1/salas/alterar-status/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusSalaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.capacidade").value(20));
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao alterar status da sala quando corpo da requisição for inválido")
    void deveriaRetornar400AoAlterarStatusSalaQuandoCorpoRequisicaoForInvalido() throws Exception {
        //ARRANGE
        StatusSalaRequest statusSalaRequest = new StatusSalaRequest(
                null
        );
        Long id = 1L;

        Authentication authenticationAdmin = new UsernamePasswordAuthenticationToken(
                new UserPrincipal("admin@email.com"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );

        // ACT + ASSERT
        mockMvc.perform(patch("/api/v1/salas/alterar-status/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(statusSalaRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(authenticationAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'status')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar status 401 Unauthorized ao alterar status da sala sem autenticação")
    void deveriaRetornar401AoAlterarStatusSalaQuandoNaoAutenticado() throws Exception {
        // ARRANGE
        Long id = 1L;

        // ACT + ASSERT
        mockMvc.perform(patch("/api/v1/salas/alterar-status/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    // Para usar o .with(authentication(...)) no MockMvc
    private Authentication createAuthentication() {
        UserPrincipal userPrincipal = new UserPrincipal("teste@email.com");
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }

    @Test
    @DisplayName("Deveria retornar 404 Not Found ao alterar status da sala quando não encontrada")
    void deveriaRetornar404AoAlterarStatusSalaQuandoNaoEncontrada() throws Exception {
        //ARRANGE
        Long id = 1L;

        StatusSalaRequest statusSalaRequest = new StatusSalaRequest(
                StatusSala.OCUPADA
        );

        // ACT + ASSERT
        doThrow(new RegraNegocioException(
                "Sala com id " + id + " não encontrada.",
                HttpStatus.NOT_FOUND
        )).when(salaService).alterarStatus(anyLong(), any(StatusSala.class));

        mockMvc.perform(patch("/api/v1/salas/alterar-status/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusSalaRequest))
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());
    }

    private SalaResponse createSalaResponse(Long id, Integer numero) {
        return new SalaResponse(
                id,
                numero,
                20,
                StatusSala.LIVRE
        );

    }
}