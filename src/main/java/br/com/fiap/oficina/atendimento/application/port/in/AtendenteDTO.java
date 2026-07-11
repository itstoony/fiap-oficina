package br.com.fiap.oficina.atendimento.application.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public class AtendenteDTO {

    public record CadastrarRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String telefone
    ) {}

    public record AtualizarRequest(
            @NotBlank(message = "Nome é obrigatório") String nome,
            @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
            @NotBlank(message = "Telefone é obrigatório") String telefone
    ) {}

    public record Response(
            UUID id,
            String nome,
            String email,
            String telefone,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {}
}
