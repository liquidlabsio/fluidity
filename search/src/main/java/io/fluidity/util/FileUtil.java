package io.fluidity.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {
    public static byte[] readFileToByteArray(final File file, int limit) throws IOException {
        try (InputStream in = FileUtils.openInputStream(file)) {
            long fileLength = file.length();
            if (fileLength > 0 && limit > 0 && fileLength > limit) fileLength = limit;
            // file.length() may return 0 for system-dependent entities, treat 0 as unknown length - see IO-453
            return fileLength > 0 ? IOUtils.toByteArray(in, fileLength) : IOUtils.toByteArray(in);
        }
    }

    public static void writeFile(String filename, byte[] fileContent) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(file, fileContent);
    }

    public static Collection<File> listDirs(String baseDir, final String extension, final String... pathFilters) {
        List<String> dirFilters = Arrays.asList(pathFilters);
        Collection<File> files = FileUtils.listFiles(new File(baseDir), new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.getName().endsWith(extension) || extension.equals("*")) {
                    String path = file.getPath();
                    List<String> matchedFilters = dirFilters.stream().filter(dirFilter -> path.contains(dirFilter)).collect(Collectors.toList());
                    return matchedFilters.size() == dirFilters.size();
                }
                return false;
            }
            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        }, new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return true;
            }

            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        });
        return files;
    }

    /**
     * Replace windows path delimiters with unix style
     *
     * @param path
     * @return
     */
    public static String fixPath(String path) {
        return path.replace('\\', '/');
    }
}
