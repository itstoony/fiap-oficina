package br.com.fiap.oficina.atendimento.application.port.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public class ClienteDTO {

    public record CadastrarRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank String telefone,
            @NotBlank String documento
    ) {
    }

    public record AtualizarRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank String telefone
    ) {
    }

    public record Response(
            UUID id,
            String nome,
            String email,
            String telefone,
            String documento,
            String tipoDocumento,
            LocalDateTime criadoEm,
            LocalDateTime atualizadoEm
    ) {
    }
}
