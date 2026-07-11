package br.com.fiap.oficina.execucao.adapter.out.persistence;

import br.com.fiap.oficina.execucao.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrdemDeServicoRepositoryAdapter implements OrdemDeServicoRepositoryPort {

    private final OrdemDeServicoJpaRepository jpaRepository;

    @Override
    public OrdemDeServico salvar(OrdemDeServico ordemDeServico) {
        return jpaRepository.save(ordemDeServico);
    }

    @Override
    public OrdemDeServico buscarPorId(UUID id) {
        OrdemDeServico ordemDeServico = jpaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com id: " + id));
        inicializarColecoes(ordemDeServico);
        return ordemDeServico;
    }

    @Override
    public OrdemDeServico buscarPorNumero(String numero) {
        OrdemDeServico ordemDeServico = jpaRepository.findByNumero(numero)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com número: " + numero));
        inicializarColecoes(ordemDeServico);
        return ordemDeServico;
    }

    @Override
    public List<OrdemDeServico> listarAtivasOrdenadas() {
        return jpaRepository.findAllAtivasOrdenadas();
    }

    @Override
    public List<OrdemDeServico> listarTodas() {
        return jpaRepository.findAll();
    }

    @Override
    public int buscarProximoSequencial(String prefixo) {
        return jpaRepository.findMaxSequencialByPrefixo(prefixo);
    }

    @Override
    public List<OrdemDeServico> buscarFinalizadasComDatas(List<StatusOS> statuses) {
        return jpaRepository.findFinalizadasComDatas(statuses);
    }

    private void inicializarColecoes(OrdemDeServico ordemDeServico) {
        ordemDeServico.getItensServico().size();
        ordemDeServico.getItensPeca().size();
    }
}
