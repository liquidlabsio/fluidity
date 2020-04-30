package io.fluidity.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Will extract tag or group attributes using:
 * groupBy(tags)
 * groupBy(path)
 * groupBy(path[0])
 * groupBy(path[1-3])
 */
public class GroupByExtractor {
    public static final String GROUP_BY_PATH = "groupBy(path";
    private final String groupBy;
    private String expression;

    public GroupByExtractor(String expression) {

        this.expression = expression;
        String[] split = expression.split("\\|");
        groupBy = (split.length > Search.EXPRESSION_PARTS.groupby.ordinal() ? split[Search.EXPRESSION_PARTS.groupby.ordinal()].trim() : "").trim();
    }

    public String applyGrouping(String tags, String sourceName) {
        if (groupBy.equals("groupBy(tag)")) {
            return tags;
        }
        if (groupBy.startsWith(GROUP_BY_PATH)) {
            return processPathExpression(groupBy.substring(GROUP_BY_PATH.length()), sourceName);
        }
        return "";
    }

    /**
     * Expecting values like
     * path[1]
     * path[1-3]
     * @param pathPath
     * @param path
     * @return
     */
    transient String splitPathSource;
    transient List<String> splitPath;
    transient List<Integer> splitCommand;
    transient boolean isSplitLast;

    private String processPathExpression(String pathPath, String path) {

        try {
            if (pathPath.endsWith(")")) pathPath = pathPath.substring(0, pathPath.length() - 1);
            if (splitPathSource == null || !splitPathSource.equals(path)) {
                splitPathSource = path;
                splitPath = Arrays.asList(path.split("/"));
            }
            if (splitCommand == null) {
                if (pathPath.length() > 0) {
                    pathPath = pathPath.replace("[", "");
                    pathPath = pathPath.replace("]", "");
                    String[] split = pathPath.split("-");
                    if (split.length == 1 && split[0].equals("last")) {
                        splitCommand = Collections.emptyList();
                        isSplitLast = true;
                    } else{
                        splitCommand = Arrays.stream(split).map(item -> Integer.parseInt(item.trim())).collect(Collectors.toList());
                    }
                } else {
                    splitCommand = Collections.emptyList();
                }
            }
            if (isSplitLast) {
                return splitPath.get(splitPath.size()-1);
            }
            if (splitCommand.size() == 1) {
                return splitPath.get(splitCommand.get(0));
            }
            if (splitCommand.size() == 2) {
                return splitPath.subList(splitCommand.get(0), splitCommand.get(1) + 1).stream().collect(Collectors.joining("-"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            splitCommand = Collections.emptyList();
        }
        return path;
    }

}
