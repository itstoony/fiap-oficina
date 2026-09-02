package br.com.fiap.oficina.execucao.application.service;

import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.atendimento.domain.valueobject.Placa;
import br.com.fiap.oficina.execucao.application.port.in.ItemPecaDTO;
import br.com.fiap.oficina.execucao.application.port.in.ItemServicoDTO;
import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoDTO;
import br.com.fiap.oficina.execucao.application.port.out.AtendenteLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.ClienteLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.EstoquePort;
import br.com.fiap.oficina.execucao.application.port.out.MetricasPort;
import br.com.fiap.oficina.execucao.application.port.out.NotificacaoEmailPort;
import br.com.fiap.oficina.execucao.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.fiap.oficina.execucao.application.port.out.PecaLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.ServicoLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.VeiculoLookupPort;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemDeServicoServiceTest {

    @Mock private OrdemDeServicoRepositoryPort repositorio;
    @Mock private ClienteLookupPort clienteLookup;
    @Mock private VeiculoLookupPort veiculoLookup;
    @Mock private AtendenteLookupPort atendenteLookup;
    @Mock private ServicoLookupPort servicoLookup;
    @Mock private PecaLookupPort pecaLookup;
    @Mock private EstoquePort estoque;
    @Mock private NotificacaoEmailPort notificacaoEmail;
    @Mock private MetricasPort metricas;

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

        when(clienteLookup.buscarPorId(clienteId)).thenReturn(cliente);
        when(veiculoLookup.buscarPorId(veiculoId)).thenReturn(veiculo);
        when(repositorio.buscarProximoSequencial(any())).thenReturn(0);
        when(repositorio.salvar(any())).thenAnswer(inv -> {
            OrdemDeServico ordemDeServico = inv.getArgument(0);
            return OrdemDeServico.builder()
                    .id(UUID.randomUUID()).numero(ordemDeServico.getNumero()).status(ordemDeServico.getStatus())
                    .cliente(ordemDeServico.getCliente()).veiculo(ordemDeServico.getVeiculo())
                    .valorTotal(BigDecimal.ZERO).itensServico(new ArrayList<>()).itensPeca(new ArrayList<>())
                    .build();
        });

        var request = new OrdemDeServicoDTO.CriarRequest(clienteId, veiculoId, null, "Troca de óleo");
        var response = service.criar(request);

        assertThat(response.status()).isEqualTo("RECEBIDA");
        assertThat(response.numero()).startsWith("OS-");
        verify(repositorio).salvar(any(OrdemDeServico.class));
    }

    @Test
    void criar_veiculoInexistente_deveLancarExcecao() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        when(clienteLookup.buscarPorId(clienteId)).thenReturn(criarCliente(clienteId));
        when(veiculoLookup.buscarPorId(veiculoId))
                .thenThrow(new RecursoNaoEncontradoException("Veículo não encontrado com id: " + veiculoId));

        assertThatThrownBy(() -> service.criar(
                new OrdemDeServicoDTO.CriarRequest(clienteId, veiculoId, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS
    // -------------------------------------------------------------------------

    @Test
    void iniciarDiagnostico_statusRecebida_deveMudarParaEmDiagnostico() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.RECEBIDA);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.iniciarDiagnostico(ordemDeServico.getId());

        assertThat(response.status()).isEqualTo("EM_DIAGNOSTICO");
    }

    @Test
    void iniciarDiagnostico_statusInvalido_deveLancarExcecao() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_EXECUCAO);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);

        assertThatThrownBy(() -> service.iniciarDiagnostico(ordemDeServico.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void enviarOrcamento_statusEmDiagnostico_deveMudarParaAguardandoAprovacaoEEnviarEmail() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_DIAGNOSTICO);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.enviarOrcamento(ordemDeServico.getId());

        assertThat(response.status()).isEqualTo("AGUARDANDO_APROVACAO");
        verify(notificacaoEmail).enviarOrcamentoParaAprovacao(
                ordemDeServico.getCliente().getEmail(),
                ordemDeServico.getNumero(),
                ordemDeServico.getValorTotal()
        );
    }

    @Test
    void iniciarExecucao_statusAprovado_deveMudarParaEmExecucaoEAtribuirAtendente() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.APROVADO);
        Atendente atendente = Atendente.builder().id(UUID.randomUUID()).nome("Carlos").email("carlos@oficina.com").telefone("11999990001").build();
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(atendenteLookup.buscarPorId(atendente.getId())).thenReturn(atendente);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.iniciarExecucao(ordemDeServico.getId(), new OrdemDeServicoDTO.IniciarExecucaoRequest(atendente.getId()));

        assertThat(response.status()).isEqualTo("EM_EXECUCAO");
        assertThat(ordemDeServico.getAtendente()).isEqualTo(atendente);
        assertThat(ordemDeServico.getDataInicioExecucao()).isNotNull();
    }

    @Test
    void iniciarExecucao_atendenteInexistente_deveLancarExcecao() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.APROVADO);
        UUID atendenteId = UUID.randomUUID();
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(atendenteLookup.buscarPorId(atendenteId))
                .thenThrow(new RecursoNaoEncontradoException("Atendente não encontrado com id: " + atendenteId));

        assertThatThrownBy(() -> service.iniciarExecucao(ordemDeServico.getId(), new OrdemDeServicoDTO.IniciarExecucaoRequest(atendenteId)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void finalizar_statusEmExecucao_deveMudarParaFinalizadaEDesativarOs() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_EXECUCAO);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.finalizar(ordemDeServico.getId());

        assertThat(response.status()).isEqualTo("FINALIZADA");
        assertThat(ordemDeServico.getDataFimExecucao()).isNotNull();
        assertThat(ordemDeServico.isAtivo()).isFalse();
    }

    @Test
    void cancelar_statusRecebida_deveMudarParaCanceladaEDesativarOs() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.RECEBIDA);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.cancelar(ordemDeServico.getId());

        assertThat(response.status()).isEqualTo("CANCELADA");
        assertThat(ordemDeServico.isAtivo()).isFalse();
    }

    @Test
    void listar_deveUsarQueryApenasComOsAtivas() {
        OrdemDeServico osAtiva = criarOS(StatusOS.RECEBIDA);
        when(repositorio.listarAtivasOrdenadas()).thenReturn(List.of(osAtiva));

        var resultado = service.listar();

        assertThat(resultado).hasSize(1);
        verify(repositorio).listarAtivasOrdenadas();
        verify(repositorio, never()).listarTodas();
    }

    @Test
    void cancelar_statusEmExecucao_deveLancarExcecao() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_EXECUCAO);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);

        assertThatThrownBy(() -> service.cancelar(ordemDeServico.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // -------------------------------------------------------------------------
    // ADIÇÃO DE SERVIÇOS
    // -------------------------------------------------------------------------

    @Test
    void adicionarServico_osEmDiagnostico_deveAdicionarItemERecalcularValor() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_DIAGNOSTICO);
        UUID servicoId = UUID.randomUUID();
        Servico servico = Servico.builder()
                .id(servicoId).nome("Troca de óleo").precoBase(new BigDecimal("150.00")).build();

        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);
        when(servicoLookup.buscarPorId(servicoId)).thenReturn(servico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        service.adicionarServico(ordemDeServico.getId(), new ItemServicoDTO.AdicionarRequest(servicoId, 1, null));

        assertThat(ordemDeServico.getItensServico()).hasSize(1);
        assertThat(ordemDeServico.getValorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void adicionarServico_osEmExecucao_deveLancarExcecao() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.EM_EXECUCAO);
        when(repositorio.buscarPorId(ordemDeServico.getId())).thenReturn(ordemDeServico);

        assertThatThrownBy(() -> service.adicionarServico(ordemDeServico.getId(),
                new ItemServicoDTO.AdicionarRequest(UUID.randomUUID(), 1, null)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // -------------------------------------------------------------------------
    // PÚBLICO
    // -------------------------------------------------------------------------

    @Test
    void aprovar_statusAguardandoAprovacao_deveMudarParaAprovado() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.AGUARDANDO_APROVACAO);
        when(repositorio.buscarPorNumero(ordemDeServico.getNumero())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.aprovar(ordemDeServico.getNumero());

        assertThat(response.status()).isEqualTo("APROVADO");
    }

    @Test
    void recusar_statusAguardandoAprovacao_deveMudarParaCancelada() {
        OrdemDeServico ordemDeServico = criarOS(StatusOS.AGUARDANDO_APROVACAO);
        when(repositorio.buscarPorNumero(ordemDeServico.getNumero())).thenReturn(ordemDeServico);
        when(repositorio.salvar(any())).thenReturn(ordemDeServico);

        var response = service.recusar(ordemDeServico.getNumero());

        assertThat(response.status()).isEqualTo("CANCELADA");
    }

    @Test
    void buscarStatusPublico_osInexistente_deveLancarExcecao() {
        when(repositorio.buscarPorNumero("OS-2026-99999"))
                .thenThrow(new RecursoNaoEncontradoException("Ordem de serviço não encontrada com número: OS-2026-99999"));

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
