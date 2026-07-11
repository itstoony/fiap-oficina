package br.com.fiap.oficina.execucao.application.port.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemServicoDTO {

    public record AdicionarRequest(
            @NotNull UUID servicoId,
            @NotNull @Min(1) Integer quantidade,
            String observacao
    ) {}

    public record Response(
            UUID id,
            UUID servicoId,
            String nomeServico,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal,
            String observacao
    ) {}
}
