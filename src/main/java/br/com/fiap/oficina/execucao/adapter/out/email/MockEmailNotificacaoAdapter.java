package br.com.fiap.oficina.execucao.adapter.out.email;

import br.com.fiap.oficina.execucao.application.port.out.NotificacaoEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile({"dev", "test"})
@Slf4j
public class MockEmailNotificacaoAdapter implements NotificacaoEmailPort {

    @Value("${oficina.app.url:http://localhost:8080}")
    private String appUrl;

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal) {
        log.info("[EMAIL MOCK] Para: {} | OS: {} | Valor: R$ {} | Aprovar: {}/api/public/ordens/{}/aprovar",
                emailCliente, numeroOS, valorTotal, appUrl, numeroOS);
    }

    @Override
    public void enviarConfirmacaoAprovacao(String emailCliente, String numeroOS) {
        log.info("[EMAIL MOCK] Confirmação de aprovação — Para: {} | OS: {}", emailCliente, numeroOS);
    }

    @Override
    public void enviarConfirmacaoRecusa(String emailCliente, String numeroOS) {
        log.info("[EMAIL MOCK] Confirmação de recusa — Para: {} | OS: {}", emailCliente, numeroOS);
    }
}
