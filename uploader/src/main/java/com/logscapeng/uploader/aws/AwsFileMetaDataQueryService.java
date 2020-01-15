package com.logscapeng.uploader.aws;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.FileMetaDataQueryService;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.QueryConditional.equalTo;

@ApplicationScoped
public class AwsFileMetaDataQueryService implements FileMetaDataQueryService {

    @Inject
    DynamoDbClient dynamoDbClient;

    private MappedDatabase database;
    private MappedTable<FileMeta> fileMetaTable;

    public AwsFileMetaDataQueryService() {
    }

    @Override
    public void createTable(){
        if (!tableExists()) {
            ProvisionedThroughput.Builder provisionedThroughput = ProvisionedThroughput.builder().readCapacityUnits(10l).writeCapacityUnits(10L);
            CreateTable<FileMeta> fileMetaCreateTable = CreateTable.of(provisionedThroughput.build()).toBuilder().build();
//            CreateTable<FileMeta> fileMetaCreateTable = CreateTable.create();
            MappedTable<FileMeta> table = getTable();
            table.execute(fileMetaCreateTable);
        }
    }

    private boolean tableExists() {
        ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
        HashSet<String> strings = new HashSet<>(listTablesResponse.tableNames());
        return strings.contains(FileMeta.class.getSimpleName());
    }

    @Override
    public void put(FileMeta fileMeta) {
        getTable().execute(PutItem.of(fileMeta));
    }
    @Override
    public FileMeta get(String tenant, String filename) {
        return getTable().execute(GetItem.of(Model.getKey(tenant, filename)));
    }
    @Override
    public FileMeta delete(String tenant, String filename) {
        return getTable().execute(DeleteItem.of(Model.getKey(tenant, filename)));
    }

    /**
     * Efficiently query against a single-tenant and apply filter
     * Note:
     * - Primary Key 'equalTo' is mandatory
     * - Primary Sort Key cannot be filtered
     * Pretty crappy!
     *
     * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html
     * @param tenant
     * @param filenamePart
     * @param tagNamePart
     * @return
     */
    @Override
    public List<FileMeta> query(final String tenant, final String filenamePart, String tagNamePart) {

        MappedTable<FileMeta> table = getTable();

        Query<FileMeta> build = Query.builder()
                .queryConditional(equalTo(Key.of(stringValue(tenant))))
                .build();
        Iterator<Page<FileMeta>> files = table.execute(build).iterator();


        // apply filtering here
        ArrayList<FileMeta> fileMetas = new ArrayList<>();
        files.forEachRemaining(action ->
                    action.items().stream().filter(
                            item -> item.isMatch(filenamePart, tagNamePart)
                    ).forEach(item -> fileMetas.add(item)
                ));
        return fileMetas;
    }

    /**
     * Scan performs a full table scan making it very expensive for large tables; filter applied after the scan
     * Pretty crappy!
     * @return
     */
    @Override
    public List<FileMeta> list() {
        Iterable<Page<FileMeta>> files = getTable().execute(Scan.create());
        ArrayList<FileMeta> fileMetas = new ArrayList<>();
        files.iterator().forEachRemaining(action -> fileMetas.addAll(action.items()));
        return fileMetas;
    }

    /**
     * MappedTable object is used repeatedly execute operations against a specific table
     * @return
     */
    private MappedTable<FileMeta> getTable() {
        if (fileMetaTable == null) {
            fileMetaTable = getDatabase().table(FileMeta.class.getSimpleName(), Model.FILE_META_TABLE_SCHEMA);
        }
        return fileMetaTable;
    }

    private MappedDatabase getDatabase() {
        if (database == null) {
            database = MappedDatabase.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();
        }
        return database;
    }


    /**
     * Testing only
     * @param client
     */
    public void setClient(DynamoDbClient client) {
        this.dynamoDbClient = client;
    }
}
