package br.com.fiap.oficina.execucao.application.port.out;

import java.math.BigDecimal;

public interface NotificacaoEmailPort {

    void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal);

    void enviarConfirmacaoAprovacao(String emailCliente, String numeroOS);

    void enviarConfirmacaoRecusa(String emailCliente, String numeroOS);
}
