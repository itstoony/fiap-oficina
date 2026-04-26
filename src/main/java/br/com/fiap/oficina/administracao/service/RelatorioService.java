package br.com.fiap.oficina.administracao.service;

import br.com.fiap.oficina.administracao.service.dto.RelatorioDTO;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.execucao.repository.OrdemDeServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    @Transactional(readOnly = true)
    public RelatorioDTO.TempoMedioResponse calcularTempoMedioExecucao() {
        List<OrdemDeServico> ordens = ordemDeServicoRepository.findFinalizadasComDatas(
                List.of(StatusOS.FINALIZADA, StatusOS.ENTREGUE));

        if (ordens.isEmpty()) {
            return new RelatorioDTO.TempoMedioResponse(0, 0, "Sem dados");
        }

        long totalMinutos = ordens.stream()
                .mapToLong(os -> ChronoUnit.MINUTES.between(os.getDataInicioExecucao(), os.getDataFimExecucao()))
                .sum();

        long mediaMinutos = totalMinutos / ordens.size();
        String mediaFormatada = formatarDuracao(mediaMinutos);

        return new RelatorioDTO.TempoMedioResponse(ordens.size(), mediaMinutos, mediaFormatada);
    }

    private String formatarDuracao(long minutos) {
        long horas = minutos / 60;
        long mins = minutos % 60;
        if (horas > 0) {
            return horas + "h " + mins + "m";
        }
        return mins + "m";
    }
}
