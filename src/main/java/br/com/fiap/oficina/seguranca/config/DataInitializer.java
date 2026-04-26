package br.com.fiap.oficina.seguranca.config;

import br.com.fiap.oficina.seguranca.domain.model.Usuario;
import br.com.fiap.oficina.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${oficina.admin.login}")
    private String adminLogin;

    @Value("${oficina.admin.senha}")
    private String adminSenha;

    @Bean
    public CommandLineRunner inicializarDados() {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario admin = Usuario.builder()
                        .login(adminLogin)
                        .senha(passwordEncoder.encode(adminSenha))
                        .nome("Administrador")
                        .build();
                usuarioRepository.save(admin);
                log.info("Usuário padrão criado — login: {}", adminLogin);
            }
        };
    }
}
