package br.com.jhohannesfreitas.user_ms.service;

import br.com.jhohannesfreitas.user_ms.domain.entity.Perfil;
import br.com.jhohannesfreitas.user_ms.domain.entity.Usuario;
import br.com.jhohannesfreitas.user_ms.domain.enums.PerfilNome;
import br.com.jhohannesfreitas.user_ms.domain.enums.ProvedorLogin;
import br.com.jhohannesfreitas.user_ms.dto.PerfilRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioRequest;
import br.com.jhohannesfreitas.user_ms.dto.UsuarioResponse;
import br.com.jhohannesfreitas.user_ms.infra.exception.RegraNegocioException;
import br.com.jhohannesfreitas.user_ms.mapper.UsuarioMapper;
import br.com.jhohannesfreitas.user_ms.repository.PerfilRepository;
import br.com.jhohannesfreitas.user_ms.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listarTodosUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioMapper::toDto)
                .toList();
    }

    public Page<UsuarioResponse> listarUsuariosPorPagina(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(UsuarioMapper::toDto);
    }

    public UsuarioResponse buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).map(UsuarioMapper::toDto)
                .orElseThrow(() -> new RegraNegocioException("Usuário com id " + id + " não encontrado",
                        HttpStatus.NOT_FOUND));
    }

    // Cadastrar

    @Transactional
    public UsuarioResponse cadastrar(UsuarioRequest usuarioRequest) {
        // Verificar se o email já está cadastrado no banco
        if (usuarioRepository.existsByEmailIgnoreCase(usuarioRequest.email())) {
            throw new RegraNegocioException("Usuário já cadastrado com este e-mail",
                    HttpStatus.CONFLICT);
        }

        // Faz o Hash da Senha
        var senha = passwordEncoder.encode(usuarioRequest.senha());
        //var senha = usuarioRequest.senha(); // provisório

        // Transformar o DTO em Entity
        var usuarioDto = UsuarioMapper.toEntity(usuarioRequest, senha);

        // Setta o provedor de login para cadastro local
        usuarioDto.setProvedorLogin(ProvedorLogin.LOCAL);

        // Adiciona Perfil Padrão
        var perfil = perfilRepository.findByPerfilNome(PerfilNome.ESTUDANTE).orElseThrow();
        usuarioDto.adicionarPerfil(perfil);

        // Salvar usuario no banco
        var usuarioEntity = usuarioRepository.save(usuarioDto);

        // Retorna o DTO de resposta para o usuário.
        return UsuarioMapper.toDto(usuarioEntity);

    }

    // Atualizar

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest usuarioRequest) {
        // Busca o usuário no banco
        var usuario = buscarPorId(id);

        // Verificar se existe OUTRO usuário com o e-mail informado e ID diferente
        validarEmailDisponivel(usuarioRequest.email(), id);

        // Faz o Hash da Senha
        var senha = passwordEncoder.encode(usuarioRequest.senha());
//      var senha = usuarioRequest.senha(); // provisório

        // ATUALIZA os dados da Entity EXISTENTE
        usuario.setNome(usuarioRequest.nome());
        usuario.setEmail(usuarioRequest.email());
        usuario.setSenha(senha);

        // Salva usuário no banco
        usuario = usuarioRepository.save(usuario);

        // Retorna o Dto de resposta
        return UsuarioMapper.toDto(usuario);

    }

    // Deletar

    @Transactional
    public void deletar(Long id) {
        // Busca o usuário no banco
        buscarPorId(id);
        usuarioRepository.deleteById(id);
    }

    public UsuarioResponse adicionarPerfil(Long id, PerfilRequest perfilRequest) {
        // Busca o usuário no banco
        var usuario = buscarPorId(id);

        // Busca se o nome do perfil é válido no banco
        var perfil = perfilRepository.findByPerfilNome(perfilRequest.perfilNome())
                .orElseThrow(() -> new RegraNegocioException("Perfil de nome: " + perfilRequest.perfilNome() + " não encontrado.",
                        HttpStatus.NOT_FOUND));

        // Valida se o usuário já não possui o perfil
        if (usuario.getPerfis().contains(perfil)) {
            throw new RegraNegocioException("O usuário já possui este perfil",
                    HttpStatus.CONFLICT);
        }

        // Adiciona o perfil
        usuario.adicionarPerfil(perfil);

        // Retorna o Dto de resposta
        return UsuarioMapper.toDto(usuarioRepository.save(usuario));
    }

    public UsuarioResponse removerPerfil(Long id, PerfilRequest perfilRequest) {
        // Busca o usuário no banco
        var usuario = buscarPorId(id);

        // Busca se o nome do perfil é válido no banco
        var perfil = perfilRepository.findByPerfilNome(perfilRequest.perfilNome())
                .orElseThrow(() -> new RegraNegocioException("Perfil de nome: " + perfilRequest.perfilNome() + " não encontrado.",
                        HttpStatus.NOT_FOUND));

        // Remover o perfil
        usuario.removePerfil(perfil);

        // Retorna o Dto de resposta
        return UsuarioMapper.toDto(usuarioRepository.save(usuario));
    }



    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário com id " + id + " não encontrado.",
                        HttpStatus.NOT_FOUND));
    }

    private void validarEmailDisponivel(String email, Long id) {
        boolean emailExistente = usuarioRepository.existsByEmailAndIdNot(email, id);
        if (emailExistente) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com este e-mail.", HttpStatus.CONFLICT);
        }
    }

    public void ativarA2f(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário com id " + id + " não encontrado.",
                HttpStatus.NOT_FOUND));

        usuario.AtivarA2f();

        usuarioRepository.save(usuario);
    }
}
