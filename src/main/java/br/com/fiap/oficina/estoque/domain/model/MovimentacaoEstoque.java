package br.com.fiap.oficina.estoque.domain.model;

import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "os_id")
    private UUID osId;

    private String observacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataMovimentacao;

    @PrePersist
    private void prePersist() {
        dataMovimentacao = LocalDateTime.now();
    }
}
