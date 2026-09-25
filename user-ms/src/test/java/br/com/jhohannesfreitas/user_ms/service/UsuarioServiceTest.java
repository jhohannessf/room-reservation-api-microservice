package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.domain.enums.ProvedorLogin;
import br.com.jhohannesfreitas.user_ms.dto.PerfilRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.repository.PerfilRepository;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PerfilRepository perfilRepository;
    
    @InjectMocks
    private UsuarioService usuarioService;
    
    private UsuarioRequest usuarioRequest;

    private Perfil perfil;
    
    @BeforeEach
    void setUp() {
        usuarioRequest = new UsuarioRequest("Usuario Teste", "teste@teste.com", "teste@123");
    }

    // Sugestão de nome para testes: deveria[Comportamento]Quando[Cenário]

    @Test
    @DisplayName("Deveria listar todos os usuários cadastrados")
    void deveriaListarTodosOsUsuariosCadastrados() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Usuario usuario = criarUsuario();

        // Simula a consulta da lista de usuários
        given(usuarioRepository.findAll()).willReturn(List.of(usuario));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        List<UsuarioResponse> listResponse = usuarioService.listarTodosUsuarios();
        UsuarioResponse usuarioResponse = listResponse.get(0);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findAll()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findAll();

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertEquals(1, listResponse.size()),
                () -> assertEquals(usuario.getId(), usuarioResponse.id()),
                () -> assertEquals(usuario.getNome(), usuarioResponse.nome()),
                () -> assertEquals(usuario.getEmail(), usuarioResponse.email())
        );
    }

    @Test
    @DisplayName("Deveria retornar lista vazia quando não tiver usuários cadastrados")
    void deveriaRetornarListaVaziaQuandoNaoTiverUsuariosCadastrados() {
        //ARRANGE (PREPARAR) - Configurar o ambiente

        // Simula a consulta da lista de usuários
        given(usuarioRepository.findAll()).willReturn(List.of());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        List<UsuarioResponse> listResponse = usuarioService.listarTodosUsuarios();

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findAll()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findAll();

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertTrue(listResponse.isEmpty());

    }

    @Test
    @DisplayName("Deveria listar todos os usuários cadastrados por páginas")
    void deveriaListarTodosOsUsuariosCadastradosPaginados() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Usuario usuario = criarUsuario();

        Usuario usuario2 = new Usuario(
                "Usuario teste 2",
                "teste2@gmail.com",
                "teste@123"
        );
        ReflectionTestUtils.setField(usuario2, "id", 2L);
        
        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome").ascending());
        Page<Usuario> page = new PageImpl<>(List.of(usuario, usuario2), pageable, 2);

        // Simula a consulta de usuários por página
        given(usuarioRepository.findAll(pageable)).willReturn(page);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        Page<UsuarioResponse> usuarioResponse = usuarioService.listarUsuariosPorPagina(pageable);
        UsuarioResponse primeiroUsuarioResponse = usuarioResponse.getContent().getFirst();
        UsuarioResponse segundoUsuarioResponse = usuarioResponse.getContent().get(1);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findAll()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findAll(pageable);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(

                // Informações da paginação
                () -> assertEquals(2, usuarioResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(2, usuarioResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(1, usuarioResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, usuarioResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, usuarioResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(2, usuarioResponse.getContent().size()), // Verificar a quantidade de elementos retornados
                () -> assertEquals(pageable.getSort(), usuarioResponse.getSort()), // verificar a ordenação

                () -> assertTrue(usuarioResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(usuarioResponse.isLast()),  // Verifica se é verdade que a página retornada é a última

                () -> assertEquals(usuario.getId(), primeiroUsuarioResponse.id()),
                () -> assertEquals(usuario.getNome(), primeiroUsuarioResponse.nome()),
                () -> assertEquals(usuario.getEmail(), primeiroUsuarioResponse.email()),

                () -> assertEquals(usuario2.getId(), segundoUsuarioResponse.id()),
                () -> assertEquals(usuario2.getNome(), segundoUsuarioResponse.nome()),
                () -> assertEquals(usuario2.getEmail(), segundoUsuarioResponse.email())
        );
    }

    @Test
    @DisplayName("Deveria retornar página vazia quando não existirem usuários cadastrados")
    void deveriaRetorarPaginaVaziaQuandoNaoExistiremUsuariosCadastrados() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("nome").ascending());
        Page<Usuario> page = new PageImpl<>(List.of(), pageable, 0);
        //Page<Usuario> page = Page.empty(); // page vazia


        // Simula a consulta de usuários por página
        given(usuarioRepository.findAll(pageable)).willReturn(page);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        Page<UsuarioResponse> usuarioResponse = usuarioService.listarUsuariosPorPagina(pageable);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findAll()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findAll(pageable);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(

                // Informações da paginação
                () -> assertTrue(usuarioResponse.isEmpty()),
                () -> assertEquals(0, usuarioResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(0, usuarioResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(0, usuarioResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, usuarioResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, usuarioResponse.getSize()), // verificar o tamanho da página

                () -> assertTrue(usuarioResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(usuarioResponse.isLast())  // Verifica se é verdade que a página retornada é a última

        );
    }

    @Test
    @DisplayName("Deveria buscar por id o usuário quando tiver cadastrado")
    void deveriaBuscarUsuarioPorIdQuandoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Usuario usuario = criarUsuario();
        
        // Simula a busca de usuários
        given(usuarioRepository.findById(usuario.getId())).willReturn(Optional.of(usuario));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        UsuarioResponse usuarioResponse = usuarioService.buscarUsuarioPorId(usuario.getId());

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuario.getId());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertEquals(usuario.getId(), usuarioResponse.id()),
                () -> assertEquals(usuario.getNome(), usuarioResponse.nome()),
                () -> assertEquals(usuario.getEmail(), usuarioResponse.email())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção quando buscar usuário por id não for encontrado")
    void deveriaLancarExcecaoQuandoBuscarUsuarioPorIdNaoForEncontrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        // Simula a busca de usuários
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.buscarUsuarioPorId(usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria cadastrar usuário quando email não tiver cadastro")
    void deveriaCadastrarUsuarioQuandoNaoEstiverEmailCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        //1- Simula que NÃO existe um usuário cadastrado com este e-mail, retornando false
        given(usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())).willReturn(false);

        //2- Simula o retorno do banco após o save(), atribuindo um ID ao usuário persistido.
        Usuario usuario = criarUsuario();
        usuario.setProvedorLogin(ProvedorLogin.LOCAL);

        // Simula o Hash da senha
        given(passwordEncoder.encode(usuarioRequest.senha())).willReturn("senha");

        // Simula o perfil
        Perfil perfil = new Perfil();
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.of(perfil));

        // Simula o save
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuario);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        UsuarioResponse response = usuarioService.cadastrar(usuarioRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.
        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        then(usuarioRepository).should().save(usuarioCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'existsByEmail()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().existsByEmailIgnoreCase(usuarioRequest.email());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(1L, response.id()),
                () -> assertEquals(usuario.getNome(), response.nome()),
                () -> assertEquals(usuario.getEmail(), response.email())

        );

        // Com vários assertEquals, o teste para no primeiro erro.
        //assertEquals(1L,response.id());
        //assertEquals(usuario.getNome(),response.nome());
        //assertEquals(usuario.getEmail(),response.email());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar usuário quando estiver email cadastrado")
    void deveriaLancarExcecaoAoCadastrarUsuarioQuandoEstiverEmailCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        //1- Simula que NÃO existe um usuário cadastrado com este e-mail, retornando false
        given(usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())).willReturn(true);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.cadastrar(usuarioRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário já cadastrado com este e-mail")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'existsByEmail()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().existsByEmailIgnoreCase(usuarioRequest.email());

        // "Então Repository, você DEVERIA verificar se foi nunca foi chamado o 'save()'
        then(usuarioRepository).should(never()).save((any()));

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria atualizar usuário com sucesso")
    void deveriaAtualizarUsuarioComSucesso() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula se existe OUTRO usuário com o e-mail informado e ID diferente
        given(usuarioRepository.existsByEmailAndIdNot(usuarioRequest.email(), usuarioId)).willReturn(false);

        // Simula o hash da senha
        given((passwordEncoder.encode(usuarioRequest.senha()))).willReturn(usuarioRequest.senha());

        // Simula o save
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuarioExistente);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        UsuarioResponse usuarioResponse = usuarioService.atualizar(usuarioId, usuarioRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.
        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        then(usuarioRepository).should().save(usuarioCaptor.capture());
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should().existsByEmailAndIdNot(usuarioRequest.email(), usuarioId);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertAll(
                () -> assertEquals(usuarioId, usuarioResponse.id()),
                () -> assertEquals(usuarioRequest.nome(), usuarioResponse.nome()),
                () -> assertEquals(usuarioRequest.email(), usuarioResponse.email())
        );

    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar atualizar usuário quando id não estiver cadastrado")
    void deveriaLancarExcecaoAoAtualizarUsuarioQuandoIdNaoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.atualizar(usuarioId, usuarioRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.

        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não tem verificação de estado quando lança exceção.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar atualizar usuário quando Email já estiver cadastrado")
    void deveriaLancarExcecaoAoAtualizarUsuarioQuandoEmailEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula se existe OUTRO usuário com o e-mail informado e ID diferente
        given(usuarioRepository.existsByEmailAndIdNot(usuarioRequest.email(), usuarioId)).willReturn(true);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.atualizar(usuarioId, usuarioRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe um usuário cadastrado com este e-mail.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.

        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should().existsByEmailAndIdNot(usuarioRequest.email(), usuarioId);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não tem verificação de estado quando lança exceção.
    }

    @Test
    @DisplayName("Deveria deletar usuário por id quando estiver cadastrado")
    void deveriaDeletarUsuarioPorIdQuandoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        usuarioService.deletar(usuarioId);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should().deleteById(usuarioId);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar deletar usuário por id quando não estiver cadastrado")
    void deveriaLancarExcecaoAoDeletarUsuarioPorIdQuandoNaoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.deletar(usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should(never()).deleteById(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria adicionar perfil para o usuário quando estiver cadastrado")
    void deveriaAdicionarPerfilParaUsuarioQuandoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o perfil
        Perfil perfil = new Perfil();
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.of(perfil));

        // Simula o save
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuarioExistente);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        UsuarioResponse usuarioResponse = usuarioService.adicionarPerfil(usuarioId, perfilRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should().save(usuarioCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(perfilRepository).should().findByPerfilNome(PerfilNome.ESTUDANTE);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertEquals(usuarioId, usuarioResponse.id()),
                () -> assertTrue(usuarioCaptor.getValue().getPerfis().contains(perfil)) // é verdade que o perfil foi adicionar ao usuario

        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar adicionar perfil ao usuário por id quando não estiver cadastrado")
    void deveriaLancarExcecaoAoAdicionarPerfilQuandoNaoTiverUsuarioCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.adicionarPerfil(usuarioId, perfilRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar adicionar perfil ao usuário quando o perfil não estiver cadastrado")
    void deveriaLancarExcecaoAoAdicionarPerfilQuandoPerfilNaoTiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o perfil
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.adicionarPerfil(usuarioId, perfilRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Perfil de nome: " + perfilRequest.perfilNome() + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(perfilRepository).should().findByPerfilNome(PerfilNome.ESTUDANTE);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar adicionar perfil ao usuário que já possui o este perfil cadastrado")
    void deveriaLancarExcecaoAoAdicionarPerfilQuandoUsuarioJaPossuiEstePerfil() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o perfil
        Perfil perfil = new Perfil(PerfilNome.ESTUDANTE);
        usuarioExistente.adicionarPerfil(perfil);

        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.of(perfil));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.adicionarPerfil(usuarioId, perfilRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("O usuário já possui este perfil.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(perfilRepository).should().findByPerfilNome(PerfilNome.ESTUDANTE);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria remover perfil do usuário quando estiver cadastrado")
    void deveriaRemoverPerfilDoUsuarioQuandoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o perfil
        Perfil perfil = new Perfil(PerfilNome.ESTUDANTE);
        usuarioExistente.adicionarPerfil(perfil); // Adiciona um perfil ao usuário para garantir que havia um perfil para remover
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.of(perfil));

        // Simula o save
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuarioExistente);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        UsuarioResponse usuarioResponse = usuarioService.removerPerfil(usuarioId, perfilRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should().save(usuarioCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(perfilRepository).should().findByPerfilNome(PerfilNome.ESTUDANTE);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertEquals(usuarioId, usuarioResponse.id()),
                () -> assertFalse(usuarioCaptor.getValue().getPerfis().contains(perfil)) // é falso que o usuario contém o perfil
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar remover perfil do usuário quando o usuário não estiver cadastrado")
    void deveriaLancarExcecaoAoRemoverPerfilDoUsuarioQuandoUsuarioNaoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );

        // Simula que não existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.removerPerfil(usuarioId, perfilRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar remover perfil do usuário quando perfil não estiver cadastrado")
    void deveriaLancarExcecaoAoRemoverPerfilDoUsuarioQuandoPerfilNaoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;
        PerfilRequest perfilRequest = new PerfilRequest(
                PerfilNome.ESTUDANTE
        );
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o perfil não encontrado
        given(perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.removerPerfil(usuarioId, perfilRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Perfil de nome: " + perfilRequest.perfilNome() + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(perfilRepository).should().findByPerfilNome(PerfilNome.ESTUDANTE);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria ativar a A2F quando o usuário estiver cadastrado")
    void deveriaA2FQuandoUsuarioEstiverCadastrado() {
        Long usuarioId = 1L;
        Usuario usuarioExistente = criarUsuario();

        // Simula que existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.of(usuarioExistente));

        // Simula o save
        given(usuarioRepository.save(any(Usuario.class))).willReturn(usuarioExistente);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        usuarioService.ativarA2f(usuarioId);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(usuarioRepository).should().save(usuarioCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertEquals(usuarioId, usuarioCaptor.getValue().getId()),
                () -> assertTrue(usuarioCaptor.getValue().isA2fAtiva())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar ativar a2f do usuário quando o usuário não estiver cadastrado")
    void deveriaLancarExcecaoAoAtivarA2fDoUsuarioQuandoUsuarioNaoEstiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long usuarioId = 1L;

        // Simula que não existe um usuário cadastrado com este id no banco
        given(usuarioRepository.findById(usuarioId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> usuarioService.ativarA2f(usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(usuarioRepository).should().findById(usuarioId);
        then(usuarioRepository).should(never()).save(any());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.
    }

    private Usuario criarUsuario(){
        Usuario usuario = new Usuario(
                usuarioRequest.nome(),
                usuarioRequest.email(),
                usuarioRequest.senha()
        );
        ReflectionTestUtils.setField(usuario, "id", 1L); // Atribuindo um ID ao usuário persistido no banco
        
        return usuario;
    }
}