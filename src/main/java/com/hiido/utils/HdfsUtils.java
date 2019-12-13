package com.hiido.utils;

import com.google.common.base.Joiner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.security.UserGroupInformation;
import org.assertj.core.util.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class HdfsUtils {
    private FileSystem fileSystem;

    public HdfsUtils(String user, String keytab, String principle, String krb5) {
        try {
            Configuration configuration = new Configuration();
            log.info("set dfs.namenode.kerberos.principal:" + principle);
            configuration.set("dfs.namenode.kerberos.principal", principle);
            if (StringUtils.isNotEmpty(krb5)) {
                log.info("java.security.krb5.conf:" + krb5);
                System.setProperty("java.security.krb5.conf", krb5);
            } else {
                System.setProperty("java.security.krb5.realm", "YYDEVOPS.COM");
                System.setProperty("java.security.krb5.kdc", "fs-hiido-kerberos-21-117-149.hiido.host.yydevops.com");
            }
            UserGroupInformation.setConfiguration(configuration);
            log.info("set user:" + user + ", keytab:" + keytab);
            UserGroupInformation.loginUserFromKeytab(user, keytab);
            this.fileSystem = FileSystem.get(configuration);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
    }

    public FileSystem fs(String path) throws IOException {
        return new Path(path).getFileSystem(new Configuration());
    }

    public double getFileSize(String filename) throws Exception {
        log.info("- - - - - - - Get File Size of {} - - - - - - - ", filename);
        Path path = new Path(filename);
        FileSystem system = fs(filename);
        ContentSummary contentSummary = system.getContentSummary(path);
        return Math.ceil(contentSummary.getLength() / 1024.0 / 1024.0);
    }


    public boolean distcp(String srcPath, String dstPath, int mapTask) {
        try {
            CmdUtils cmdUtils = new CmdUtils();
            String cmd = String.format("hadoop distcp -Dipc.client.fallback-to-simple-auth-allowed=true -Ddfs.checksum.type=CRC32C -m %s -update -p %s %s", mapTask, srcPath, dstPath);
            log.info(cmd);
            int code = cmdUtils.execute(cmd.split("\\s+"), log);
            if (code != 0) {
                log.error(cmd + " --- failed with code {}", code);
                return false;
            }
            log.info("- -- - - - - -- - - - fileSystem copy finished ,now check them all -- - - -- - -- - - -  !!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<FileStatus> listFileStatus(String file, boolean recusive) throws IOException {
        Path path = new Path(file);
        final RemoteIterator<LocatedFileStatus> iterator = fileSystem.listFiles(path, recusive);
        final List<FileStatus> fileList = new ArrayList<>();
        while (iterator.hasNext()) {
            fileList.add(iterator.next());
        }
        return fileList;
    }

    public boolean checkFilesOfTwoPath(List<String> srcPathList, String dstPath) {
        try {
            log.info("- - - - - - - Check File Num of Two Path - - - - - - -");
            log.info("SrcPath: {}", Joiner.on(",").join(srcPathList));
            log.info("DstPath: {}", dstPath);
            List<FileStatus> srcFiles = Lists.newArrayList();
            for (String path : srcPathList) {
                srcFiles.addAll(listFileStatus(path, true));
            }
            log.info("File Num of Input is {}", srcFiles.size());
            List<FileStatus> dstFiles = listFileStatus(dstPath, true);
            log.info("File Num of OutPut is {}", dstFiles.size());
            if (srcFiles.size() != dstFiles.size()) {
                log.error("File Num of Two Pathes is Not Same !!!");
                return false;
            } else {
                log.info("File Num of Two Pathes is Same ~~~");
            }
            for (FileStatus srcFile : srcFiles) {
                for (FileStatus dstFile : dstFiles) {
                    double srcFileSize = getFileSize(srcFile.getPath().getName());
                    double dstFileSize = getFileSize(dstFile.getPath().getName());
                    if (Math.abs(srcFileSize - dstFileSize) > 1) {
                        log.info("{} size :", srcFile.getPath());
                        log.info("{} size :", dstFile.getPath());
                        log.error("$ $ Two File Has Diff Size $ $");
                        return false;
                    }
                }
            }
            log.info("- - - - - - - - - Copy Data Success ,All Files Are Same - - - - - - - - - ");
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

}
