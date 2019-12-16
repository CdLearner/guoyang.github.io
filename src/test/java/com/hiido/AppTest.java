package com.hiido;

import com.hiido.utils.HiveMetaUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
@Slf4j
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() throws Exception {
        String metaUri="thrift://14.17.119.2:9084";
//        String metaUri="thrift://14.17.109.45:9083";
        String timeout="5";
        HiveConf conf = new HiveConf();
//        conf.setVar(HiveConf.ConfVars.METASTORE_KERBEROS_PRINCIPAL,"hiidoagent");
//        conf.setVar(HiveConf.ConfVars.METASTORE_USE_THRIFT_SASL,"true");
        conf.setVar(HiveConf.ConfVars.METASTORE_CLIENT_SOCKET_TIMEOUT, timeout);
        conf.setVar(HiveConf.ConfVars.METASTOREURIS, metaUri);
        HiveMetaStoreClient client = new HiveMetaStoreClient(conf);
//        log.info(""+client.tableExists("temp","testTable"));
        Table table = client.getTable("temp", "testTable");
        log.info(table.getSd().getLocation());
        log.info("--");
    }
}
