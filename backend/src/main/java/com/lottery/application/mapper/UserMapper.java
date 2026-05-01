package com.lottery.application.mapper;

import com.lottery.application.dto.UserDto;
import com.lottery.domain.model.User;

public final class UserMapper {
    public UserDto toDto(User user) {
        return new UserDto(user.id(), user.email(), user.login(), user.status().name(), user.createdAt(), user.version());
    }
}
