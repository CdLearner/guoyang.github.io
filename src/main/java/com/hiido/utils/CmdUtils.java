package com.hiido.utils;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.util.List;


@Slf4j
public class CmdUtils {


    public static class CmdResponse {
        int code;
        List<String> stdout;
        List<String> stderr;

        public CmdResponse() {
            code = 0;
            stderr = Lists.newArrayList();
            stdout = Lists.newArrayList();
        }
    }

    ProcessExecutor executor = new ProcessExecutor();
    private Runtime runtime = Runtime.getRuntime();
    private Process process;
    private BufferedReader br = null;
    private BufferedWriter wr = null;
    private CmdResponse res;

    public CmdResponse runCammand(String cmd) throws IOException {
        res = new CmdResponse();
        log.info("Cammand:('" + cmd + "')");
        try {
            process = runtime.exec(cmd);
            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            wr = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            String inline;
            while ((inline = br.readLine()) != null) {
                res.stdout.add(inline);
                log.info(inline);
            }
            br.close();
            //错误信息
            br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((inline = br.readLine()) != null) {
                res.stderr.add(inline);
                log.error(inline);
            }
            process.waitFor();
            res.code = process.exitValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (br != null) {
                br.close();
            }
            if (wr != null) {
                wr.close();
            }
            return res;
        }
    }

    public int execute(String cmd, org.slf4j.Logger logger) {

        try {
            return executor.command(cmd).redirectOutput(Slf4jStream.of(logger).asInfo()).execute().getExitValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 1;
        }
    }
    public int execute(String[] cmd, org.slf4j.Logger logger) {

        try {
            return executor.command(cmd).redirectOutput(Slf4jStream.of(logger).asInfo()).execute().getExitValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 1;
        }
    }
    public int execute(List<String> cmd, org.slf4j.Logger logger) {

        try {
            return executor.command(cmd).redirectOutput(Slf4jStream.of(logger).asInfo()).execute().getExitValue();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 1;
        }
    }

}
