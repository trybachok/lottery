create index idx_draws_report_status_created_at
    on draws (status, created_at desc)
    where deleted_at is null;

create index idx_draws_report_manager_created_at
    on draws (manager_id, created_at desc)
    where deleted_at is null;

create index idx_tickets_report_user_created_at
    on tickets (user_id, created_at desc)
    where deleted_at is null;

create index idx_tickets_report_draw_created_at
    on tickets (draw_id, created_at desc)
    where deleted_at is null;

create index idx_tickets_report_status_created_at
    on tickets (status, created_at desc)
    where deleted_at is null;
