/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Set<String> dirFilters = new HashSet<>(Arrays.asList(pathFilters));

        try (Stream<Path> walk = Files.walk(Paths.get(baseDir))) {
            return walk.filter(Files::isRegularFile)
                    .filter(x -> filenameAndPathMatches(extension, dirFilters, x))
                    .map(x -> x.toFile()).collect(Collectors.toList());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static boolean filenameAndPathMatches(String extension, Set<String> dirFilters, Path x) {
        String filename = x.toString();
        if (isPathMatch(dirFilters, filename)) {
            return (extension.equals("*") || filename.endsWith(extension));
        } else {
            return false;
        }
    }

    private static boolean isPathMatch(Set<String> dirFilters, String filename) {
        return dirFilters.stream().filter(dirFilter -> filename.contains(dirFilter)).count() == dirFilters.size();
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
