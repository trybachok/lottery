package com.lottery.domain.repository;

import com.lottery.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    User update(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmailOrLogin(String loginOrEmail);

    List<User> findAll(int limit, int offset);

    boolean existsByEmail(String email);

    boolean existsByLogin(String login);
}
