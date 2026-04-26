package br.com.fiap.oficina.administracao.service;

import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
import br.com.fiap.oficina.atendimento.domain.valueobject.Placa;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.execucao.repository.OrdemDeServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private OrdemDeServicoRepository ordemDeServicoRepository;

    @InjectMocks
    private RelatorioService service;

    @Test
    void calcularTempoMedioExecucao_semOrdens_deveRetornarZero() {
        when(ordemDeServicoRepository.findFinalizadasComDatas(anyList())).thenReturn(List.of());

        var result = service.calcularTempoMedioExecucao();

        assertThat(result.totalOrdens()).isEqualTo(0);
        assertThat(result.mediaMinutos()).isEqualTo(0);
        assertThat(result.mediaFormatada()).isEqualTo("Sem dados");
    }

    @Test
    void calcularTempoMedioExecucao_comUmaOrdem_deveCalcularMediaCorretamente() {
        LocalDateTime inicio = LocalDateTime.now().minusHours(3);
        LocalDateTime fim = inicio.plusHours(2).plusMinutes(30);
        OrdemDeServico os = criarOsMock(StatusOS.FINALIZADA, inicio, fim);

        when(ordemDeServicoRepository.findFinalizadasComDatas(anyList())).thenReturn(List.of(os));

        var result = service.calcularTempoMedioExecucao();

        assertThat(result.totalOrdens()).isEqualTo(1);
        assertThat(result.mediaMinutos()).isEqualTo(150);
        assertThat(result.mediaFormatada()).isEqualTo("2h 30m");
    }

    @Test
    void calcularTempoMedioExecucao_comDuasOrdens_deveCalcularMedia() {
        LocalDateTime base = LocalDateTime.now().minusHours(5);
        OrdemDeServico os1 = criarOsMock(StatusOS.ENTREGUE, base, base.plusHours(1));      // 60 min
        OrdemDeServico os2 = criarOsMock(StatusOS.FINALIZADA, base, base.plusHours(3));    // 180 min

        when(ordemDeServicoRepository.findFinalizadasComDatas(anyList())).thenReturn(List.of(os1, os2));

        var result = service.calcularTempoMedioExecucao();

        assertThat(result.totalOrdens()).isEqualTo(2);
        assertThat(result.mediaMinutos()).isEqualTo(120);
        assertThat(result.mediaFormatada()).isEqualTo("2h 0m");
    }

    @Test
    void calcularTempoMedioExecucao_mediaMenorQueUmaHora_deveFormatarSoMinutos() {
        LocalDateTime base = LocalDateTime.now();
        OrdemDeServico os = criarOsMock(StatusOS.FINALIZADA, base, base.plusMinutes(45));

        when(ordemDeServicoRepository.findFinalizadasComDatas(anyList())).thenReturn(List.of(os));

        var result = service.calcularTempoMedioExecucao();

        assertThat(result.mediaFormatada()).isEqualTo("45m");
    }

    private OrdemDeServico criarOsMock(StatusOS status, LocalDateTime inicio, LocalDateTime fim) {
        Cliente cliente = Cliente.builder()
                .id(UUID.randomUUID()).nome("Cliente Teste").email("c@c.com").telefone("11999999999")
                .documento(Documento.of("529.982.247-25")).build();

        Veiculo veiculo = Veiculo.builder()
                .id(UUID.randomUUID()).marca("Toyota").modelo("Corolla").ano(2020).cor("Prata")
                .placa(Placa.of("ABC1234")).cliente(cliente).build();

        return OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .numero("OS-2026-00001")
                .status(status)
                .cliente(cliente)
                .veiculo(veiculo)
                .dataInicioExecucao(inicio)
                .dataFimExecucao(fim)
                .itensServico(new ArrayList<>())
                .itensPeca(new ArrayList<>())
                .build();
    }
}
