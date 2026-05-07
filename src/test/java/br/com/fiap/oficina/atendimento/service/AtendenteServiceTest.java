package br.com.fiap.oficina.atendimento.service;

import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.atendimento.repository.AtendenteRepository;
import br.com.fiap.oficina.atendimento.service.dto.AtendenteDTO;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtendenteServiceTest {

    @Mock
    private AtendenteRepository repository;

    @InjectMocks
    private AtendenteService service;

    private Atendente buildAtendente(UUID id) {
        return Atendente.builder()
                .id(id)
                .nome("Carlos Mendes")
                .email("carlos@oficina.com")
                .telefone("(11) 98765-4321")
                .build();
    }

    @Test
    void cadastrar_comEmailNovo_deveSalvarERetornarResponse() {
        UUID id = UUID.randomUUID();
        when(repository.existsByEmail("carlos@oficina.com")).thenReturn(false);
        when(repository.save(any(Atendente.class))).thenReturn(buildAtendente(id));

        var request = new AtendenteDTO.CadastrarRequest("Carlos Mendes", "carlos@oficina.com", "(11) 98765-4321");
        var response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Carlos Mendes");
        assertThat(response.email()).isEqualTo("carlos@oficina.com");
        verify(repository).save(any(Atendente.class));
    }

    @Test
    void cadastrar_comEmailExistente_deveLancarRegraDeNegocioException() {
        when(repository.existsByEmail("carlos@oficina.com")).thenReturn(true);

        var request = new AtendenteDTO.CadastrarRequest("Carlos Mendes", "carlos@oficina.com", "(11) 98765-4321");

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("carlos@oficina.com");
    }

    @Test
    void listar_deveRetornarListaDeAtendentes() {
        UUID id = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of(buildAtendente(id)));

        var result = service.listar();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("carlos@oficina.com");
    }

    @Test
    void buscarPorId_comIdExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(buildAtendente(id)));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("Carlos Mendes");
    }

    @Test
    void buscarPorId_comIdInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void atualizar_comEmailIgual_deveAtualizarSemVerificarDuplicidade() {
        UUID id = UUID.randomUUID();
        Atendente atendente = buildAtendente(id);
        when(repository.findById(id)).thenReturn(Optional.of(atendente));
        when(repository.save(any(Atendente.class))).thenReturn(atendente);

        var request = new AtendenteDTO.AtualizarRequest("Carlos M. Atualizado", "carlos@oficina.com", "(11) 11111-1111");
        var response = service.atualizar(id, request);

        assertThat(response.nome()).isEqualTo("Carlos M. Atualizado");
        verify(repository, never()).existsByEmail(any());
    }

    @Test
    void atualizar_comEmailDiferenteJaExistente_deveLancarRegraDeNegocioException() {
        UUID id = UUID.randomUUID();
        Atendente atendente = buildAtendente(id);
        when(repository.findById(id)).thenReturn(Optional.of(atendente));
        when(repository.existsByEmail("outro@oficina.com")).thenReturn(true);

        var request = new AtendenteDTO.AtualizarRequest("Carlos", "outro@oficina.com", "(11) 98765-4321");

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("outro@oficina.com");
    }

    @Test
    void atualizar_comEmailDiferenteDisponivel_deveAtualizarComSucesso() {
        UUID id = UUID.randomUUID();
        Atendente atendente = buildAtendente(id);
        when(repository.findById(id)).thenReturn(Optional.of(atendente));
        when(repository.existsByEmail("novo@oficina.com")).thenReturn(false);
        when(repository.save(any(Atendente.class))).thenReturn(atendente);

        var request = new AtendenteDTO.AtualizarRequest("Carlos", "novo@oficina.com", "(11) 98765-4321");
        var response = service.atualizar(id, request);

        assertThat(response).isNotNull();
        verify(repository).save(any(Atendente.class));
    }

    @Test
    void excluir_comIdExistente_deveDeletarAtendente() {
        UUID id = UUID.randomUUID();
        Atendente atendente = buildAtendente(id);
        when(repository.findById(id)).thenReturn(Optional.of(atendente));

        service.excluir(id);

        verify(repository).delete(atendente);
    }

    @Test
    void excluir_comIdInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
