insert into roles (id, code, name, description, is_system)
values
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrator', 'Full access to all system capabilities', true),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER', 'Manager', 'Manage assigned draws, tickets, payments, and reports', true),
    ('00000000-0000-0000-0000-000000000003', 'CLIENT', 'Client', 'Buy tickets and view own profile, tickets, payments, and results', true);

insert into permissions (id, code, description)
values
    ('10000000-0000-0000-0000-000000000001', 'user.read', 'Read users'),
    ('10000000-0000-0000-0000-000000000002', 'user.create', 'Create users'),
    ('10000000-0000-0000-0000-000000000003', 'user.update', 'Update users'),
    ('10000000-0000-0000-0000-000000000004', 'user.delete', 'Delete users'),
    ('10000000-0000-0000-0000-000000000005', 'role.read', 'Read roles'),
    ('10000000-0000-0000-0000-000000000006', 'role.manage', 'Manage roles'),
    ('10000000-0000-0000-0000-000000000007', 'permission.manage', 'Manage permissions'),
    ('10000000-0000-0000-0000-000000000008', 'draw.read', 'Read draws'),
    ('10000000-0000-0000-0000-000000000009', 'draw.create', 'Create draws'),
    ('10000000-0000-0000-0000-000000000010', 'draw.update', 'Update draws'),
    ('10000000-0000-0000-0000-000000000011', 'draw.cancel', 'Cancel draws'),
    ('10000000-0000-0000-0000-000000000012', 'draw.run', 'Run draws'),
    ('10000000-0000-0000-0000-000000000013', 'draw.result.read', 'Read draw results'),
    ('10000000-0000-0000-0000-000000000014', 'ticket.read', 'Read tickets'),
    ('10000000-0000-0000-0000-000000000015', 'ticket.create', 'Create tickets'),
    ('10000000-0000-0000-0000-000000000016', 'ticket.cancel', 'Cancel tickets'),
    ('10000000-0000-0000-0000-000000000017', 'payment.read', 'Read payments'),
    ('10000000-0000-0000-0000-000000000018', 'payment.refund', 'Refund payments'),
    ('10000000-0000-0000-0000-000000000019', 'report.draw.export', 'Export draw reports'),
    ('10000000-0000-0000-0000-000000000020', 'report.ticket.export', 'Export ticket reports'),
    ('10000000-0000-0000-0000-000000000021', 'ui.theme.manage', 'Manage UI themes'),
    ('10000000-0000-0000-0000-000000000022', 'ui.template.manage', 'Manage UI templates'),
    ('10000000-0000-0000-0000-000000000023', 'audit.read', 'Read audit logs'),
    ('10000000-0000-0000-0000-000000000024', 'system.settings.manage', 'Manage system settings');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
cross join permissions p
where r.code = 'ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'user.read',
    'draw.read',
    'draw.create',
    'draw.update',
    'draw.cancel',
    'draw.run',
    'draw.result.read',
    'ticket.read',
    'ticket.cancel',
    'payment.read',
    'payment.refund',
    'report.draw.export',
    'report.ticket.export'
)
where r.code = 'MANAGER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'draw.read',
    'draw.result.read',
    'ticket.read',
    'ticket.create',
    'ticket.cancel',
    'payment.read'
)
where r.code = 'CLIENT';
