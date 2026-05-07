package br.com.fiap.oficina.execucao.controller;

import br.com.fiap.oficina.execucao.service.OrdemDeServicoService;
import br.com.fiap.oficina.execucao.service.dto.OrdemDeServicoDTO;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import br.com.fiap.oficina.seguranca.service.JwtService;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdemDeServicoAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser
class OrdemDeServicoAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrdemDeServicoService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void criar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new OrdemDeServicoDTO.CriarRequest(UUID.randomUUID(), UUID.randomUUID(), null, null);
        when(service.criar(any())).thenReturn(criarResponse("RECEBIDA"));

        mockMvc.perform(post("/api/admin/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.numero").value("OS-2026-00001"));
    }

    @Test
    void criar_semClienteId_deveRetornarBadRequest() throws Exception {
        var body = "{\"veiculoId\": \"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/admin/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_deveRetornarListaDeOrdens() throws Exception {
        when(service.listar()).thenReturn(List.of(criarResponse("RECEBIDA"), criarResponse("EM_DIAGNOSTICO")));

        mockMvc.perform(get("/api/admin/ordens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorId_osExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse("RECEBIDA"));

        mockMvc.perform(get("/api/admin/ordens/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("OS-2026-00001"));
    }

    @Test
    void buscarPorId_osInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("OS não encontrada"));

        mockMvc.perform(get("/api/admin/ordens/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void iniciarDiagnostico_transicaoValida_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.iniciarDiagnostico(id)).thenReturn(criarResponse("EM_DIAGNOSTICO"));

        mockMvc.perform(post("/api/admin/ordens/{id}/iniciar-diagnostico", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    void iniciarDiagnostico_transicaoInvalida_deveRetornarUnprocessableEntity() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.iniciarDiagnostico(id)).thenThrow(new RegraDeNegocioException("Transição não permitida"));

        mockMvc.perform(post("/api/admin/ordens/{id}/iniciar-diagnostico", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void iniciarExecucao_comAtendente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID atendenteId = UUID.randomUUID();
        var body = "{\"atendenteId\": \"" + atendenteId + "\"}";
        when(service.iniciarExecucao(eq(id), any())).thenReturn(criarResponse("EM_EXECUCAO"));

        mockMvc.perform(post("/api/admin/ordens/{id}/iniciar-execucao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void iniciarExecucao_semAtendente_deveRetornarBadRequest() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/admin/ordens/{id}/iniciar-execucao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelar_osExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.cancelar(id)).thenReturn(criarResponse("CANCELADA"));

        mockMvc.perform(post("/api/admin/ordens/{id}/cancelar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void enviarOrcamento_osExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.enviarOrcamento(id)).thenReturn(criarResponse("AGUARDANDO_APROVACAO"));

        mockMvc.perform(post("/api/admin/ordens/{id}/enviar-orcamento", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
    }

    @Test
    void finalizar_osExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.finalizar(id)).thenReturn(criarResponse("FINALIZADA"));

        mockMvc.perform(post("/api/admin/ordens/{id}/finalizar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));
    }

    @Test
    void entregar_osExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.entregar(id)).thenReturn(criarResponse("ENTREGUE"));

        mockMvc.perform(post("/api/admin/ordens/{id}/entregar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void adicionarServico_comDadosValidos_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.adicionarServico(eq(id), any())).thenReturn(criarResponse("RECEBIDA"));

        var body = "{\"servicoId\": \"" + UUID.randomUUID() + "\", \"quantidade\": 1}";
        mockMvc.perform(post("/api/admin/ordens/{id}/servicos", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("OS-2026-00001"));
    }

    @Test
    void adicionarPeca_comDadosValidos_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.adicionarPeca(eq(id), any())).thenReturn(criarResponse("RECEBIDA"));

        var body = "{\"pecaId\": \"" + UUID.randomUUID() + "\", \"quantidade\": 1}";
        mockMvc.perform(post("/api/admin/ordens/{id}/pecas", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("OS-2026-00001"));
    }

    @Test
    void removerServico_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(service.removerServico(id, itemId)).thenReturn(criarResponse("RECEBIDA"));

        mockMvc.perform(delete("/api/admin/ordens/{id}/servicos/{itemId}", id, itemId))
                .andExpect(status().isOk());
    }

    @Test
    void removerPeca_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(service.removerPeca(id, itemId)).thenReturn(criarResponse("RECEBIDA"));

        mockMvc.perform(delete("/api/admin/ordens/{id}/pecas/{itemId}", id, itemId))
                .andExpect(status().isOk());
    }

    private OrdemDeServicoDTO.Response criarResponse(String status) {
        return new OrdemDeServicoDTO.Response(
                UUID.randomUUID(), "OS-2026-00001", status,
                UUID.randomUUID(), "João Silva",
                UUID.randomUUID(), "ABC1234",
                null, null,
                BigDecimal.ZERO, null,
                List.of(), List.of(),
                LocalDateTime.now(), null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
