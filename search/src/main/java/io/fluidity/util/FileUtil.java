/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {

    public static long inferFakeStartTimeFromSize(long size, long lastModified) {
        if (size <= 0) return lastModified - DateUtil.HOUR;
        int fudgeLineLength = 128;
        int fudgeLineCount = (int) (size / fudgeLineLength);
        long fudgedTimeIntervalPerLineMs = 100;
        long startTimeOffset = fudgedTimeIntervalPerLineMs * fudgeLineCount;
        if (startTimeOffset < DateUtil.MINUTE) startTimeOffset = DateUtil.MINUTE;
        return lastModified - startTimeOffset;
    }

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

    public static List<File> listDirs(String baseDir, final String extension, final String... pathFilters) {
        String[] dirFilters = pathFilters;

        try (Stream<Path> walk = Files.walk(Paths.get(baseDir))) {
            return walk.filter(Files::isRegularFile)
                    .filter(x -> filenameAndPathMatches(extension, dirFilters, x))
                    .map(x -> x.toFile()).collect(Collectors.toList());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static boolean filenameAndPathMatches(String extension, String[] dirFilters, Path x) {
        String filename = x.toString();
        return (extension.equals("*") || filename.endsWith(extension)) && isPathMatch(dirFilters, filename);
    }

    private static boolean isPathMatch(String[] dirFilters, String filename) {
        for (int i = 0; i < dirFilters.length; i++) {
            if (filename.indexOf(dirFilters[i]) == -1) return false;
        }
        return true;
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
