alter table invoices add column if not exists payment_url varchar(1024);

create table payment_outbox (
    id uuid primary key default gen_random_uuid(),
    type varchar(64) not null,
    status varchar(32) not null,
    invoice_id uuid not null,
    payment_id uuid,
    provider_code varchar(64) not null,
    payload_json jsonb not null,
    attempts integer not null default 0,
    next_attempt_at timestamptz not null,
    last_error text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    processed_at timestamptz,
    constraint fk_payment_outbox_invoice foreign key (invoice_id) references invoices (id),
    constraint fk_payment_outbox_payment foreign key (payment_id) references payments (id),
    constraint chk_payment_outbox_type check (type in ('CREATE_INVOICE', 'CANCEL_PAYMENT', 'REFUND_PAYMENT')),
    constraint chk_payment_outbox_status check (status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),
    constraint chk_payment_outbox_provider_code_not_blank check (length(trim(provider_code)) > 0),
    constraint chk_payment_outbox_payload_object check (jsonb_typeof(payload_json) = 'object'),
    constraint chk_payment_outbox_attempts_non_negative check (attempts >= 0)
);

create index idx_payment_outbox_due on payment_outbox (status, next_attempt_at, created_at);
create index idx_payment_outbox_invoice_id on payment_outbox (invoice_id);
create index idx_payment_outbox_payment_id on payment_outbox (payment_id);
