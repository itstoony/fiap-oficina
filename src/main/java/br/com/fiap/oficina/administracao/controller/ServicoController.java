package br.com.fiap.oficina.administracao.controller;

import br.com.fiap.oficina.administracao.service.ServicoService;
import br.com.fiap.oficina.administracao.service.dto.ServicoDTO;
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
@RequestMapping("/api/admin/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final ServicoService servicoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar serviço no catálogo")
    public ServicoDTO.Response cadastrar(@Valid @RequestBody ServicoDTO.CadastrarRequest request) {
        return servicoService.cadastrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os serviços")
    public List<ServicoDTO.Response> listar() {
        return servicoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ServicoDTO.Response buscarPorId(@PathVariable UUID id) {
        return servicoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ServicoDTO.Response atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoDTO.AtualizarRequest request) {
        return servicoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir serviço")
    public void excluir(@PathVariable UUID id) {
        servicoService.excluir(id);
    }
}
