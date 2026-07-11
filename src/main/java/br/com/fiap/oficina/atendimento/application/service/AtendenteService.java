package br.com.fiap.oficina.atendimento.application.service;

import br.com.fiap.oficina.atendimento.application.port.in.AtendenteDTO;
import br.com.fiap.oficina.atendimento.application.port.in.AtendenteUseCase;
import br.com.fiap.oficina.atendimento.application.port.out.AtendenteRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtendenteService implements AtendenteUseCase {

    private final AtendenteRepositoryPort atendenteRepository;

    @Transactional
    public AtendenteDTO.Response cadastrar(AtendenteDTO.CadastrarRequest request) {
        if (atendenteRepository.existePorEmail(request.email())) {
            throw new RegraDeNegocioException("Já existe um atendente cadastrado com o email: " + request.email());
        }

        Atendente atendente = Atendente.builder()
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .build();

        return toResponse(atendenteRepository.salvar(atendente));
    }

    @Transactional(readOnly = true)
    public List<AtendenteDTO.Response> listar() {
        return atendenteRepository.listarTodos().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AtendenteDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Atendente buscarEntidade(UUID id) {
        return atendenteRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendente não encontrado com id: " + id));
    }

    @Transactional
    public AtendenteDTO.Response atualizar(UUID id, AtendenteDTO.AtualizarRequest request) {
        Atendente atendente = buscarEntidade(id);

        if (!atendente.getEmail().equals(request.email()) && atendenteRepository.existePorEmail(request.email())) {
            throw new RegraDeNegocioException("Já existe um atendente cadastrado com o email: " + request.email());
        }

        atendente.setNome(request.nome());
        atendente.setEmail(request.email());
        atendente.setTelefone(request.telefone());
        return toResponse(atendenteRepository.salvar(atendente));
    }

    @Transactional
    public void excluir(UUID id) {
        atendenteRepository.deletar(buscarEntidade(id));
    }

    private AtendenteDTO.Response toResponse(Atendente atendente) {
        return new AtendenteDTO.Response(
                atendente.getId(),
                atendente.getNome(),
                atendente.getEmail(),
                atendente.getTelefone(),
                atendente.getCriadoEm(),
                atendente.getAtualizadoEm()
        );
    }
}
