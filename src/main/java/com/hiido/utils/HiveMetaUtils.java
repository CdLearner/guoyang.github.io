package com.hiido.utils;

import com.google.common.base.Joiner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Data
public class HiveMetaUtils {
    HiveMetaStoreClient client;

    public HiveMetaUtils(String url, String timeout,String sasl,String principal) throws Exception {
        HiveConf conf = new HiveConf();
        conf.setVar(HiveConf.ConfVars.METASTORE_KERBEROS_PRINCIPAL,principal);
        log.info("set {}:{}",HiveConf.ConfVars.METASTORE_KERBEROS_PRINCIPAL,principal);
        conf.setVar(HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL,sasl);
        log.info("set {}:{}",HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL,sasl);
        conf.setVar(HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT, timeout);
        log.info("set {}:{}",HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT,timeout);
        conf.setVar(HiveConf.ConfVars.METASTOREURIS, url);
        log.info("set {}:{}",HiveConf.ConfVars.METASTOREURIS,url);
        this.client = new HiveMetaStoreClient(conf);
    }
    public HiveMetaUtils(String url, String timeout) throws Exception {
        HiveConf conf = new HiveConf();
        conf.setVar(HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT, timeout);
        conf.setVar(HiveConf.ConfVars.METASTOREURIS, url);
        this.client = new HiveMetaStoreClient(conf);
    }

    public List<String> getPartitionKeys(String database, String table) throws Exception {
        Table hiveTable = client.getTable(database, table);
        return hiveTable.getPartitionKeys().stream().map(FieldSchema::getName).collect(Collectors.toList());
    }

    public void alterPartition(String database, String tableName, List<String> partitionVals, String location) throws Exception {
        Partition partition = client.getPartition(database, tableName, partitionVals);
        partition.getSd().setLocation(location);
        client.alter_partition(database, tableName, partition, null);
    }

    public void addPartition(String database, String tableName, List<String> partitionVals, String location) throws Exception {
        try {
            client.getPartition(database, tableName, partitionVals);
            alterPartition(database, tableName, partitionVals, location);
            log.info("Partition Already Exist,Only Change Location");
        } catch (NoSuchObjectException e) {
            Table table = client.getTable(database, tableName);
            Partition newPartition = new Partition();
            newPartition.setDbName(database);
            newPartition.setTableName(tableName);
            newPartition.setValues(partitionVals);
            newPartition.setSd(table.getSd());
            newPartition.getSd().setLocation(location);
            log.info("Add Partition to {}.{}", database, tableName);
            client.add_partition(newPartition);
        }

    }

    private static String generatePartitionKVPairs(List<String> partitionkeys, List<String> partitionVals, String joiner, String queto, String keySep) {
        Map<String, String> maps = partitionkeys.stream().collect(Collectors.toMap(
                x -> x, x -> queto + partitionVals.get(partitionkeys.indexOf(x)) + queto
        ));
        return Joiner.on(joiner).withKeyValueSeparator(keySep).join(maps);
    }

    public void close() {
        client.close();
    }

}
