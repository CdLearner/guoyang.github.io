package com.hiido;

import com.hiido.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hello world!
 */
@EnableAsync
@SpringBootApplication
@EnableRetry
@Slf4j
public class App implements CommandLineRunner {

    @Autowired
    TransferService aggregateService;

    @Override
    public void run(String[] args) {
        try {
            for (String arg : args) {
                log.info(arg);
            }

            aggregateService.processJob(args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        context.registerShutdownHook();

    }
}
