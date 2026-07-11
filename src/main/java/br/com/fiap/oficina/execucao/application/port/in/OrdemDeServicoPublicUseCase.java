package br.com.fiap.oficina.execucao.application.port.in;

public interface OrdemDeServicoPublicUseCase {

    OrdemDeServicoDTO.StatusPublicoResponse buscarStatusPublico(String numero);

    OrdemDeServicoDTO.StatusPublicoResponse aprovar(String numero);

    OrdemDeServicoDTO.StatusPublicoResponse recusar(String numero);
}
