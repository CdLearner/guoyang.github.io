package com.hiido;

import com.hiido.service.TransferService;
import com.hiido.service.impl.AggregateServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello world!
 */

@Slf4j
public class App {

    public static void main(String[] args) {
        TransferService aggregateService = new AggregateServiceImpl();
        try {
            aggregateService.processJob(args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}
