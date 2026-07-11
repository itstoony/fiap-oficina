package br.com.fiap.oficina.atendimento.application.port.in;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class VeiculoDTO {

    public record CadastrarRequest(
            @NotBlank String marca,
            @NotBlank String modelo,
            @NotNull @Min(1900) @Max(2100) Integer ano,
            @NotBlank String cor,
            @NotBlank String placa,
            @NotNull UUID clienteId
    ) {
    }

    public record AtualizarRequest(
            @NotBlank String marca,
            @NotBlank String modelo,
            @NotNull @Min(1900) @Max(2100) Integer ano,
            @NotBlank String cor
    ) {
    }

    public record Response(
            UUID id,
            String marca,
            String modelo,
            Integer ano,
            String cor,
            String placa,
            UUID clienteId,
            String nomeCliente,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {
    }
}
