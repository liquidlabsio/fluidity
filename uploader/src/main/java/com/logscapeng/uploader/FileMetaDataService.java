package com.logscapeng.uploader;

import java.util.List;

public interface FileMetaDataService {
    void createTable();

    void put(FileMeta fileMeta);

    FileMeta get(String tenant, String filename);

    FileMeta delete(String tenant, String filename);

    List<FileMeta> query(String tenant, String filenamePart, String tagNamePart);

    List<FileMeta> list();
}
