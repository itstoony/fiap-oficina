package br.com.fiap.oficina.execucao.adapter.out.email;

import br.com.fiap.oficina.execucao.application.port.out.NotificacaoEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class EmailNotificacaoAdapter implements NotificacaoEmailPort {

    private final JavaMailSender mailSender;

    @Value("${oficina.app.url}")
    private String appUrl;

    @Override
    public void enviarOrcamentoParaAprovacao(String emailCliente, String numeroOS, BigDecimal valorTotal) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(emailCliente);
        mensagem.setSubject("Orçamento disponível — OS " + numeroOS);
        mensagem.setText("""
                Olá! Seu orçamento está pronto.
                OS: %s | Valor: R$ %s
                Aprovar: %s/api/public/ordens/%s/aprovar
                Recusar: %s/api/public/ordens/%s/recusar
                """.formatted(numeroOS, valorTotal, appUrl, numeroOS, appUrl, numeroOS));
        mailSender.send(mensagem);
    }

    @Override
    public void enviarConfirmacaoAprovacao(String emailCliente, String numeroOS) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(emailCliente);
        mensagem.setSubject("Orçamento aprovado — OS " + numeroOS);
        mensagem.setText("Seu orçamento foi aprovado. A execução dos serviços da OS %s foi iniciada.".formatted(numeroOS));
        mailSender.send(mensagem);
    }

    @Override
    public void enviarConfirmacaoRecusa(String emailCliente, String numeroOS) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(emailCliente);
        mensagem.setSubject("Orçamento recusado — OS " + numeroOS);
        mensagem.setText("Seu orçamento foi recusado. A OS %s foi cancelada.".formatted(numeroOS));
        mailSender.send(mensagem);
    }
}
