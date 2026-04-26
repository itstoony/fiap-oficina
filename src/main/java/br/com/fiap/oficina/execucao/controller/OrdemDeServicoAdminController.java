package br.com.fiap.oficina.execucao.controller;

import br.com.fiap.oficina.execucao.service.OrdemDeServicoService;
import br.com.fiap.oficina.execucao.service.dto.ItemPecaDTO;
import br.com.fiap.oficina.execucao.service.dto.ItemServicoDTO;
import br.com.fiap.oficina.execucao.service.dto.OrdemDeServicoDTO;
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
@RequestMapping("/api/admin/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço (Admin)", description = "Gestão completa das Ordens de Serviço")
@SecurityRequirement(name = "bearerAuth")
public class OrdemDeServicoAdminController {

    private final OrdemDeServicoService service;

    @PostMapping
    @Operation(summary = "Criar nova Ordem de Serviço")
    public ResponseEntity<OrdemDeServicoDTO.Response> criar(
            @RequestBody @Valid OrdemDeServicoDTO.CriarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping
    @Operation(summary = "Listar todas as Ordens de Serviço")
    public ResponseEntity<List<OrdemDeServicoDTO.Response>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Ordem de Serviço por ID")
    public ResponseEntity<OrdemDeServicoDTO.Response> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // -------------------------------------------------------------------------
    // ITENS
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/servicos")
    @Operation(summary = "Adicionar serviço à OS")
    public ResponseEntity<OrdemDeServicoDTO.Response> adicionarServico(
            @PathVariable UUID id,
            @RequestBody @Valid ItemServicoDTO.AdicionarRequest request) {
        return ResponseEntity.ok(service.adicionarServico(id, request));
    }

    @DeleteMapping("/{id}/servicos/{itemId}")
    @Operation(summary = "Remover serviço da OS")
    public ResponseEntity<OrdemDeServicoDTO.Response> removerServico(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.removerServico(id, itemId));
    }

    @PostMapping("/{id}/pecas")
    @Operation(summary = "Adicionar peça à OS (cria reserva no estoque)")
    public ResponseEntity<OrdemDeServicoDTO.Response> adicionarPeca(
            @PathVariable UUID id,
            @RequestBody @Valid ItemPecaDTO.AdicionarRequest request) {
        return ResponseEntity.ok(service.adicionarPeca(id, request));
    }

    @DeleteMapping("/{id}/pecas/{itemId}")
    @Operation(summary = "Remover peça da OS (libera reserva no estoque)")
    public ResponseEntity<OrdemDeServicoDTO.Response> removerPeca(
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(service.removerPeca(id, itemId));
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/iniciar-diagnostico")
    @Operation(summary = "Iniciar diagnóstico — RECEBIDA → EM_DIAGNOSTICO")
    public ResponseEntity<OrdemDeServicoDTO.Response> iniciarDiagnostico(@PathVariable UUID id) {
        return ResponseEntity.ok(service.iniciarDiagnostico(id));
    }

    @PostMapping("/{id}/enviar-orcamento")
    @Operation(summary = "Enviar orçamento ao cliente — EM_DIAGNOSTICO → AGUARDANDO_APROVACAO")
    public ResponseEntity<OrdemDeServicoDTO.Response> enviarOrcamento(@PathVariable UUID id) {
        return ResponseEntity.ok(service.enviarOrcamento(id));
    }

    @PostMapping("/{id}/iniciar-execucao")
    @Operation(summary = "Iniciar execução (admin) — AGUARDANDO_APROVACAO → EM_EXECUCAO + baixa de estoque")
    public ResponseEntity<OrdemDeServicoDTO.Response> iniciarExecucao(@PathVariable UUID id) {
        return ResponseEntity.ok(service.iniciarExecucao(id));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar serviços — EM_EXECUCAO → FINALIZADA")
    public ResponseEntity<OrdemDeServicoDTO.Response> finalizar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.finalizar(id));
    }

    @PostMapping("/{id}/entregar")
    @Operation(summary = "Registrar entrega do veículo — FINALIZADA → ENTREGUE")
    public ResponseEntity<OrdemDeServicoDTO.Response> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.entregar(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar OS + liberar reservas — qualquer estado antes de EM_EXECUCAO → CANCELADA")
    public ResponseEntity<OrdemDeServicoDTO.Response> cancelar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancelar(id));
    }
}
