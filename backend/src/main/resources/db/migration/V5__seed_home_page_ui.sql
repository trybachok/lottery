insert into ui_themes (id, name, tokens_json, is_default, created_at)
values
    (
        '5b7d0b8e-0f3a-4b6f-8d35-7b6e8e38520d',
        'Light',
        '{
            "mode": "light",
            "colors": {
                "background": "#f7f5ef",
                "surface": "#ffffff",
                "text": "#1d2433",
                "mutedText": "#697386",
                "primary": "#0f766e",
                "primaryText": "#ffffff",
                "accent": "#d97706",
                "border": "#d9dee8",
                "banner": "#e6f4f1",
                "sidebar": "#fff8ed"
            }
        }'::jsonb,
        true,
        now()
    ),
    (
        'b8d6fd2b-1d33-4f13-a831-6554de329784',
        'Dark',
        '{
            "mode": "dark",
            "colors": {
                "background": "#101820",
                "surface": "#17212b",
                "text": "#f4f7fb",
                "mutedText": "#a7b3c2",
                "primary": "#2dd4bf",
                "primaryText": "#062520",
                "accent": "#fbbf24",
                "border": "#2d3a46",
                "banner": "#0f2c2c",
                "sidebar": "#211b2d"
            }
        }'::jsonb,
        false,
        now()
    )
on conflict (id) do nothing;

insert into ui_templates (id, name, layout_json, created_at)
values (
    '1ce3fdcb-ff31-4962-bf25-7fc1effebf91',
    'Default home page',
    '{
        "version": 1,
        "regions": {
            "header": {
                "type": "header",
                "title": "Lottery",
                "subtitle": "Онлайн-лотерея с прозрачными розыгрышами",
                "actions": [
                    { "label": "Розыгрыши", "to": "/draws" },
                    { "label": "Войти", "to": "/login" }
                ]
            },
            "banner": {
                "type": "banner",
                "title": "Проверь удачу в ближайшем розыгрыше",
                "subtitle": "Покупайте билеты, оплачивайте онлайн и отслеживайте результаты в личном кабинете.",
                "action": { "label": "Смотреть розыгрыши", "to": "/draws" }
            },
            "sidebar": {
                "type": "sidebar",
                "title": "Быстрые действия",
                "blocks": [
                    { "type": "link", "label": "Мои билеты", "to": "/tickets" },
                    { "type": "link", "label": "История розыгрышей", "to": "/draws" },
                    { "type": "text", "text": "Темы и оформление сохраняются в этом браузере." }
                ]
            },
            "main": {
                "type": "main",
                "title": "Главная",
                "blocks": [
                    {
                        "type": "card",
                        "title": "Покупка билетов",
                        "text": "Выберите активный розыгрыш, заполните комбинацию и оплатите билет."
                    },
                    {
                        "type": "card",
                        "title": "Результаты",
                        "text": "После завершения розыгрыша результат доступен в карточке билета."
                    },
                    {
                        "type": "card",
                        "title": "Админ-настройки",
                        "text": "Шаблон этой страницы и тема по умолчанию управляются из панели администратора."
                    }
                ]
            },
            "footer": {
                "type": "footer",
                "text": "Lottery system. Настраиваемая главная страница."
            }
        }
    }'::jsonb,
    now()
)
on conflict (id) do nothing;

insert into system_settings (key, value_json, updated_by, updated_at)
values (
    'home_page',
    '{
        "activeTemplateId": "1ce3fdcb-ff31-4962-bf25-7fc1effebf91",
        "defaultThemeId": "5b7d0b8e-0f3a-4b6f-8d35-7b6e8e38520d"
    }'::jsonb,
    null,
    now()
)
on conflict (key) do nothing;
