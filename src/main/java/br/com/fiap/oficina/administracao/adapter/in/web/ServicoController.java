package br.com.fiap.oficina.administracao.adapter.in.web;

import br.com.fiap.oficina.administracao.application.port.in.ServicoDTO;
import br.com.fiap.oficina.administracao.application.port.in.ServicoUseCase;
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
@RequestMapping("/api/admin/servicos")
@RequiredArgsConstructor
@Tag(name = "Serviços", description = "Catálogo de serviços da oficina")
@SecurityRequirement(name = "bearerAuth")
public class ServicoController {

    private final ServicoUseCase servicoUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar serviço no catálogo")
    public ServicoDTO.Response cadastrar(@Valid @RequestBody ServicoDTO.CadastrarRequest request) {
        return servicoUseCase.cadastrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar todos os serviços")
    public List<ServicoDTO.Response> listar() {
        return servicoUseCase.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço por ID")
    public ServicoDTO.Response buscarPorId(@PathVariable UUID id) {
        return servicoUseCase.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço")
    public ServicoDTO.Response atualizar(@PathVariable UUID id, @Valid @RequestBody ServicoDTO.AtualizarRequest request) {
        return servicoUseCase.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir serviço")
    public void excluir(@PathVariable UUID id) {
        servicoUseCase.excluir(id);
    }
}
