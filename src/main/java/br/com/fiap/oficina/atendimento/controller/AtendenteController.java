package br.com.fiap.oficina.atendimento.controller;

import br.com.fiap.oficina.atendimento.service.AtendenteService;
import br.com.fiap.oficina.atendimento.service.dto.AtendenteDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/atendentes")
@RequiredArgsConstructor
@Tag(name = "Atendentes", description = "Gestão de atendentes da oficina")
@SecurityRequirement(name = "bearerAuth")
public class AtendenteController {

    private final AtendenteService atendenteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar atendente")
    public AtendenteDTO.Response cadastrar(@Valid @RequestBody AtendenteDTO.CadastrarRequest request) {
        return atendenteService.cadastrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os atendentes")
    public List<AtendenteDTO.Response> listar() {
        return atendenteService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar atendente por ID")
    public AtendenteDTO.Response buscarPorId(@PathVariable UUID id) {
        return atendenteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar atendente")
    public AtendenteDTO.Response atualizar(@PathVariable UUID id, @Valid @RequestBody AtendenteDTO.AtualizarRequest request) {
        return atendenteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir atendente")
    public void excluir(@PathVariable UUID id) {
        atendenteService.excluir(id);
    }
}
