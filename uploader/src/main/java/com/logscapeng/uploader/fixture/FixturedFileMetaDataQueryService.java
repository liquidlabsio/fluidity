package com.logscapeng.uploader.fixture;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.FileMetaDataQueryService;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FixturedFileMetaDataQueryService implements FileMetaDataQueryService {

    public static final Map<String, FileMeta> storage = new HashMap<>();

    @Override
    public void createTable() {
    }

    @Override
    public void put(FileMeta fileMeta) {
        storage.put(fileMeta.filename, fileMeta);
    }

    @Override
    public FileMeta get(String tenant, String filename) {
        return storage.get(filename);
    }

    @Override
    public FileMeta delete(String tenant, String filename) {
        return storage.remove(filename);
    }

    @Override
    public List<FileMeta> query(String tenant, String filenamePart, String tagNamePart) {
        return storage.values()
                .stream().filter(entry -> entry.getFilename().contains(filenamePart))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileMeta> list() {
        return new ArrayList<>(storage.values());
    }
}
