package br.com.fiap.oficina.administracao.service;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.administracao.repository.ServicoRepository;
import br.com.fiap.oficina.administracao.service.dto.ServicoDTO;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;

    @Transactional
    public ServicoDTO.Response cadastrar(ServicoDTO.CadastrarRequest request) {
        if (servicoRepository.existsByNome(request.nome())) {
            throw new RegraDeNegocioException("Já existe um serviço cadastrado com o nome: " + request.nome());
        }

        Servico servico = Servico.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .precoBase(request.precoBase())
                .build();

        return toResponse(servicoRepository.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoDTO.Response> listar() {
        return servicoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServicoDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Servico buscarEntidade(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado com id: " + id));
    }

    @Transactional
    public ServicoDTO.Response atualizar(UUID id, ServicoDTO.AtualizarRequest request) {
        Servico servico = buscarEntidade(id);

        if (!servico.getNome().equals(request.nome()) && servicoRepository.existsByNome(request.nome())) {
            throw new RegraDeNegocioException("Já existe um serviço cadastrado com o nome: " + request.nome());
        }

        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setPrecoBase(request.precoBase());
        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(UUID id) {
        servicoRepository.delete(buscarEntidade(id));
    }

    private ServicoDTO.Response toResponse(Servico servico) {
        return new ServicoDTO.Response(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPrecoBase(),
                servico.getCriadoEm(),
                servico.getAtualizadoEm()
        );
    }
}
