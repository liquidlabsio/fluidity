package io.fluidity.services.fixture;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.StorageIndexer;
import io.fluidity.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FixturedIndexerService implements StorageIndexer {

    private final Logger log = LoggerFactory.getLogger(FixturedIndexerService.class);

    public FixturedIndexerService(){
        log.info("Created");
    }
    @Override
    public FileMeta enrichMeta(FileMeta fileMeta) {
        return fileMeta;
    }

    /**
     * TODO: perform real time-range extraction.
     * @param fileMeta
     * @param cloudRegion
     * @return
     */
    @Override
    public FileMeta index(FileMeta fileMeta, String cloudRegion) {
        if (fileMeta.getToTime() == 0) {
            fileMeta.setToTime(System.currentTimeMillis());
            fileMeta.setFromTime(fileMeta.getToTime() - DateUtil.HOUR);
        }

        if (fileMeta.fileContent != null && fileMeta.fileContent.length > 0) {
            getStartTimeFromLengthAndLastMod(fileMeta);
        } else {
            fileMeta.setFromTime(fileMeta.getToTime() - DateUtil.HOUR);
        }

        return fileMeta;
    }

    private void getStartTimeFromLengthAndLastMod(FileMeta fileMeta) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(fileMeta.fileContent));
        List<String> lines = new ArrayList<>();
        int lineLengths = 0;
        while (scanner.hasNextLine()) {
            String nextLine = scanner.nextLine();
            lines.add(nextLine);
            lineLengths += nextLine.length();
        }
        int avgLineLength = lineLengths/lines.size();
        int estimateLines = (int) (fileMeta.size/avgLineLength);
        if (estimateLines == 0) estimateLines = 10;
        long lineInterval = 100;
        fileMeta.setFromTime(fileMeta.toTime - estimateLines * lineInterval);
    }
}
