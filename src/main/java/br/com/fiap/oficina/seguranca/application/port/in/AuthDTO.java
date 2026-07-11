package br.com.fiap.oficina.seguranca.application.port.in;

import jakarta.validation.constraints.NotBlank;

public class AuthDTO {

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String senha
    ) {}

    public record LoginResponse(String token) {}
}
