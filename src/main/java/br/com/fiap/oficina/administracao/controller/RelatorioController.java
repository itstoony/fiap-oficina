package br.com.fiap.oficina.administracao.controller;

import br.com.fiap.oficina.administracao.service.RelatorioService;
import br.com.fiap.oficina.administracao.service.dto.RelatorioDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Relatórios administrativos da oficina")
@SecurityRequirement(name = "bearerAuth")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/tempo-medio")
    @Operation(summary = "Tempo médio de execução das OSs finalizadas/entregues")
    public RelatorioDTO.TempoMedioResponse tempoMedioExecucao() {
        return relatorioService.calcularTempoMedioExecucao();
    }
}
