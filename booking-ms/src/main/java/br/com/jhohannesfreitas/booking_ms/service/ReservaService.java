package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.ReservaResponse;
import br.com.jhohannesfreitas.booking_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.booking_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.mapper.ReservaMapper;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioClient usuarioClient;
    private final SalaClient salaClient;

    public ReservaService(ReservaRepository reservaRepository, UsuarioClient usuarioClient, SalaClient salaClient) {
        this.reservaRepository = reservaRepository;
        this.usuarioClient = usuarioClient;
        this.salaClient = salaClient;
    }

    public List<ReservaResponse> listar() {
        return reservaRepository.findAll()
                .stream()
                .map(ReservaMapper::toDto)
                .toList();
    }

    public Page<ReservaResponse> listarPaginado(Pageable pageable) {
        return reservaRepository.findAll(pageable)
                .map(ReservaMapper::toDto);

    }

    public Page<ReservaResponse> listarPorSalaEIntervalo(Long salaId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        return reservaRepository.findBySalaIdAndDataBetweenAndStatus(
                salaId,
                inicio,
                fim,
                StatusReserva.ATIVA,
                pageable
        )
                .map(ReservaMapper::toDto);
    }

    public ReservaResponse buscarPorId(Long id) {
        return ReservaMapper.toDto(buscarReservaPorId(id));
    }

    @Transactional
    public ReservaResponse cadastrar(ReservaRequest reservaRequest, Long usuarioId) {
        // Validar se o usuário AUTENTICADO existe
        UsuarioRequest usuarioRequest = usuarioClient.buscarPorId(usuarioId);

        //Validar se o ID da Sala informado no DTO ReservaRequest existe
        SalaRequest salaRequest = salaClient.buscarPorId(reservaRequest.salaId());

        // Validar se a sala está com Status Livre. Não pode reservar sala inativa.
        validarStatusSala(salaRequest);

        // Validar se a data é anterior a data atual
        validarDataNaoPodeSerNoPassado(reservaRequest.data());

        // Validar intervalo entre as Reservas
        validarIntervaloReserva(reservaRequest.horaInicial(), reservaRequest.horaFinal());

        // Validar horário de funcionamento
        validarHorarioFuncionamento(reservaRequest.horaInicial(), reservaRequest.horaFinal());

        // Validar conflitos de horário
        validarConflitoHorarioCadastro(reservaRequest);

        // Validar Capacidade
        validarCapacidade(reservaRequest.quantidadePessoas(), salaRequest.capacidade());

        // Transforma DTO em ENTITY
        Reserva reserva = ReservaMapper.toEntity(reservaRequest, usuarioId, salaRequest.id());

        // Salva a ENTITY no banco
        Reserva reservaSalva = reservaRepository.save(reserva);

        // Altera o status da sala lá no room-ms para OCUPADA
        salaClient.alterarStatusSala(salaRequest.id(), StatusSala.OCUPADA);

        // Retorna o DTO de resposta
        return  ReservaMapper.toDto(reservaSalva);

    }

    @Transactional
    public ReservaResponse atualizar(Long id, ReservaRequest reservaRequest, Long usuarioId) {
        // Verifica se a Reserva existe
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);

        // Validar se o usuário AUTENTICADO existe
        UsuarioRequest usuarioRequest = usuarioClient.buscarPorId(usuarioId);

        //Validar se o ID da Sala informado no DTO ReservaRequest existe
        SalaRequest salaRequest = salaClient.buscarPorId(reservaRequest.salaId());

        // Validar status da sala
        validarStatusSala(salaRequest);

        // Validar a Data - não pode ser no passado
        validarDataNaoPodeSerNoPassado(reservaRequest.data());

        // Validar intervalo entre as reservas
        validarIntervaloReserva(reservaRequest.horaInicial(), reservaRequest.horaFinal());

        // Validar horário de funcionamento
        validarHorarioFuncionamento(reservaRequest.horaInicial(), reservaRequest.horaFinal());

        // Validar conflitos de horário, menos para o id da reserva
        validarConflitoHorarioAtualizacao(reservaRequest.salaId(),reservaRequest.data(),reservaRequest.horaInicial(),reservaRequest.horaFinal(),StatusReserva.ATIVA,id);

        // Validar capacidade
        validarCapacidade(reservaRequest.quantidadePessoas(), salaRequest.capacidade());

        // Atualizar a Entity Reserva com os dados DTO
        reserva.atualizar(reservaRequest, usuarioId, salaRequest.id());

        // Salvar a nova Entity atualizada
        Reserva reservaSalva = reservaRepository.save(reserva);

        // Retornar o DTO de resposta
        return ReservaMapper.toDto(reservaSalva);
    }

    @Transactional
    public void deletar(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);
        reservaRepository.deleteById(id);
        salaClient.alterarStatusSala(reserva.getSalaId(), StatusSala.LIVRE);
    }

    private Reserva buscarReservaPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Reserva com id " + id + " não encontrada.",
                        HttpStatus.NOT_FOUND));
    }

    private Reserva buscarReservaPorIdAndUsuarioId(Long id, Long usuarioId) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Reserva com id " + id + " não encontrada.",
                        HttpStatus.NOT_FOUND));

        if (!reserva.getUsuarioId().equals(usuarioId)) {
            throw new RegraNegocioException("Você não tem permissão para alterar ou deletar a reserva de outro usuário.",
                    HttpStatus.FORBIDDEN);
        }

        return reserva;
    }

    private void validarStatusSala(SalaRequest salaRequest) {
        if (salaRequest.status() != StatusSala.LIVRE) {
            throw new RegraNegocioException("Sala inválida. Não é possível realizar reserva para uma sala que não esteja livre.",
                    HttpStatus.CONFLICT);
        }
    }

    private void validarDataNaoPodeSerNoPassado(LocalDate data) {
        if (data.isBefore(LocalDate.now())) {
            throw new RegraNegocioException(
                    "Não é permitido realizar reservas em datas passadas.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validarIntervaloReserva(LocalTime horaInicial, LocalTime horaFinal) {
        if (!horaInicial.isBefore(horaFinal)) {
            throw new RegraNegocioException(
                    "A hora inicial deve ser anterior à hora final.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validarHorarioFuncionamento(LocalTime horaInicial, LocalTime horaFinal) {
        LocalTime abertura = LocalTime.of(8, 0);
        LocalTime fechamento = LocalTime.of(18, 0);
        if (horaInicial.isBefore(abertura) || horaFinal.isAfter(fechamento)) {
            throw new RegraNegocioException(
                    "Reservas devem ocorrer entre 08:00 e 18:00.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validarConflitoHorarioCadastro(ReservaRequest reservaRequest) {
        List<Reserva> listaReservas = reservaRepository.findBySalaIdAndDataAndStatus(reservaRequest.salaId(), reservaRequest.data(),StatusReserva.ATIVA);
        for (Reserva reservaExistente : listaReservas) {
            //verificar se há conflitos de horário
            // 1- Se existir um horário reservado que inicia ANTES(BEFORE) do horário final passado na requisição
            // 2- Se existir um horário reservado que termine DEPOIS(AFTER) do horário inicial passado na requisição
            if (reservaExistente.getHoraInicial().isBefore(reservaRequest.horaFinal())
                    && reservaExistente.getHoraFinal().isAfter(reservaRequest.horaInicial())) {
                throw new RegraNegocioException("Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    private void validarConflitoHorarioAtualizacao(Long salaId, LocalDate data, LocalTime horaInicial, LocalTime horaFinal, StatusReserva status, Long idReserva) {
        List<Reserva> listaReservas = reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(salaId, data, status, idReserva);
        for (Reserva reservaExistente : listaReservas) {
            if (reservaExistente.getHoraInicial().isBefore(horaFinal)
                    && reservaExistente.getHoraFinal().isAfter(horaInicial)) {
                throw new RegraNegocioException("Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    private void validarCapacidade(Integer quantidade, Integer capacidade) {
        if (quantidade > capacidade) {
            throw new RegraNegocioException("A quantidade de pessoas excede a capacidade máxima da sala.",
                    HttpStatus.CONFLICT);
        }
    }

    public void confirmarReservaSemIntegracao(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);

        reserva.setStatus(StatusReserva.ATIVA);
        reservaRepository.save(reserva);

        salaClient.alterarStatusSala(reserva.getSalaId(), StatusSala.OCUPADA);
    }

    public void alterarStatusReserva(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);
        reserva.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        reservaRepository.save(reserva);
    }

    // Roda automaticamente a cada 60.000 milissegundos (1 minuto)
    @Scheduled(fixedDelay = 60000)
    public void tentarIntegrarSalasPendentes() {
        // 1. Busca todas as reservas que estão aguardando integração
        List<Reserva> reservasPendentes = reservaRepository.findByStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        for (Reserva reserva : reservasPendentes) {
            try {
                // 2. Tenta fazer a comunicação com a room-ms
                var sala = salaClient.buscarPorId(reserva.getSalaId());
                salaClient.alterarStatusSala(reserva.getSalaId(), StatusSala.OCUPADA);

                // 3. Se passou pela linha de cima, significa que a room-ms VOLTOU a funcionar!
                // Então, atualizamos a reserva para totalmente ATIVA.
                reserva.setStatus(StatusReserva.ATIVA);
                reservaRepository.save(reserva);
                System.out.println("Integração pendente resolvida para a reserva: " + reserva.getId());
            } catch (Exception e) {
                // Se der erro, a room-ms AINDA está fora do ar.
                // O bloco catch impede que o sistema quebre, e ele apenas vai tentar de novo no próximo minuto.
                System.out.println("Tentativa de integrar reserva " + reserva.getId() + " falhou. room-ms ainda offline.");
            }
        }
    }
}
