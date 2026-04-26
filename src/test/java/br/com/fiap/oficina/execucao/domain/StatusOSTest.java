package br.com.fiap.oficina.execucao.domain;

import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusOSTest {

    @Test
    void recebida_podeTransicionarParaEmDiagnostico() {
        assertThatCode(() -> StatusOS.RECEBIDA.validarTransicaoPara(StatusOS.EM_DIAGNOSTICO))
                .doesNotThrowAnyException();
    }

    @Test
    void recebida_podeTransicionarParaCancelada() {
        assertThatCode(() -> StatusOS.RECEBIDA.validarTransicaoPara(StatusOS.CANCELADA))
                .doesNotThrowAnyException();
    }

    @Test
    void recebida_naoPoderTransicionarParaEmExecucao() {
        assertThatThrownBy(() -> StatusOS.RECEBIDA.validarTransicaoPara(StatusOS.EM_EXECUCAO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void emDiagnostico_podeTransicionarParaAguardandoAprovacao() {
        assertThatCode(() -> StatusOS.EM_DIAGNOSTICO.validarTransicaoPara(StatusOS.AGUARDANDO_APROVACAO))
                .doesNotThrowAnyException();
    }

    @Test
    void emDiagnostico_podeTransicionarParaCancelada() {
        assertThatCode(() -> StatusOS.EM_DIAGNOSTICO.validarTransicaoPara(StatusOS.CANCELADA))
                .doesNotThrowAnyException();
    }

    @Test
    void emDiagnostico_naoPodeRetrocederParaRecebida() {
        assertThatThrownBy(() -> StatusOS.EM_DIAGNOSTICO.validarTransicaoPara(StatusOS.RECEBIDA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aguardandoAprovacao_podeTransicionarParaAprovado() {
        assertThatCode(() -> StatusOS.AGUARDANDO_APROVACAO.validarTransicaoPara(StatusOS.APROVADO))
                .doesNotThrowAnyException();
    }

    @Test
    void aguardandoAprovacao_podeTransicionarParaCancelada() {
        assertThatCode(() -> StatusOS.AGUARDANDO_APROVACAO.validarTransicaoPara(StatusOS.CANCELADA))
                .doesNotThrowAnyException();
    }

    @Test
    void aguardandoAprovacao_naoPodeTransicionarDiretoParaEmExecucao() {
        assertThatThrownBy(() -> StatusOS.AGUARDANDO_APROVACAO.validarTransicaoPara(StatusOS.EM_EXECUCAO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aguardandoAprovacao_naoPodeRetrocederParaEmDiagnostico() {
        assertThatThrownBy(() -> StatusOS.AGUARDANDO_APROVACAO.validarTransicaoPara(StatusOS.EM_DIAGNOSTICO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aprovado_podeTransicionarParaEmExecucao() {
        assertThatCode(() -> StatusOS.APROVADO.validarTransicaoPara(StatusOS.EM_EXECUCAO))
                .doesNotThrowAnyException();
    }

    @Test
    void aprovado_podeTransicionarParaCancelada() {
        assertThatCode(() -> StatusOS.APROVADO.validarTransicaoPara(StatusOS.CANCELADA))
                .doesNotThrowAnyException();
    }

    @Test
    void aprovado_naoPodeRetrocederParaAguardandoAprovacao() {
        assertThatThrownBy(() -> StatusOS.APROVADO.validarTransicaoPara(StatusOS.AGUARDANDO_APROVACAO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void emExecucao_podeTransicionarParaFinalizada() {
        assertThatCode(() -> StatusOS.EM_EXECUCAO.validarTransicaoPara(StatusOS.FINALIZADA))
                .doesNotThrowAnyException();
    }

    @Test
    void emExecucao_naoPodeCancelar() {
        assertThatThrownBy(() -> StatusOS.EM_EXECUCAO.validarTransicaoPara(StatusOS.CANCELADA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void finalizada_podeTransicionarParaEntregue() {
        assertThatCode(() -> StatusOS.FINALIZADA.validarTransicaoPara(StatusOS.ENTREGUE))
                .doesNotThrowAnyException();
    }

    @Test
    void entregue_naoPodeTerNenhumaTransicao() {
        assertThatThrownBy(() -> StatusOS.ENTREGUE.validarTransicaoPara(StatusOS.FINALIZADA))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void cancelada_naoPodeTerNenhumaTransicao() {
        assertThatThrownBy(() -> StatusOS.CANCELADA.validarTransicaoPara(StatusOS.RECEBIDA))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
