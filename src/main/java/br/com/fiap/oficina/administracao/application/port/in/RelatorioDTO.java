package br.com.fiap.oficina.administracao.application.port.in;

public class RelatorioDTO {

    public record TempoMedioResponse(
            int totalOrdens,
            long mediaMinutos,
            String mediaFormatada
    ) {}
}
