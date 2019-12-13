package com.hiido.utils;

import com.google.common.base.Joiner;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HiveMetaUtils {
    HiveMetaStoreClient client;

    public HiveMetaUtils(String url, String timeout) throws Exception {
        HiveConf conf = new HiveConf();
        conf.setVar(HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT, "1000");
        conf.setVar(HiveConf.ConfVars.METASTOREURIS, url);
        this.client = new HiveMetaStoreClient(conf);
    }

    public void addPartition(String database, String tableName, List<String> partitionVals, String location) throws Exception {

        Table table = client.getTable(database, tableName);
        Partition newPartition = new Partition();
        newPartition.setDbName(database);
        newPartition.setTableName(tableName);
        newPartition.setValues(partitionVals);
        newPartition.setSd(table.getSd());
        newPartition.getSd().setLocation(location);
        client.add_partition(newPartition);

    }
    private static String generatePartitionKVPairs(List<String> partitionkeys, List<String> partitionVals,String joiner,String queto,String keySep) {
        Map<String, String> maps = partitionkeys.stream().collect(Collectors.toMap(
                x -> x, x -> queto + partitionVals.get(partitionkeys.indexOf(x)) + queto
        ));
        return Joiner.on(joiner).withKeyValueSeparator(keySep).join(maps);
    }

    public void close() {
        client.close();
    }

}
