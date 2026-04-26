package br.com.fiap.oficina.estoque.controller;

import br.com.fiap.oficina.estoque.service.PecaService;
import br.com.fiap.oficina.estoque.service.dto.PecaDTO;
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
@RequestMapping("/api/admin/pecas")
@RequiredArgsConstructor
@Tag(name = "Peças", description = "Gestão de peças e controle de estoque")
@SecurityRequirement(name = "bearerAuth")
public class PecaController {

    private final PecaService pecaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar peça")
    public PecaDTO.Response cadastrar(@Valid @RequestBody PecaDTO.CadastrarRequest request) {
        return pecaService.cadastrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todas as peças")
    public List<PecaDTO.Response> listar() {
        return pecaService.listar();
    }

    @GetMapping("/criticas")
    @Operation(summary = "Listar peças com estoque crítico (qtdEstoque <= qtdMinima)")
    public List<PecaDTO.Response> buscarCriticas() {
        return pecaService.buscarCriticas();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar peça por ID")
    public PecaDTO.Response buscarPorId(@PathVariable UUID id) {
        return pecaService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados da peça")
    public PecaDTO.Response atualizar(@PathVariable UUID id, @Valid @RequestBody PecaDTO.AtualizarRequest request) {
        return pecaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir peça")
    public void excluir(@PathVariable UUID id) {
        pecaService.excluir(id);
    }

    @PostMapping("/{id}/entrada")
    @Operation(summary = "Registrar entrada de estoque")
    public PecaDTO.Response registrarEntrada(@PathVariable UUID id, @Valid @RequestBody PecaDTO.EntradaRequest request) {
        return pecaService.registrarEntrada(id, request);
    }

    @GetMapping("/{id}/movimentacoes")
    @Operation(summary = "Listar histórico de movimentações da peça")
    public List<PecaDTO.MovimentacaoResponse> buscarMovimentacoes(@PathVariable UUID id) {
        return pecaService.buscarMovimentacoes(id);
    }
}
