package com.lottery.domain.valueobject;

import java.util.Set;

public final class PermissionCodes {
    public static final String USER_READ = "user.read";
    public static final String USER_CREATE = "user.create";
    public static final String USER_UPDATE = "user.update";
    public static final String USER_DELETE = "user.delete";
    public static final String ROLE_READ = "role.read";
    public static final String ROLE_MANAGE = "role.manage";
    public static final String PERMISSION_MANAGE = "permission.manage";
    public static final String DRAW_READ = "draw.read";
    public static final String DRAW_CREATE = "draw.create";
    public static final String DRAW_UPDATE = "draw.update";
    public static final String DRAW_CANCEL = "draw.cancel";
    public static final String DRAW_RUN = "draw.run";
    public static final String DRAW_RESULT_READ = "draw.result.read";
    public static final String TICKET_READ = "ticket.read";
    public static final String TICKET_CREATE = "ticket.create";
    public static final String TICKET_CANCEL = "ticket.cancel";
    public static final String PAYMENT_READ = "payment.read";
    public static final String PAYMENT_REFUND = "payment.refund";
    public static final String REPORT_DRAW_EXPORT = "report.draw.export";
    public static final String REPORT_TICKET_EXPORT = "report.ticket.export";
    public static final String UI_THEME_MANAGE = "ui.theme.manage";
    public static final String UI_TEMPLATE_MANAGE = "ui.template.manage";
    public static final String AUDIT_READ = "audit.read";
    public static final String SYSTEM_SETTINGS_MANAGE = "system.settings.manage";

    public static final Set<String> SYSTEM_CODES = Set.of(
            USER_READ,
            USER_CREATE,
            USER_UPDATE,
            USER_DELETE,
            ROLE_READ,
            ROLE_MANAGE,
            PERMISSION_MANAGE,
            DRAW_READ,
            DRAW_CREATE,
            DRAW_UPDATE,
            DRAW_CANCEL,
            DRAW_RUN,
            DRAW_RESULT_READ,
            TICKET_READ,
            TICKET_CREATE,
            TICKET_CANCEL,
            PAYMENT_READ,
            PAYMENT_REFUND,
            REPORT_DRAW_EXPORT,
            REPORT_TICKET_EXPORT,
            UI_THEME_MANAGE,
            UI_TEMPLATE_MANAGE,
            AUDIT_READ,
            SYSTEM_SETTINGS_MANAGE);

    private PermissionCodes() {
    }
}
