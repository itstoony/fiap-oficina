package br.com.fiap.oficina.atendimento.adapter.in.web;

import br.com.fiap.oficina.atendimento.application.port.in.ClienteDTO;
import br.com.fiap.oficina.atendimento.application.port.in.ClienteUseCase;
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
@RequestMapping("/api/admin/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestão de clientes da oficina")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteUseCase clienteUseCase;

    @PostMapping
    @Operation(summary = "Cadastrar novo cliente")
    public ResponseEntity<ClienteDTO.Response> cadastrar(@RequestBody @Valid ClienteDTO.CadastrarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteUseCase.cadastrar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todos os clientes")
    public ResponseEntity<List<ClienteDTO.Response>> listar() {
        return ResponseEntity.ok(clienteUseCase.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteDTO.Response> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteUseCase.buscarPorId(id));
    }

    @GetMapping("/documento/{documento}")
    @Operation(summary = "Buscar cliente por CPF ou CNPJ")
    public ResponseEntity<ClienteDTO.Response> buscarPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(clienteUseCase.buscarPorDocumento(documento));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do cliente")
    public ResponseEntity<ClienteDTO.Response> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid ClienteDTO.AtualizarRequest request) {
        return ResponseEntity.ok(clienteUseCase.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cliente")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        clienteUseCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
