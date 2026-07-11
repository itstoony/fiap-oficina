package br.com.fiap.oficina.seguranca.adapter.in.web;

import br.com.fiap.oficina.seguranca.application.port.in.AuthDTO;
import br.com.fiap.oficina.seguranca.application.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos de autenticação")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter token JWT")
    public AuthDTO.LoginResponse login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return authUseCase.login(request);
    }
}
