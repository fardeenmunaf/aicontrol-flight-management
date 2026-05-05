package user.repository;

import eapli.framework.domain.repositories.DomainRepository;
import user.domain.User;
import shared.domain.UserId;

import java.util.Optional;

public interface UserRepository extends DomainRepository<UserId, User> {

    Optional<User> findByEmail(String email);
}