package br.com.fiap.oficina.seguranca.adapter.out.persistence;

import br.com.fiap.oficina.seguranca.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByLogin(String login);
}
