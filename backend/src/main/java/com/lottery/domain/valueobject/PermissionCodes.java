package com.lottery.domain.valueobject;

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

    private PermissionCodes() {
    }
}
