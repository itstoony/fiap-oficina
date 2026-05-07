package br.com.fiap.oficina.administracao.service.dto;

public class RelatorioDTO {

    public record TempoMedioResponse(
            int totalOrdens,
            long mediaMinutos,
            String mediaFormatada
    ) { }
}
