package br.com.fiap.oficina.execucao.domain.model;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordens_de_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemDeServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOS status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Atendente atendente;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column
    private String observacoes;

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemServico> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPeca> itensPeca = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    @Column
    private LocalDateTime dataInicioExecucao;

    @Column
    private LocalDateTime dataFimExecucao;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
        if (dataAbertura == null) dataAbertura = LocalDateTime.now();
        if (status == null) status = StatusOS.RECEBIDA;
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
    }

    @PreUpdate
    private void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public void transicionarPara(StatusOS novoStatus) {
        this.status.validarTransicaoPara(novoStatus);
        if (novoStatus == StatusOS.EM_EXECUCAO) {
            this.dataInicioExecucao = LocalDateTime.now();
        }
        if (novoStatus == StatusOS.FINALIZADA) {
            this.dataFimExecucao = LocalDateTime.now();
        }
        if (novoStatus == StatusOS.FINALIZADA || novoStatus == StatusOS.ENTREGUE || novoStatus == StatusOS.CANCELADA) {
            this.ativo = false;
        }
        this.status = novoStatus;
    }

    public void recalcularValorTotal() {
        BigDecimal totalServicos = itensServico.stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPecas = itensPeca.stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.valorTotal = totalServicos.add(totalPecas);
    }

    public void validarEdicaoPermitida() {
        if (status == StatusOS.APROVADO || status == StatusOS.EM_EXECUCAO
                || status == StatusOS.FINALIZADA || status == StatusOS.ENTREGUE
                || status == StatusOS.CANCELADA) {
            throw new br.com.fiap.oficina.shared.exception.RegraDeNegocioException(
                    "Não é possível modificar itens de uma OS com status " + status.name());
        }
    }
}
