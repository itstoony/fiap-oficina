package br.com.fiap.oficina.atendimento.adapter.in.web;

import br.com.fiap.oficina.atendimento.application.port.in.AtendenteDTO;
import br.com.fiap.oficina.atendimento.application.port.in.AtendenteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/atendentes")
@RequiredArgsConstructor
@Tag(name = "Atendentes", description = "Gestão de atendentes da oficina")
@SecurityRequirement(name = "bearerAuth")
public class AtendenteController {

    private final AtendenteUseCase atendenteUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar atendente")
    public AtendenteDTO.Response cadastrar(@Valid @RequestBody AtendenteDTO.CadastrarRequest request) {
        return atendenteUseCase.cadastrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os atendentes")
    public List<AtendenteDTO.Response> listar() {
        return atendenteUseCase.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar atendente por ID")
    public AtendenteDTO.Response buscarPorId(@PathVariable UUID id) {
        return atendenteUseCase.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar atendente")
    public AtendenteDTO.Response atualizar(@PathVariable UUID id, @Valid @RequestBody AtendenteDTO.AtualizarRequest request) {
        return atendenteUseCase.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir atendente")
    public void excluir(@PathVariable UUID id) {
        atendenteUseCase.excluir(id);
    }
}
