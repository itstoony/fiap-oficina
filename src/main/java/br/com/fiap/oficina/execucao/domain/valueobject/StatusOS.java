package br.com.fiap.oficina.execucao.domain.valueobject;

import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;

import java.util.Collections;
import java.util.Set;

public enum StatusOS {

    RECEBIDA {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(EM_DIAGNOSTICO, CANCELADA);
        }
    },
    EM_DIAGNOSTICO {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(AGUARDANDO_APROVACAO, CANCELADA);
        }
    },
    AGUARDANDO_APROVACAO {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(EM_EXECUCAO, CANCELADA);
        }
    },
    EM_EXECUCAO {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(FINALIZADA);
        }
    },
    FINALIZADA {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Set.of(ENTREGUE);
        }
    },
    ENTREGUE {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Collections.emptySet();
        }
    },
    CANCELADA {
        @Override
        public Set<StatusOS> transicoesPermitidas() {
            return Collections.emptySet();
        }
    };

    public abstract Set<StatusOS> transicoesPermitidas();

    public void validarTransicaoPara(StatusOS novoStatus) {
        if (!transicoesPermitidas().contains(novoStatus)) {
            throw new RegraDeNegocioException(
                    "Transição de " + this.name() + " para " + novoStatus.name() + " não é permitida");
        }
    }
}
