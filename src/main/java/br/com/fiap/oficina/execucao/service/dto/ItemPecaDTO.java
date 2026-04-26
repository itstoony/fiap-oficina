package br.com.fiap.oficina.execucao.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemPecaDTO {

    public record AdicionarRequest(
            @NotNull UUID pecaId,
            @NotNull @Min(1) Integer quantidade
    ) {
    }

    public record Response(
            UUID id,
            UUID pecaId,
            String nomePeca,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {
    }
}
