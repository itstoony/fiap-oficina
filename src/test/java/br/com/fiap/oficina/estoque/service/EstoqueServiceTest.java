package br.com.fiap.oficina.estoque.service;

import br.com.fiap.oficina.estoque.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.fiap.oficina.estoque.application.port.out.PecaRepositoryPort;
import br.com.fiap.oficina.estoque.application.service.EstoqueService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private PecaRepositoryPort pecaRepository;

    @Mock
    private MovimentacaoEstoqueRepositoryPort movimentacaoRepository;

    @InjectMocks
    private EstoqueService service;

    @Test
    void verificarDisponibilidadeEReservar_comEstoqueSuficiente_deveReservarERegistrar() {
        UUID pecaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Peca peca = criarPeca(pecaId, 10, 2);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);

        service.verificarDisponibilidadeEReservar(pecaId, 3, osId);

        assertThat(peca.getQtdReservada()).isEqualTo(5);
        verify(pecaRepository).salvar(peca);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.RESERVA);
        assertThat(captor.getValue().getQuantidade()).isEqualTo(3);
    }

    @Test
    void verificarDisponibilidadeEReservar_comEstoqueInsuficiente_deveLancarRegraDeNegocioException() {
        UUID pecaId = UUID.randomUUID();
        Peca peca = criarPeca(pecaId, 5, 4);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        assertThatThrownBy(() -> service.verificarDisponibilidadeEReservar(pecaId, 3, UUID.randomUUID()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(pecaRepository, never()).salvar(any());
    }

    @Test
    void verificarDisponibilidadeEReservar_pecaNaoEncontrada_deveLancarRecursoNaoEncontradoException() {
        UUID pecaId = UUID.randomUUID();
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificarDisponibilidadeEReservar(pecaId, 1, UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void liberarReserva_pecaExistente_deveDiminuirReservaERegistrar() {
        UUID pecaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Peca peca = criarPeca(pecaId, 10, 5);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);

        service.liberarReserva(pecaId, 3, osId);

        assertThat(peca.getQtdReservada()).isEqualTo(2);
        verify(pecaRepository).salvar(peca);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.LIBERACAO_RESERVA);
    }

    @Test
    void liberarReserva_pecaNaoEncontrada_naoDeveRealizarNenhumaOperacao() {
        UUID pecaId = UUID.randomUUID();
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.empty());

        service.liberarReserva(pecaId, 2, UUID.randomUUID());

        verify(pecaRepository, never()).salvar(any());
        verify(movimentacaoRepository, never()).salvar(any());
    }

    @Test
    void liberarReserva_quantidadeMaiorQueReservada_deveZerarReserva() {
        UUID pecaId = UUID.randomUUID();
        Peca peca = criarPeca(pecaId, 10, 2);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);

        service.liberarReserva(pecaId, 10, UUID.randomUUID());

        assertThat(peca.getQtdReservada()).isEqualTo(0);
    }

    @Test
    void baixarEstoque_comDadosValidos_deveDiminuirEstoqueEReservaERegistrar() {
        UUID pecaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Peca peca = criarPeca(pecaId, 10, 3);

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));
        when(pecaRepository.salvar(any())).thenReturn(peca);

        service.baixarEstoque(osId, pecaId, 3);

        assertThat(peca.getQtdEstoque()).isEqualTo(7);
        assertThat(peca.getQtdReservada()).isEqualTo(0);
        verify(pecaRepository).salvar(peca);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoRepository).salvar(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.BAIXA);
        assertThat(captor.getValue().getQuantidade()).isEqualTo(3);
    }

    @Test
    void baixarEstoque_pecaNaoEncontrada_deveLancarRecursoNaoEncontradoException() {
        UUID pecaId = UUID.randomUUID();
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.baixarEstoque(UUID.randomUUID(), pecaId, 1))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Peca criarPeca(UUID id, int qtdEstoque, int qtdReservada) {
        return Peca.builder()
                .id(id)
                .nome("Filtro de Óleo")
                .codigo("FILTRO-001")
                .precoUnitario(new BigDecimal("45.90"))
                .qtdEstoque(qtdEstoque)
                .qtdReservada(qtdReservada)
                .qtdMinima(2)
                .build();
    }
}
