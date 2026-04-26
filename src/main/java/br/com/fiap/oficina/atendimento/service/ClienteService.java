package br.com.fiap.oficina.atendimento.service;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.atendimento.repository.ClienteRepository;
import br.com.fiap.oficina.atendimento.service.dto.ClienteDTO;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;

    @Transactional(readOnly = true)
    public List<ClienteDTO.Response> listar() {
        return repository.findAll().stream()
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
        return repository.findByDocumentoNumero(digits)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado com documento: " + documento));
    }

    @Transactional
    public ClienteDTO.Response cadastrar(ClienteDTO.CadastrarRequest request) {
        Documento documento = Documento.of(request.documento());
        if (repository.existsByDocumentoNumero(documento.getNumero())) {
            throw new RegraDeNegocioException("Já existe um cliente cadastrado com este documento");
        }
        Cliente cliente = Cliente.builder()
                .nome(request.nome())
                .email(request.email())
                .telefone(request.telefone())
                .documento(documento)
                .build();
        return toResponse(repository.save(cliente));
    }

    @Transactional
    public ClienteDTO.Response atualizar(UUID id, ClienteDTO.AtualizarRequest request) {
        Cliente cliente = buscarEntidade(id);
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        return toResponse(repository.save(cliente));
    }

    @Transactional
    public void excluir(UUID id) {
        repository.delete(buscarEntidade(id));
    }

    public Cliente buscarEntidade(UUID id) {
        return repository.findById(id)
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