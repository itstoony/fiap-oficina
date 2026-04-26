package br.com.fiap.oficina.atendimento.controller;

import br.com.fiap.oficina.atendimento.service.ClienteService;
import br.com.fiap.oficina.atendimento.service.dto.ClienteDTO;
import br.com.fiap.oficina.seguranca.config.SecurityConfig;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService service;

    @Test
    void cadastrar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new ClienteDTO.CadastrarRequest("João Silva", "joao@email.com", "11999999999", "529.982.247-25");
        var response = criarResponse(UUID.randomUUID());
        when(service.cadastrar(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.tipoDocumento").value("CPF"));
    }

    @Test
    void cadastrar_comCamposEmBranco_deveRetornarBadRequest() throws Exception {
        var request = new ClienteDTO.CadastrarRequest("", "", "", "");

        mockMvc.perform(post("/api/admin/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").exists());
    }

    @Test
    void cadastrar_comDocumentoDuplicado_deveRetornarUnprocessableEntity() throws Exception {
        var request = new ClienteDTO.CadastrarRequest("João Silva", "joao@email.com", "11999999999", "529.982.247-25");
        when(service.cadastrar(any())).thenThrow(new RegraDeNegocioException("Já existe um cliente cadastrado com este documento"));

        mockMvc.perform(post("/api/admin/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_deveRetornarListaDeClientes() throws Exception {
        when(service.listar()).thenReturn(List.of(criarResponse(UUID.randomUUID()), criarResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/admin/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void buscarPorId_clienteExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse(id));

        mockMvc.perform(get("/api/admin/clientes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_clienteInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado"));

        mockMvc.perform(get("/api/admin/clientes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }

    @Test
    void buscarPorDocumento_clienteExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorDocumento("52998224725")).thenReturn(criarResponse(id));

        mockMvc.perform(get("/api/admin/clientes/documento/{documento}", "52998224725"))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_clienteExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new ClienteDTO.AtualizarRequest("Novo Nome", "novo@email.com", "11888888888");
        when(service.atualizar(eq(id), any())).thenReturn(criarResponse(id));

        mockMvc.perform(put("/api/admin/clientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void excluir_clienteExistente_deveRetornarNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/clientes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_clienteInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RecursoNaoEncontradoException("Cliente não encontrado")).when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/clientes/{id}", id))
                .andExpect(status().isNotFound());
    }

    private ClienteDTO.Response criarResponse(UUID id) {
        return new ClienteDTO.Response(
                id,
                "João Silva",
                "joao@email.com",
                "11999999999",
                "529.982.247-25",
                "CPF",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
