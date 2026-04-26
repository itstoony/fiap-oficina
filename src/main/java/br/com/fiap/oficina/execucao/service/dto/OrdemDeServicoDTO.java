package br.com.fiap.oficina.execucao.service.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrdemDeServicoDTO {

    public record CriarRequest(
            @NotNull UUID clienteId,
            @NotNull UUID veiculoId,
            UUID atendenteId,
            String observacoes
    ) {
    }

    public record Response(
            UUID id,
            String numero,
            String status,
            UUID clienteId,
            String nomeCliente,
            UUID veiculoId,
            String placaVeiculo,
            UUID atendenteId,
            String nomeAtendente,
            BigDecimal valorTotal,
            String observacoes,
            List<ItemServicoDTO.Response> itensServico,
            List<ItemPecaDTO.Response> itensPeca,
            LocalDateTime dataAbertura,
            LocalDateTime dataInicioExecucao,
            LocalDateTime dataFimExecucao,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {
    }

    public record StatusPublicoResponse(
            String numero,
            String status,
            BigDecimal valorTotal,
            LocalDateTime dataAbertura,
            LocalDateTime dataInicioExecucao,
            LocalDateTime dataFimExecucao
    ) {
    }
}
