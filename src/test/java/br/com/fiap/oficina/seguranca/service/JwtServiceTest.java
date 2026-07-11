package br.com.fiap.oficina.seguranca.service;

import br.com.fiap.oficina.seguranca.application.service.JwtService;
import br.com.fiap.oficina.seguranca.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "chave-secreta-minimo-256-bits-para-o-jwt-do-sistema-oficina";
    private static final long EXPIRACAO = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiracao", EXPIRACAO);
    }

    @Test
    void gerarToken_deveRetornarTokenNaoNulo() {
        String token = jwtService.gerarToken("admin");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extrairLogin_tokenValido_deveRetornarLoginCorreto() {
        String token = jwtService.gerarToken("admin");

        assertThat(jwtService.extrairLogin(token)).isEqualTo("admin");
    }

    @Test
    void validarToken_tokenValidoEUsuarioCorreto_deveRetornarTrue() {
        Usuario usuario = Usuario.builder().login("admin").senha("senha").nome("Admin").build();
        String token = jwtService.gerarToken("admin");

        assertThat(jwtService.validarToken(token, usuario)).isTrue();
    }

    @Test
    void validarToken_tokenDeOutroUsuario_deveRetornarFalse() {
        Usuario usuario = Usuario.builder().login("outro").senha("senha").nome("Outro").build();
        String token = jwtService.gerarToken("admin");

        assertThat(jwtService.validarToken(token, usuario)).isFalse();
    }

    @Test
    void validarToken_tokenExpirado_deveRetornarFalse() {
        JwtService serviceComTokenExpirado = new JwtService();
        ReflectionTestUtils.setField(serviceComTokenExpirado, "secret", SECRET);
        ReflectionTestUtils.setField(serviceComTokenExpirado, "expiracao", -1000L);

        Usuario usuario = Usuario.builder().login("admin").senha("senha").nome("Admin").build();
        String token = serviceComTokenExpirado.gerarToken("admin");

        assertThat(jwtService.validarToken(token, usuario)).isFalse();
    }

    @Test
    void extrairLogin_tokenInvalido_deveLancarException() {
        assertThatThrownBy(() -> jwtService.extrairLogin("token.invalido.aqui"))
                .isInstanceOf(Exception.class);
    }
}
