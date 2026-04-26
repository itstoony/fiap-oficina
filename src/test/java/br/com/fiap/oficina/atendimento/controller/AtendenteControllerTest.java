package br.com.fiap.oficina.atendimento.controller;

import br.com.fiap.oficina.atendimento.service.AtendenteService;
import br.com.fiap.oficina.atendimento.service.dto.AtendenteDTO;
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

@WebMvcTest(AtendenteController.class)
@Import(SecurityConfig.class)
@WithMockUser
class AtendenteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AtendenteService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void cadastrar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new AtendenteDTO.CadastrarRequest("Carlos Silva", "carlos@oficina.com", "(11) 99999-0001");
        UUID id = UUID.randomUUID();
        when(service.cadastrar(any())).thenReturn(criarResponse(id, "Carlos Silva", "carlos@oficina.com"));

        mockMvc.perform(post("/api/admin/atendentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Carlos Silva"))
                .andExpect(jsonPath("$.email").value("carlos@oficina.com"));
    }

    @Test
    void cadastrar_comCamposInvalidos_deveRetornarBadRequest() throws Exception {
        var request = new AtendenteDTO.CadastrarRequest("", "email-invalido", "");

        mockMvc.perform(post("/api/admin/atendentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_comEmailDuplicado_deveRetornarUnprocessableEntity() throws Exception {
        var request = new AtendenteDTO.CadastrarRequest("Carlos Silva", "carlos@oficina.com", "(11) 99999-0001");
        when(service.cadastrar(any())).thenThrow(new RegraDeNegocioException("Já existe um atendente com este email"));

        mockMvc.perform(post("/api/admin/atendentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_deveRetornarListaDeAtendentes() throws Exception {
        when(service.listar()).thenReturn(List.of(
                criarResponse(UUID.randomUUID(), "Carlos Silva", "carlos@oficina.com"),
                criarResponse(UUID.randomUUID(), "Ana Lima", "ana@oficina.com")
        ));

        mockMvc.perform(get("/api/admin/atendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorId_atendenteExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse(id, "Carlos Silva", "carlos@oficina.com"));

        mockMvc.perform(get("/api/admin/atendentes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_atendenteInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("Atendente não encontrado"));

        mockMvc.perform(get("/api/admin/atendentes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_atendenteExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new AtendenteDTO.AtualizarRequest("Novo Nome", "novo@oficina.com", "(11) 88888-0001");
        when(service.atualizar(eq(id), any())).thenReturn(criarResponse(id, "Novo Nome", "novo@oficina.com"));

        mockMvc.perform(put("/api/admin/atendentes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"));
    }

    @Test
    void excluir_atendenteExistente_deveRetornarNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/atendentes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_atendenteInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RecursoNaoEncontradoException("Atendente não encontrado")).when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/atendentes/{id}", id))
                .andExpect(status().isNotFound());
    }

    private AtendenteDTO.Response criarResponse(UUID id, String nome, String email) {
        return new AtendenteDTO.Response(
                id, nome, email, "(11) 99999-0001",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
