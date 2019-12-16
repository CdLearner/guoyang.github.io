package com.hiido.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.hiido.service.TransferService;
import com.hiido.utils.HdfsUtils;
import com.hiido.utils.HiveMetaUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class AggregateServiceImpl implements TransferService {

    CommandLine resolveArgs(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("help", false, "help information");
        Option input = Option.builder("i")
                .longOpt("input")
                .required()
                .hasArg()
                .desc("输入文件的路径")
                .build();
        Option output = Option.builder("o")
                .longOpt("output")
                .required()
                .hasArg()
                .desc("输出文件的路径")
                .build();
        Option metaUri = Option.builder("m")
                .longOpt("meta")
                .required()
                .hasArg()
                .desc("Hive的metastore地址")
                .build();
        Option partiton = Option.builder("p")
                .longOpt("partition")
                .required()
                .hasArg()
                .desc("需要添加的partition")
                .build();

        Option database = Option.builder("d")
                .longOpt("partition")
                .required()
                .hasArg()
                .desc("数据库名称")
                .build();

        Option tableName = Option.builder("t")
                .longOpt("partition")
                .required()
                .hasArg()
                .desc("表名")
                .build();

        Option user = Option.builder("u")
                .longOpt("user")
                .required()
                .hasArg()
                .desc("用户名")
                .build();

        Option keytab = Option.builder("k")
                .longOpt("keytab")
                .hasArg()
                .required()
                .desc("keytab文件")
                .build();
        Option rename = Option.builder("r")
                .longOpt("rename")
                .desc("是否需要重命名")
                .build();
        Option hadoop = Option.builder("b")
                .longOpt("hadoop")
                .required()
                .hasArg()
                .desc("Hadoop Bin Path")
                .build();

        Option java = Option.builder("j")
                .longOpt("java")
                .required()
                .hasArg()
                .desc("Java Home")
                .build();

        options.addOption(input);
        options.addOption(output);
        options.addOption(metaUri);
        options.addOption(partiton);
        options.addOption(database);
        options.addOption(tableName);
        options.addOption(user);
        options.addOption(keytab);
        options.addOption(rename);
        options.addOption(hadoop);
        options.addOption(java);

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }


    @Override
    public void processJob(String... args) throws Exception {

        HiveMetaUtils hiveMetaUtils = null;
        HdfsUtils hdfsUtils = null;
        //参数初始化
        try {
            CommandLine commandLine = resolveArgs(args);
            String input = commandLine.getOptionValue('i');
            log.info("input:{}", input);
            String output = commandLine.getOptionValue('o');
            log.info("output:{}", output);
            String metaUri = commandLine.getOptionValue('m');
            log.info("metaUri:{}", metaUri);
            String partition = commandLine.getOptionValue('p');
            log.info("partition:{}", partition);
            String database = commandLine.getOptionValue('d');
            log.info("database:{}", database);
            String tableName = commandLine.getOptionValue('t');
            log.info("table:{}", tableName);
            String user = commandLine.getOptionValue('u');
            log.info("database:{}", database);
            String keytab = commandLine.getOptionValue('k');
            log.info("table:{}", tableName);
            boolean rename = commandLine.hasOption('r');
            log.info("need rename:{}", rename);
            String bin = commandLine.getOptionValue('b');
            log.info("Hadoop Bin Path:{}", bin);
            String java = commandLine.getOptionValue('j');
            log.info("Java Bin Path:{}", java);
            String principle = commandLine.getOptionValue("principle", "nn/_HOST@YYDEVOPS.COM");
            hdfsUtils = new HdfsUtils(user, keytab, principle, "");
            hiveMetaUtils = new HiveMetaUtils(metaUri, "5", user);
            List<String> inputList = Splitter.on(",").trimResults().splitToList(input);
            Map<String, String> partitionMap = Splitter.on(",").withKeyValueSeparator("=").split(partition);

            Map<String, String> env = Maps.newHashMap();
            env.put("KRB5PRINCIPAL", user);
            env.put("KRB5KEYTAB", keytab);
            env.put("JAVA_HOME", java);
            //输入目录逐个进行distcp
            for (String srcPath : inputList) {
                log.info("拷贝文件 {} 到 {}", srcPath, output);
                int mapNum = (int) Math.ceil(hdfsUtils.getFileSize(srcPath) / 128.0);
                String postfix = hdfsUtils.getParentDir(srcPath, rename);
                hdfsUtils.renameFile(srcPath, postfix, rename);
                if (hdfsUtils.distcp(bin, srcPath, output, mapNum, env)) {
                    log.info("- - - - - - - - - 数据传输完成 - - - - - - - - - - -\n\n");
                    hdfsUtils.resumeFile(srcPath, postfix, rename);
                } else {
                    hdfsUtils.resumeFile(srcPath, postfix, rename);
                    log.info("- - - - - - - - - 数据传输失败 - - - - - - - - - - -\n\n");
                    throw new Exception("数据传输失败，失败文件为:" + srcPath);
                }
            }
            if (hdfsUtils.checkFilesOfTwoPath(inputList, output, rename)) {
                log.info("- - - - - - - - - 添加分区 - - - - - - - - -");
                log.info("alter table {}.{} add if not exist partition({}) ", database, tableName, partition);
                List<String> partitionKeys = hiveMetaUtils.getPartitionKeys(database, tableName);
                List<String> partitionVals = partitionKeys.stream().map(key -> partitionMap.get(key)).collect(Collectors.toList());
                hiveMetaUtils.addPartition(database, tableName, partitionVals, output);
                log.info("- - - - - - - - - - -任务成功，程序退出 - - - - - - - - - - - ");
                System.exit(0);
            } else {
                log.error("_-_-_-_-_-_-_-_-_-数据不一致,任务失败_-_-_-_-_-_-_-_-_-");
                System.exit(-1);
            }
        } catch (Exception e) {
            log.error("- - - - - - - - - - 任务失败，抛出未知异常 - - - - - - - - - - ");
            log.error(e.getMessage(), e);
            System.exit(-1);
        } finally {
            if (hiveMetaUtils != null) {
                hiveMetaUtils.close();
            }

        }
    }


}
