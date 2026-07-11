package br.com.fiap.oficina.seguranca.application.service;

import br.com.fiap.oficina.seguranca.application.port.in.AuthDTO;
import br.com.fiap.oficina.seguranca.application.port.in.AuthUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.senha()));
        String token = jwtService.gerarToken(request.login());
        return new AuthDTO.LoginResponse(token);
    }
}
