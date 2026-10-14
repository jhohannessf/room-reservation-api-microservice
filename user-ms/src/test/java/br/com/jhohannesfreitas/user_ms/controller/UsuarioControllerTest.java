package br.com.jhohannesfreitas.user_ms.controller;

import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.dto.PerfilRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import br.com.jhohannesfreitas.user_ms.infra.config.JwtAuthenticationFilter;
import br.com.jhohannesfreitas.user_ms.infra.config.OAuth2LoginSuccessHandler;
import br.com.jhohannesfreitas.user_ms.infra.config.SecurityConfiguration;
import br.com.jhohannesfreitas.user_ms.infra.config.TokenProvider;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.service.UsuarioService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TokenProvider.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deveria retornar 200 OK quando listar todos os usuários")
    void deveriaRetornar200QuandoListarTodosUsuarios() throws Exception {
        // ARRANGE
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        UsuarioResponse usuarioResponse2 = new UsuarioResponse(
                43L,
                "Teste 2",
                "teste2@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );

        given(usuarioService.listar()).willReturn(List.of(usuarioResponse, usuarioResponse2));

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios")
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // tamanho do json esperado
                .andExpect(jsonPath("$[0].id").value(42L)) // id esperado para [0]
                .andExpect(jsonPath("$[1].id").value(43L)); // id esperado para [1]
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar todos os usuários sem autenticação")
    void deveriaRetornar401QuandoListarTodosOsUsuariosSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando listar usuários por páginas")
    void deveriaRetornar200QuandoListarUsuariosPorPaginas() throws Exception {
        // ARRANGE
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        UsuarioResponse usuarioResponse2 = new UsuarioResponse(
                43L,
                "Teste 2",
                "teste2@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );

        // Paginação
        Page<UsuarioResponse> page = new PageImpl<>(
                List.of(usuarioResponse, usuarioResponse2), // Conteúdo
                PageRequest.of(0, 10), // Paginação
                2 // Total de elementos
        );

        given(usuarioService.listarPaginado(any(Pageable.class))).willReturn(page);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios/listar-paginado")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2)) // tamanho do json esperado
                .andExpect(jsonPath("$.content[0].id").value(42L)) // id esperado para [0]
                .andExpect(jsonPath("$.content[1].id").value(43L)) // id esperado para [1]
                .andExpect(jsonPath("$.totalElements").value(2)); // total de elementos esperado
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando listar usuários por página sem autenticação")
    void deveriaRetornar401QuandoListarUsuariosPorPaginaSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios/listar-paginado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 200 OK quando buscar por id usuário")
    void deveriaRetornar200QuandoBuscarPorIdUsuario() throws Exception {
        // ARRANGE
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        given(usuarioService.buscarUsuarioPorId(usuarioResponse.id())).willReturn(usuarioResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios/{id}", usuarioResponse.id())
                        .with(authentication(createAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42L)); // id esperado para [0]
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized quando buscar por id usuário sem autenticação")
    void deveriaRetornar401QuandoBuscarPorIdUsuarioSemAutenticacao() throws Exception {
        //ARRANGE

        // ACT + ASSERT
        mockMvc.perform(get("/api/v1/usuarios/{id}", 44L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not found quando usuário não for encontrado por id")
    void deveriaRetornar404QuandoUsuarioNaoForEncontradoPorId() throws Exception {
        // ARRANGE
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        // ACT + ASSERT
        when(usuarioService.buscarUsuarioPorId(anyLong()))
                .thenThrow(new RegraNegocioException("Usuário com id " + usuarioResponse.id() + " não encontrado.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/usuarios/{id}", usuarioResponse.id())
                        .with(authentication(createAuthentication())))
                .andExpect(status().isNotFound());

        verify(usuarioService).buscarUsuarioPorId(42L);

    }

    @Test
    @DisplayName("Deveria retornar 201 CREATED ao cadastrar novo usuário quando dados forem válidos")
    void deveriaRetornar201AoCadastrarUsuarioQuandoDadosForemVida() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        given(usuarioService.cadastrar(usuarioRequest)).willReturn(usuarioResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.email").value("teste@email.com"));
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao cadastrar novo usuário quando dados corpo da requisição for inválido")
    void deveriaRetornar400AoCadastrarUsuarioQuandoCorpoRequisicaoForInvalido() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                null,
                "teste@email.com",
                "senha123"
        );
        UsuarioResponse usuarioResponse = createUsuarioResponse();

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao cadastrar novo usuário quando não autenticado")
    void deveriaRetornar401AoCadastrarUsuarioQuandoNaoAutenticado() throws Exception {
        //ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );

        // ACT + ASSERT
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao cadastrar novo usuário quando o service lançar exceção de usuário com este email já existe")
    void deveriaRetornar409AoCadastrarUsuarioQuandoEmailJaExiste() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );

        // ACT + ASSERT
        when(usuarioService.cadastrar(any(UsuarioRequest.class)))
                .thenThrow(new RegraNegocioException("Usuário já cadastrado com este e-mail",
                        HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthentication())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar 200 Ok ao atualizar um usuário quando dados forem válidos")
    void deveriaRetornar200AoAtualizarUsuarioQuandoDadosForemVida() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        Long id = 40L;
        UsuarioResponse usuarioResponse = createUsuarioResponsePreAuthorize();

        given(usuarioService.atualizar(id, usuarioRequest)).willReturn(usuarioResponse);

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40L))
                .andExpect(jsonPath("$.nome").value("Teste 2"))
                .andExpect(jsonPath("$.email").value("teste2@email.com"));
    }

    @Test
    @DisplayName("Deveria retornar 400 Bad Request ao atualizar um usuário quando corpo da requisição for inválido")
    void deveriaRetornar400AoAtualizarUsuarioQuandoCorpoRequisicaoForInvalido() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                null,
                "teste2@email.com",
                "senha1234"
        );
        Long id = 40L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)) //Transforma em json o objeto que foi enviando no body (serializar)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')]").exists());
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao atualizar um usuário quando não autenticado")
    void deveriaRetornar401AoAtualizarUsuarioQuandoNaoAutenticado() throws Exception {
        //ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste",
                "teste@email.com",
                "senha123"
        );
        Long id = 42L;

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden ao atualizar um usuário quando não autorizado (Id diferente)")
    void deveriaRetornar403AoAtualizarUsuarioQuandoNaoAutorizado() throws Exception {
        //ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        Long id = 42L; // a URL usa 42L e o principal usa 40L

        // ACT + ASSERT
        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest))
                        .with(authentication(createAuthenticationPreAuthorizeEstudante())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not found ao atualizar quando usuário não for encontrado")
    void deveriaRetornar404AoAtualizarQuandoUsuarioNaoForEncontrado() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        Long id = 40L;

        // ACT + ASSERT
        when(usuarioService.atualizar(any(Long.class), any(UsuarioRequest.class)))
                .thenThrow(new RegraNegocioException("Usuário com id " + id + " não encontrado.",
                        HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 409 Conflict ao atualizar usuario quando o service lançar exceção de usuário com este email já existe")
    void deveriaRetornar409AoAtualizarUsuarioQuandoEmailJaExiste() throws Exception {
        // ARRANGE
        UsuarioRequest usuarioRequest = new UsuarioRequest(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        Long id = 40L;

        // ACT + ASSERT
        when(usuarioService.atualizar(any(Long.class), any(UsuarioRequest.class)))
                .thenThrow(new RegraNegocioException("Já existe um usuário cadastrado com este e-mail.",
                        HttpStatus.CONFLICT));

        mockMvc.perform(put("/api/v1/usuarios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(usuarioRequest))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar status 204 No Content ao deletar um usuário com sucesso quando Id existir")
    void deveriaRetornar204AoDeletarUsuarioQuandoIdExistir() throws Exception {
        // ARRANGE
        Long id = 40L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/usuarios/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNoContent());

        // Prova que o service foi chamado com argumentos certos. Não somente status.
        then(usuarioService).should().deletar(id);

    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao deletar um usuário quando não autenticado")
    void deveriaRetornar401AoDeletarUsuarioQuandoNaoAutenticado() throws Exception {
        //ARRANGE
        Long id = 42L;

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/usuarios/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 Forbidden ao deletar um usuário quando não autorizado (Id diferente ou não tem ROLE_ADMINISTRADOR)")
    void deveriaRetornar403AoDeletarUsuarioQuandoNaoAutorizado() throws Exception {
        //ARRANGE
        Long id = 42L; // a URL usa 42L e o principal usa 41L

        // ACT + ASSERT
        mockMvc.perform(delete("/api/v1/usuarios/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeEstudante()))) // ROLE_ESTUDANTE
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 Not found ao deletar quando usuário não for encontrado")
    void deveriaRetornar404AoDeletarQuandoUsuarioNaoForEncontrado() throws Exception {
        // ARRANGE
        Long id = 40L;

        // ACT + ASSERT
        doThrow(new RegraNegocioException("Usuário com id " + id + " não encontrado.",
                        HttpStatus.NOT_FOUND))
                .when(usuarioService).deletar(id);

        mockMvc.perform(delete("/api/v1/usuarios/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 200 ao adicionar perfil")
    void deveriaRetornar200AoAdicionarPerfil() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        given(usuarioService.adicionarPerfil(id, request))
                .willReturn(createUsuarioResponsePreAuthorize());

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40L));
    }

    @Test
    @DisplayName("Deveria retornar 400 ao adicionar perfil com dados inválidos")
    void deveriaRetornar400AoAdicionarPerfilComDadosInvalidos() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(null);

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isBadRequest());

        then(usuarioService).should(never())
                .adicionarPerfil(anyLong(), any(PerfilRequest.class));
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao adicionar perfil sem autenticação")
    void deveriaRetornar401AoAdicionarPerfilSemAutenticacao() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 ao adicionar perfil sem ser administrador")
    void deveriaRetornar403AoAdicionarPerfilSemPermissao() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeEstudante())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 ao adicionar perfil inexistente")
    void deveriaRetornar404AoAdicionarPerfilInexistente() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        given(usuarioService.adicionarPerfil(id, request))
                .willThrow(new RegraNegocioException(
                        "Perfil não encontrado.", HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 409 ao adicionar perfil já existente")
    void deveriaRetornar409AoAdicionarPerfilExistente() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        given(usuarioService.adicionarPerfil(id, request))
                .willThrow(new RegraNegocioException(
                        "O usuário já possui este perfil.", HttpStatus.CONFLICT));

        mockMvc.perform(patch("/api/v1/usuarios/adicionar-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deveria retornar 204 ao remover perfil")
    void deveriaRetornar204AoRemoverPerfil() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ESTUDANTE);

        mockMvc.perform(patch("/api/v1/usuarios/remover-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNoContent());

        then(usuarioService).should().removerPerfil(id, request);
    }

    @Test
    @DisplayName("Deveria retornar 400 ao remover perfil com dados inválidos")
    void deveriaRetornar400AoRemoverPerfilComDadosInvalidos() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(null);

        mockMvc.perform(patch("/api/v1/usuarios/remover-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isBadRequest());

        then(usuarioService).should(never())
                .removerPerfil(anyLong(), any(PerfilRequest.class));
    }

    @Test
    @DisplayName("Deveria retornar 401 Unauthorized ao remover perfil sem autenticação")
    void deveriaRetornar401AoRemoverPerfilSemAutenticacao() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ADMINISTRADOR);

        mockMvc.perform(patch("/api/v1/usuarios/remover-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 ao remover perfil sem ser administrador")
    void deveriaRetornar403AoRemoverPerfilSemPermissao() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ESTUDANTE);

        mockMvc.perform(patch("/api/v1/usuarios/remover-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeEstudante())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 ao remover perfil inexistente")
    void deveriaRetornar404AoRemoverPerfilInexistente() throws Exception {
        Long id = 40L;
        PerfilRequest request = new PerfilRequest(PerfilNome.ESTUDANTE);

        given(usuarioService.removerPerfil(id, request))
                .willThrow(new RegraNegocioException(
                        "Perfil não encontrado.", HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/api/v1/usuarios/remover-perfil/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deveria retornar 200 ao ativar A2F")
    void deveriaRetornar200AoAtivarA2f() throws Exception {
        Long id = 40L;

        mockMvc.perform(patch("/api/v1/usuarios/ativar-a2f/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isOk());

        then(usuarioService).should().ativarA2f(id);
    }

    @Test
    @DisplayName("Deveria retornar 401 ao ativar A2F sem autenticação")
    void deveriaRetornar401AoAtivarA2fSemAutenticacao() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/ativar-a2f/{id}", 40L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deveria retornar 403 ao ativar A2F de outro usuário sem ser administrador")
    void deveriaRetornar403AoAtivarA2fSemPermissao() throws Exception {
        Long id = 42L;

        mockMvc.perform(patch("/api/v1/usuarios/ativar-a2f/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeEstudante())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deveria retornar 404 ao ativar A2F de usuário inexistente")
    void deveriaRetornar404AoAtivarA2fQuandoUsuarioNaoEncontrado() throws Exception {
        Long id = 40L;

        doThrow(new RegraNegocioException(
                "Usuário com id " + id + " não encontrado.",
                HttpStatus.NOT_FOUND))
                .when(usuarioService).ativarA2f(id);

        mockMvc.perform(patch("/api/v1/usuarios/ativar-a2f/{id}", id)
                        .with(authentication(createAuthenticationPreAuthorizeAdministrador())))
                .andExpect(status().isNotFound());
    }

    private UsuarioResponse createUsuarioResponse() {
        return new UsuarioResponse(
                42L,
                "Teste",
                "teste@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }

    private UsuarioResponse createUsuarioResponsePreAuthorize() {
        return new UsuarioResponse(
                40L,
                "Teste 2",
                "teste2@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
    }

    private Authentication createAuthentication() {
        UserPrincipal userPrincipal = new UserPrincipal("teste@email.com");
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }

    private Authentication createAuthenticationPreAuthorizeAdministrador() {
        Usuario usuario = new Usuario(
                "Teste 2",
                "teste2@email.com",
                "senha1234"
        );
        // Define o ID para simular o usuário autenticado.
        ReflectionTestUtils.setField(usuario, "id", 40L);

        return new UsernamePasswordAuthenticationToken(usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
    }

    private Authentication createAuthenticationPreAuthorizeEstudante() {
        Usuario usuario = new Usuario("Teste 3", "teste3@email.com", "senha123");
        ReflectionTestUtils.setField(usuario, "id", 41L);

        return new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ESTUDANTE"))
        );
    }
}