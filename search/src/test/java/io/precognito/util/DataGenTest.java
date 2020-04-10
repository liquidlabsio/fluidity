package io.precognito.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.util.Date;

public class DataGenTest {
    @Test
    public void testGenerateTestData() throws Exception {
        if (true) return;

        String prefix = "/Volumes/SSD2/logs/precog-logs/test-cpu-olay";
        FileOutputStream fos = new FileOutputStream(prefix + new Date().getTime() + ".log");

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm.ss");

        // 1 days of CPU data
        long time = System.currentTimeMillis() - (DateUtil.DAY * 1);
        int num = 0;
        while (time < System.currentTimeMillis()) {
            double cpu = num  + 5 * Math.random();
            fos.write(String.format("%s INFO CPU:%d\n", dateTimeFormatter.print(time), Double.valueOf(cpu).intValue()).getBytes());
            time += DateUtil.MINUTE;
            num++;
            if (num > 95) num = 0;
        }
        fos.close();
    }

}
