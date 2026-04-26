package br.com.fiap.oficina.atendimento.controller;

import br.com.fiap.oficina.atendimento.service.VeiculoService;
import br.com.fiap.oficina.atendimento.service.dto.VeiculoDTO;
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

@WebMvcTest(VeiculoController.class)
@Import(SecurityConfig.class)
class VeiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VeiculoService service;

    private final UUID clienteId = UUID.randomUUID();

    @Test
    void cadastrar_comDadosValidos_deveRetornarCreated() throws Exception {
        var request = new VeiculoDTO.CadastrarRequest("Toyota", "Corolla", 2020, "Prata", "ABC1234", clienteId);
        var response = criarResponse(UUID.randomUUID());
        when(service.cadastrar(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marca").value("Toyota"))
                .andExpect(jsonPath("$.placa").value("ABC1234"));
    }

    @Test
    void cadastrar_comCamposInvalidos_deveRetornarBadRequest() throws Exception {
        var request = new VeiculoDTO.CadastrarRequest("", "", null, "", "", null);

        mockMvc.perform(post("/api/admin/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_comPlacaDuplicada_deveRetornarUnprocessableEntity() throws Exception {
        var request = new VeiculoDTO.CadastrarRequest("Toyota", "Corolla", 2020, "Prata", "ABC1234", clienteId);
        when(service.cadastrar(any())).thenThrow(new RegraDeNegocioException("Já existe um veículo cadastrado com esta placa"));

        mockMvc.perform(post("/api/admin/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_deveRetornarListaDeVeiculos() throws Exception {
        when(service.listar()).thenReturn(List.of(criarResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/admin/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void buscarPorId_veiculoExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenReturn(criarResponse(id));

        mockMvc.perform(get("/api/admin/veiculos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void buscarPorId_veiculoInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.buscarPorId(id)).thenThrow(new RecursoNaoEncontradoException("Veículo não encontrado"));

        mockMvc.perform(get("/api/admin/veiculos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorPlaca_veiculoExistente_deveRetornarOk() throws Exception {
        when(service.buscarPorPlaca("ABC1234")).thenReturn(criarResponse(UUID.randomUUID()));

        mockMvc.perform(get("/api/admin/veiculos/placa/{placa}", "ABC1234"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorCliente_deveRetornarVeiculosDoCliente() throws Exception {
        when(service.buscarPorCliente(clienteId)).thenReturn(List.of(criarResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/admin/veiculos/cliente/{clienteId}", clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void atualizar_veiculoExistente_deveRetornarOk() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new VeiculoDTO.AtualizarRequest("Honda", "Civic", 2022, "Branco");
        when(service.atualizar(eq(id), any())).thenReturn(criarResponse(id));

        mockMvc.perform(put("/api/admin/veiculos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void excluir_veiculoExistente_deveRetornarNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/veiculos/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_veiculoInexistente_deveRetornarNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RecursoNaoEncontradoException("Veículo não encontrado")).when(service).excluir(id);

        mockMvc.perform(delete("/api/admin/veiculos/{id}", id))
                .andExpect(status().isNotFound());
    }

    private VeiculoDTO.Response criarResponse(UUID id) {
        return new VeiculoDTO.Response(
                id,
                "Toyota",
                "Corolla",
                2020,
                "Prata",
                "ABC1234",
                clienteId,
                "João Silva",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
