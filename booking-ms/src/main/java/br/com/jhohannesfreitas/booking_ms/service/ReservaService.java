package br.com.jhohannesfreitas.booking_ms.service;

import br.com.jhohannesfreitas.booking_ms.domain.entity.Reserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusReserva;
import br.com.jhohannesfreitas.booking_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.booking_ms.dto.*;
import br.com.jhohannesfreitas.booking_ms.http.SalaClient;
import br.com.jhohannesfreitas.booking_ms.http.UsuarioClient;
import br.com.jhohannesfreitas.booking_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.booking_ms.mapper.ReservaMapper;
import br.com.jhohannesfreitas.booking_ms.repository.ReservaRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final SalaIntegracaoService salaIntegracaoService;
    private final RabbitTemplate rabbitTemplate;

    public ReservaService(ReservaRepository reservaRepository,
                          UsuarioClient usuarioClient,
                          SalaClient salaClient,
                          SalaIntegracaoService salaIntegracaoService, RabbitTemplate rabbitTemplate) {
        this.reservaRepository = reservaRepository;
        this.usuarioClient = usuarioClient;
        this.salaClient = salaClient;
        this.salaIntegracaoService = salaIntegracaoService;
        this.rabbitTemplate = rabbitTemplate;
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
        return reservaRepository.findBySalaIdAndDataBetween(salaId, inicio, fim, pageable)
                .map(ReservaMapper::toDto);
    }

    public ReservaResponse buscarPorId(Long id) {
        return ReservaMapper.toDto(buscarReservaPorId(id));
    }

    @Transactional
    public ReservaResponse cadastrar(ReservaRequest reservaRequest, Long usuarioId) {
        // Validar se o usuário autenticado existe — erro real propaga se não existir
        UsuarioRequest usuarioRequest = usuarioClient.buscarPorId(usuarioId);

        // Validar se a sala informada existe — erro real propaga se não existir
        SalaRequest salaRequest = salaClient.buscarPorId(reservaRequest.salaId());

        validarDataNaoPodeSerNoPassado(reservaRequest.data());
        validarIntervaloReserva(reservaRequest.horaInicial(), reservaRequest.horaFinal());
        validarHorarioFuncionamento(reservaRequest.horaInicial(), reservaRequest.horaFinal());
        validarConflitoHorarioCadastro(reservaRequest);
        validarCapacidade(reservaRequest.quantidadePessoas(), salaRequest.capacidade());

        Reserva reserva = ReservaMapper.toEntity(reservaRequest, usuarioId, salaRequest.id());
        Reserva reservaSalva = reservaRepository.save(reserva);

        // Tenta marcar a sala como OCUPADA no room-ms via Circuit Breaker.
        // Se o room-ms estiver fora do ar, o fallback retorna false e a reserva
        // fica como ATIVA_SEM_INTEGRACAO até o job de reconciliação resolver.
        boolean integracaoOk = salaIntegracaoService.marcarSalaOcupada(salaRequest.id());
        if (!integracaoOk) {
            reservaSalva.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
            reservaRepository.save(reservaSalva);
        }

        // Enviar/Publicar mensagens para o RabbitMQ
        //Message message = new Message(("Crie uma reserva para a sala de id: " + reservaRequest.salaId()).getBytes());
        //rabbitTemplate.send("reserva.concluida", message);

        // Enviar/Publicar Json do DTO para o RabbitMQ
        rabbitTemplate.convertAndSend("reserva.fanout.ex","", reservaRequest); // Exchange Fanout não precisa de routingKey

        return ReservaMapper.toDto(reservaSalva);
    }

    @Transactional
    public ReservaResponse atualizar(Long id, ReservaRequest reservaRequest, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);

        // Validar se o usuário autenticado existe — erro real propaga se não existir
        UsuarioRequest usuarioRequest = usuarioClient.buscarPorId(usuarioId);

        // Validar se a sala informada existe — erro real propaga se não existir
        SalaRequest salaRequest = salaClient.buscarPorId(reservaRequest.salaId());

        validarDataNaoPodeSerNoPassado(reservaRequest.data());
        validarIntervaloReserva(reservaRequest.horaInicial(), reservaRequest.horaFinal());
        validarHorarioFuncionamento(reservaRequest.horaInicial(), reservaRequest.horaFinal());
        validarConflitoHorarioAtualizacao(reservaRequest.salaId(), reservaRequest.data(),
                reservaRequest.horaInicial(), reservaRequest.horaFinal(), StatusReserva.ATIVA, id);
        validarCapacidade(reservaRequest.quantidadePessoas(), salaRequest.capacidade());

        reserva.atualizar(reservaRequest, usuarioId, salaRequest.id());
        Reserva reservaSalva = reservaRepository.save(reserva);

        return ReservaMapper.toDto(reservaSalva);
    }

    @Transactional
    public void deletar(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);
        reservaRepository.deleteById(reserva.getId());
    }

    // Job de reconciliação: roda a cada 60 segundos e tenta resolver reservas
    // que ficaram ATIVA_SEM_INTEGRACAO porque o room-ms estava fora do ar.
    @Scheduled(fixedDelay = 60000)
    public void tentarIntegrarSalasPendentes() {
        List<Reserva> reservasPendentes = reservaRepository.findByStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        for (Reserva reserva : reservasPendentes) {
            try {
                // buscarPorId: sem Circuit Breaker — se lançar exceção, o catch captura e tenta no próximo ciclo
                var sala = salaClient.buscarPorId(reserva.getSalaId());

                // Aqui usamos salaIntegracaoService para manter o padrão, mas no job
                // é mais simples deixar o try/catch cuidar da resiliência — o Scheduled
                // já é um contexto de retry implícito (vai tentar de novo no próximo minuto)
                salaClient.alterarStatusSala(reserva.getSalaId(),
                        new StatusSalaRequest(StatusSala.OCUPADA));

                reserva.setStatus(StatusReserva.ATIVA);
                reservaRepository.save(reserva);
                System.out.println("Integração pendente resolvida para a reserva: " + reserva.getId());
            } catch (Exception e) {
                System.out.println("Tentativa de integrar reserva " + reserva.getId()
                        + " falhou. Motivo: " + e.getMessage());
            }
        }
    }

    // Rota de reconciliação manual: usada quando o operador sabe que o room-ms voltou
    public void confirmarReservaSemIntegracao(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);
        reserva.setStatus(StatusReserva.ATIVA);
        reservaRepository.save(reserva);
        salaClient.alterarStatusSala(reserva.getSalaId(),
                new StatusSalaRequest(StatusSala.OCUPADA));
        rabbitTemplate.convertAndSend("reserva.direct.ex", "reserva.detalhes-status-sala", StatusSala.OCUPADA);
    }

    public void alterarStatusReserva(Long id, Long usuarioId) {
        Reserva reserva = buscarReservaPorIdAndUsuarioId(id, usuarioId);
        reserva.setStatus(StatusReserva.ATIVA_SEM_INTEGRACAO);
        reservaRepository.save(reserva);
    }

    private Reserva buscarReservaPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException(
                        "Reserva com id " + id + " não encontrada.", HttpStatus.NOT_FOUND));
    }

    private Reserva buscarReservaPorIdAndUsuarioId(Long id, Long usuarioId) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException(
                        "Reserva com id " + id + " não encontrada.", HttpStatus.NOT_FOUND));

        if (!reserva.getUsuarioId().equals(usuarioId)) {
            throw new RegraNegocioException(
                    "Você não tem permissão para alterar ou deletar a reserva de outro usuário.",
                    HttpStatus.FORBIDDEN);
        }
        return reserva;
    }

    private void validarStatusSala(SalaRequest salaRequest) {
        if (salaRequest.status() != StatusSala.LIVRE) {
            throw new RegraNegocioException(
                    "Sala inválida. Não é possível realizar reserva para uma sala que não esteja livre.",
                    HttpStatus.CONFLICT);
        }
    }

    private void validarDataNaoPodeSerNoPassado(LocalDate data) {
        if (data.isBefore(LocalDate.now())) {
            throw new RegraNegocioException(
                    "Não é permitido realizar reservas em datas passadas.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarIntervaloReserva(LocalTime horaInicial, LocalTime horaFinal) {
        if (!horaInicial.isBefore(horaFinal)) {
            throw new RegraNegocioException(
                    "A hora inicial deve ser anterior à hora final.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarHorarioFuncionamento(LocalTime horaInicial, LocalTime horaFinal) {
        LocalTime abertura = LocalTime.of(8, 0);
        LocalTime fechamento = LocalTime.of(18, 0);
        if (horaInicial.isBefore(abertura) || horaFinal.isAfter(fechamento)) {
            throw new RegraNegocioException(
                    "Reservas devem ocorrer entre 08:00 e 18:00.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarConflitoHorarioCadastro(ReservaRequest reservaRequest) {
        List<Reserva> listaReservas = reservaRepository.findBySalaIdAndDataAndStatus(
                reservaRequest.salaId(), reservaRequest.data(), StatusReserva.ATIVA);
        for (Reserva reservaExistente : listaReservas) {
            if (reservaExistente.getHoraInicial().isBefore(reservaRequest.horaFinal())
                    && reservaExistente.getHoraFinal().isAfter(reservaRequest.horaInicial())) {
                throw new RegraNegocioException(
                        "Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    private void validarConflitoHorarioAtualizacao(Long salaId, LocalDate data,
                                                   LocalTime horaInicial, LocalTime horaFinal,
                                                   StatusReserva status, Long idReserva) {
        List<Reserva> listaReservas = reservaRepository.findBySalaIdAndDataAndStatusAndIdNot(
                salaId, data, status, idReserva);
        for (Reserva reservaExistente : listaReservas) {
            if (reservaExistente.getHoraInicial().isBefore(horaFinal)
                    && reservaExistente.getHoraFinal().isAfter(horaInicial)) {
                throw new RegraNegocioException(
                        "Conflito de horário. Já existe uma reserva para o período informado.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    private void validarCapacidade(Integer quantidade, Integer capacidade) {
        if (quantidade > capacidade) {
            throw new RegraNegocioException(
                    "A quantidade de pessoas excede a capacidade máxima da sala.", HttpStatus.CONFLICT);
        }
    }
}