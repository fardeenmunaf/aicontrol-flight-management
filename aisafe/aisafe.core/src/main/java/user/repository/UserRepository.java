package user.repository;

import eapli.framework.domain.repositories.DomainRepository;
import user.domain.User;

import java.util.Optional;

public interface UserRepository extends DomainRepository<Long, User> {

    Optional<User> findByEmail(String email);
}