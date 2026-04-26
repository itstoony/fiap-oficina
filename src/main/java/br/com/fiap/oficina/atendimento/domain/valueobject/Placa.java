package br.com.fiap.oficina.atendimento.domain.valueobject;

import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Placa {

    @Column(name = "placa")
    private String valor;

    protected Placa() {
    }

    private Placa(String valor) {
        this.valor = valor;
    }

    public static Placa of(String valor) {
        String normalizado = valor.toUpperCase().replaceAll("[^A-Z0-9]", "");
        boolean antigoValido = normalizado.matches("[A-Z]{3}[0-9]{4}");
        boolean mercosulValido = normalizado.matches("[A-Z]{3}[0-9][A-Z][0-9]{2}");
        if (!antigoValido && !mercosulValido) {
            throw new RegraDeNegocioException(
                    "Placa inválida: " + valor + ". Formatos aceitos: ABC1234 (antigo) ou ABC1D23 (Mercosul)");
        }
        return new Placa(normalizado);
    }

    public String getValor() {
        return valor;
    }
}