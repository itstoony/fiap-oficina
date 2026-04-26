package br.com.fiap.oficina.execucao.controller;

import br.com.fiap.oficina.execucao.service.OrdemDeServicoService;
import br.com.fiap.oficina.execucao.service.dto.OrdemDeServicoDTO;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import br.com.fiap.oficina.seguranca.service.JwtService;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdemDeServicoPublicController.class)
@Import(SecurityConfig.class)
class OrdemDeServicoPublicControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrdemDeServicoService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void consultarStatus_osExistente_deveRetornarOk() throws Exception {
        when(service.buscarStatusPublico("OS-2026-00001"))
                .thenReturn(criarStatusResponse("AGUARDANDO_APROVACAO"));

        mockMvc.perform(get("/api/public/ordens/{numero}/status", "OS-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("OS-2026-00001"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
    }

    @Test
    void consultarStatus_osInexistente_deveRetornarNotFound() throws Exception {
        when(service.buscarStatusPublico("OS-2026-99999"))
                .thenThrow(new RecursoNaoEncontradoException("OS não encontrada"));

        mockMvc.perform(get("/api/public/ordens/{numero}/status", "OS-2026-99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aprovar_statusAguardandoAprovacao_deveRetornarOk() throws Exception {
        when(service.aprovar("OS-2026-00001"))
                .thenReturn(criarStatusResponse("EM_EXECUCAO"));

        mockMvc.perform(post("/api/public/ordens/{numero}/aprovar", "OS-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void aprovar_statusInvalido_deveRetornarUnprocessableEntity() throws Exception {
        when(service.aprovar("OS-2026-00001"))
                .thenThrow(new RegraDeNegocioException("Transição não permitida"));

        mockMvc.perform(post("/api/public/ordens/{numero}/aprovar", "OS-2026-00001"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void recusar_statusAguardandoAprovacao_deveRetornarOk() throws Exception {
        when(service.recusar("OS-2026-00001"))
                .thenReturn(criarStatusResponse("CANCELADA"));

        mockMvc.perform(post("/api/public/ordens/{numero}/recusar", "OS-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    private OrdemDeServicoDTO.StatusPublicoResponse criarStatusResponse(String status) {
        return new OrdemDeServicoDTO.StatusPublicoResponse(
                "OS-2026-00001", status, BigDecimal.ZERO,
                LocalDateTime.now(), null, null
        );
    }
}
