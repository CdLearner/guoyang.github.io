package com.hiido;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
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
        FileSystem fileSystem = FileSystem.get(new Configuration());
        FileStatus[] fileStatuses = fileSystem.globStatus(new Path("/tmp/a*"));
        for (FileStatus fileStatus : fileStatuses) {
            if (fileStatus.isFile()) {
                log.info(fileStatus.getPath().toString() + ":" + fileStatus.getLen());
            }
        }
    }
}
