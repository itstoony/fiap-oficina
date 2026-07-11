package br.com.fiap.oficina.estoque.application.port.in;

import br.com.fiap.oficina.estoque.domain.model.Peca;

import java.util.List;
import java.util.UUID;

public interface PecaUseCase {
    PecaDTO.Response cadastrar(PecaDTO.CadastrarRequest request);
    List<PecaDTO.Response> listar();
    PecaDTO.Response buscarPorId(UUID id);
    Peca buscarEntidade(UUID id);
    PecaDTO.Response atualizar(UUID id, PecaDTO.AtualizarRequest request);
    void excluir(UUID id);
    PecaDTO.Response registrarEntrada(UUID id, PecaDTO.EntradaRequest request);
    List<PecaDTO.Response> buscarCriticas();
    List<PecaDTO.MovimentacaoResponse> buscarMovimentacoes(UUID id);
}
