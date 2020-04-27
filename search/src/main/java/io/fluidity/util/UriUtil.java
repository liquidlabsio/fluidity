package io.fluidity.util;

public class UriUtil {

    public static String cleanPath(String url) {
        url = url.replace("(", "%28").replace(")", "'%29");
        url = url.replace("[", "%5B").replace("]", "'%5D");
        url = url.replace("$", "%24");
        return url;
    }

    public static String[] getHostnameAndPath(String url) {
        if (isWindowsVersion(url)) {
            int beginIndex = url.indexOf(":\\") + 2;
            int toIndex = url.indexOf("\\", beginIndex);
            String hostname = url.substring(beginIndex, toIndex);
            String path = url.substring(toIndex + 1);
            return new String[]{hostname, path};

        } else {
            int beginIndex = url.indexOf("//") + 2;
            int toIndex = url.indexOf("/", beginIndex);
            String hostname = url.substring(beginIndex, toIndex);
            String path = url.substring(toIndex + 1);
            return new String[]{hostname, path};
        }
    }

    private static boolean isWindowsVersion(String url) {
        return url.contains(":\\");
    }

}
