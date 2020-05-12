package io.fluidity.dataflow;

public class Model {

    public final static String DELIM = "_";
    // correlation-start-end
    public final static String CORR_FILE_FMT = "%s/corr_%s_%d_%d_.log";
    public final static String CORR_PREFIX = "/corr_";

    public final static String CORR_DAT_FMT = "%s/dat_%s_%d_%d_.dat";
    public final static String CORR_DAT_PREFIX = "/dat_";

    public final static String CORR_FLOW_FMT = "%s/flow_%s_%d_%d_.dat";
    public final static String CORR_FLOW_PREFIX = "/flow_";

    public final static String CORR_HIST_FMT = "%s/histo_%d_%d_.histo";
    public final static String CORR_HIST_PREFIX = "/histo_";
}
