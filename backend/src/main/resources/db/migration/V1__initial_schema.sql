create extension if not exists pgcrypto;

create table users (
    id uuid primary key default gen_random_uuid(),
    email varchar(320) not null unique,
    login varchar(128) not null unique,
    password_hash varchar(255),
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    version bigint not null default 0,
    constraint chk_users_status check (status in ('ACTIVE', 'BLOCKED', 'DELETED')),
    constraint chk_users_email_not_blank check (length(trim(email)) > 0),
    constraint chk_users_login_not_blank check (length(trim(login)) > 0),
    constraint chk_users_version_non_negative check (version >= 0)
);

create index idx_users_status on users (status);
create index idx_users_created_at on users (created_at desc);

create table roles (
    id uuid primary key default gen_random_uuid(),
    code varchar(64) not null unique,
    name varchar(128) not null,
    description text,
    is_system boolean not null default false,
    constraint chk_roles_code_not_blank check (length(trim(code)) > 0),
    constraint chk_roles_name_not_blank check (length(trim(name)) > 0)
);

create table permissions (
    id uuid primary key default gen_random_uuid(),
    code varchar(128) not null unique,
    description text,
    constraint chk_permissions_code_not_blank check (length(trim(code)) > 0)
);

create table user_roles (
    user_id uuid not null,
    role_id uuid not null,
    primary key (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users (id) on delete cascade,
    constraint fk_user_roles_role foreign key (role_id) references roles (id) on delete cascade
);

create index idx_user_roles_role_id on user_roles (role_id);

create table role_permissions (
    role_id uuid not null,
    permission_id uuid not null,
    primary key (role_id, permission_id),
    constraint fk_role_permissions_role foreign key (role_id) references roles (id) on delete cascade,
    constraint fk_role_permissions_permission foreign key (permission_id) references permissions (id) on delete cascade
);

create index idx_role_permissions_permission_id on role_permissions (permission_id);

create table combination_schemas (
    id uuid primary key default gen_random_uuid(),
    name varchar(128) not null,
    schema_json jsonb not null,
    created_at timestamptz not null,
    constraint chk_combination_schemas_name_not_blank check (length(trim(name)) > 0),
    constraint chk_combination_schemas_schema_object check (jsonb_typeof(schema_json) = 'object')
);

create index idx_combination_schemas_created_at on combination_schemas (created_at desc);

create table ui_themes (
    id uuid primary key default gen_random_uuid(),
    name varchar(128) not null,
    tokens_json jsonb not null,
    is_default boolean not null default false,
    created_at timestamptz not null,
    constraint chk_ui_themes_name_not_blank check (length(trim(name)) > 0),
    constraint chk_ui_themes_tokens_object check (jsonb_typeof(tokens_json) = 'object')
);

create unique index uq_ui_themes_single_default on ui_themes (is_default) where is_default;
create index idx_ui_themes_created_at on ui_themes (created_at desc);

create table ui_templates (
    id uuid primary key default gen_random_uuid(),
    name varchar(128) not null,
    layout_json jsonb not null,
    created_at timestamptz not null,
    constraint chk_ui_templates_name_not_blank check (length(trim(name)) > 0),
    constraint chk_ui_templates_layout_object check (jsonb_typeof(layout_json) = 'object')
);

create index idx_ui_templates_created_at on ui_templates (created_at desc);

create table draws (
    id uuid primary key default gen_random_uuid(),
    title varchar(255) not null,
    description text,
    status varchar(32) not null,
    manager_id uuid,
    combination_schema_id uuid not null,
    ui_theme_id uuid,
    ui_template_id uuid,
    sales_start_at timestamptz not null,
    sales_end_at timestamptz not null,
    draw_at timestamptz not null,
    max_tickets integer,
    is_test boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz,
    version bigint not null default 0,
    constraint fk_draws_manager foreign key (manager_id) references users (id),
    constraint fk_draws_combination_schema foreign key (combination_schema_id) references combination_schemas (id),
    constraint fk_draws_ui_theme foreign key (ui_theme_id) references ui_themes (id),
    constraint fk_draws_ui_template foreign key (ui_template_id) references ui_templates (id),
    constraint chk_draws_title_not_blank check (length(trim(title)) > 0),
    constraint chk_draws_status check (status in (
        'DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'POSTPONED', 'SALES_CLOSED',
        'DRAWING', 'COMPLETED', 'CANCELLED', 'TEST', 'ARCHIVED'
    )),
    constraint chk_draws_sales_window check (sales_start_at < sales_end_at),
    constraint chk_draws_draw_at_after_sales check (draw_at >= sales_end_at),
    constraint chk_draws_max_tickets_positive check (max_tickets is null or max_tickets > 0),
    constraint chk_draws_version_non_negative check (version >= 0)
);

create index idx_draws_status_sales_end_at on draws (status, sales_end_at);
create index idx_draws_manager_status on draws (manager_id, status);
create index idx_draws_draw_at on draws (draw_at);
create index idx_draws_created_at on draws (created_at desc);
create index idx_draws_active_sales_window on draws (sales_start_at, sales_end_at) where status = 'ACTIVE' and deleted_at is null;

create table prizes (
    id uuid primary key default gen_random_uuid(),
    type varchar(32) not null,
    name varchar(255) not null,
    amount numeric(19, 4),
    currency char(3),
    product_id uuid,
    quantity numeric(19, 4),
    unit varchar(64),
    constraint chk_prizes_type check (type in ('MONEY', 'FOOD_PRODUCT', 'SPORT_SUPPLEMENT')),
    constraint chk_prizes_name_not_blank check (length(trim(name)) > 0),
    constraint chk_prizes_amount_non_negative check (amount is null or amount >= 0),
    constraint chk_prizes_quantity_non_negative check (quantity is null or quantity >= 0),
    constraint chk_prizes_currency_code check (currency is null or currency ~ '^[A-Z]{3}$'),
    constraint chk_prizes_money_fields check (
        (type = 'MONEY' and amount is not null and currency is not null)
        or (type <> 'MONEY')
    ),
    constraint chk_prizes_product_fields check (
        (type in ('FOOD_PRODUCT', 'SPORT_SUPPLEMENT') and product_id is not null and quantity is not null and unit is not null)
        or (type = 'MONEY')
    )
);

create index idx_prizes_type on prizes (type);

create table tickets (
    id uuid not null default gen_random_uuid(),
    user_id uuid not null,
    draw_id uuid not null,
    status varchar(32) not null,
    combination_json jsonb not null,
    price_amount numeric(19, 4) not null,
    price_currency char(3) not null,
    match_percent numeric(5, 2),
    prize_id uuid,
    is_test boolean not null default false,
    created_at timestamptz not null,
    paid_at timestamptz,
    participated_at timestamptz,
    checked_at timestamptz,
    cancelled_at timestamptz,
    deleted_at timestamptz,
    version bigint not null default 0,
    primary key (id),
    constraint fk_tickets_user foreign key (user_id) references users (id),
    constraint fk_tickets_draw foreign key (draw_id) references draws (id),
    constraint fk_tickets_prize foreign key (prize_id) references prizes (id),
    constraint chk_tickets_status check (status in (
        'CREATED', 'PAYMENT_PENDING', 'PAID', 'PAYMENT_FAILED', 'REFUND_PENDING', 'REFUNDED',
        'CANCELLED', 'PARTICIPATED', 'NOT_PARTICIPATED', 'CHECKED', 'WIN', 'LOSE', 'DELETED', 'TEST'
    )),
    constraint chk_tickets_combination_array check (jsonb_typeof(combination_json) = 'array'),
    constraint chk_tickets_price_amount_non_negative check (price_amount >= 0),
    constraint chk_tickets_price_currency_code check (price_currency ~ '^[A-Z]{3}$'),
    constraint chk_tickets_match_percent_range check (match_percent is null or (match_percent >= 0 and match_percent <= 100)),
    constraint chk_tickets_version_non_negative check (version >= 0)
) partition by hash (id);

create table tickets_p00 partition of tickets for values with (modulus 16, remainder 0);
create table tickets_p01 partition of tickets for values with (modulus 16, remainder 1);
create table tickets_p02 partition of tickets for values with (modulus 16, remainder 2);
create table tickets_p03 partition of tickets for values with (modulus 16, remainder 3);
create table tickets_p04 partition of tickets for values with (modulus 16, remainder 4);
create table tickets_p05 partition of tickets for values with (modulus 16, remainder 5);
create table tickets_p06 partition of tickets for values with (modulus 16, remainder 6);
create table tickets_p07 partition of tickets for values with (modulus 16, remainder 7);
create table tickets_p08 partition of tickets for values with (modulus 16, remainder 8);
create table tickets_p09 partition of tickets for values with (modulus 16, remainder 9);
create table tickets_p10 partition of tickets for values with (modulus 16, remainder 10);
create table tickets_p11 partition of tickets for values with (modulus 16, remainder 11);
create table tickets_p12 partition of tickets for values with (modulus 16, remainder 12);
create table tickets_p13 partition of tickets for values with (modulus 16, remainder 13);
create table tickets_p14 partition of tickets for values with (modulus 16, remainder 14);
create table tickets_p15 partition of tickets for values with (modulus 16, remainder 15);

create index idx_tickets_user_created_at on tickets (user_id, created_at desc);
create index idx_tickets_draw_status on tickets (draw_id, status);
create index idx_tickets_status_created_at on tickets (status, created_at desc);
create index idx_tickets_draw_user on tickets (draw_id, user_id);
create index idx_tickets_created_at on tickets (created_at desc);

create table draw_results (
    id uuid primary key default gen_random_uuid(),
    draw_id uuid not null unique,
    winning_combination_json jsonb not null,
    algorithm_version varchar(64) not null,
    random_provider varchar(128),
    proof_hash varchar(255),
    generated_by uuid,
    generated_at timestamptz not null,
    request_id varchar(128),
    correlation_id varchar(128),
    constraint fk_draw_results_draw foreign key (draw_id) references draws (id),
    constraint fk_draw_results_generated_by foreign key (generated_by) references users (id),
    constraint chk_draw_results_winning_combination_array check (jsonb_typeof(winning_combination_json) = 'array'),
    constraint chk_draw_results_algorithm_version_not_blank check (length(trim(algorithm_version)) > 0)
);

create index idx_draw_results_generated_at on draw_results (generated_at desc);

create table winning_rules (
    id uuid primary key default gen_random_uuid(),
    draw_id uuid not null,
    match_percent_from numeric(5, 2) not null,
    match_percent_to numeric(5, 2) not null,
    prize_id uuid not null,
    priority integer not null,
    constraint fk_winning_rules_draw foreign key (draw_id) references draws (id) on delete cascade,
    constraint fk_winning_rules_prize foreign key (prize_id) references prizes (id),
    constraint chk_winning_rules_percent_range check (
        match_percent_from >= 0
        and match_percent_to <= 100
        and match_percent_from <= match_percent_to
    ),
    constraint chk_winning_rules_percent_step check (
        mod(match_percent_from, 5) = 0
        and mod(match_percent_to, 5) = 0
    ),
    constraint chk_winning_rules_priority_non_negative check (priority >= 0)
);

create index idx_winning_rules_draw_priority on winning_rules (draw_id, priority);
create index idx_winning_rules_prize_id on winning_rules (prize_id);

create table invoices (
    id uuid primary key default gen_random_uuid(),
    ticket_id uuid not null,
    user_id uuid not null,
    provider_code varchar(64) not null,
    status varchar(32) not null,
    amount numeric(19, 4) not null,
    currency char(3) not null,
    external_invoice_id varchar(255),
    idempotency_key varchar(255) not null unique,
    created_at timestamptz not null,
    expires_at timestamptz,
    paid_at timestamptz,
    constraint fk_invoices_ticket foreign key (ticket_id) references tickets (id),
    constraint fk_invoices_user foreign key (user_id) references users (id),
    constraint chk_invoices_provider_code_not_blank check (length(trim(provider_code)) > 0),
    constraint chk_invoices_status check (status in (
        'CREATED', 'PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED', 'EXPIRED'
    )),
    constraint chk_invoices_amount_non_negative check (amount >= 0),
    constraint chk_invoices_currency_code check (currency ~ '^[A-Z]{3}$'),
    constraint chk_invoices_expiration_after_created check (expires_at is null or expires_at > created_at)
);

create unique index uq_invoices_ticket_active on invoices (ticket_id) where status in ('CREATED', 'PENDING');
create index idx_invoices_ticket_id on invoices (ticket_id);
create index idx_invoices_user_created_at on invoices (user_id, created_at desc);
create index idx_invoices_status_created_at on invoices (status, created_at desc);
create index idx_invoices_provider_external_id on invoices (provider_code, external_invoice_id);

create table payments (
    id uuid primary key default gen_random_uuid(),
    invoice_id uuid not null,
    provider_code varchar(64) not null,
    status varchar(32) not null,
    amount numeric(19, 4) not null,
    currency char(3) not null,
    external_payment_id varchar(255) unique,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint fk_payments_invoice foreign key (invoice_id) references invoices (id),
    constraint chk_payments_provider_code_not_blank check (length(trim(provider_code)) > 0),
    constraint chk_payments_status check (status in ('INITIATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    constraint chk_payments_amount_non_negative check (amount >= 0),
    constraint chk_payments_currency_code check (currency ~ '^[A-Z]{3}$')
);

create index idx_payments_invoice_id on payments (invoice_id);
create index idx_payments_status_created_at on payments (status, created_at desc);
create index idx_payments_provider_external_id on payments (provider_code, external_payment_id);

create table payment_webhook_events (
    id uuid not null default gen_random_uuid(),
    provider_code varchar(64) not null,
    event_type varchar(128) not null,
    external_event_id varchar(255) not null,
    payload jsonb not null,
    signature_valid boolean not null,
    processed boolean not null default false,
    received_at timestamptz not null,
    primary key (id, received_at),
    constraint chk_payment_webhook_events_provider_not_blank check (length(trim(provider_code)) > 0),
    constraint chk_payment_webhook_events_type_not_blank check (length(trim(event_type)) > 0),
    constraint chk_payment_webhook_events_external_id_not_blank check (length(trim(external_event_id)) > 0),
    constraint chk_payment_webhook_events_payload_object check (jsonb_typeof(payload) = 'object')
) partition by range (received_at);

create table payment_webhook_events_2026_05 partition of payment_webhook_events
    for values from ('2026-05-01 00:00:00+00') to ('2026-06-01 00:00:00+00');
create table payment_webhook_events_default partition of payment_webhook_events default;

create index idx_payment_webhook_events_provider_external_id on payment_webhook_events (provider_code, external_event_id);
create index idx_payment_webhook_events_processed_received_at on payment_webhook_events (processed, received_at);
create index idx_payment_webhook_events_provider_received_at on payment_webhook_events (provider_code, received_at desc);
create unique index uq_payment_webhook_events_provider_external_received
    on payment_webhook_events (provider_code, external_event_id, received_at);

create table audit_logs (
    id uuid not null default gen_random_uuid(),
    actor_user_id uuid,
    actor_role_codes text[] not null,
    action varchar(128) not null,
    entity_type varchar(128) not null,
    entity_id uuid,
    request_id varchar(128) not null,
    ip_address inet,
    user_agent text,
    before_json jsonb,
    after_json jsonb,
    created_at timestamptz not null,
    primary key (id, created_at),
    constraint fk_audit_logs_actor_user foreign key (actor_user_id) references users (id),
    constraint chk_audit_logs_action_not_blank check (length(trim(action)) > 0),
    constraint chk_audit_logs_entity_type_not_blank check (length(trim(entity_type)) > 0),
    constraint chk_audit_logs_request_id_not_blank check (length(trim(request_id)) > 0),
    constraint chk_audit_logs_before_object check (before_json is null or jsonb_typeof(before_json) = 'object'),
    constraint chk_audit_logs_after_object check (after_json is null or jsonb_typeof(after_json) = 'object')
) partition by range (created_at);

create table audit_logs_2026_05 partition of audit_logs
    for values from ('2026-05-01 00:00:00+00') to ('2026-06-01 00:00:00+00');
create table audit_logs_default partition of audit_logs default;

create index idx_audit_logs_actor_created_at on audit_logs (actor_user_id, created_at desc);
create index idx_audit_logs_entity_created_at on audit_logs (entity_type, entity_id, created_at desc);
create index idx_audit_logs_action_created_at on audit_logs (action, created_at desc);
create index idx_audit_logs_request_id on audit_logs (request_id);

create function prevent_audit_logs_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit_logs are append-only';
end;
$$;

create trigger trg_audit_logs_prevent_update_delete
before update or delete on audit_logs
for each row execute function prevent_audit_logs_mutation();

create table system_settings (
    key varchar(128) primary key,
    value_json jsonb not null,
    updated_by uuid,
    updated_at timestamptz not null,
    constraint fk_system_settings_updated_by foreign key (updated_by) references users (id),
    constraint chk_system_settings_key_not_blank check (length(trim(key)) > 0),
    constraint chk_system_settings_value_object check (jsonb_typeof(value_json) = 'object')
);

create index idx_system_settings_updated_at on system_settings (updated_at desc);
