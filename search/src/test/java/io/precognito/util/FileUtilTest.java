package io.precognito.util;

import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;


class FileUtilTest {

    @Test
    void listDirs() throws IOException {

        String baseDir = "./target/fs-util-test" + System.currentTimeMillis();
        String firstDir = baseDir + "/tenant-1/part2/";
        FileUtil.writeFile(new File(firstDir, "file1.log").getPath(), "hello1".getBytes());
        FileUtil.writeFile(new File(firstDir, "file2.log").getPath(), "hello1".getBytes());
        FileUtil.writeFile(new File(firstDir, "file3.log").getPath(), "hello1".getBytes());

        Collection<File> files = FileUtil.listDirs(baseDir, ".log", "tenant", "part2");
        Assert.assertTrue(files.size() == 3);
    }
}