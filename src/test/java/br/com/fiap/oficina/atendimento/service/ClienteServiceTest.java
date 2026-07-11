package br.com.fiap.oficina.atendimento.service;

import br.com.fiap.oficina.atendimento.application.port.in.ClienteDTO;
import br.com.fiap.oficina.atendimento.application.port.out.ClienteRepositoryPort;
import br.com.fiap.oficina.atendimento.application.service.ClienteService;
import br.com.fiap.oficina.atendimento.domain.model.Cliente;
import br.com.fiap.oficina.atendimento.domain.valueobject.Documento;
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
class ClienteServiceTest {

    // CPF válido para testes: 529.982.247-25
    private static final String CPF_VALIDO = "529.982.247-25";
    private static final String CPF_INVALIDO = "111.111.111-11";
    // CNPJ válido para testes: 11.222.333/0001-81
    private static final String CNPJ_VALIDO = "11.222.333/0001-81";

    @Mock
    private ClienteRepositoryPort repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void cadastrar_comCpfValido_deveSalvarERetornarResponse() {
        when(repository.existePorDocumentoNumero("52998224725")).thenReturn(false);
        when(repository.salvar(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return Cliente.builder()
                    .id(UUID.randomUUID())
                    .nome(c.getNome())
                    .email(c.getEmail())
                    .telefone(c.getTelefone())
                    .documento(c.getDocumento())
                    .build();
        });

        var request = new ClienteDTO.CadastrarRequest("João Silva", "joao@email.com", "11999999999", CPF_VALIDO);
        var response = service.cadastrar(request);

        assertThat(response.nome()).isEqualTo("João Silva");
        assertThat(response.tipoDocumento()).isEqualTo("CPF");
        assertThat(response.documento()).isEqualTo("529.982.247-25");
        verify(repository).salvar(any(Cliente.class));
    }

    @Test
    void cadastrar_comDocumentoDuplicado_deveLancarRegraDeNegocioException() {
        when(repository.existePorDocumentoNumero("52998224725")).thenReturn(true);

        var request = new ClienteDTO.CadastrarRequest("João Silva", "joao@email.com", "11999999999", CPF_VALIDO);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um cliente");
    }

    @Test
    void cadastrar_comCpfInvalido_deveLancarRegraDeNegocioException() {
        var request = new ClienteDTO.CadastrarRequest("João Silva", "joao@email.com", "11999999999", CPF_INVALIDO);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CPF inválido");
    }

    @Test
    void cadastrar_comCnpjValido_deveSalvarComTipoCnpj() {
        when(repository.existePorDocumentoNumero("11222333000181")).thenReturn(false);
        when(repository.salvar(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return Cliente.builder()
                    .id(UUID.randomUUID())
                    .nome(c.getNome())
                    .email(c.getEmail())
                    .telefone(c.getTelefone())
                    .documento(c.getDocumento())
                    .build();
        });

        var request = new ClienteDTO.CadastrarRequest("Empresa XYZ", "empresa@email.com", "1133334444", CNPJ_VALIDO);
        var response = service.cadastrar(request);

        assertThat(response.tipoDocumento()).isEqualTo("CNPJ");
    }

    @Test
    void buscarPorId_clienteExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(criarClienteMock(id)));

        var response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.nome()).isEqualTo("João Silva");
    }

    @Test
    void buscarPorId_clienteInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorDocumento_clienteExistente_deveRetornarResponse() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorDocumentoNumero("52998224725"))
                .thenReturn(Optional.of(criarClienteMock(id)));

        var response = service.buscarPorDocumento(CPF_VALIDO);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void buscarPorDocumento_clienteInexistente_deveLancarRecursoNaoEncontradoException() {
        when(repository.buscarPorDocumentoNumero("52998224725")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorDocumento(CPF_VALIDO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listar_deveRetornarListaComTodosClientes() {
        when(repository.listarTodos()).thenReturn(List.of(
                criarClienteMock(UUID.randomUUID()),
                criarClienteMock(UUID.randomUUID())
        ));

        var result = service.listar();

        assertThat(result).hasSize(2);
    }

    @Test
    void atualizar_clienteExistente_deveAtualizarDados() {
        UUID id = UUID.randomUUID();
        Cliente cliente = criarClienteMock(id);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));
        when(repository.salvar(any())).thenReturn(cliente);

        var request = new ClienteDTO.AtualizarRequest("Novo Nome", "novo@email.com", "11888888888");
        service.atualizar(id, request);

        assertThat(cliente.getNome()).isEqualTo("Novo Nome");
        assertThat(cliente.getEmail()).isEqualTo("novo@email.com");
        verify(repository).salvar(cliente);
    }

    @Test
    void excluir_clienteExistente_deveChamarDelete() {
        UUID id = UUID.randomUUID();
        Cliente cliente = criarClienteMock(id);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));

        service.excluir(id);

        verify(repository).deletar(cliente);
    }

    @Test
    void excluir_clienteInexistente_deveLancarRecursoNaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Cliente criarClienteMock(UUID id) {
        return Cliente.builder()
                .id(id)
                .nome("João Silva")
                .email("joao@email.com")
                .telefone("11999999999")
                .documento(Documento.of(CPF_VALIDO))
                .build();
    }
}
