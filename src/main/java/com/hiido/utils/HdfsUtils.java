package com.hiido.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Data
public class HdfsUtils {

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
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }
    }

    public void mkdir(String path) throws Exception {
        FileSystem fs = fs(path);
        Path p = new Path(path);
        if (!fs.exists(p)) {
            log.info("{} Does Not Exist,Now Create It", path);
            fs.mkdirs(p, FsPermission.getCachePoolDefault());
            fs.setPermission(p, FsPermission.getCachePoolDefault());
        }
    }

    public String getParentDir(String file, boolean rename) throws Exception {
        if (rename) {
            return file;
        }
        if (StringUtils.isEmpty(file)) {
            log.error("Can Not Get Parent Dir of {}", file);
            return file;
        }
        Path path = new Path(file);
        if (fs(file).isDirectory(path)) {
            return path.getName();
        } else if (fs(file).isFile(path)) {
            return getParentDir(path.getParent().toString(), rename);
        } else {
            log.error("Can Not Get Parent Dir of {}", file);
            return file;
        }
    }


    public void addSuffix(String file, String suffix, boolean rename) throws Exception {
        if (!rename) {
            return;
        }
        List<FileStatus> fileStatuses = listFileStatus(file, true);
        for (FileStatus fileStatus : fileStatuses) {
            Path origin = fileStatus.getPath();
            if (origin.toString().contains("$" + suffix)) {
                continue;
            }
            Path target = new Path(origin.toString() + "$" + suffix);
            log.info("Rename File {} to {}", origin.toString(), target.toString());
            fs(file).rename(origin, target);
        }
    }

    public void removeSuffix(String file, String suffix, boolean rename) throws Exception {
        if (!rename) {
            return;
        }
        List<FileStatus> fileStatuses = listFileStatus(file, true);
        for (FileStatus fileStatus : fileStatuses) {
            Path origin = fileStatus.getPath();
            if (!origin.toString().contains("$" + suffix)) {
                continue;
            }
            Path target = new Path(origin.toString().replace("$" + suffix, ""));
            log.info("Resume File {} to {}", origin.toString(), target.toString());
            fs(file).rename(origin, target);
        }
    }




    public FileSystem fs(String path) throws IOException {
        return new Path(path).getFileSystem(new Configuration());
    }


    public double getFileSize(String filename) throws Exception {
        log.info("- - - - - - - Get File Size of {} - - - - - - - ", filename);
        FileStatus[] fileStatuses = fs(filename).globStatus(new Path(filename));
        double size = 0.0;
        for (FileStatus fileStatus : fileStatuses) {
            if (fileStatus.isFile()) {
                size += fileStatus.getLen();
            }
        }
        log.info("File Size of {} is {}", filename, size);
        size = size / 1024.0 / 1024.0;
        return size;
    }


    public boolean distcp(String binPath, String srcPath, String dstPath, int mapTask, Map<String, String> env, String queeue, String name) {
        try {
            CmdUtils cmdUtils = new CmdUtils();
            String cmd = String.format("%s/hadoop distcp -Dmapreduce.job.name=%s -Dmapreduce.job.queuename=%s  -Dipc.client.fallback-to-simple-auth-allowed=true -Ddfs.checksum.type=CRC32C -m %s -update  %s %s", binPath, name, queeue, mapTask, srcPath, dstPath);
            log.info(cmd);
            int code = cmdUtils.execute(cmd.split("\\s+"), log, env);
            if (code != 0) {
                log.error(cmd + "\n --- Failed With Code {}", code);
                return false;
            }
            log.info("- -- - - - - -- - - - Files Copy Finished ,Now Check Them All -- - - -- - -- - - -  !!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<FileStatus> listFileStatus(String file, boolean recusive) throws Exception {
        Path path = new Path(file);
        if (file.contains("*")) {
            FileStatus[] fileStatuses = fs(file).globStatus(path);
            return Arrays.stream(fileStatuses).collect(Collectors.toList());
        } else {
            RemoteIterator<LocatedFileStatus> iterator = fs(file).listFiles(new Path(file), recusive);
            ArrayList<FileStatus> res = Lists.newArrayList();
            while (iterator.hasNext()) {
                res.add(iterator.next());
            }
            return res;
        }

    }

    public void checkInputFileNum(List<String> srcPathList) throws Exception {
        int sum = 0;
        for (String path : srcPathList) {
            sum += listFileStatus(path, true).size();
        }
        log.info("File Num of Input is {}", sum);
    }


    public boolean checkFilesOfTwoPath(List<String> srcPathList, String dstPath, boolean rename) {
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

            for (FileStatus srcFile : srcFiles) {
                String filename = srcFile.getPath().getName();
                String postfix = srcFile.getPath().getParent().getName();
                String targetFile = rename ? filename + postfix : filename;
                boolean has = false;
                for (FileStatus dstFile : dstFiles) {
                    if (dstFile.getPath().getName().equals(targetFile)) {
                        has = true;
                        double srcFileSize = getFileSize(srcFile.getPath().toString());
                        double dstFileSize = getFileSize(dstFile.getPath().toString());
                        log.info("{} size :{}", srcFile.getPath(), srcFileSize);
                        log.info("{} size :{}", dstFile.getPath(), dstFileSize);
                        if (Math.abs(srcFileSize - dstFileSize) > 1) {
                            log.error("$ $ Two File Has Diff Size $ $");
                            return false;
                        }
                        log.info("- - - - - - - - - - - - - - - - - - - - - - - -");
                    }
                }
                if (!has) {
                    log.error("{} Is Not Found In {}", srcFile.getPath().toString(), dstPath);
                    return false;
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
