package com.hiido;

import com.google.common.collect.Lists;
import com.hiido.service.TransferService;
import com.hiido.service.impl.AggregateServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;

import java.util.List;

/**
 * Hello world!
 */

@Slf4j
public class App {

    public static void main(String[] args) throws Exception{
        TransferService aggregateService = new AggregateServiceImpl();
        try {
            aggregateService.processJob(args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


    }
}
