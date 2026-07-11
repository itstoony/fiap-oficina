package br.com.fiap.oficina.estoque.application.service;

import br.com.fiap.oficina.estoque.application.port.in.PecaDTO;
import br.com.fiap.oficina.estoque.application.port.in.PecaUseCase;
import br.com.fiap.oficina.estoque.application.port.out.MovimentacaoEstoqueRepositoryPort;
import br.com.fiap.oficina.estoque.application.port.out.PecaRepositoryPort;
import br.com.fiap.oficina.estoque.domain.model.MovimentacaoEstoque;
import br.com.fiap.oficina.estoque.domain.model.Peca;
import br.com.fiap.oficina.estoque.domain.valueobject.TipoMovimentacao;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PecaService implements PecaUseCase {

    private final PecaRepositoryPort pecaRepository;
    private final MovimentacaoEstoqueRepositoryPort movimentacaoRepository;

    @Transactional
    public PecaDTO.Response cadastrar(PecaDTO.CadastrarRequest request) {
        if (pecaRepository.existePorCodigo(request.codigo())) {
            throw new RegraDeNegocioException("Já existe uma peça cadastrada com o código: " + request.codigo());
        }

        Peca peca = Peca.builder()
                .nome(request.nome())
                .codigo(request.codigo().toUpperCase())
                .precoUnitario(request.precoUnitario())
                .qtdEstoque(request.qtdEstoque())
                .qtdReservada(0)
                .qtdMinima(request.qtdMinima())
                .build();

        peca = pecaRepository.salvar(peca);

        if (request.qtdEstoque() > 0) {
            registrarMovimentacao(peca, TipoMovimentacao.ENTRADA, request.qtdEstoque(), null, "Estoque inicial");
        }

        return toResponse(peca);
    }

    @Transactional(readOnly = true)
    public List<PecaDTO.Response> listar() {
        return pecaRepository.listarTodos().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PecaDTO.Response buscarPorId(UUID id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Peca buscarEntidade(UUID id) {
        return pecaRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada com id: " + id));
    }

    @Transactional
    public PecaDTO.Response atualizar(UUID id, PecaDTO.AtualizarRequest request) {
        Peca peca = buscarEntidade(id);
        peca.setNome(request.nome());
        peca.setPrecoUnitario(request.precoUnitario());
        peca.setQtdMinima(request.qtdMinima());
        return toResponse(pecaRepository.salvar(peca));
    }

    @Transactional
    public void excluir(UUID id) {
        Peca peca = buscarEntidade(id);
        if (peca.getQtdReservada() > 0) {
            throw new RegraDeNegocioException(
                    "Não é possível excluir a peça '" + peca.getNome() + "' pois há " +
                    peca.getQtdReservada() + " unidade(s) reservada(s) em Ordens de Serviço abertas.");
        }
        pecaRepository.deletar(peca);
    }

    @Transactional
    public PecaDTO.Response registrarEntrada(UUID id, PecaDTO.EntradaRequest request) {
        Peca peca = buscarEntidade(id);
        peca.setQtdEstoque(peca.getQtdEstoque() + request.quantidade());
        peca = pecaRepository.salvar(peca);
        registrarMovimentacao(peca, TipoMovimentacao.ENTRADA, request.quantidade(), null, request.observacao());
        return toResponse(peca);
    }

    @Transactional(readOnly = true)
    public List<PecaDTO.Response> buscarCriticas() {
        return pecaRepository.buscarCriticas().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PecaDTO.MovimentacaoResponse> buscarMovimentacoes(UUID id) {
        buscarEntidade(id);
        return movimentacaoRepository.buscarPorPecaIdOrdenado(id)
                .stream()
                .map(m -> new PecaDTO.MovimentacaoResponse(
                        m.getId(),
                        m.getTipo(),
                        m.getQuantidade(),
                        m.getOsId(),
                        m.getObservacao(),
                        m.getDataMovimentacao()
                ))
                .toList();
    }

    private void registrarMovimentacao(Peca peca, TipoMovimentacao tipo, Integer quantidade, UUID osId, String observacao) {
        movimentacaoRepository.salvar(MovimentacaoEstoque.builder()
                .peca(peca)
                .tipo(tipo)
                .quantidade(quantidade)
                .osId(osId)
                .observacao(observacao)
                .build());
    }

    private PecaDTO.Response toResponse(Peca peca) {
        return new PecaDTO.Response(
                peca.getId(),
                peca.getNome(),
                peca.getCodigo(),
                peca.getPrecoUnitario(),
                peca.getQtdEstoque(),
                peca.getQtdReservada(),
                peca.getQtdDisponivel(),
                peca.getQtdMinima(),
                peca.getQtdEstoque() <= peca.getQtdMinima(),
                peca.getCriadoEm(),
                peca.getAtualizadoEm()
        );
    }
}
