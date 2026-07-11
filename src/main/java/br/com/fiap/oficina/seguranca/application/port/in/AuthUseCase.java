package br.com.fiap.oficina.seguranca.application.port.in;

public interface AuthUseCase {
    AuthDTO.LoginResponse login(AuthDTO.LoginRequest request);
}
