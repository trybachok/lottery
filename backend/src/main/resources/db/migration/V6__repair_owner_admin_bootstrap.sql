insert into user_roles (user_id, role_id)
select u.id, r.id
from users u
join roles r on r.code = 'ADMIN'
where u.login = 'owner'
  and u.deleted_at is null
  and not exists (
      select 1
      from user_roles ur
      join roles admin_role on admin_role.id = ur.role_id
      join users admin_user on admin_user.id = ur.user_id
      where admin_role.code = 'ADMIN'
        and admin_user.deleted_at is null
  )
on conflict do nothing;
