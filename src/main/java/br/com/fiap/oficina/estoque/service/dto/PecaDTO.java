package br.com.fiap.oficina.estoque.service.dto;

import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PecaDTO {

    public record CadastrarRequest(
            @NotBlank String nome,
            @NotBlank String codigo,
            @NotNull @Positive BigDecimal precoUnitario,
            @NotNull @Min(0) Integer qtdEstoque,
            @NotNull @Min(0) Integer qtdMinima
    ) {}

    public record AtualizarRequest(
            @NotBlank String nome,
            @NotNull @Positive BigDecimal precoUnitario,
            @NotNull @Min(0) Integer qtdMinima
    ) {}

    public record EntradaRequest(
            @NotNull @Min(1) Integer quantidade,
            String observacao
    ) {}

    public record Response(
            UUID id,
            String nome,
            String codigo,
            BigDecimal precoUnitario,
            Integer qtdEstoque,
            Integer qtdReservada,
            Integer qtdDisponivel,
            Integer qtdMinima,
            boolean estoqueCritico,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {}

    public record MovimentacaoResponse(
            UUID id,
            TipoMovimentacao tipo,
            Integer quantidade,
            UUID osId,
            String observacao,
            LocalDateTime dataMovimentacao
    ) {}
}
