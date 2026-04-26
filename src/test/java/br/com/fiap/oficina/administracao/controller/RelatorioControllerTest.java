package br.com.fiap.oficina.administracao.controller;

import br.com.fiap.oficina.administracao.service.RelatorioService;
import br.com.fiap.oficina.administracao.service.dto.RelatorioDTO;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RelatorioController.class)
@Import(SecurityConfig.class)
class RelatorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService service;

    @Test
    void tempoMedioExecucao_comDados_deveRetornarOk() throws Exception {
        when(service.calcularTempoMedioExecucao())
                .thenReturn(new RelatorioDTO.TempoMedioResponse(5, 150, "2h 30m"));

        mockMvc.perform(get("/api/admin/relatorios/tempo-medio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrdens").value(5))
                .andExpect(jsonPath("$.mediaMinutos").value(150))
                .andExpect(jsonPath("$.mediaFormatada").value("2h 30m"));
    }

    @Test
    void tempoMedioExecucao_semDados_deveRetornarZerado() throws Exception {
        when(service.calcularTempoMedioExecucao())
                .thenReturn(new RelatorioDTO.TempoMedioResponse(0, 0, "Sem dados"));

        mockMvc.perform(get("/api/admin/relatorios/tempo-medio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrdens").value(0))
                .andExpect(jsonPath("$.mediaFormatada").value("Sem dados"));
    }
}
