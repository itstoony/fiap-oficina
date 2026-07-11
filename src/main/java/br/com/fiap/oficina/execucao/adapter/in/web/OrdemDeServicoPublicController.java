package br.com.fiap.oficina.execucao.adapter.in.web;

import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoDTO;
import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoPublicUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/ordens")
@RequiredArgsConstructor
@Tag(name = "Ordens de Serviço (Público)", description = "Consulta e aprovação de OS pelo cliente — sem autenticação")
public class OrdemDeServicoPublicController {

    private final OrdemDeServicoPublicUseCase useCase;

    @GetMapping("/{numero}/status")
    @Operation(summary = "Consultar status da OS pelo número (público, sem JWT)")
    public ResponseEntity<OrdemDeServicoDTO.StatusPublicoResponse> consultarStatus(
            @PathVariable String numero) {
        return ResponseEntity.ok(useCase.buscarStatusPublico(numero));
    }

    @PostMapping("/{numero}/aprovar")
    @Operation(summary = "Cliente aprova o orçamento — AGUARDANDO_APROVACAO → EM_EXECUCAO")
    public ResponseEntity<OrdemDeServicoDTO.StatusPublicoResponse> aprovar(
            @PathVariable String numero) {
        return ResponseEntity.ok(useCase.aprovar(numero));
    }

    @PostMapping("/{numero}/recusar")
    @Operation(summary = "Cliente recusa o orçamento — AGUARDANDO_APROVACAO → CANCELADA")
    public ResponseEntity<OrdemDeServicoDTO.StatusPublicoResponse> recusar(
            @PathVariable String numero) {
        return ResponseEntity.ok(useCase.recusar(numero));
    }
}
