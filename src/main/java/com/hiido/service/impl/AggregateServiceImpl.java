package com.hiido.service.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.hiido.service.TransferService;
import com.hiido.utils.HdfsUtils;
import com.hiido.utils.HiveMetaUtils;
import jodd.typeconverter.Convert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service("Aggregate")
@Slf4j
public class AggregateServiceImpl implements TransferService {

    CommandLine resolveArgs(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("help", false, "help information");
        Option input = Option.builder("i")
                .longOpt("input")
                .required()
                .desc("输入文件的路径")
                .build();
        Option output = Option.builder("o")
                .longOpt("output")
                .required()
                .desc("输出文件的路径")
                .build();
        Option metaUri = Option.builder("m")
                .longOpt("meta")
                .required()
                .desc("Hive的metastore地址")
                .build();
        Option partiton = Option.builder("p")
                .longOpt("partition")
                .required()
                .desc("需要添加的partition")
                .build();

        Option database = Option.builder("d")
                .longOpt("partition")
                .required()
                .desc("数据库名称")
                .build();

        Option tableName = Option.builder("t")
                .longOpt("partition")
                .required()
                .desc("表名")
                .build();

        Option user = Option.builder("u")
                .longOpt("user")
                .required()
                .desc("用户名")
                .build();

        Option keytab = Option.builder("k")
                .longOpt("keytab")
                .required()
                .desc("keytab文件")
                .build();

        options.addOption(input);
        options.addOption(output);
        options.addOption(metaUri);
        options.addOption(partiton);
        options.addOption(database);
        options.addOption(tableName);
        options.addOption(user);
        options.addOption(keytab);

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
            String principle = commandLine.getOptionValue("principle", "nn/_HOST@YYDEVOPS.COM");
            hiveMetaUtils = new HiveMetaUtils(metaUri, "1000");
            hdfsUtils = new HdfsUtils(user, keytab, principle, "");
            FileSystem fileSystem = hdfsUtils.getFileSystem();
            List<String> inputList = Splitter.on(",").trimResults().splitToList(input);
            List<String> partitionVals = Splitter.on(",").trimResults().splitToList(partition);


            //输入目录逐个进行distcp
            for (String srcPath : inputList) {
                log.info("拷贝文件 {} 到 {}", srcPath, output);
                Path path = new Path(srcPath);
                ContentSummary contentSummary = fileSystem.getContentSummary(path);
                int mapNum = Convert.toInteger(Math.ceil(contentSummary.getLength() / 1024.0 / 1024.0 / 256.0));
                hdfsUtils.distcp(srcPath, output, mapNum);
                log.info("- - - - - - - - - 数据传输完成 - - - - - - - - - - -\n\n");
            }
            if (hdfsUtils.checkFilesOfTwoPath(inputList, output)) {
                log.info("- - - - - - - - - 添加分区 - - - - - - - - -");
                log.info("alter table {}.{} add if not exist partition({}) ", database, Joiner.on(",").join(partitionVals));
                hiveMetaUtils.addPartition(database, tableName, partitionVals, output);
                System.exit(0);
            } else {
                log.error("_-_-_-_-_-_-_-_-_-数据拷贝失败_-_-_-_-_-_-_-_-_-");
                System.exit(-1);
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(-1);
        } finally {
            if (hiveMetaUtils != null) {
                hiveMetaUtils.close();
            }
            if (hdfsUtils != null) {
                hdfsUtils.getFileSystem().close();
            }
        }


    }


}
