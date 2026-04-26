package br.com.fiap.oficina.atendimento.controller;

import br.com.fiap.oficina.atendimento.service.VeiculoService;
import br.com.fiap.oficina.atendimento.service.dto.VeiculoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/veiculos")
@RequiredArgsConstructor
@Tag(name = "Veículos", description = "Gestão de veículos da oficina")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private final VeiculoService service;

    @PostMapping
    @Operation(summary = "Cadastrar novo veículo")
    public ResponseEntity<VeiculoDTO.Response> cadastrar(@RequestBody @Valid VeiculoDTO.CadastrarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todos os veículos")
    public ResponseEntity<List<VeiculoDTO.Response>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public ResponseEntity<VeiculoDTO.Response> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/placa/{placa}")
    @Operation(summary = "Buscar veículo por placa")
    public ResponseEntity<VeiculoDTO.Response> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(service.buscarPorPlaca(placa));
    }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Listar veículos de um cliente")
    public ResponseEntity<List<VeiculoDTO.Response>> buscarPorCliente(@PathVariable UUID clienteId) {
        return ResponseEntity.ok(service.buscarPorCliente(clienteId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do veículo")
    public ResponseEntity<VeiculoDTO.Response> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid VeiculoDTO.AtualizarRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir veículo")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
