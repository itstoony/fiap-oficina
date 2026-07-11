package br.com.fiap.oficina.atendimento.service;

import br.com.fiap.oficina.atendimento.application.port.in.AtendenteDTO;
import br.com.fiap.oficina.atendimento.application.port.out.AtendenteRepositoryPort;
import br.com.fiap.oficina.atendimento.application.service.AtendenteService;
import br.com.fiap.oficina.atendimento.domain.model.Atendente;
import br.com.fiap.oficina.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    private AtendenteRepositoryPort atendenteRepository;

    @InjectMocks
    private AtendenteService service;

    @Test
    void cadastrar_comDadosValidos_deveSalvarERetornarResponse() {
        when(atendenteRepository.existePorEmail("carlos@oficina.com")).thenReturn(false);
        when(atendenteRepository.salvar(any(Atendente.class))).thenAnswer(inv -> criarAtendenteMock(UUID.randomUUID()));

        var request = new AtendenteDTO.CadastrarRequest("Carlos Lima", "carlos@oficina.com", "11988887777");
        var response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("Carlos Lima");
        assertThat(response.email()).isEqualTo("carlos@oficina.com");
        verify(atendenteRepository).salvar(any(Atendente.class));
    }

    @Test
    void cadastrar_comEmailDuplicado_deveLancarRegraDeNegocioException() {
        when(atendenteRepository.existePorEmail("carlos@oficina.com")).thenReturn(true);

        var request = new AtendenteDTO.CadastrarRequest("Carlos Lima", "carlos@oficina.com", "11988887777");

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("email");
    }

    @Test
    void listar_deveRetornarTodosOsAtendentes() {
        when(atendenteRepository.listarTodos()).thenReturn(List.of(
                criarAtendenteMock(UUID.randomUUID()),
                criarAtendenteMock(UUID.randomUUID())
        ));

        var result = service.listar();

        assertThat(result).hasSize(2);
    }

    @Test
    void buscarPorId_atendenteExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.of(criarAtendenteMock(id)));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("Carlos Lima");
    }

    @Test
    void buscarPorId_atendenteInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_comEmailDiferenteEDisponivel_deveAtualizarESalvar() {
        UUID id = UUID.randomUUID();
        Atendente atendente = criarAtendenteMock(id);
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.of(atendente));
        when(atendenteRepository.existePorEmail("novo@oficina.com")).thenReturn(false);
        when(atendenteRepository.salvar(any())).thenReturn(atendente);

        var request = new AtendenteDTO.AtualizarRequest("Carlos Atualizado", "novo@oficina.com", "11977776666");
        service.atualizar(id, request);

        assertThat(atendente.getNome()).isEqualTo("Carlos Atualizado");
        assertThat(atendente.getEmail()).isEqualTo("novo@oficina.com");
        verify(atendenteRepository).salvar(atendente);
    }

    @Test
    void atualizar_comMesmoEmail_naoDeveValidarDuplicidade() {
        UUID id = UUID.randomUUID();
        Atendente atendente = criarAtendenteMock(id);
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.of(atendente));
        when(atendenteRepository.salvar(any())).thenReturn(atendente);

        var request = new AtendenteDTO.AtualizarRequest("Carlos Lima", "carlos@oficina.com", "11988887777");
        service.atualizar(id, request);

        verify(atendenteRepository, never()).existePorEmail(any());
        verify(atendenteRepository).salvar(atendente);
    }

    @Test
    void atualizar_comEmailDuplicado_deveLancarRegraDeNegocioException() {
        UUID id = UUID.randomUUID();
        Atendente atendente = criarAtendenteMock(id);
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.of(atendente));
        when(atendenteRepository.existePorEmail("outro@oficina.com")).thenReturn(true);

        var request = new AtendenteDTO.AtualizarRequest("Carlos Lima", "outro@oficina.com", "11988887777");

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("email");
    }

    @Test
    void excluir_atendenteExistente_deveChamarDeletar() {
        UUID id = UUID.randomUUID();
        Atendente atendente = criarAtendenteMock(id);
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.of(atendente));

        service.excluir(id);

        verify(atendenteRepository).deletar(atendente);
    }

    @Test
    void excluir_atendenteInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(atendenteRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Atendente criarAtendenteMock(UUID id) {
        return Atendente.builder()
                .id(id)
                .nome("Carlos Lima")
                .email("carlos@oficina.com")
                .telefone("11988887777")
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }
}
