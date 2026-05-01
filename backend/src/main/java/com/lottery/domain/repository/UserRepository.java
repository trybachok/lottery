package com.lottery.domain.repository;

import com.lottery.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);
}
