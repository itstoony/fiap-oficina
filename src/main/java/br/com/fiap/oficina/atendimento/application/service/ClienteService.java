package br.com.fiap.oficina.atendimento.application.service;

import br.com.fiap.oficina.atendimento.application.port.in.ClienteDTO;
import br.com.fiap.oficina.atendimento.application.port.in.ClienteUseCase;
import br.com.fiap.oficina.atendimento.application.port.out.ClienteRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService implements ClienteUseCase {

    private final ClienteRepositoryPort repository;

    @Transactional(readOnly = true)
    public List<ClienteDTO.Response> listar() {
        return repository.listarTodos().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public ClienteDTO.Response buscarPorDocumento(String documento) {
        String digits = documento.replaceAll("[^0-9]", "");
        return repository.buscarPorDocumentoNumero(digits)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado com documento: " + documento));
    }

    @Transactional
    public ClienteDTO.Response cadastrar(ClienteDTO.CadastrarRequest request) {
        Documento documento = Documento.of(request.documento());
        if (repository.existePorDocumentoNumero(documento.getNumero())) {
            throw new RegraDeNegocioException("Já existe um cliente cadastrado com este documento");
        }
        Cliente cliente = Cliente.builder()
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .documento(documento)
                .build();
        return toResponse(repository.salvar(cliente));
    }

    @Transactional
    public ClienteDTO.Response atualizar(UUID id, ClienteDTO.AtualizarRequest request) {
        Cliente cliente = buscarEntidade(id);
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        return toResponse(repository.salvar(cliente));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.deletar(buscarEntidade(id));
    }

    public Cliente buscarEntidade(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com id: " + id));
    }

    private ClienteDTO.Response toResponse(Cliente cliente) {
        return new ClienteDTO.Response(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDocumento().getFormatado(),
                cliente.getDocumento().getTipo().name(),
                cliente.getCriadoEm(),
                cliente.getAtualizadoEm()
        );
    }
}
