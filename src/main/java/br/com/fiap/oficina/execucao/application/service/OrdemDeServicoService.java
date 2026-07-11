package br.com.fiap.oficina.execucao.application.service;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.execucao.application.port.in.ItemPecaDTO;
import br.com.fiap.oficina.execucao.application.port.in.ItemServicoDTO;
import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoAdminUseCase;
import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoDTO;
import br.com.fiap.oficina.execucao.application.port.in.OrdemDeServicoPublicUseCase;
import br.com.fiap.oficina.execucao.application.port.out.AtendenteLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.ClienteLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.EstoquePort;
import br.com.fiap.oficina.execucao.application.port.out.NotificacaoEmailPort;
import br.com.fiap.oficina.execucao.application.port.out.OrdemDeServicoRepositoryPort;
import br.com.fiap.oficina.execucao.application.port.out.PecaLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.ServicoLookupPort;
import br.com.fiap.oficina.execucao.application.port.out.VeiculoLookupPort;
import br.com.fiap.oficina.execucao.domain.model.ItemPeca;
import br.com.fiap.oficina.execucao.domain.model.ItemServico;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdemDeServicoService implements OrdemDeServicoAdminUseCase, OrdemDeServicoPublicUseCase {

    private final OrdemDeServicoRepositoryPort repositorio;
    private final ClienteLookupPort clienteLookup;
    private final VeiculoLookupPort veiculoLookup;
    private final AtendenteLookupPort atendenteLookup;
    private final ServicoLookupPort servicoLookup;
    private final PecaLookupPort pecaLookup;
    private final EstoquePort estoque;
    private final NotificacaoEmailPort notificacaoEmail;

    // -------------------------------------------------------------------------
    // CONSULTAS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<OrdemDeServicoDTO.Response> listar() {
        return repositorio.listarAtivasOrdenadas().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemDeServicoDTO.Response buscarPorId(UUID id) {
        return toResponse(repositorio.buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemDeServicoDTO.StatusPublicoResponse buscarStatusPublico(String numero) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorNumero(numero);
        return toStatusPublicoResponse(ordemDeServico);
    }

    // -------------------------------------------------------------------------
    // CRIAÇÃO
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response criar(OrdemDeServicoDTO.CriarRequest request) {
        Cliente cliente = clienteLookup.buscarPorId(request.clienteId());
        Veiculo veiculo = veiculoLookup.buscarPorId(request.veiculoId());

        Atendente atendente = null;
        if (request.atendenteId() != null) {
            atendente = atendenteLookup.buscarPorId(request.atendenteId());
        }

        OrdemDeServico ordemDeServico = OrdemDeServico.builder()
                .numero(gerarNumero())
                .status(StatusOS.RECEBIDA)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendente(atendente)
                .observacoes(request.observacoes())
                .build();

        return toResponse(repositorio.salvar(ordemDeServico));
    }

    // -------------------------------------------------------------------------
    // ITENS DE SERVIÇO
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response adicionarServico(UUID osId, ItemServicoDTO.AdicionarRequest request) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(osId);
        ordemDeServico.validarEdicaoPermitida();

        Servico servico = servicoLookup.buscarPorId(request.servicoId());

        ItemServico itemServico = ItemServico.builder()
                .ordemDeServico(ordemDeServico)
                .servico(servico)
                .quantidade(request.quantidade())
                .precoUnitario(servico.getPrecoBase())
                .observacao(request.observacao())
                .build();

        ordemDeServico.getItensServico().add(itemServico);
        ordemDeServico.recalcularValorTotal();
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response removerServico(UUID osId, UUID itemId) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(osId);
        ordemDeServico.validarEdicaoPermitida();

        boolean removido = ordemDeServico.getItensServico().removeIf(item -> item.getId().equals(itemId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Item de serviço não encontrado com id: " + itemId);
        }

        ordemDeServico.recalcularValorTotal();
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    // -------------------------------------------------------------------------
    // ITENS DE PEÇA
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response adicionarPeca(UUID osId, ItemPecaDTO.AdicionarRequest request) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(osId);
        ordemDeServico.validarEdicaoPermitida();

        Peca peca = pecaLookup.buscarPorId(request.pecaId());

        estoque.verificarDisponibilidadeEReservar(peca.getId(), request.quantidade(), osId);

        ItemPeca itemPeca = ItemPeca.builder()
                .ordemDeServico(ordemDeServico)
                .peca(peca)
                .quantidade(request.quantidade())
                .precoUnitario(peca.getPrecoUnitario())
                .build();

        ordemDeServico.getItensPeca().add(itemPeca);
        ordemDeServico.recalcularValorTotal();
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response removerPeca(UUID osId, UUID itemId) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(osId);
        ordemDeServico.validarEdicaoPermitida();

        ItemPeca itemPeca = ordemDeServico.getItensPeca().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de peça não encontrado com id: " + itemId));

        estoque.liberarReserva(itemPeca.getPeca().getId(), itemPeca.getQuantidade(), osId);
        ordemDeServico.getItensPeca().remove(itemPeca);
        ordemDeServico.recalcularValorTotal();
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS (ADMIN)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response iniciarDiagnostico(UUID id) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        ordemDeServico.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response enviarOrcamento(UUID id) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        ordemDeServico.transicionarPara(StatusOS.AGUARDANDO_APROVACAO);
        OrdemDeServico salva = repositorio.salvar(ordemDeServico);
        notificacaoEmail.enviarOrcamentoParaAprovacao(
                ordemDeServico.getCliente().getEmail(),
                ordemDeServico.getNumero(),
                ordemDeServico.getValorTotal()
        );
        return toResponse(salva);
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response iniciarExecucao(UUID id, OrdemDeServicoDTO.IniciarExecucaoRequest request) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        Atendente atendente = atendenteLookup.buscarPorId(request.atendenteId());
        ordemDeServico.setAtendente(atendente);
        ordemDeServico.transicionarPara(StatusOS.EM_EXECUCAO);
        ordemDeServico.getItensPeca().forEach(item ->
                estoque.baixarEstoque(ordemDeServico.getId(), item.getPeca().getId(), item.getQuantidade()));
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response finalizar(UUID id) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        ordemDeServico.transicionarPara(StatusOS.FINALIZADA);
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response entregar(UUID id) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        ordemDeServico.transicionarPara(StatusOS.ENTREGUE);
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.Response cancelar(UUID id) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorId(id);
        ordemDeServico.transicionarPara(StatusOS.CANCELADA);
        ordemDeServico.getItensPeca().forEach(item ->
                estoque.liberarReserva(item.getPeca().getId(), item.getQuantidade(), ordemDeServico.getId()));
        return toResponse(repositorio.salvar(ordemDeServico));
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS (PÚBLICO)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrdemDeServicoDTO.StatusPublicoResponse aprovar(String numero) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorNumero(numero);
        ordemDeServico.transicionarPara(StatusOS.APROVADO);
        repositorio.salvar(ordemDeServico);
        return toStatusPublicoResponse(ordemDeServico);
    }

    @Override
    @Transactional
    public OrdemDeServicoDTO.StatusPublicoResponse recusar(String numero) {
        OrdemDeServico ordemDeServico = repositorio.buscarPorNumero(numero);
        ordemDeServico.transicionarPara(StatusOS.CANCELADA);
        ordemDeServico.getItensPeca().forEach(item ->
                estoque.liberarReserva(item.getPeca().getId(), item.getQuantidade(), ordemDeServico.getId()));
        repositorio.salvar(ordemDeServico);
        return toStatusPublicoResponse(ordemDeServico);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String gerarNumero() {
        int ano = LocalDateTime.now().getYear();
        String prefixo = "OS-" + ano + "-%";
        int proximo = repositorio.buscarProximoSequencial(prefixo) + 1;
        return String.format("OS-%d-%05d", ano, proximo);
    }

    private OrdemDeServicoDTO.StatusPublicoResponse toStatusPublicoResponse(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoDTO.StatusPublicoResponse(
                ordemDeServico.getNumero(),
                ordemDeServico.getStatus().name(),
                ordemDeServico.getValorTotal(),
                ordemDeServico.getDataAbertura(),
                ordemDeServico.getDataInicioExecucao(),
                ordemDeServico.getDataFimExecucao()
        );
    }

    private OrdemDeServicoDTO.Response toResponse(OrdemDeServico ordemDeServico) {
        List<ItemServicoDTO.Response> itensServico = ordemDeServico.getItensServico().stream()
                .map(item -> new ItemServicoDTO.Response(
                        item.getId(),
                        item.getServico().getId(),
                        item.getServico().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())),
                        item.getObservacao()))
                .toList();

        List<ItemPecaDTO.Response> itensPeca = ordemDeServico.getItensPeca().stream()
                .map(item -> new ItemPecaDTO.Response(
                        item.getId(),
                        item.getPeca().getId(),
                        item.getPeca().getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario(),
                        item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))))
                .toList();

        return new OrdemDeServicoDTO.Response(
                ordemDeServico.getId(),
                ordemDeServico.getNumero(),
                ordemDeServico.getStatus().name(),
                ordemDeServico.getCliente().getId(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getVeiculo().getId(),
                ordemDeServico.getVeiculo().getPlaca().getValor(),
                ordemDeServico.getAtendente() != null ? ordemDeServico.getAtendente().getId() : null,
                ordemDeServico.getAtendente() != null ? ordemDeServico.getAtendente().getNome() : null,
                ordemDeServico.getValorTotal(),
                ordemDeServico.getObservacoes(),
                itensServico,
                itensPeca,
                ordemDeServico.getDataAbertura(),
                ordemDeServico.getDataInicioExecucao(),
                ordemDeServico.getDataFimExecucao(),
                ordemDeServico.getCriadoEm(),
                ordemDeServico.getAtualizadoEm()
        );
    }
}
