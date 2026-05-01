package com.lottery;

import com.lottery.infrastructure.config.ApplicationConfig;
import com.lottery.infrastructure.config.ApplicationProperties;
import org.eclipse.jetty.server.Server;

public final class LotteryApplication {
    private LotteryApplication() {
    }

    public static void main(String[] args) throws Exception {
        ApplicationProperties properties = ApplicationProperties.fromEnvironment(System.getenv());
        Server server = new ApplicationConfig().createServer(properties);
        server.start();
        server.join();
    }
}
