package br.com.fiap.oficina.estoque.service;

import br.com.fiap.oficina.estoque.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.fiap.oficina.estoque.application.port.out.PecaRepositoryPort;
import br.com.fiap.oficina.estoque.application.service.PecaService;
import br.com.fiap.oficina.estoque.application.port.in.PecaDTO;
import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepositoryPort pecaRepository;

    @Mock
    private MovimentacaoEstoqueRepositoryPort movimentacaoRepository;

    @InjectMocks
    private PecaService service;

    @Test
    void cadastrar_comDadosValidos_deveSalvarERetornarResponse() {
        when(pecaRepository.existePorCodigo("FILTRO-001")).thenReturn(false);
        when(pecaRepository.salvar(any(Peca.class))).thenAnswer(inv -> {
            Peca p = inv.getArgument(0);
            return Peca.builder().id(UUID.randomUUID()).nome(p.getNome()).codigo(p.getCodigo())
                    .precoUnitario(p.getPrecoUnitario()).qtdEstoque(p.getQtdEstoque())
                    .qtdReservada(0).qtdMinima(p.getQtdMinima())
                    .criadoEm(LocalDateTime.now()).atualizadoEm(LocalDateTime.now()).build();
        });
        when(movimentacaoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PecaDTO.CadastrarRequest("Filtro de Óleo", "FILTRO-001", new BigDecimal("45.90"), 10, 3);
        var response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Filtro de Óleo");
        assertThat(response.codigo()).isEqualTo("FILTRO-001");
        assertThat(response.qtdEstoque()).isEqualTo(10);
        assertThat(response.qtdReservada()).isEqualTo(0);
        assertThat(response.qtdDisponivel()).isEqualTo(10);
        assertThat(response.estoqueCritico()).isFalse();
        verify(pecaRepository).salvar(any(Peca.class));
        verify(movimentacaoRepository).salvar(any(MovimentacaoEstoque.class));
    }

    @Test
    void cadastrar_semEstoqueInicial_naoDeveRegistrarMovimentacao() {
        when(pecaRepository.existePorCodigo("PECA-002")).thenReturn(false);
        when(pecaRepository.salvar(any(Peca.class))).thenAnswer(inv -> {
            Peca p = inv.getArgument(0);
            return Peca.builder().id(UUID.randomUUID()).nome(p.getNome()).codigo(p.getCodigo())
                    .precoUnitario(p.getPrecoUnitario()).qtdEstoque(0).qtdReservada(0).qtdMinima(2)
                    .criadoEm(LocalDateTime.now()).atualizadoEm(LocalDateTime.now()).build();
        });

        var request = new PecaDTO.CadastrarRequest("Peça Nova", "PECA-002", new BigDecimal("10.00"), 0, 2);
        service.cadastrar(request);

        verify(movimentacaoRepository, never()).salvar(any());
    }

    @Test
    void cadastrar_comCodigoDuplicado_deveLancarRegraDeNegocioException() {
        when(pecaRepository.existePorCodigo("FILTRO-001")).thenReturn(true);

        var request = new PecaDTO.CadastrarRequest("Filtro", "FILTRO-001", new BigDecimal("45.90"), 10, 3);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("FILTRO-001");
    }

    @Test
    void buscarPorId_pecaExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(criarPecaMock(id, 10, 0, 3)));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.estoqueCritico()).isFalse();
    }

    @Test
    void buscarPorId_pecaInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_pecaExistente_deveAtualizarDados() {
        UUID id = UUID.randomUUID();
        Peca peca = criarPecaMock(id, 10, 0, 3);
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(peca)).thenReturn(peca);

        var request = new PecaDTO.AtualizarRequest("Novo Nome", new BigDecimal("55.00"), 5);
        var response = service.atualizar(id, request);

        assertThat(peca.getNome()).isEqualTo("Novo Nome");
        assertThat(peca.getPrecoUnitario()).isEqualByComparingTo("55.00");
        assertThat(peca.getQtdMinima()).isEqualTo(5);
        verify(pecaRepository).salvar(peca);
    }

    @Test
    void excluir_semReservas_deveChamarDelete() {
        UUID id = UUID.randomUUID();
        Peca peca = criarPecaMock(id, 10, 0, 3);
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(peca));

        service.excluir(id);

        verify(pecaRepository).deletar(peca);
    }

    @Test
    void excluir_comReservas_deveLancarRegraDeNegocioException() {
        UUID id = UUID.randomUUID();
        Peca peca = criarPecaMock(id, 10, 2, 3);
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("reservada");
    }

    @Test
    void registrarEntrada_deveAumentarEstoqueECriarMovimentacao() {
        UUID id = UUID.randomUUID();
        Peca peca = criarPecaMock(id, 5, 0, 3);
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(peca)).thenReturn(peca);
        when(movimentacaoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new PecaDTO.EntradaRequest(10, "Reposição de estoque");
        var response = service.registrarEntrada(id, request);

        assertThat(peca.getQtdEstoque()).isEqualTo(15);
        assertThat(response.qtdEstoque()).isEqualTo(15);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(captor.getValue().getQuantidade()).isEqualTo(10);
    }

    @Test
    void buscarCriticas_deveRetornarApenasPecasComEstoqueCritico() {
        UUID id = UUID.randomUUID();
        when(pecaRepository.buscarCriticas()).thenReturn(List.of(criarPecaMock(id, 2, 0, 3)));

        var result = service.buscarCriticas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).estoqueCritico()).isTrue();
    }

    @Test
    void buscarMovimentacoes_deveRetornarHistorico() {
        UUID id = UUID.randomUUID();
        Peca peca = criarPecaMock(id, 10, 0, 3);
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.of(peca));
        when(movimentacaoRepository.buscarPorPecaIdOrdenado(id))
                .thenReturn(List.of(
                        MovimentacaoEstoque.builder().id(UUID.randomUUID()).peca(peca)
                                .tipo(TipoMovimentacao.ENTRADA).quantidade(10).dataMovimentacao(LocalDateTime.now()).build()
                ));

        var result = service.buscarMovimentacoes(id);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tipo()).isEqualTo(TipoMovimentacao.ENTRADA);
    }

    @Test
    void buscarMovimentacoes_pecaInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(pecaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarMovimentacoes(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Peca criarPecaMock(UUID id, int qtdEstoque, int qtdReservada, int qtdMinima) {
        return Peca.builder()
                .id(id)
                .nome("Filtro de Óleo")
                .codigo("FILTRO-001")
                .precoUnitario(new BigDecimal("45.90"))
                .qtdEstoque(qtdEstoque)
                .qtdReservada(qtdReservada)
                .qtdMinima(qtdMinima)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
