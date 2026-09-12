package br.com.jhohannesfreitas.room_ms.service;

import br.com.jhohannesfreitas.room_ms.domain.entity.Sala;
import br.com.jhohannesfreitas.room_ms.domain.enums.StatusSala;
import br.com.jhohannesfreitas.room_ms.dto.SalaRequest;
import br.com.jhohannesfreitas.room_ms.dto.SalaResponse;
import br.com.jhohannesfreitas.room_ms.dto.StatusSalaRequest;
import br.com.jhohannesfreitas.room_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.room_ms.mapper.SalaMapper;
import br.com.jhohannesfreitas.room_ms.repository.SalaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salaRepository;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    public List<SalaResponse> listarSalas(){
        return salaRepository.findAll()
                .stream()
                .map(SalaMapper::toDTO) // sala -> SalaMapper.toResponse(sala)
                .toList();
    }

    public Page<SalaResponse> listarSalasPorPagina(Pageable pageable) {
        return salaRepository.findAll(pageable)
                .map(SalaMapper::toDTO); // sala -> SalaMapper.toDTO(sala) = Para cada Sala dentro dessa Page, execute SalaMapper.toDTO().
    }

    public SalaResponse buscarSalaPorId(Long id) {
        return SalaMapper.toDTO(buscarPorId(id));
    }

    public SalaResponse cadastrar(SalaRequest salaRequest) {
        // Verifica se a sala já existe
        verificarSalaExistente(salaRequest.numero());

        // Transforma o DTO em Entity
        Sala sala = SalaMapper.toEntity(salaRequest);

        // Salvo no banco
        Sala salaSala = salaRepository.save(sala);

        // Retorno o DTO de resposta
        return SalaMapper.toDTO(salaSala);
    }

    public SalaResponse atualizar(Long id, SalaRequest salaRequest) {
        // Busca a sala pelo id no banco
        Sala sala = buscarPorId(id);

        // Verificar se existe outra sala com este número por ID
        verificarOutraSalaExistenteComMesmoNumero(salaRequest.numero(), id);

        // Settar os dados do Request na minha Entity
        sala.atualizar(salaRequest);

        // Salvar a Entity
        Sala salaAtualizada = salaRepository.save(sala);

        // Retorna o Dto de Resposta
        return SalaMapper.toDTO(salaAtualizada);

    }

    public void deletar(Long id) {
        buscarPorId(id);
        salaRepository.deleteById(id);
    }

    public SalaResponse alterarStatus(Long id, StatusSala status) {
        // verificar se a sala existe
        Sala sala = buscarPorId(id);

        // Altera o Status da sala
        sala.alterarStatus(status);

        salaRepository.save(sala);

        return SalaMapper.toDTO(sala);
    }

    private Sala buscarPorId(Long id) {
        return salaRepository.findById(id).orElseThrow(() -> new RegraNegocioException("Sala com id " + id + " não encontrada.",
                HttpStatus.NOT_FOUND));
    }

    private void verificarSalaExistente(Integer numero) {
        if (salaRepository.existsByNumero(numero)) {
            throw new RegraNegocioException("Sala já cadastrada com este número.",
                    HttpStatus.CONFLICT);
        }
    }

    private void verificarOutraSalaExistenteComMesmoNumero(Integer numero, Long id) {
        salaRepository.findByNumeroAndIdNot(numero, id)
                .ifPresent(sala -> {throw new RegraNegocioException(
                        "Sala já cadastrada com este número.",
                        HttpStatus.CONFLICT); // ifPresent = Se encontrou outra sala, lança CONFLICT.
                });
    }
}
