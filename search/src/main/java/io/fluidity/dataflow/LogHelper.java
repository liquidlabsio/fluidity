package io.fluidity.dataflow;

public class LogHelper {

    public static String DF_FORMAT = "\"corr\":\"%s\", \"service\": \"%s\" \"operation\": \"%s\" \"msg\": \"%s\"";

    public static String format(String correlation, String service, String operation, String logMsg) {
        return String.format(DF_FORMAT, correlation, service, operation, logMsg);
    }
}
