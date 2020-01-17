package com.logscapeng.uploader;

import javax.inject.Singleton;
import java.util.List;

public interface FileMetaDataQueryService {
    void createTable();

    void put(FileMeta fileMeta);

    FileMeta find(String tenant, String filename);

    //TODO: get content needs to be removed to projection providers (i.e. deletegate to appropriate projection layer)
    byte[] get(String tenant, String filename);

    FileMeta delete(String tenant, String filename);

    List<FileMeta> query(String tenant, String filenamePart, String tagNamePart);

    List<FileMeta> list();
}
