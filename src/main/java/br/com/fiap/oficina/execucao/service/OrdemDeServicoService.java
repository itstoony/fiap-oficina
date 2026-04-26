package br.com.fiap.oficina.execucao.service;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.model.Veiculo;
import br.com.fiap.oficina.atendimento.repository.AtendenteRepository;
import br.com.fiap.oficina.atendimento.repository.VeiculoRepository;
import br.com.fiap.oficina.atendimento.service.ClienteService;
import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.administracao.repository.ServicoRepository;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.repository.PecaRepository;
import br.com.fiap.oficina.estoque.service.EstoqueService;
import br.com.fiap.oficina.execucao.domain.model.ItemPeca;
import br.com.fiap.oficina.execucao.domain.model.ItemServico;
import br.com.fiap.oficina.execucao.domain.model.OrdemDeServico;
import br.com.fiap.oficina.execucao.domain.valueobject.StatusOS;
import br.com.fiap.oficina.execucao.repository.OrdemDeServicoRepository;
import br.com.fiap.oficina.execucao.service.dto.ItemPecaDTO;
import br.com.fiap.oficina.execucao.service.dto.ItemServicoDTO;
import br.com.fiap.oficina.execucao.service.dto.OrdemDeServicoDTO;
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
public class OrdemDeServicoService {

    private final OrdemDeServicoRepository repository;
    private final ClienteService clienteService;
    private final VeiculoRepository veiculoRepository;
    private final AtendenteRepository atendenteRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final EstoqueService estoqueService;

    // -------------------------------------------------------------------------
    // CONSULTAS
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrdemDeServicoDTO.Response> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdemDeServicoDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarComItens(id));
    }

    @Transactional(readOnly = true)
    public OrdemDeServicoDTO.StatusPublicoResponse buscarStatusPublico(String numero) {
        OrdemDeServico os = repository.findByNumero(numero)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com número: " + numero));
        return new OrdemDeServicoDTO.StatusPublicoResponse(
                os.getNumero(),
                os.getStatus().name(),
                os.getValorTotal(),
                os.getDataAbertura(),
                os.getDataInicioExecucao(),
                os.getDataFimExecucao()
        );
    }

    // -------------------------------------------------------------------------
    // CRIAÇÃO
    // -------------------------------------------------------------------------

    @Transactional
    public OrdemDeServicoDTO.Response criar(OrdemDeServicoDTO.CriarRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado com id: " + request.veiculoId()));

        Atendente atendente = null;
        if (request.atendenteId() != null) {
            atendente = atendenteRepository.findById(request.atendenteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Atendente não encontrado com id: " + request.atendenteId()));
        }

        OrdemDeServico os = OrdemDeServico.builder()
                .numero(gerarNumero())
                .status(StatusOS.RECEBIDA)
                .cliente(cliente)
                .veiculo(veiculo)
                .atendente(atendente)
                .observacoes(request.observacoes())
                .build();

        return toResponse(repository.save(os));
    }

    // -------------------------------------------------------------------------
    // ITENS DE SERVIÇO
    // -------------------------------------------------------------------------

    @Transactional
    public OrdemDeServicoDTO.Response adicionarServico(UUID osId, ItemServicoDTO.AdicionarRequest request) {
        OrdemDeServico os = buscarComItens(osId);
        os.validarEdicaoPermitida();

        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Serviço não encontrado com id: " + request.servicoId()));

        ItemServico item = ItemServico.builder()
                .ordemDeServico(os)
                .servico(servico)
                .quantidade(request.quantidade())
                .precoUnitario(servico.getPrecoBase())
                .observacao(request.observacao())
                .build();

        os.getItensServico().add(item);
        os.recalcularValorTotal();
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response removerServico(UUID osId, UUID itemId) {
        OrdemDeServico os = buscarComItens(osId);
        os.validarEdicaoPermitida();

        boolean removido = os.getItensServico().removeIf(i -> i.getId().equals(itemId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Item de serviço não encontrado com id: " + itemId);
        }

        os.recalcularValorTotal();
        return toResponse(repository.save(os));
    }

    // -------------------------------------------------------------------------
    // ITENS DE PEÇA
    // -------------------------------------------------------------------------

    @Transactional
    public OrdemDeServicoDTO.Response adicionarPeca(UUID osId, ItemPecaDTO.AdicionarRequest request) {
        OrdemDeServico os = buscarComItens(osId);
        os.validarEdicaoPermitida();

        Peca peca = pecaRepository.findById(request.pecaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Peça não encontrada com id: " + request.pecaId()));

        estoqueService.verificarDisponibilidadeEReservar(peca.getId(), request.quantidade(), osId);

        ItemPeca item = ItemPeca.builder()
                .ordemDeServico(os)
                .peca(peca)
                .quantidade(request.quantidade())
                .precoUnitario(peca.getPrecoUnitario())
                .build();

        os.getItensPeca().add(item);
        os.recalcularValorTotal();
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response removerPeca(UUID osId, UUID itemId) {
        OrdemDeServico os = buscarComItens(osId);
        os.validarEdicaoPermitida();

        ItemPeca item = os.getItensPeca().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de peça não encontrado com id: " + itemId));

        estoqueService.liberarReserva(item.getPeca().getId(), item.getQuantidade(), osId);
        os.getItensPeca().remove(item);
        os.recalcularValorTotal();
        return toResponse(repository.save(os));
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS (ADMIN)
    // -------------------------------------------------------------------------

    @Transactional
    public OrdemDeServicoDTO.Response iniciarDiagnostico(UUID id) {
        OrdemDeServico os = buscarComItens(id);
        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response enviarOrcamento(UUID id) {
        OrdemDeServico os = buscarComItens(id);
        os.transicionarPara(StatusOS.AGUARDANDO_APROVACAO);
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response iniciarExecucao(UUID id, OrdemDeServicoDTO.IniciarExecucaoRequest request) {
        OrdemDeServico os = buscarComItens(id);
        Atendente atendente = atendenteRepository.findById(request.atendenteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendente não encontrado com id: " + request.atendenteId()));
        os.setAtendente(atendente);
        os.transicionarPara(StatusOS.EM_EXECUCAO);
        os.getItensPeca().forEach(item ->
                estoqueService.baixarEstoque(osId(os), item.getPeca().getId(), item.getQuantidade()));
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response finalizar(UUID id) {
        OrdemDeServico os = buscarComItens(id);
        os.transicionarPara(StatusOS.FINALIZADA);
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response entregar(UUID id) {
        OrdemDeServico os = buscarComItens(id);
        os.transicionarPara(StatusOS.ENTREGUE);
        return toResponse(repository.save(os));
    }

    @Transactional
    public OrdemDeServicoDTO.Response cancelar(UUID id) {
        OrdemDeServico os = buscarComItens(id);
        os.transicionarPara(StatusOS.CANCELADA);
        os.getItensPeca().forEach(item ->
                estoqueService.liberarReserva(item.getPeca().getId(), item.getQuantidade(), osId(os)));
        return toResponse(repository.save(os));
    }

    // -------------------------------------------------------------------------
    // TRANSIÇÕES DE STATUS (PÚBLICO)
    // -------------------------------------------------------------------------

    @Transactional
    public OrdemDeServicoDTO.StatusPublicoResponse aprovar(String numero) {
        OrdemDeServico os = buscarPorNumeroComItens(numero);
        os.transicionarPara(StatusOS.APROVADO);
        repository.save(os);
        return new OrdemDeServicoDTO.StatusPublicoResponse(
                os.getNumero(), os.getStatus().name(), os.getValorTotal(),
                os.getDataAbertura(), os.getDataInicioExecucao(), os.getDataFimExecucao());
    }

    @Transactional
    public OrdemDeServicoDTO.StatusPublicoResponse recusar(String numero) {
        OrdemDeServico os = buscarPorNumeroComItens(numero);
        os.transicionarPara(StatusOS.CANCELADA);
        os.getItensPeca().forEach(item ->
                estoqueService.liberarReserva(item.getPeca().getId(), item.getQuantidade(), osId(os)));
        repository.save(os);
        return new OrdemDeServicoDTO.StatusPublicoResponse(
                os.getNumero(), os.getStatus().name(), os.getValorTotal(),
                os.getDataAbertura(), os.getDataInicioExecucao(), os.getDataFimExecucao());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private String gerarNumero() {
        int ano = LocalDateTime.now().getYear();
        String prefixo = "OS-" + ano + "-%";
        int proximo = repository.findMaxSequencialByPrefixo(prefixo) + 1;
        return String.format("OS-%d-%05d", ano, proximo);
    }

    private OrdemDeServico buscarComItens(UUID id) {
        OrdemDeServico os = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com id: " + id));
        os.getItensServico().size();
        os.getItensPeca().size();
        return os;
    }

    private OrdemDeServico buscarPorNumeroComItens(String numero) {
        OrdemDeServico os = repository.findByNumero(numero)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com número: " + numero));
        os.getItensServico().size();
        os.getItensPeca().size();
        return os;
    }

    private UUID osId(OrdemDeServico os) {
        return os.getId();
    }

    private OrdemDeServicoDTO.Response toResponse(OrdemDeServico os) {
        List<ItemServicoDTO.Response> itensServico = os.getItensServico().stream()
                .map(i -> new ItemServicoDTO.Response(
                        i.getId(),
                        i.getServico().getId(),
                        i.getServico().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())),
                        i.getObservacao()))
                .toList();

        List<ItemPecaDTO.Response> itensPeca = os.getItensPeca().stream()
                .map(i -> new ItemPecaDTO.Response(
                        i.getId(),
                        i.getPeca().getId(),
                        i.getPeca().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnitario(),
                        i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade()))))
                .toList();

        return new OrdemDeServicoDTO.Response(
                os.getId(),
                os.getNumero(),
                os.getStatus().name(),
                os.getCliente().getId(),
                os.getCliente().getNome(),
                os.getVeiculo().getId(),
                os.getVeiculo().getPlaca().getValor(),
                os.getAtendente() != null ? os.getAtendente().getId() : null,
                os.getAtendente() != null ? os.getAtendente().getNome() : null,
                os.getValorTotal(),
                os.getObservacoes(),
                itensServico,
                itensPeca,
                os.getDataAbertura(),
                os.getDataInicioExecucao(),
                os.getDataFimExecucao(),
                os.getCriadoEm(),
                os.getAtualizadoEm()
        );
    }
}
