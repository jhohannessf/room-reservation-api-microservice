package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.*;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private SalaClient salaClient;

    @Mock
    private SalaIntegracaoService salaIntegracaoService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private KafkaTemplate<String, ReservaRequest> kafkaTemplate;

    @InjectMocks
    private ReservaService reservaService;

    private UsuarioRequest usuarioRequest;
    private SalaRequest salaRequest;

    @BeforeEach
    void setUp() {
        usuarioRequest = new UsuarioRequest(1L, "Jhou", "jhou@email.com");
        salaRequest = new SalaRequest(10L, 101, 20, StatusSala.LIVRE);
    }

    @Test
    @DisplayName("Deveria listar todas as reservas cadastradas")
    void deveriaListarTodasAsReservasCadastradas() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste

        Reserva reserva1 = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                10
        );
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        ReflectionTestUtils.setField(reserva1, "id", 1L); // Devido Reserva não ter setId público

        Reserva reserva2 = new Reserva(
                LocalDate.now(),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                10
        );
        reserva2.setUsuarioId(2L);
        reserva2.setSalaId(20L);
        ReflectionTestUtils.setField(reserva2, "id", 2L); // Devido Reserva não ter setId público

        // Então deverá retornar uma lista de reservas
        given(reservaRepository.findAll()).willReturn(List.of(reserva1, reserva2));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        List<ReservaResponse> listar = reservaService.listar();

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findAll();

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertEquals(2, listar.size());
        assertAll(
                () -> assertEquals(1L, listar.getFirst().id()),
                () -> assertEquals(1L, listar.getFirst().usuarioId()),
                () -> assertEquals(10L, listar.getFirst().salaId()),
                () -> assertEquals(2L, listar.get(1).id()),
                () -> assertEquals(2L, listar.get(1).usuarioId()),
                () -> assertEquals(20L, listar.get(1).salaId())
        );
    }

    @Test
    @DisplayName("Deveria retornar lista vazia quando não houver reservas cadastradas")
    void deveriaRetornarListaVaziaQuandoNaoHouverReservas() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        given(reservaRepository.findAll()).willReturn(List.of());

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        List<ReservaResponse> listar = reservaService.listar();

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram
        then(reservaRepository).should().findAll();

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertTrue(listar.isEmpty());
    }

    @Test
    @DisplayName("Deveria listar todas as reservas cadastradas por paginação")
    void deveriaListarTodasAsReservasCadastradasPaginadas() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste

        Reserva reserva1 = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                10
        );
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        ReflectionTestUtils.setField(reserva1, "id", 1L); // Devido Reserva não ter setId público

        Reserva reserva2 = new Reserva(
                LocalDate.now(),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                10
        );
        reserva2.setUsuarioId(2L);
        reserva2.setSalaId(20L);
        ReflectionTestUtils.setField(reserva2, "id", 2L); // Devido Reserva não ter setId público

        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("data").ascending());
        Page<Reserva> pagina = new PageImpl<>(List.of(reserva1, reserva2), pageable, 2);

        // Então deverá retornar uma página de reservas
        given(reservaRepository.findAll(pageable)).willReturn(pagina);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        Page<ReservaResponse> reservaResponse = reservaService.listarPaginado(pageable);
        ReservaResponse primeiraReserva = reservaResponse.getContent().getFirst();
        ReservaResponse segundaReserva = reservaResponse.getContent().get(1);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findAll(pageable);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(

                // Informações da paginação
                () -> assertEquals(2, reservaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(2, reservaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(1, reservaResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, reservaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, reservaResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(2, reservaResponse.getContent().size()), // Verificar a quantidade de elementos retornados
                () -> assertEquals(pageable.getSort(), reservaResponse.getSort()), // verificar a ordenação
                () -> assertTrue(reservaResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(reservaResponse.isLast()),  // Verifica se é verdade que a página retornada é a última

                // Primeira reserva
                () -> assertEquals(reserva1.getUsuarioId(), primeiraReserva.usuarioId()),
                () -> assertEquals(reserva1.getSalaId(), primeiraReserva.salaId()),
                () -> assertEquals(reserva1.getData(), primeiraReserva.data()),
                () -> assertEquals(reserva1.getHoraInicial(), primeiraReserva.horaInicial()),
                () -> assertEquals(reserva1.getHoraFinal(), primeiraReserva.horaFinal()),
                () -> assertEquals(reserva1.getQuantidadePessoas(), primeiraReserva.quantidadePessoas()),
                () -> assertEquals(reserva1.getStatus(), primeiraReserva.status()),

                // Segunda reserva
                () -> assertEquals(reserva2.getUsuarioId(), segundaReserva.usuarioId()),
                () -> assertEquals(reserva2.getSalaId(), segundaReserva.salaId()),
                () -> assertEquals(reserva2.getData(), segundaReserva.data()),
                () -> assertEquals(reserva2.getHoraInicial(), segundaReserva.horaInicial()),
                () -> assertEquals(reserva2.getHoraFinal(), segundaReserva.horaFinal()),
                () -> assertEquals(reserva2.getQuantidadePessoas(), segundaReserva.quantidadePessoas()),
                () -> assertEquals(reserva2.getStatus(), segundaReserva.status())
        );
    }

    @Test
    @DisplayName("Deveria listar pagina vazia quando não houver reservas cadastradas")
    void deveriaRetornarPaginaVaziaQuandoNaoExistiremReservasCadastradas() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste

        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("data").ascending());
        Page<Reserva> paginaVazia = Page.empty(pageable);

        // Então deverá retornar uma página vazia
        given(reservaRepository.findAll(pageable)).willReturn(paginaVazia);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        Page<ReservaResponse> reservaResponse = reservaService.listarPaginado(pageable);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findAll(pageable);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertTrue(reservaResponse.isEmpty()),
                () -> assertEquals(0, reservaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(0, reservaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(0, reservaResponse.getContent().size()), // Verificar a quantidade de elementos retornados

                () -> assertEquals(0, reservaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, reservaResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(pageable.getSort(), reservaResponse.getSort()), // verificar a ordenação
                () -> assertTrue(reservaResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(reservaResponse.isLast())  // Verifica se é verdade que a página retornada é a última
        );
    }

    @Test
    @DisplayName("Deveria listar todas as reservas cadastradas por sala e intervalo por paginação")
    void deveriaListarTodasAsReservasCadastradasPorSalaEIntervaloPaginadas() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste

        Reserva reserva1 = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                10
        );
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        ReflectionTestUtils.setField(reserva1, "id", 1L); // Devido Reserva não ter setId público

        Reserva reserva2 = new Reserva(
                LocalDate.now(),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                10
        );
        reserva2.setUsuarioId(2L);
        reserva2.setSalaId(20L);
        ReflectionTestUtils.setField(reserva2, "id", 2L); // Devido Reserva não ter setId público

        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("data").ascending());
        Page<Reserva> pagina = new PageImpl<>(List.of(reserva1, reserva2), pageable, 2);

        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);

        // Simula a busca da Reserva por salaId, Data e Status
        given(reservaRepository.findBySalaIdAndDataBetween(reserva1.getSalaId(), inicio, fim, pageable)).willReturn(pagina);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        Page<ReservaResponse> reservaResponse = reservaService.listarPorSalaEIntervalo(reserva1.getSalaId(), inicio, fim, pageable);
        ReservaResponse primeiraReserva = reservaResponse.getContent().getFirst();
        ReservaResponse segundaReserva = reservaResponse.getContent().get(1);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findBySalaIdAndDataBetween(reserva1.getSalaId(), inicio, fim, pageable);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(

                // Informações da paginação
                () -> assertEquals(2, reservaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(2, reservaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(1, reservaResponse.getTotalPages()), // verificar a quantidade total de páginas
                () -> assertEquals(0, reservaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, reservaResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(2, reservaResponse.getContent().size()), // Verificar a quantidade de elementos retornados
                () -> assertEquals(pageable.getSort(), reservaResponse.getSort()), // verificar a ordenação
                () -> assertTrue(reservaResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(reservaResponse.isLast()),  // Verifica se é verdade que a página retornada é a última

                // Primeira reserva
                () -> assertEquals(reserva1.getUsuarioId(), primeiraReserva.usuarioId()),
                () -> assertEquals(reserva1.getSalaId(), primeiraReserva.salaId()),
                () -> assertEquals(reserva1.getData(), primeiraReserva.data()),
                () -> assertEquals(reserva1.getHoraInicial(), primeiraReserva.horaInicial()),
                () -> assertEquals(reserva1.getHoraFinal(), primeiraReserva.horaFinal()),
                () -> assertEquals(reserva1.getQuantidadePessoas(), primeiraReserva.quantidadePessoas()),
                () -> assertEquals(reserva1.getStatus(), primeiraReserva.status()),

                // Segunda reserva
                () -> assertEquals(reserva2.getUsuarioId(), segundaReserva.usuarioId()),
                () -> assertEquals(reserva2.getSalaId(), segundaReserva.salaId()),
                () -> assertEquals(reserva2.getData(), segundaReserva.data()),
                () -> assertEquals(reserva2.getHoraInicial(), segundaReserva.horaInicial()),
                () -> assertEquals(reserva2.getHoraFinal(), segundaReserva.horaFinal()),
                () -> assertEquals(reserva2.getQuantidadePessoas(), segundaReserva.quantidadePessoas()),
                () -> assertEquals(reserva2.getStatus(), segundaReserva.status())
        );
    }

    @Test
    @DisplayName("Deveria retornar página vazia quando não existem reservas da sala no intervalo informado")
    void deveriaRetornarPaginaVaziaQuandoNaoExistiremReservasDaSalaNoIntervaloInformado() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long salaId = 1L;

        // Paginação
        Pageable pageable = PageRequest.of(0, 10, Sort.by("data").ascending());
        Page<Reserva> paginaVazia = Page.empty(pageable);

        LocalDate inicio = LocalDate.of(2026, 7, 1);
        LocalDate fim = LocalDate.of(2026, 7, 31);

        // Simula a busca da Reserva por salaId, Data e Status
        given(reservaRepository.findBySalaIdAndDataBetween(salaId, inicio, fim, pageable)).willReturn(paginaVazia);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        Page<ReservaResponse> reservaResponse = reservaService.listarPorSalaEIntervalo(salaId, inicio, fim, pageable);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findBySalaIdAndDataBetween(salaId, inicio, fim, pageable);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(
                () -> assertTrue(reservaResponse.isEmpty()),
                () -> assertEquals(0, reservaResponse.getNumberOfElements()), // verificar a quantidade de elementos na página atual
                () -> assertEquals(0, reservaResponse.getTotalElements()), // verificar a quantidade total de registros
                () -> assertEquals(0, reservaResponse.getContent().size()), // Verificar a quantidade de elementos retornados

                () -> assertEquals(0, reservaResponse.getNumber()), // verificar o número da página atual
                () -> assertEquals(10, reservaResponse.getSize()), // verificar o tamanho da página
                () -> assertEquals(pageable.getSort(), reservaResponse.getSort()), // verificar a ordenação
                () -> assertTrue(reservaResponse.isFirst()), // Verifica se é verdade que a página retornada é a primeira
                () -> assertTrue(reservaResponse.isLast())  // Verifica se é verdade que a página retornada é a última
        );
    }

    @Test
    @DisplayName("Deveria buscar a reserva por id se tiver cadastro")
    void deveriaBuscarPorIdReservaQuandoEstiverCadastrada() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste

        Reserva reserva1 = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                10
        );
        reserva1.setUsuarioId(1L);
        reserva1.setSalaId(10L);
        ReflectionTestUtils.setField(reserva1, "id", 1L); // Devido Reserva não ter setId público

        // Simula a busca da Reserva por id
        given(reservaRepository.findById(reserva1.getId())).willReturn(Optional.of(reserva1));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        ReservaResponse reservaResponse = reservaService.buscarPorId(reserva1.getId());

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(reservaRepository).should().findById(reserva1.getId());

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertAll(

                () -> assertEquals(reserva1.getUsuarioId(), reservaResponse.usuarioId()),
                () -> assertEquals(reserva1.getSalaId(), reservaResponse.salaId()),
                () -> assertEquals(reserva1.getData(), reservaResponse.data()),
                () -> assertEquals(reserva1.getHoraInicial(), reservaResponse.horaInicial()),
                () -> assertEquals(reserva1.getHoraFinal(), reservaResponse.horaFinal()),
                () -> assertEquals(reserva1.getQuantidadePessoas(), reservaResponse.quantidadePessoas()),
                () -> assertEquals(reserva1.getStatus(), reservaResponse.status())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção buscar reserva por id não for encontrada")
    void deveriaLancarExcecaoQuandoBuscarReservaPorIdInexistente() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long reservaId = 1L;
        given(reservaRepository.findById(reservaId)).willReturn(Optional.empty());

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado

        assertThatThrownBy(() -> reservaService.buscarPorId(reservaId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reserva com id " + reservaId + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        then(reservaRepository).should().findById(reservaId);

    }

    @Test
    @DisplayName("Deveria cadastrar a reserva com sucesso quando os dados informados forem válidos")
    void deveriaCadastrarReservaQuandoDadosForemValidos() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva existente por salaId, data e status retornando uma lista vazia
        given(reservaRepository.findBySalaIdAndDataAndStatus(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA))
                .willReturn(List.of());

        // Simula que a integração está ok, não entra no Circuit Breaker e retorna true
        given(salaIntegracaoService.marcarSalaOcupada(salaRequest.id())).willReturn(true);

        // Simula um save() "de verdade": devolve o próprio objeto que recebeu como argumento
        given(reservaRepository.save(any(Reserva.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        ReservaResponse reservaResponse = reservaService.cadastrar(reservaRequest, usuarioId);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should().save(reservaCaptor.capture());
        Reserva reservaCapturada = reservaCaptor.getValue();

        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Usuario".
        then(usuarioClient).should().buscarPorId(usuarioId);
        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Sala".
        then(salaClient).should().buscarPorId(reservaRequest.salaId());
        // "Então Repository, você DEVERIA verificar se foi chamado o 'findBySalaIdAndDataAndStatus()' do objeto Reserva".
        then(reservaRepository).should().findBySalaIdAndDataAndStatus(reservaRequest.salaId(), reservaRequest.data(), StatusReserva.ATIVA);
        // "Então kafkaTemplate, você DEVERIA verificar se foi enviado mensagens
        then(kafkaTemplate).should().send("booking-created", reservaRequest);
        // "Então rabbitTemplate, você DEVERIA verificar que não há mensagem interação de mensagens
        then(rabbitTemplate).shouldHaveNoInteractions();
        //then(rabbitTemplate).convertAndSend("reserva.fanout.ex","", reservaRequest); // Foi comentado no cadastro também

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Verificando se os dados retornados são iguais aos da entidade persistida
        assertAll(
                () -> assertEquals(reservaRequest.data(), reservaCapturada.getData()),
                () -> assertEquals(reservaRequest.horaInicial(), reservaCapturada.getHoraInicial()),
                () -> assertEquals(reservaRequest.horaFinal(), reservaCapturada.getHoraFinal()),
                () -> assertEquals(reservaRequest.quantidadePessoas(), reservaCapturada.getQuantidadePessoas()),
                () -> assertEquals(usuarioId, reservaCapturada.getUsuarioId()),
                () -> assertEquals(salaRequest.id(), reservaCapturada.getSalaId()),
                () -> assertEquals(StatusReserva.ATIVA, reservaCapturada.getStatus()),
                () -> assertEquals(reservaCapturada.getData(), reservaResponse.data()),
                () -> assertEquals(usuarioId, reservaResponse.usuarioId())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção quando o usuário informado não existir")
    void deveriaLancarExcecaoQuandoUsuarioNaoExistir() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);

        given(usuarioClient.buscarPorId(usuarioId))
                .willThrow(new RegraNegocioException("Usuário com id " + usuarioId + " não encontrado.",
                        HttpStatus.NOT_FOUND));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado

        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Garante que a falha interrompeu o fluxo antes de qualquer validação/persistência ou envio de mensagens
        then(salaClient).shouldHaveNoInteractions();
        then(reservaRepository).shouldHaveNoInteractions();
        then(salaIntegracaoService).shouldHaveNoInteractions();
        then(kafkaTemplate).shouldHaveNoInteractions();
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Deveria lançar exceção quando a sala informada não existir")
    void deveriaLancarExcecaoQuandoSalaNaoExistir() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);

        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId()))
                .willThrow(new RegraNegocioException("Sala com id " + reservaRequest.salaId() + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado

        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + reservaRequest.salaId() + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Garante que a falha interrompeu o fluxo antes de qualquer validação/persistência ou envio de mensagens
        then(reservaRepository).shouldHaveNoInteractions();
        then(salaIntegracaoService).shouldHaveNoInteractions();
        then(kafkaTemplate).shouldHaveNoInteractions();
        then(rabbitTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva com data no passado")
    void deveriaLancarExcecaoQuandoDataForNoPassado() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now().minusDays(5),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                20
        );

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é permitido realizar reservas em datas passadas.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva quando Hora inicial for depois da hora final")
    void deveriaLancarExcecaoQuandoHoraInicialForDepoisHoraFinal() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(11, 0),
                LocalTime.of(10, 0),
                20
        );

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A hora inicial deve ser anterior à hora final.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva quando Hora inicial for igual a hora final")
    void deveriaLancarExcecaoQuandoHoraInicialForIgualAHoraFinal() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(10, 0),
                LocalTime.of(10, 0),
                20
        );

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A hora inicial deve ser anterior à hora final.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva antes do horário de funcionamento")
    void deveriaLancarExcecaoQuandoForAntesDoHorarioFuncionamento() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(7, 0),
                LocalTime.of(8, 0),
                20
        );

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reservas devem ocorrer entre 08:00 e 18:00.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva depois do horário de funcionamento")
    void deveriaLancarExcecaoQuandoForDepoisDoHorarioFuncionamento() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                20
        );

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reservas devem ocorrer entre 08:00 e 18:00.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva com data e horário existente")
    void deveriaLancarExcecaoQuandoExisteConflitoDeHorarioAoCadastrar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva por salaId, data e status
        given(reservaRepository.findBySalaIdAndDataAndStatus(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA))
                .willReturn(List.of(reservaExistente));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Conflito de horário. Já existe uma reserva para o período informado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());

    }

    @Test
    @DisplayName("Deveria lançar exceção ao cadastrar reserva com quantidade de pessoas maior do que a capacidade da sala")
    void deveriaLancarExcecaoQuandoQuantidadePessoasForMaiorQueCapacidadeSala() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 30);

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva existente por salaId, data e status retornando uma lista vazia
        given(reservaRepository.findBySalaIdAndDataAndStatus(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA))
                .willReturn(List.of());

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.cadastrar(reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A quantidade de pessoas excede a capacidade máxima da sala.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // Garante que, tendo lançado exceção, NADA foi salvo nem publicado no RabbitMQ ou Kafka
        verify(reservaRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(any(), anyString()); // Foi comentado no cadastro
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("Deveria atualizar a reserva com sucesso quando os dados informados forem válidos")
    void deveriaAtualizarReservaQuandoDadosForemValidos() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva existente por salaId, data e status, menos do ID da própria Reserva
        given(reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA,
                reservaId))
                .willReturn(List.of());

        // Simula um save() "de verdade": devolve o próprio objeto que recebeu como argumento
        given(reservaRepository.save(any(Reserva.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        ReservaResponse reservaResponse = reservaService.atualizar(reservaId, reservaRequest, usuarioId);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should().save(reservaCaptor.capture());
        Reserva reservaCapturada = reservaCaptor.getValue();

        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Usuario".
        then(usuarioClient).should().buscarPorId(usuarioId);
        // "Então Client, você DEVERIA verificar se foi chamado o 'buscarPorId()' do objeto Sala".
        then(salaClient).should().buscarPorId(reservaRequest.salaId());
        // "Então Repository, você DEVERIA verificar se foi chamado o 'findBySalaIdAndDataAndStatus()' do objeto Reserva".
        then(reservaRepository).should().findBySalaIdAndDataAndStatusAndIdNot(reservaRequest.salaId(), reservaRequest.data(), StatusReserva.ATIVA, reservaId);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

        // Verificando se os dados retornados são iguais aos da entidade persistida
        assertAll(
                () -> assertEquals(reservaRequest.data(), reservaCapturada.getData()),
                () -> assertEquals(reservaRequest.horaInicial(), reservaCapturada.getHoraInicial()),
                () -> assertEquals(reservaRequest.horaFinal(), reservaCapturada.getHoraFinal()),
                () -> assertEquals(reservaRequest.quantidadePessoas(), reservaCapturada.getQuantidadePessoas()),
                () -> assertEquals(usuarioId, reservaCapturada.getUsuarioId()),
                () -> assertEquals(salaRequest.id(), reservaCapturada.getSalaId()),
                () -> assertEquals(StatusReserva.ATIVA, reservaCapturada.getStatus()),
                () -> assertEquals(reservaCapturada.getData(), reservaResponse.data()),
                () -> assertEquals(usuarioId, reservaResponse.usuarioId())
        );
    }

    @Test
    @DisplayName("Deveria lançar exceção quando o usuário informado não existir ao atualizar")
    void deveLancarExcecaoQuandoUsuarioNaoExistirAoAtualizar() {
        // ARRANGE
        Long usuarioId = 1L;
        Long reservaId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 5);

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(), LocalTime.of(9, 30), LocalTime.of(10, 30), 5);
        reservaExistente.setUsuarioId(usuarioId); // precisa ser do usuário autenticado, senão FORBIDDEN estoura antes

        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));
        given(usuarioClient.buscarPorId(usuarioId))
                .willThrow(new RegraNegocioException(
                        "Usuário com id " + usuarioId + " não encontrado.", HttpStatus.NOT_FOUND));

        // ACT + ASSERT
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Usuário com id " + usuarioId + " não encontrado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        then(salaClient).shouldHaveNoInteractions();
        then(reservaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva de outro usuário")
    void deveriaLancarExcecaoQuandoAtualizarReservaNaoPertenceAoUsuario() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioAutenticadoId = 1L;
        Long usuarioDonoReservaId = 2L;
        Long reservaId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);

        Reserva reservaOutroUsuario = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20
        );
        reservaOutroUsuario.setUsuarioId(usuarioDonoReservaId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaOutroUsuario));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioAutenticadoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Você não tem permissão para alterar ou deletar a reserva de outro usuário.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // A checagem de posse acontece ANTES de qualquer chamada externa ou persistência
        then(usuarioClient).shouldHaveNoInteractions();
        then(salaClient).shouldHaveNoInteractions();
        then(reservaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar quando a sala informada não existir")
    void deveriaLancarExcecaoQuandoSalaNaoExistirAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula a busca de usuário e sala via open Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId()))
                .willThrow(new RegraNegocioException("Sala com id " + reservaRequest.salaId() + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado

        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Sala com id " + reservaRequest.salaId() + " não encontrada.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva com data no passado")
    void deveriaLancarExcecaoQuandoDataForNoPassadoAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now().minusDays(5), 20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não é permitido realizar reservas em datas passadas.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva quando Hora inicial for depois da hora final")
    void deveriaLancarExcecaoQuandoHoraInicialForDepoisHoraFinalAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now(),
                LocalTime.of(12, 0),
                LocalTime.of(11, 0),
                20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A hora inicial deve ser anterior à hora final.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva quando Hora inicial for igual a hora final")
    void deveriaLancarExcecaoQuandoHoraInicialForIgualAHoraFinalAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now(),
                LocalTime.of(12, 0),
                LocalTime.of(12, 0),
                20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A hora inicial deve ser anterior à hora final.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva antes do horário de funcionamento")
    void deveriaLancarExcecaoQuandoForAntesDoHorarioFuncionamentoAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                10L,
                LocalDate.now(),
                LocalTime.of(7, 0),
                LocalTime.of(8, 0),
                20
        );
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reservas devem ocorrer entre 08:00 e 18:00.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva depois do horário de funcionamento")
    void deveriaLancarExcecaoQuandoForDepoisDoHorarioFuncionamentoAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = new ReservaRequest(
                1L,
                LocalDate.now(),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Reservas devem ocorrer entre 08:00 e 18:00.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva com data e horário existente")
    void deveriaLancarExcecaoQuandoExisteConflitoDeHorarioAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 20);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        Reserva reservaConflitante = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaConflitante.setUsuarioId(2L); // outro usuário, outra reserva

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva por salaId, data e status
        given(reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA,
                reservaId))
                .willReturn(List.of(reservaConflitante));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Conflito de horário. Já existe uma reserva para o período informado.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // A checagem de posse acontece ANTES de qualquer persistência
        then(reservaRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("Deveria lançar exceção ao atualizar reserva com quantidade de pessoas maior do que a capacidade da sala")
    void deveriaLancarExcecaoQuandoQuantidadePessoasForMaiorQueCapacidadeSalaAoAtualizar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        ReservaRequest reservaRequest = criarReservaTest(LocalDate.now(), 30);
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                reservaRequest.data(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // Simula busca por id usando Feing
        given(usuarioClient.buscarPorId(usuarioId)).willReturn(usuarioRequest);
        given(salaClient.buscarPorId(reservaRequest.salaId())).willReturn(salaRequest);

        // Simula consulta do banco para buscar Reserva por salaId, data e status. Com exceção do id da própria reserva.
        given(reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(
                reservaRequest.salaId(),
                reservaRequest.data(),
                StatusReserva.ATIVA,
                reservaId))
                .willReturn(List.of());

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.atualizar(reservaId, reservaRequest, usuarioId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("A quantidade de pessoas excede a capacidade máxima da sala.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        // Garante que, tendo lançado exceção, NADA foi salvo
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deveria deletar a reserva com sucesso quando os dados informados forem válidos")
    void deveriaDeletarReservaQuandoDadosForemValidos() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioId = 1L;
        Long reservaId = 1L;

        Reserva reservaExistente = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaExistente.setUsuarioId(usuarioId);
        ReflectionTestUtils.setField(reservaExistente, "id", reservaId); // porque não existe setId() publico

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaExistente));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.deletar(reservaId, usuarioId);

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.

        // Captura o que realmente foi passado para o deleteById()
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        then(reservaRepository).should().deleteById(idCaptor.capture());
        assertEquals(reservaId, idCaptor.getValue());

        then(usuarioClient).shouldHaveNoInteractions();
        then(salaClient).shouldHaveNoInteractions();
        then(reservaRepository).should(never()).save(any());

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.

    }

    @Test
    @DisplayName("Deveria lançar exceção ao deletar reserva de outro usuário")
    void deveriaLancarExcecaoQuandoDeletarReservaNaoPertenceAoUsuario() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Long usuarioAutenticadoId = 1L;
        Long usuarioDonoReservaId = 2L;
        Long reservaId = 1L;

        Reserva reservaOutroUsuario = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20
        );
        reservaOutroUsuario.setUsuarioId(usuarioDonoReservaId);

        // Simula a busca por Reserva no banco
        given(reservaRepository.findById(reservaId)).willReturn(Optional.of(reservaOutroUsuario));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        assertThatThrownBy(() -> reservaService.deletar(reservaId, usuarioAutenticadoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Você não tem permissão para alterar ou deletar a reserva de outro usuário.")
                .extracting(ex -> ((RegraNegocioException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // A checagem de posse acontece ANTES de qualquer chamada externa ou persistência
        then(reservaRepository).should(never()).deleteById((any()));
        then(usuarioClient).shouldHaveNoInteractions();
        then(salaClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Deveria integrar a sala e marcar reserva como ATIVA quando a chamada ao room-ms tiver sucesso")
    void deveriaIntegrarSalaPendenteComSucesso() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Reserva reservaPendente = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaPendente.setUsuarioId(1L);
        reservaPendente.setSalaId(10L);
        reservaPendente.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        ReflectionTestUtils.setField(reservaPendente, "id", 1L); // porque não existe setId() publico

        // Simula a busca de Reservas por status no banco
        given(reservaRepository.findByStatus(reservaPendente.getStatus())).willReturn(List.of(reservaPendente));

        // Simula busca de sala por id via open feing
        given(salaClient.buscarPorId(reservaPendente.getSalaId())).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.tentarIntegrarSalasPendentes();

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaClient).should().alterarStatusSala(eq(10L), any(StatusSalaRequest.class));

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should().save(reservaCaptor.capture());
        assertEquals(StatusReserva.ATIVA, reservaCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deveria manter reserva como ATIVA_SEM_INTEGRACAO quando a chamada ao room-ms falhar")
    void deveriaManterReservaPendenteQuandoIntegracaoFalhar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Reserva reservaPendente = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaPendente.setUsuarioId(1L);
        reservaPendente.setSalaId(10L);
        reservaPendente.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        ReflectionTestUtils.setField(reservaPendente, "id", 1L); // porque não existe setId() publico

        // Simula a busca de Reservas por status no banco
        given(reservaRepository.findByStatus(reservaPendente.getStatus())).willReturn(List.of(reservaPendente));

        // Simula busca de sala por id via open feing
        given(salaClient.buscarPorId(reservaPendente.getSalaId()))
                .willThrow(new RuntimeException("room-ms indisponível"));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.tentarIntegrarSalasPendentes();

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaClient).should(never()).alterarStatusSala(any(), any());
        then(reservaRepository).should(never()).save(any());

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, reservaPendente.getStatus());
    }

    @Test
    @DisplayName("Deveria continuar processando as demais reservas quando uma delas falhar")
    void deveriaContinuarProcessandoQuandoUmaReservaFalhar() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Reserva reservaComFalha = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaComFalha.setUsuarioId(1L);
        reservaComFalha.setSalaId(10L);
        reservaComFalha.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        ReflectionTestUtils.setField(reservaComFalha, "id", 1L); // porque não existe setId() publico

        Reserva reservaComSucesso = new Reserva(LocalDate.now(), LocalTime.of(11, 0), LocalTime.of(12, 0), 5);
        reservaComSucesso.setUsuarioId(2L);
        reservaComSucesso.setSalaId(20L);
        reservaComSucesso.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        ReflectionTestUtils.setField(reservaComSucesso, "id", 2L);

        // Simula a busca de Reservas por status no banco
        given(reservaRepository.findByStatus(reservaComFalha.getStatus())).willReturn(List.of(reservaComFalha, reservaComSucesso));

        // Simula busca de sala por id via open feing
        given(salaClient.buscarPorId(10L)).willThrow(new RuntimeException("room-ms indisponível"));
        given(salaClient.buscarPorId(20L)).willReturn(salaRequest);

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.tentarIntegrarSalasPendentes();

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        then(salaClient).should(never()).alterarStatusSala(eq(10L), any());
        then(salaClient).should().alterarStatusSala(eq(20L), any(StatusSalaRequest.class));

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        // Captura o que realmente foi passado para o save()
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should(times(1)).save(reservaCaptor.capture());
        assertEquals(20L, reservaCaptor.getValue().getSalaId());
        assertEquals(StatusReserva.ATIVA, reservaCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deveria confirmar reserva sem integração manualmente")
    void deveriaConfirmarReservaSemIntegracao(){
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Reserva reservaPendente = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaPendente.setUsuarioId(1L);
        reservaPendente.setSalaId(10L);
        reservaPendente.setStatus(StatusReserva.ATIVA);
        ReflectionTestUtils.setField(reservaPendente, "id", 1L); // porque não existe setId() publico

        //Simula busca reserva por id e usuarioId
        given(reservaRepository.findById(reservaPendente.getId())).willReturn(Optional.of(reservaPendente));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.confirmarReservaSemIntegracao(reservaPendente.getId(), reservaPendente.getUsuarioId());

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should(times(1)).save(reservaCaptor.capture());
        assertEquals(StatusReserva.ATIVA, reservaCaptor.getValue().getStatus());

        then(salaClient).should().alterarStatusSala(eq(10L), any(StatusSalaRequest.class));
        then(rabbitTemplate).should().convertAndSend("reserva.direct.ex", "reserva.detalhes-status-sala", StatusSala.OCUPADA);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        // Captura o que realmente foi passado para o save()

        assertEquals(reservaPendente.getSalaId(), reservaCaptor.getValue().getSalaId());
        assertEquals(reservaPendente.getUsuarioId(), reservaCaptor.getValue().getUsuarioId());
    }

    @Test
    @DisplayName("Deveria alterar o status da reserva manual")
    void deveriaAlterarStatusReserva() {
        // Padrão AAA
        // 1- ARRANGE -> Preparar o ambiente de teste
        Reserva reservaPendente = new Reserva(
                LocalDate.now(),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                20);
        reservaPendente.setUsuarioId(1L);
        reservaPendente.setSalaId(10L);
        reservaPendente.setStatus(StatusReserva.ATIVA);
        ReflectionTestUtils.setField(reservaPendente, "id", 1L); // porque não existe setId() publico

        //Simula busca reserva por id e usuarioId
        given(reservaRepository.findById(reservaPendente.getId())).willReturn(Optional.of(reservaPendente));

        // 2- ACT -> Ação que deseja testar é executada (Chamada do Método)
        reservaService.alterarStatusReserva(reservaPendente.getId(), reservaPendente.getUsuarioId());

        // 3 - ASSERT -> Verificar se o resultado obtido após a ação é o esperado
        // 3.1. Verificação de comportamento (Behavior Verification): Você verifica que determinadas chamadas aconteceram.
        ArgumentCaptor<Reserva> reservaCaptor = ArgumentCaptor.forClass(Reserva.class);
        then(reservaRepository).should(times(1)).save(reservaCaptor.capture()); // times(1) é padrão, desnecessário colocar.
        then(reservaRepository).should(times(1)).save(reservaPendente);

        // 3.2. Verificação de estado (State Verification): Você verifica o resultado obtido.
        assertEquals(StatusReserva.ATIVA_SEM_INTEGRACAO, reservaCaptor.getValue().getStatus());

    }

    private ReservaRequest criarReservaTest(LocalDate data, Integer quantidadePessoas) {
        return new ReservaRequest(10L, data, LocalTime.of(10, 0), LocalTime.of(11, 0), quantidadePessoas);
    }
}