package com.lottery.application.usecase.admin;

import com.lottery.application.ConflictException;
import com.lottery.application.NotFoundException;
import com.lottery.application.UseCaseContext;
import com.lottery.application.ValidationException;
import com.lottery.application.audit.AuditService;
import com.lottery.application.dto.PermissionDto;
import com.lottery.application.dto.RoleDto;
import com.lottery.application.dto.UserDto;
import com.lottery.application.mapper.UserMapper;
import com.lottery.application.port.auth.AuthorizationPort;
import com.lottery.application.port.auth.PasswordHasher;
import com.lottery.application.port.transaction.TransactionManager;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import com.lottery.domain.model.User;
import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.service.DomainClock;
import com.lottery.domain.valueobject.DomainIds;
import com.lottery.domain.valueobject.PermissionCodes;
import com.lottery.domain.valueobject.UserStatus;
import java.util.List;
import java.util.UUID;

public final class AdminRbacUseCase {
    private final UserRepository userRepository;
    private final RbacRepository rbacRepository;
    private final AuthorizationPort authorizationPort;
    private final PasswordHasher passwordHasher;
    private final TransactionManager transactionManager;
    private final DomainClock clock;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public AdminRbacUseCase(
            UserRepository userRepository,
            RbacRepository rbacRepository,
            AuthorizationPort authorizationPort,
            PasswordHasher passwordHasher,
            TransactionManager transactionManager,
            DomainClock clock,
            UserMapper userMapper,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.rbacRepository = rbacRepository;
        this.authorizationPort = authorizationPort;
        this.passwordHasher = passwordHasher;
        this.transactionManager = transactionManager;
        this.clock = clock;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    public List<UserDto> listUsers(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_READ);
            return userRepository.findAll(limit, offset).stream().map(userMapper::toDto).toList();
        });
    }

    public UserDto getUser(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_READ);
            return userMapper.toDto(findUser(id));
        });
    }

    public UserDto createUser(String email, String login, String password, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_CREATE);
            ensureUserUnique(email, login, null);
            String hash = password == null || password.isBlank() ? null : passwordHasher.hash(password);
            UserDto dto = userMapper.toDto(userRepository.save(User.create(email, login, hash, clock.now())));
            auditService.record(context, "ADMIN_USER_CREATE", "USER", dto.id());
            return dto;
        });
    }

    public UserDto updateUser(UUID id, String email, String login, String password, String status, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_UPDATE);
            User current = findUser(id);
            ensureUserUnique(email, login, id);
            String hash = password == null || password.isBlank() ? current.passwordHash().orElse(null) : passwordHasher.hash(password);
            User updated = new User(
                    current.id(),
                    email,
                    login,
                    hash,
                    status == null || status.isBlank() ? current.status() : UserStatus.valueOf(status),
                    current.createdAt(),
                    clock.now(),
                    current.deletedAt().orElse(null),
                    current.version());
            UserDto dto = userMapper.toDto(userRepository.update(updated));
            auditService.record(context, "ADMIN_USER_UPDATE", "USER", id);
            return dto;
        });
    }

    public void deleteUser(UUID id, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_DELETE);
            User current = findUser(id);
            userRepository.update(new User(
                    current.id(),
                    current.email(),
                    current.login(),
                    current.passwordHash().orElse(null),
                    UserStatus.DELETED,
                    current.createdAt(),
                    clock.now(),
                    clock.now(),
                    current.version()));
            auditService.record(context, "ADMIN_USER_DELETE", "USER", id);
            return null;
        });
    }

    public List<RoleDto> listRoles(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_READ);
            return rbacRepository.findAllRoles(limit, offset).stream().map(this::roleDto).toList();
        });
    }

    public RoleDto getRole(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_READ);
            return roleDto(findRole(id));
        });
    }

    public RoleDto createRole(String code, String name, String description, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_MANAGE);
            RoleDto dto = roleDto(rbacRepository.saveRole(new Role(DomainIds.newId(), code, name, description, false)));
            auditService.record(context, "ADMIN_ROLE_CREATE", "ROLE", dto.id());
            return dto;
        });
    }

    public RoleDto updateRole(UUID id, String code, String name, String description, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_MANAGE);
            Role current = findRole(id);
            RoleDto dto = roleDto(rbacRepository.updateRole(new Role(id, code, name, description, current.system())));
            auditService.record(context, "ADMIN_ROLE_UPDATE", "ROLE", id);
            return dto;
        });
    }

    public void deleteRole(UUID id, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_MANAGE);
            Role role = findRole(id);
            if (role.system()) {
                throw new ValidationException("System role cannot be deleted");
            }
            rbacRepository.deleteRole(id);
            auditService.record(context, "ADMIN_ROLE_DELETE", "ROLE", id);
            return null;
        });
    }

    public List<PermissionDto> listPermissions(int limit, int offset, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PERMISSION_MANAGE);
            return rbacRepository.findAllPermissions(limit, offset).stream().map(this::permissionDto).toList();
        });
    }

    public PermissionDto getPermission(UUID id, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PERMISSION_MANAGE);
            return permissionDto(findPermission(id));
        });
    }

    public PermissionDto createPermission(String code, String description, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PERMISSION_MANAGE);
            PermissionDto dto = permissionDto(rbacRepository.savePermission(new Permission(DomainIds.newId(), code, description)));
            auditService.record(context, "ADMIN_PERMISSION_CREATE", "PERMISSION", dto.id());
            return dto;
        });
    }

    public PermissionDto updatePermission(UUID id, String code, String description, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PERMISSION_MANAGE);
            findPermission(id);
            PermissionDto dto = permissionDto(rbacRepository.updatePermission(new Permission(id, code, description)));
            auditService.record(context, "ADMIN_PERMISSION_UPDATE", "PERMISSION", id);
            return dto;
        });
    }

    public void deletePermission(UUID id, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.PERMISSION_MANAGE);
            findPermission(id);
            rbacRepository.deletePermission(id);
            auditService.record(context, "ADMIN_PERMISSION_DELETE", "PERMISSION", id);
            return null;
        });
    }

    public List<RoleDto> listUserRoles(UUID userId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_READ);
            findUser(userId);
            return rbacRepository.findRolesByUserId(userId).stream().map(this::roleDto).toList();
        });
    }

    public void assignUserRole(UUID userId, UUID roleId, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_UPDATE);
            findUser(userId);
            findRole(roleId);
            rbacRepository.assignRole(userId, roleId);
            auditService.record(context, "ADMIN_USER_ROLE_ASSIGN", "USER", userId);
            return null;
        });
    }

    public void removeUserRole(UUID userId, UUID roleId, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.USER_UPDATE);
            findUser(userId);
            findRole(roleId);
            rbacRepository.removeRole(userId, roleId);
            auditService.record(context, "ADMIN_USER_ROLE_REMOVE", "USER", userId);
            return null;
        });
    }

    public List<PermissionDto> listRolePermissions(UUID roleId, UseCaseContext context) {
        return transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_READ);
            findRole(roleId);
            return rbacRepository.findPermissionsByRoleId(roleId).stream().map(this::permissionDto).toList();
        });
    }

    public void assignRolePermission(UUID roleId, UUID permissionId, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_MANAGE);
            findRole(roleId);
            findPermission(permissionId);
            rbacRepository.assignPermission(roleId, permissionId);
            auditService.record(context, "ADMIN_ROLE_PERMISSION_ASSIGN", "ROLE", roleId);
            return null;
        });
    }

    public void removeRolePermission(UUID roleId, UUID permissionId, UseCaseContext context) {
        transactionManager.inTransaction(() -> {
            authorizationPort.ensurePermission(context, PermissionCodes.ROLE_MANAGE);
            findRole(roleId);
            findPermission(permissionId);
            rbacRepository.removePermission(roleId, permissionId);
            auditService.record(context, "ADMIN_ROLE_PERMISSION_REMOVE", "ROLE", roleId);
            return null;
        });
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User"));
    }

    private Role findRole(UUID id) {
        return rbacRepository.findRoleById(id).orElseThrow(() -> new NotFoundException("Role"));
    }

    private Permission findPermission(UUID id) {
        return rbacRepository.findPermissionById(id)
                .orElseThrow(() -> new NotFoundException("Permission"));
    }

    private void ensureUserUnique(String email, String login, UUID id) {
        if (id == null ? userRepository.existsByEmail(email) : userRepository.existsByEmailExceptId(email, id)) {
            throw new ConflictException("USER_EMAIL_ALREADY_EXISTS", "User email already exists");
        }
        if (id == null ? userRepository.existsByLogin(login) : userRepository.existsByLoginExceptId(login, id)) {
            throw new ConflictException("USER_LOGIN_ALREADY_EXISTS", "User login already exists");
        }
    }

    private RoleDto roleDto(Role role) {
        return new RoleDto(role.id(), role.code(), role.name(), role.description(), role.system());
    }

    private PermissionDto permissionDto(Permission permission) {
        return new PermissionDto(permission.id(), permission.code(), permission.description());
    }
}
