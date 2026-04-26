package br.com.fiap.oficina.atendimento.domain.valueobject;

import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Documento {

    @Column(name = "documento_numero")
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "documento_tipo")
    private TipoDocumento tipo;

    protected Documento() {
    }

    private Documento(String numero, TipoDocumento tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public static Documento of(String valor) {
        String digits = valor.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            if (!isValidCpf(digits)) {
                throw new RegraDeNegocioException("CPF inválido: " + valor);
            }
            return new Documento(digits, TipoDocumento.CPF);
        } else if (digits.length() == 14) {
            if (!isValidCnpj(digits)) {
                throw new RegraDeNegocioException("CNPJ inválido: " + valor);
            }
            return new Documento(digits, TipoDocumento.CNPJ);
        } else {
            throw new RegraDeNegocioException(
                    "Documento inválido: deve ser CPF (11 dígitos) ou CNPJ (14 dígitos). Valor recebido: " + valor);
        }
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;

        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int resto = 11 - (soma % 11);
        int digito1 = (resto >= 10) ? 0 : resto;
        if (digito1 != (cpf.charAt(9) - '0')) return false;

        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        resto = 11 - (soma % 11);
        int digito2 = (resto >= 10) ? 0 : resto;
        return digito2 == (cpf.charAt(10) - '0');
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;

        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos1[i];
        }
        int resto = soma % 11;
        int digito1 = (resto < 2) ? 0 : 11 - resto;
        if (digito1 != (cnpj.charAt(12) - '0')) return false;

        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos2[i];
        }
        resto = soma % 11;
        int digito2 = (resto < 2) ? 0 : 11 - resto;
        return digito2 == (cnpj.charAt(13) - '0');
    }

    public String getNumero() {
        return numero;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getFormatado() {
        if (tipo == TipoDocumento.CPF) {
            return numero.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }
        return numero.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }
}