package br.com.jhohannesfreitas.room_ms.service;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import br.com.jhohannesfreitas.room_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.room_ms.repository.SalaRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SalaServiceTest {

    @Mock
    private SalaRepository salaRepository;

    @InjectMocks
    private SalaService salaService;

    private SalaRequest salaRequest;

    @BeforeEach
    void setUp(){
        salaRequest = new SalaRequest(1, 20);
    }

    @Test
    @DisplayName("Deveria listar todas as salas cadastradas")
    void deverialistarTodasSalasCadastradas() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Sala sala = criarSala();

        // Simula a consulta da lista com todas as salas
        given(salaRepository.findAll()).willReturn(List.of(sala));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        List<SalaResponse> listSalaResponse = salaService.listar();
        SalaResponse salaResponse = listSalaResponse.getFirst();

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaRepository).should().findAll();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertAll(
                () -> assertEquals(1, listSalaResponse.size()),
                () -> assertEquals(sala.getId(), salaResponse.id()),
                () -> assertEquals(sala.getNumero(), salaResponse.numero()),
                () -> assertEquals(sala.getCapacidade(), salaResponse.capacidade())
        );
    }

    @Test
    @DisplayName("Deveria retornar lista vazia quando não tiver as salas cadastradas")
    void deveriaRetornarListaVaziaQuandoNaoTiverSalasCadastradas() {
        //ARRANGE (PREPARAR) - Configurar o ambiente

        // Simula a consulta das salas retornando lista vazia
        given(salaRepository.findAll()).willReturn(List.of());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        List<SalaResponse> listSalaResponse = salaService.listar();

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaRepository).should().findAll();

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertTrue(listSalaResponse.isEmpty());
    }

    @Test
    @DisplayName("Deveria paginar salas quando tiver cadastradas")
    void deveriaListarPorPaginaQuandoTiverCadastradas() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Sala sala = criarSala();
        
        Sala sala2 = new Sala(2, 30);
        ReflectionTestUtils.setField(sala2, "id", 2L);
        
        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("numero").ascending());
        Page<Sala> page = new PageImpl<>(List.of(sala, sala2), pageable, 2);

        // Simula a consulta da lista com todas as salas
        given(salaRepository.findAll(pageable)).willReturn(page);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        Page<SalaResponse> salaResponse = salaService.listarPaginado(pageable);
        SalaResponse primeiraSalaResponse = salaResponse.getContent().getFirst();
        SalaResponse segundaSalaResponse = salaResponse.getContent().get(1);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaRepository).should().findAll(pageable);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertAll(
                // Informações da paginação
                () -> assertEquals(2, salaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(2, salaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(1, salaResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, salaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, salaResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(2, salaResponse.getContent().size()), // Verificar a quantidade de elementos retornados
                () -> assertEquals(pageable.getSort(), salaResponse.getSort()), // verificar a ordenação
                () -> assertTrue(salaResponse.isFirst()), // verifica se a página retornada é a primeira
                () -> assertTrue(salaResponse.isLast()), // verifica se a página retornada é a última

                () -> assertEquals(sala.getId(), primeiraSalaResponse.id()),
                () -> assertEquals(sala.getNumero(), primeiraSalaResponse.numero()),
                () -> assertEquals(sala.getCapacidade(), primeiraSalaResponse.capacidade()),
                () -> assertEquals(sala.getStatus(), primeiraSalaResponse.status()),

                () -> assertEquals(sala2.getId(), segundaSalaResponse.id()),
                () -> assertEquals(sala2.getNumero(), segundaSalaResponse.numero()),
                () -> assertEquals(sala2.getCapacidade(), segundaSalaResponse.capacidade()),
                () -> assertEquals(sala2.getStatus(), segundaSalaResponse.status())

        );
        
    }

    @Test
    @DisplayName("Deveria retornar pagina vazia de salas quando não tiverem cadastradas")
    void deveriaRetornarPaginaVaziaQuandoSalasNaoTiveremCadastradas() {
        //ARRANGE (PREPARAR) - Configurar o ambiente

        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("numero").ascending());
        Page<Sala> page = new PageImpl<>(List.of(), pageable, 0);

        // Simula a consulta da lista com todas as salas
        given(salaRepository.findAll(pageable)).willReturn(page);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        Page<SalaResponse> salaResponse = salaService.listarPaginado(pageable);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaRepository).should().findAll(pageable);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertAll(
                // Informações da paginação
                () -> assertEquals(0, salaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(0, salaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(0, salaResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, salaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, salaResponse.getSize()), // verificar o tamanho da página

                () -> assertTrue(salaResponse.isFirst()), // verifica se a página retornada é a primeira
                () -> assertTrue(salaResponse.isLast()) // verifica se a página retornada é a última

        );

    }

    @Test
    @DisplayName("Deveria buscar sala por id quando tiver cadastrada")
    void deveriaBuscarSalaPorIdQuandoTiverCadastrada() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Sala sala = criarSala();

        // Simula busca da sala por id
        given(salaRepository.findById(sala.getId())).willReturn(Optional.of(sala));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        SalaResponse salaResponse = salaService.buscarSalaPorId(sala.getId());

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaRepository).should().findById(sala.getId());

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        assertAll(
                () -> assertEquals(sala.getId(), salaResponse.id()),
                () -> assertEquals(sala.getNumero(), salaResponse.numero()),
                () -> assertEquals(sala.getCapacidade(), salaResponse.capacidade())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao buscar sala por id quando não tiver cadastrada")
    void deveriaLancarExcecaoAoBuscarSalaPorIdQuandoNaoTiverCadastrada() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long salaId = 1L;

        // Simula busca da sala por id
        given(salaRepository.findById(salaId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.buscarSalaPorId(salaId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + salaId + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().findById(salaId);

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria cadastrar sala quando não tiver número cadastrado")
    void deveriaCadastrarSalaQuandoNaoTiverNumeroCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente

        // Simula consulta retornando false que o número da sala já existe
        given(salaRepository.existsByNumero(salaRequest.numero())).willReturn(false);

        // Simula o save
        given(salaRepository.save(any(Sala.class))).willReturn(criarSala());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        SalaResponse response = salaService.cadastrar(salaRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.
        // Captura o que realmente foi passado para o save(). // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock
        ArgumentCaptor<Sala> salaCaptor = ArgumentCaptor.forClass(Sala.class);
        then(salaRepository).should().save(salaCaptor.capture());

        // "Então Repository, você DEVERIA verificar se foi chamado o 'existsByEmail()' 1x para algum objeto Usuario.
        then(salaRepository).should().existsByNumero(salaRequest.numero());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(1L, response.id()),

                // O objeto retornado pelo service
                () -> assertEquals(salaRequest.numero(), response.numero()),
                () -> assertEquals(salaRequest.capacidade(), response.capacidade()),

                // O objeto enviado ao repository
                () -> assertEquals(salaRequest.numero(), salaCaptor.getValue().getNumero()),
                () -> assertEquals(salaRequest.capacidade(), salaCaptor.getValue().getCapacidade())

        );

        // Com vários assertEquals, o teste para no primeiro erro.
        //assertEquals(1L,response.id());
        //assertEquals(usuario.getNome(),response.nome());
        //assertEquals(usuario.getEmail(),response.email());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar cadastrar sala quando tiver número já cadastrado")
    void deveriaLancarExcecaoAoCadastrarSalaQuandoTiverNumeroCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente

        // Simula consulta retornando false que o número da sala já existe
        given(salaRepository.existsByNumero(salaRequest.numero())).willReturn(true);

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.cadastrar(salaRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala já cadastrada com este número.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().existsByNumero(salaRequest.numero());

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria atualizar sala com sucesso quando tiver cadastro e dados válidos")
    void deveriaAtualizarSalaQuandoCadastrada() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Sala sala = criarSala();

        // Simula que a sala existe no banco
        given(salaRepository.findById(sala.getId())).willReturn(Optional.of(sala));

        // Simula que não existe outra sala com mesmo número por ID
        given(salaRepository.findByNumeroAndIdNot(salaRequest.numero(), sala.getId())).willReturn(Optional.empty());

        // Simula o save retornar o próprio objeto recebido
        given(salaRepository.save(any(Sala.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        SalaResponse response = salaService.atualizar(sala.getId(), salaRequest);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'save()' 1x para algum objeto Usuario.
        // Captura o que realmente foi passado para o save(). // Uso do ArgumentCaptor: Quando quero verificar se um método foi chamado, quantas vezes, objeto retornado, objeto enviado pelo mock
        ArgumentCaptor<Sala> salaCaptor = ArgumentCaptor.forClass(Sala.class);
        then(salaRepository).should().save(salaCaptor.capture());

        then(salaRepository).should().findById(sala.getId());
        then(salaRepository).should().findByNumeroAndIdNot(salaRequest.numero(), sala.getId());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(sala.getId(), response.id()),

                // O objeto retornado pelo service
                () -> assertEquals(salaRequest.numero(), response.numero()),
                () -> assertEquals(salaRequest.capacidade(), response.capacidade()),

                // O objeto enviado ao repository
                () -> assertEquals(salaRequest.numero(), salaCaptor.getValue().getNumero()),
                () -> assertEquals(salaRequest.capacidade(), salaCaptor.getValue().getCapacidade())

        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar atualizar sala quando não tiver cadastrada")
    void deveriaLancarExcecaoAoAtualizarSalaQuandoNaoTiverCadastrada() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Long salaId = 1L;

        // Simula que a sala não existe no banco
        given(salaRepository.findById(salaId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.atualizar(salaId, salaRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + salaId + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().findById(salaId);

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar atualizar sala quando número já tiver cadastrado")
    void deveriaLancarExcecaoAoAtualizarSalaQuandoNumeroJaTiverCadastrado() {
        //ARRANGE (PREPARAR) - Configurar o ambiente
        Sala sala = criarSala();

        // Outra sala
        Sala outraSala = new Sala(2, 30);
        ReflectionTestUtils.setField(outraSala, "id", 2L);

        // Simula que a sala existe no banco
        given(salaRepository.findById(sala.getId())).willReturn(Optional.of(sala));

        // Simula que existe OUTRA sala com este mesmo número
        given(salaRepository.findByNumeroAndIdNot(salaRequest.numero(), sala.getId())).willReturn(Optional.of(outraSala));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.atualizar(sala.getId(), salaRequest))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala já cadastrada com este número.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().findById(sala.getId());
        then(salaRepository).should().findByNumeroAndIdNot(salaRequest.numero(), sala.getId());

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria deletar sala quando estiver cadastrada")
    void deveriaDeletarSalaQuandoTiverCadastrada() {
        Sala sala = criarSala();

        // Simula que a sala existe no banco
        given(salaRepository.findById(sala.getId())).willReturn(Optional.of(sala));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        salaService.deletar(sala.getId());

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(salaRepository).should().findById(sala.getId());
        then(salaRepository).should().deleteById(sala.getId());

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar deletar sala quando não tiver cadastrada")
    void deveriaLancarExcecaoAoDeletarSalaQuandoNaoTiverCadastrada() {
        Long salaId = 1L;

        // Simula que a sala existe no banco
        given(salaRepository.findById(salaId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.deletar(salaId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + salaId + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().findById(salaId);
        // "Então Repository, você DEVERIA verificar que nunca foi chamado o 'deleteById()' 1x para algum objeto Usuario.
        then(salaRepository).should(never()).deleteById(salaId);

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    @Test
    @DisplayName("Deveria alterar status quando a sala existir")
    void deveriaAlterarStatusQuandoSalaExistir() {
        Sala sala = criarSala();
        sala.setStatus(StatusSala.LIVRE);

        // Simula que a sala existe no banco
        given(salaRepository.findById(sala.getId())).willReturn(Optional.of(sala));

        // Simula o save retornar o próprio objeto recebido
        given(salaRepository.save(any(Sala.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        SalaResponse salaResponse = salaService.alterarStatus(sala.getId(), StatusSala.OCUPADA);

        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        then(salaRepository).should().findById(sala.getId());
        then(salaRepository).should().save(sala);

        // "Verifique se o nome que eu esperava é igual ao nome que o método retornou."
        // assertEquals(valorEsperado, valorObtido);

        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Com assertAll, você recebe os erros de uma vez.
        assertAll(
                () -> assertEquals(sala.getId(), salaResponse.id()),
                () -> assertEquals(sala.getNumero(), salaResponse.numero()),
                () -> assertEquals(sala.getCapacidade(), salaResponse.capacidade()),
                () -> assertEquals(StatusSala.OCUPADA, salaResponse.status())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção ao tentar alterar status da sala quando não tiver cadastrada")
    void deveriaLancarExcecaoAoAlterarStatusSalaQuandoNaoTiverCadastrada() {
        Long salaId = 1L;
        StatusSala statusSala = StatusSala.LIVRE;

        // Simula que a sala existe no banco
        given(salaRepository.findById(salaId)).willReturn(Optional.empty());

        //ACT (AGIR) - A ação que se deseja testar é executada (método)
        //ASSERT (VERIFICAR) - Verifica se o resultado obtido após a ação está de acordo com o que se esperava do teste
        assertThatThrownBy(() -> salaService.alterarStatus(salaId, statusSala))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + salaId + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // "Então Repository, você DEVERIA verificar se foi chamado o 'findById()' 1x para algum objeto Usuario.
        then(salaRepository).should().findById(salaId);

        // assertEquals(valorEsperado, valorObtido);
        // 2. Verificação de estado (State Verification): Você verifica o resultado obtido. Não há estado pra verificar quando ocorre exceção.
    }

    private Sala criarSala(){
        Sala sala = new Sala(
                1,
                20
        );
        ReflectionTestUtils.setField(sala, "id", 1L);

        return sala;
    }
}