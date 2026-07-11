package br.com.fiap.oficina.seguranca.controller;

import br.com.fiap.oficina.seguranca.adapter.in.web.AuthController;
import br.com.fiap.oficina.seguranca.application.port.in.AuthDTO;
import br.com.fiap.oficina.seguranca.application.port.in.AuthUseCase;
import br.com.fiap.oficina.seguranca.application.service.JwtService;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthUseCase authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void login_comCredenciaisValidas_deveRetornarToken() throws Exception {
        var request = new AuthDTO.LoginRequest("admin", "admin123");
        when(authService.login(any())).thenReturn(new AuthDTO.LoginResponse("token.jwt.gerado"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.gerado"));
    }

    @Test
    void login_comCamposEmBranco_deveRetornarBadRequest() throws Exception {
        var request = new AuthDTO.LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_comCredenciaisInvalidas_deveRetornarUnauthorized() throws Exception {
        var request = new AuthDTO.LoginRequest("admin", "errada");
        when(authService.login(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
