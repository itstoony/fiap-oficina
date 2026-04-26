package br.com.fiap.oficina.atendimento.service;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.atendimento.domain.valueobject.Placa;
import br.com.fiap.oficina.atendimento.repository.VeiculoRepository;
import br.com.fiap.oficina.atendimento.service.dto.VeiculoDTO;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private VeiculoService service;

    @Test
    void cadastrar_comDadosValidos_deveSalvarERetornarResponse() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = criarClienteMock(clienteId);

        when(veiculoRepository.existsByPlacaValor("ABC1234")).thenReturn(false);
        when(clienteService.buscarEntidade(clienteId)).thenReturn(cliente);
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> {
            Veiculo v = inv.getArgument(0);
            return Veiculo.builder()
                    .id(UUID.randomUUID())
                    .marca(v.getMarca())
                    .modelo(v.getModelo())
                    .ano(v.getAno())
                    .cor(v.getCor())
                    .placa(v.getPlaca())
                    .cliente(v.getCliente())
                    .build();
        });

        var request = new VeiculoDTO.CadastrarRequest("Toyota", "Corolla", 2020, "Prata", "ABC-1234", clienteId);
        var response = service.cadastrar(request);

        assertThat(response.marca()).isEqualTo("Toyota");
        assertThat(response.placa()).isEqualTo("ABC1234");
        assertThat(response.clienteId()).isEqualTo(clienteId);
        verify(veiculoRepository).save(any(Veiculo.class));
    }

    @Test
    void cadastrar_comPlacaMercosul_deveNormalizarEAceitar() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = criarClienteMock(clienteId);

        when(veiculoRepository.existsByPlacaValor("ABC1D23")).thenReturn(false);
        when(clienteService.buscarEntidade(clienteId)).thenReturn(cliente);
        when(veiculoRepository.save(any())).thenAnswer(inv -> {
            Veiculo v = inv.getArgument(0);
            return Veiculo.builder().id(UUID.randomUUID()).marca(v.getMarca()).modelo(v.getModelo())
                    .ano(v.getAno()).cor(v.getCor()).placa(v.getPlaca()).cliente(v.getCliente()).build();
        });

        var request = new VeiculoDTO.CadastrarRequest("Honda", "Civic", 2023, "Preto", "abc1d23", clienteId);
        var response = service.cadastrar(request);

        assertThat(response.placa()).isEqualTo("ABC1D23");
    }

    @Test
    void cadastrar_comPlacaDuplicada_deveLancarRegraDeNegocioException() {
        UUID clienteId = UUID.randomUUID();
        when(veiculoRepository.existsByPlacaValor("ABC1234")).thenReturn(true);

        var request = new VeiculoDTO.CadastrarRequest("Toyota", "Corolla", 2020, "Prata", "ABC-1234", clienteId);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("placa");
    }

    @Test
    void cadastrar_comPlacaInvalida_deveLancarRegraDeNegocioException() {
        UUID clienteId = UUID.randomUUID();
        var request = new VeiculoDTO.CadastrarRequest("Toyota", "Corolla", 2020, "Prata", "INVALIDA", clienteId);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Placa inválida");
    }

    @Test
    void buscarPorId_veiculoExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        Cliente cliente = criarClienteMock(UUID.randomUUID());
        when(veiculoRepository.findById(id)).thenReturn(Optional.of(criarVeiculoMock(id, cliente)));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void buscarPorId_veiculoInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(veiculoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorPlaca_veiculoExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        Cliente cliente = criarClienteMock(UUID.randomUUID());
        when(veiculoRepository.findByPlacaValor("ABC1234"))
                .thenReturn(Optional.of(criarVeiculoMock(id, cliente)));

        var response = service.buscarPorPlaca("ABC-1234");

        assertThat(response.placa()).isEqualTo("ABC1234");
    }

    @Test
    void buscarPorCliente_deveRetornarApenasSeuVeiculos() {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = criarClienteMock(clienteId);
        when(veiculoRepository.findByClienteId(clienteId))
                .thenReturn(List.of(criarVeiculoMock(UUID.randomUUID(), cliente)));

        var result = service.buscarPorCliente(clienteId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).clienteId()).isEqualTo(clienteId);
    }

    @Test
    void excluir_veiculoExistente_deveChamarDelete() {
        UUID id = UUID.randomUUID();
        Cliente cliente = criarClienteMock(UUID.randomUUID());
        Veiculo veiculo = criarVeiculoMock(id, cliente);
        when(veiculoRepository.findById(id)).thenReturn(Optional.of(veiculo));

        service.excluir(id);

        verify(veiculoRepository).delete(veiculo);
    }

    private Cliente criarClienteMock(UUID id) {
        return Cliente.builder()
                .id(id)
                .nome("João Silva")
                .email("joao@email.com")
                .telefone("11999999999")
                .documento(Documento.of("529.982.247-25"))
                .build();
    }

    private Veiculo criarVeiculoMock(UUID id, Cliente cliente) {
        return Veiculo.builder()
                .id(id)
                .marca("Toyota")
                .modelo("Corolla")
                .ano(2020)
                .cor("Prata")
                .placa(Placa.of("ABC1234"))
                .cliente(cliente)
                .build();
    }
}
