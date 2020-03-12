package io.precognito.services.query;

import java.util.List;

public interface FileMetaDataQueryService {

    void putList(List<FileMeta> fileMetas);

    void put(FileMeta fileMeta);

    FileMeta find(String tenant, String filename);

    //TODO: get content needs to be removed to projection providers (i.e. delegate to appropriate projection layer)
    byte[] get(String tenant, String filename, int offset);

    FileMeta delete(String tenant, String filename);

    List<FileMeta> query(String tenant, String filenamePart, String tagNamePart);

    List<FileMeta> list();

    void deleteList(List<FileMeta> removed);
}