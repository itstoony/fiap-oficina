package br.com.fiap.oficina.administracao.service;

import br.com.fiap.oficina.administracao.application.port.in.ServicoDTO;
import br.com.fiap.oficina.administracao.application.port.out.ServicoRepositoryPort;
import br.com.fiap.oficina.administracao.application.service.ServicoService;
import br.com.fiap.oficina.administracao.domain.model.Servico;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepositoryPort servicoRepository;

    @InjectMocks
    private ServicoService service;

    @Test
    void cadastrar_comDadosValidos_deveSalvarERetornarResponse() {
        when(servicoRepository.existePorNome("Troca de Óleo")).thenReturn(false);
        when(servicoRepository.salvar(any(Servico.class))).thenAnswer(inv -> {
            Servico s = inv.getArgument(0);
            return Servico.builder().id(UUID.randomUUID()).nome(s.getNome())
                    .descricao(s.getDescricao()).precoBase(s.getPrecoBase())
                    .criadoEm(LocalDateTime.now()).atualizadoEm(LocalDateTime.now()).build();
        });

        var request = new ServicoDTO.CadastrarRequest("Troca de Óleo", "Troca de óleo e filtro", new BigDecimal("120.00"));
        var response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Troca de Óleo");
        assertThat(response.precoBase()).isEqualByComparingTo("120.00");
        verify(servicoRepository).salvar(any(Servico.class));
    }

    @Test
    void cadastrar_comNomeDuplicado_deveLancarRegraDeNegocioException() {
        when(servicoRepository.existePorNome("Troca de Óleo")).thenReturn(true);

        var request = new ServicoDTO.CadastrarRequest("Troca de Óleo", "Desc", new BigDecimal("120.00"));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Troca de Óleo");
    }

    @Test
    void listar_deveRetornarTodosOsServicos() {
        when(servicoRepository.listarTodos()).thenReturn(List.of(
                criarServicoMock(UUID.randomUUID(), "Troca de Óleo"),
                criarServicoMock(UUID.randomUUID(), "Alinhamento")
        ));

        var result = service.listar();

        assertThat(result).hasSize(2);
    }

    @Test
    void buscarPorId_servicoExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.of(criarServicoMock(id, "Troca de Óleo")));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("Troca de Óleo");
    }

    @Test
    void buscarPorId_servicoInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_comMesmoNome_deveAtualizarSemVerificarDuplicata() {
        UUID id = UUID.randomUUID();
        Servico servico = criarServicoMock(id, "Troca de Óleo");
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.of(servico));
        when(servicoRepository.salvar(servico)).thenReturn(servico);

        var request = new ServicoDTO.AtualizarRequest("Troca de Óleo", "Nova descrição", new BigDecimal("150.00"));
        service.atualizar(id, request);

        assertThat(servico.getDescricao()).isEqualTo("Nova descrição");
        assertThat(servico.getPrecoBase()).isEqualByComparingTo("150.00");
        verify(servicoRepository, never()).existePorNome(any());
    }

    @Test
    void atualizar_comNomeDiferenteDuplicado_deveLancarRegraDeNegocioException() {
        UUID id = UUID.randomUUID();
        Servico servico = criarServicoMock(id, "Troca de Óleo");
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.of(servico));
        when(servicoRepository.existePorNome("Alinhamento")).thenReturn(true);

        var request = new ServicoDTO.AtualizarRequest("Alinhamento", "Desc", new BigDecimal("80.00"));

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Alinhamento");
    }

    @Test
    void excluir_servicoExistente_deveChamarDelete() {
        UUID id = UUID.randomUUID();
        Servico servico = criarServicoMock(id, "Troca de Óleo");
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.of(servico));

        service.excluir(id);

        verify(servicoRepository).deletar(servico);
    }

    @Test
    void excluir_servicoInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(servicoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Servico criarServicoMock(UUID id, String nome) {
        return Servico.builder()
                .id(id)
                .nome(nome)
                .descricao("Descrição do serviço")
                .precoBase(new BigDecimal("120.00"))
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
