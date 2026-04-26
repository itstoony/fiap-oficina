package br.com.fiap.oficina.administracao.controller;

import br.com.fiap.oficina.administracao.service.ServicoService;
import br.com.fiap.oficina.administracao.service.dto.ServicoDTO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServicoController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ServicoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ServicoService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void cadastrar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new ServicoDTO.CadastrarRequest("Troca de Óleo", "Troca de óleo e filtro", new BigDecimal("120.00"));
        when(service.cadastrar(any())).thenReturn(criarResponse(UUID.randomUUID(), "Troca de Óleo"));

        mockMvc.perform(post("/api/admin/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Troca de Óleo"));
    }

    @Test
    void cadastrar_comCamposInvalidos_deveRetornarBadRequest() throws Exception {
        var request = new ServicoDTO.CadastrarRequest("", "", null);

        mockMvc.perform(post("/api/admin/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_comNomeDuplicado_deveRetornarUnprocessableEntity() throws Exception {
        var request = new ServicoDTO.CadastrarRequest("Troca de Óleo", "Desc", new BigDecimal("120.00"));
        when(service.cadastrar(any())).thenThrow(new RegraDeNegocioException("Já existe um serviço com este nome"));

        mockMvc.perform(post("/api/admin/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_deveRetornarListaDeServicos() throws Exception {
        when(service.listar()).thenReturn(List.of(
                criarResponse(UUID.randomUUID(), "Troca de Óleo"),
                criarResponse(UUID.randomUUID(), "Alinhamento")
        ));

        mockMvc.perform(get("/api/admin/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorId_servicoExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse(id, "Troca de Óleo"));

        mockMvc.perform(get("/api/admin/servicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_servicoInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("Serviço não encontrado"));

        mockMvc.perform(get("/api/admin/servicos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_servicoExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new ServicoDTO.AtualizarRequest("Novo Nome", "Nova desc", new BigDecimal("150.00"));
        when(service.atualizar(eq(id), any())).thenReturn(criarResponse(id, "Novo Nome"));

        mockMvc.perform(put("/api/admin/servicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void excluir_servicoExistente_deveRetornarNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/servicos/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_servicoInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RecursoNaoEncontradoException("Serviço não encontrado")).when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/servicos/{id}", id))
                .andExpect(status().isNotFound());
    }

    private ServicoDTO.Response criarResponse(UUID id, String nome) {
        return new ServicoDTO.Response(
                id, nome, "Descrição do serviço", new BigDecimal("120.00"),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
