package br.com.fiap.oficina.execucao.service;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.administracao.repository.ServicoRepository;
import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.atendimento.domain.valueobject.Placa;
import br.com.fiap.oficina.atendimento.repository.AtendenteRepository;
import br.com.fiap.oficina.atendimento.repository.VeiculoRepository;
import br.com.fiap.oficina.atendimento.service.ClienteService;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.repository.PecaRepository;
import br.com.fiap.oficina.estoque.service.EstoqueService;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.execucao.repository.OrdemDeServicoRepository;
import br.com.fiap.oficina.execucao.service.dto.ItemPecaDTO;
import br.com.fiap.oficina.execucao.service.dto.ItemServicoDTO;
import br.com.fiap.oficina.execucao.service.dto.OrdemDeServicoDTO;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoServiceTest {

    @Mock private OrdemDeServicoRepository repository;
    @Mock private ClienteService clienteService;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private AtendenteRepository atendenteRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private PecaRepository pecaRepository;
    @Mock private EstoqueService estoqueService;

    @InjectMocks
    private OrdemDeServicoService service;

    // -------------------------------------------------------------------------
    // CRIAÇÃO
    // -------------------------------------------------------------------------

    @Test
    void criar_comDadosValidos_deveSalvarOsComStatusRecebida() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        Cliente cliente = criarCliente(clienteId);
        Veiculo veiculo = criarVeiculo(veiculoId, cliente);

        when(clienteService.buscarEntidade(clienteId)).thenReturn(cliente);
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(repository.findMaxSequencialByPrefixo(any())).thenReturn(0);
        when(repository.save(any())).thenAnswer(inv -> {
            OrdemDeServico os = inv.getArgument(0);
            os = OrdemDeServico.builder()
                    .id(UUID.randomUUID()).numero(os.getNumero()).status(os.getStatus())
                    .cliente(os.getCliente()).veiculo(os.getVeiculo())
                    .valorTotal(BigDecimal.ZERO).itensServico(new ArrayList<>()).itensPeca(new ArrayList<>())
                    .build();
            return os;
        });

        var request = new OrdemDeServicoDTO.CriarRequest(clienteId, veiculoId, null, "Troca de óleo");
        var response = service.criar(request);

        assertThat(response.status()).isEqualTo("RECEBIDA");
        assertThat(response.numero()).startsWith("OS-");
        verify(repository).save(any(OrdemDeServico.class));
    }

    @Test
    void criar_veiculoInexistente_deveLancarExcecao() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteService.buscarEntidade(clienteId)).thenReturn(criarCliente(clienteId));
        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(
                new OrdemDeServicoDTO.CriarRequest(clienteId, veiculoId, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS
    // -------------------------------------------------------------------------

    @Test
    void iniciarDiagnostico_statusRecebida_deveMudarParaEmDiagnostico() {
        OrdemDeServico os = criarOS(StatusOS.RECEBIDA);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.iniciarDiagnostico(os.getId());

        assertThat(response.status()).isEqualTo("EM_DIAGNOSTICO");
    }

    @Test
    void iniciarDiagnostico_statusInvalido_deveLancarExcecao() {
        OrdemDeServico os = criarOS(StatusOS.EM_EXECUCAO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.iniciarDiagnostico(os.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void enviarOrcamento_statusEmDiagnostico_deveMudarParaAguardandoAprovacao() {
        OrdemDeServico os = criarOS(StatusOS.EM_DIAGNOSTICO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.enviarOrcamento(os.getId());

        assertThat(response.status()).isEqualTo("AGUARDANDO_APROVACAO");
    }

    @Test
    void iniciarExecucao_statusAguardandoAprovacao_deveMudarParaEmExecucao() {
        OrdemDeServico os = criarOS(StatusOS.AGUARDANDO_APROVACAO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.iniciarExecucao(os.getId());

        assertThat(response.status()).isEqualTo("EM_EXECUCAO");
        assertThat(os.getDataInicioExecucao()).isNotNull();
    }

    @Test
    void finalizar_statusEmExecucao_deveMudarParaFinalizada() {
        OrdemDeServico os = criarOS(StatusOS.EM_EXECUCAO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.finalizar(os.getId());

        assertThat(response.status()).isEqualTo("FINALIZADA");
        assertThat(os.getDataFimExecucao()).isNotNull();
    }

    @Test
    void cancelar_statusRecebida_deveMudarParaCancelada() {
        OrdemDeServico os = criarOS(StatusOS.RECEBIDA);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.cancelar(os.getId());

        assertThat(response.status()).isEqualTo("CANCELADA");
    }

    @Test
    void cancelar_statusEmExecucao_deveLancarExcecao() {
        OrdemDeServico os = criarOS(StatusOS.EM_EXECUCAO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.cancelar(os.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // -------------------------------------------------------------------------
    // ADIÇÃO DE SERVIÇOS
    // -------------------------------------------------------------------------

    @Test
    void adicionarServico_osEmDiagnostico_deveAdicionarItemERecalcularValor() {
        OrdemDeServico os = criarOS(StatusOS.EM_DIAGNOSTICO);
        UUID servicoId = UUID.randomUUID();
        Servico servico = Servico.builder()
                .id(servicoId).nome("Troca de óleo").precoBase(new BigDecimal("150.00")).build();

        when(repository.findById(os.getId())).thenReturn(Optional.of(os));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(repository.save(any())).thenReturn(os);

        service.adicionarServico(os.getId(), new ItemServicoDTO.AdicionarRequest(servicoId, 1, null));

        assertThat(os.getItensServico()).hasSize(1);
        assertThat(os.getValorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void adicionarServico_osEmExecucao_deveLancarExcecao() {
        OrdemDeServico os = criarOS(StatusOS.EM_EXECUCAO);
        when(repository.findById(os.getId())).thenReturn(Optional.of(os));

        assertThatThrownBy(() -> service.adicionarServico(os.getId(),
                new ItemServicoDTO.AdicionarRequest(UUID.randomUUID(), 1, null)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // -------------------------------------------------------------------------
    // PÚBLICO
    // -------------------------------------------------------------------------

    @Test
    void aprovar_statusAguardandoAprovacao_deveMudarParaEmExecucao() {
        OrdemDeServico os = criarOS(StatusOS.AGUARDANDO_APROVACAO);
        when(repository.findByNumero(os.getNumero())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.aprovar(os.getNumero());

        assertThat(response.status()).isEqualTo("EM_EXECUCAO");
    }

    @Test
    void recusar_statusAguardandoAprovacao_deveMudarParaCancelada() {
        OrdemDeServico os = criarOS(StatusOS.AGUARDANDO_APROVACAO);
        when(repository.findByNumero(os.getNumero())).thenReturn(Optional.of(os));
        when(repository.save(any())).thenReturn(os);

        var response = service.recusar(os.getNumero());

        assertThat(response.status()).isEqualTo("CANCELADA");
    }

    @Test
    void buscarStatusPublico_osInexistente_deveLancarExcecao() {
        when(repository.findByNumero("OS-2026-99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarStatusPublico("OS-2026-99999"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private OrdemDeServico criarOS(StatusOS status) {
        UUID clienteId = UUID.randomUUID();
        Cliente cliente = criarCliente(clienteId);
        Veiculo veiculo = criarVeiculo(UUID.randomUUID(), cliente);
        return OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-2026-00001")
                .status(status)
                .cliente(cliente)
                .veiculo(veiculo)
                .valorTotal(BigDecimal.ZERO)
                .itensServico(new ArrayList<>())
                .itensPeca(new ArrayList<>())
                .build();
    }

    private Cliente criarCliente(UUID id) {
        return Cliente.builder()
                .id(id).nome("João Silva").email("joao@email.com").telefone("11999999999")
                .documento(Documento.of("529.982.247-25"))
                .build();
    }

    private Veiculo criarVeiculo(UUID id, Cliente cliente) {
        return Veiculo.builder()
                .id(id).marca("Toyota").modelo("Corolla").ano(2020).cor("Prata")
                .placa(Placa.of("ABC1234")).cliente(cliente)
                .build();
    }
}
