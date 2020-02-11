package io.precognito.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
    public static byte[] readFileToByteArray(final File file, int limit) throws IOException {
        try (InputStream in = FileUtils.openInputStream(file)) {
            final long fileLength = file.length();
            final long amount = Math.min(fileLength, limit);
            // file.length() may return 0 for system-dependent entities, treat 0 as unknown length - see IO-453
            return amount > 0 ? IOUtils.toByteArray(in, fileLength) : IOUtils.toByteArray(in);
        }
    }
}
