package br.com.fiap.oficina.estoque.controller;

import br.com.fiap.oficina.estoque.adapter.in.web.PecaController;
import br.com.fiap.oficina.estoque.application.port.in.PecaDTO;
import br.com.fiap.oficina.estoque.application.port.in.PecaUseCase;
import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import br.com.fiap.oficina.seguranca.application.service.JwtService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PecaController.class)
@Import(SecurityConfig.class)
@WithMockUser
class PecaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PecaUseCase service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void cadastrar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new PecaDTO.CadastrarRequest("Filtro de Óleo", "FILTRO-001", new BigDecimal("45.90"), 10, 3);
        when(service.cadastrar(any())).thenReturn(criarResponse(UUID.randomUUID(), 10, 0, 3));

        mockMvc.perform(post("/api/admin/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Filtro de Óleo"))
                .andExpect(jsonPath("$.estoqueCritico").value(false));
    }

    @Test
    void cadastrar_comCamposInvalidos_deveRetornarBadRequest() throws Exception {
        var request = new PecaDTO.CadastrarRequest("", "", null, null, null);

        mockMvc.perform(post("/api/admin/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_comCodigoDuplicado_deveRetornarUnprocessableEntity() throws Exception {
        var request = new PecaDTO.CadastrarRequest("Filtro", "FILTRO-001", new BigDecimal("45.90"), 10, 3);
        when(service.cadastrar(any())).thenThrow(new RegraDeNegocioException("Já existe uma peça com o código FILTRO-001"));

        mockMvc.perform(post("/api/admin/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_deveRetornarListaDePecas() throws Exception {
        when(service.listar()).thenReturn(List.of(
                criarResponse(UUID.randomUUID(), 10, 0, 3),
                criarResponse(UUID.randomUUID(), 2, 1, 5)
        ));

        mockMvc.perform(get("/api/admin/pecas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorId_pecaExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse(id, 10, 0, 3));

        mockMvc.perform(get("/api/admin/pecas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_pecaInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("Peça não encontrada"));

        mockMvc.perform(get("/api/admin/pecas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarCriticas_deveRetornarApenasCriticas() throws Exception {
        when(service.buscarCriticas()).thenReturn(List.of(criarResponse(UUID.randomUUID(), 2, 0, 5)));

        mockMvc.perform(get("/api/admin/pecas/criticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estoqueCritico").value(true));
    }

    @Test
    void atualizar_pecaExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new PecaDTO.AtualizarRequest("Novo Nome", new BigDecimal("55.00"), 5);
        when(service.atualizar(eq(id), any())).thenReturn(criarResponse(id, 10, 0, 5));

        mockMvc.perform(put("/api/admin/pecas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void excluir_pecaExistente_deveRetornarNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/pecas/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_pecaComReservas_deveRetornarUnprocessableEntity() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RegraDeNegocioException("Peça com reservas ativas")).when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/pecas/{id}", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registrarEntrada_deveRetornarOkComNovoEstoque() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new PecaDTO.EntradaRequest(10, "Reposição");
        when(service.registrarEntrada(eq(id), any())).thenReturn(criarResponse(id, 20, 0, 3));

        mockMvc.perform(post("/api/admin/pecas/{id}/entrada", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qtdEstoque").value(20));
    }

    @Test
    void buscarMovimentacoes_deveRetornarHistorico() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarMovimentacoes(id)).thenReturn(List.of(
                new PecaDTO.MovimentacaoResponse(UUID.randomUUID(), TipoMovimentacao.ENTRADA, 10, null, "Estoque inicial", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/admin/pecas/{id}/movimentacoes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"));
    }

    private PecaDTO.Response criarResponse(UUID id, int qtdEstoque, int qtdReservada, int qtdMinima) {
        return new PecaDTO.Response(
                id, "Filtro de Óleo", "FILTRO-001", new BigDecimal("45.90"),
                qtdEstoque, qtdReservada, qtdEstoque - qtdReservada, qtdMinima,
                qtdEstoque <= qtdMinima, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
