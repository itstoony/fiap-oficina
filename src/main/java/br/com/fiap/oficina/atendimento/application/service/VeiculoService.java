package br.com.fiap.oficina.atendimento.application.service;

import br.com.fiap.oficina.atendimento.application.port.in.ClienteUseCase;
import br.com.fiap.oficina.atendimento.application.port.in.VeiculoDTO;
import br.com.fiap.oficina.atendimento.application.port.in.VeiculoUseCase;
import br.com.fiap.oficina.atendimento.application.port.out.VeiculoRepositoryPort;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.domain.valueobject.Placa;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VeiculoService implements VeiculoUseCase {

    private final VeiculoRepositoryPort veiculoRepository;
    private final ClienteUseCase clienteUseCase;

    @Transactional(readOnly = true)
    public List<VeiculoDTO.Response> listar() {
        return veiculoRepository.listarTodos().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public VeiculoDTO.Response buscarPorPlaca(String placa) {
        String normalizada = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return veiculoRepository.buscarPorPlacaValor(normalizada)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado com placa: " + placa));
    }

    @Transactional(readOnly = true)
    public List<VeiculoDTO.Response> buscarPorCliente(UUID clienteId) {
        return veiculoRepository.buscarPorClienteId(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VeiculoDTO.Response cadastrar(VeiculoDTO.CadastrarRequest request) {
        Placa placa = Placa.of(request.placa());
        if (veiculoRepository.existePorPlacaValor(placa.getValor())) {
            throw new RegraDeNegocioException("Já existe um veículo cadastrado com esta placa");
        }
        Cliente cliente = clienteUseCase.buscarEntidade(request.clienteId());
        Veiculo veiculo = Veiculo.builder()
                .marca(request.marca())
                .modelo(request.modelo())
                .ano(request.ano())
                .cor(request.cor())
                .placa(placa)
                .cliente(cliente)
                .build();
        return toResponse(veiculoRepository.salvar(veiculo));
    }

    @Transactional
    public VeiculoDTO.Response atualizar(UUID id, VeiculoDTO.AtualizarRequest request) {
        Veiculo veiculo = buscarEntidade(id);
        veiculo.setMarca(request.marca());
        veiculo.setModelo(request.modelo());
        veiculo.setAno(request.ano());
        veiculo.setCor(request.cor());
        return toResponse(veiculoRepository.salvar(veiculo));
    }

    @Transactional
    public void excluir(UUID id) {
        veiculoRepository.deletar(buscarEntidade(id));
    }

    private Veiculo buscarEntidade(UUID id) {
        return veiculoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado com id: " + id));
    }

    private VeiculoDTO.Response toResponse(Veiculo veiculo) {
        return new VeiculoDTO.Response(
                veiculo.getId(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getCor(),
                veiculo.getPlaca().getValor(),
                veiculo.getCliente().getId(),
                veiculo.getCliente().getNome(),
                veiculo.getCriadoEm(),
                veiculo.getAtualizadoEm()
        );
    }
}
