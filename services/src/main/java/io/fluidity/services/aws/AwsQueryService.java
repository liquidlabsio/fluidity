package io.fluidity.services.aws;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.QueryService;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryConditional.equalTo;

public class AwsQueryService implements QueryService {
    public static final int BATCH_LIMIT = 24;

    public static final int BATCH_PAUSE_MS = 50;
    public static final long READ_CAPACITY = 30L;
    public static final long WRITE_CAPACITY = 15L;
    private final Logger log = LoggerFactory.getLogger(AwsQueryService.class);

    @Inject
    DynamoDbClient dynamoDbClient;

    @ConfigProperty(name = "fluidity.prefix", defaultValue = "fluidity.")
    String PREFIX;

    private DynamoDbTable<FileMeta> fileMetaTable;
    private DynamoDbEnhancedClient enhancedClient;


    public AwsQueryService() {
        new RuntimeException().printStackTrace();
        log.info("Created");
    }

    private boolean created = false;

    synchronized public void createTable() {
        if (!created && !tableExists()) {

            getTable();
            created = true;
            log.info("Creating table:" + getTableName());

            fileMetaTable.createTable(CreateTableEnhancedRequest.builder().provisionedThroughput(
                    ProvisionedThroughput.builder().readCapacityUnits(READ_CAPACITY).writeCapacityUnits(WRITE_CAPACITY).build()).build());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean tableExists() {
        ListTablesResponse listTablesResponse = dynamoDbClient.listTables();
        HashSet<String> strings = new HashSet<>(listTablesResponse.tableNames());
        return strings.contains(getTableName());
    }

    @Override
    public void putList(List<FileMeta> fileMetas) {
        ArrayList<FileMeta> batch = new ArrayList<>();

        // break into chunks of 25
        fileMetas.forEach(item -> {
            batch.add(item);
            if (batch.size() > BATCH_LIMIT) {
                try {
                    putListBatch(batch);
                    Thread.sleep(BATCH_PAUSE_MS);
                } catch (Exception e) {
                    log.error("Failed to execute batch:{}", e.toString(), e);
                    e.printStackTrace();
                } finally {
                    batch.clear();
                }

            }
        });
        putListBatch(batch);
    }

    private void putListBatch(List<FileMeta> fileMeta) {

        log.info("Write:" + fileMeta.size());

        if (fileMeta.size() > 0) {

            WriteBatch.Builder<FileMeta> fileMetaBuilder = WriteBatch.builder(FileMeta.class)
                    .mappedTableResource(getTable());
            fileMeta.forEach(item -> fileMetaBuilder.addPutItem(PutItemEnhancedRequest.builder(FileMeta.class).item(item).build()));

            enhancedClient.batchWriteItem(
                    BatchWriteItemEnhancedRequest.builder()
                            .addWriteBatch(fileMetaBuilder.build()).build());
        }
    }

    @Override
    public void put(FileMeta fileMeta) {
        getTable().putItem(PutItemEnhancedRequest.builder(FileMeta.class)
                .item(fileMeta)
                .build());
    }

    @Override
    public void deleteList(final List<FileMeta> fileMetas) {

        /**
         * Filter the remove list to those items listed in our DB to prevent - noops
         */
        SdkIterable<Page<FileMeta>> existingFileMetasPage = getTable().scan(
                ScanEnhancedRequest.builder().build());

        ArrayList<FileMeta> existingFileMetas = new ArrayList<>();
        existingFileMetasPage.forEach(action -> existingFileMetas.addAll(action.items()));

        List<FileMeta> matchedList = existingFileMetas.stream().filter(item -> fileMetas.contains(item)).collect(Collectors.toList());

        ArrayList<FileMeta> batch = new ArrayList<>();

        // break into chunks of 25
        matchedList.forEach(item -> {
            batch.add(item);
            if (batch.size() > BATCH_LIMIT) {
                try {
                    deleteListBatch(batch);
                    batch.clear();
                    Thread.sleep(BATCH_PAUSE_MS);
                } catch (Exception e) {
                    log.error("Failed to execute batch:{}", e.toString(), e);
                    e.printStackTrace();
                }
            }
        });
        deleteListBatch(batch);
    }


    private void deleteListBatch(List<FileMeta> removed) {
        if (removed.size() > 0) {

            log.info("Deleting:{}", removed.size());

            WriteBatch.Builder<FileMeta> writeBatchBuilder = WriteBatch.builder(FileMeta.class)
                    .mappedTableResource(getTable());
            removed.forEach(item -> writeBatchBuilder.addDeleteItem(
                    DeleteItemEnhancedRequest.builder().key(Model.getKey(item.tenant, item.filename)).build()
            ));

            enhancedClient.batchWriteItem(
                    BatchWriteItemEnhancedRequest.builder()
                            .addWriteBatch(
                                    writeBatchBuilder.build())
                            .build());
        }

    }


    @Override
    public FileMeta delete(String tenant, String filename) {
        return getTable().deleteItem(DeleteItemEnhancedRequest.builder()
                .key(Model.getKey(tenant, filename))
                .build());
    }


    @Override
    public FileMeta find(String tenant, String filename) {
        return getTable().getItem(GetItemEnhancedRequest.builder()
                .key(Model.getKey(tenant, filename))
                .build());
    }

    @Override
    public byte[] get(String tenant, String filename, int offset) {
        FileMeta fileMeta = find(tenant, filename);
        // TODO: fix content getting - content should be from the storage projection view - not here - it needs to be injected and delegated
        return fileMeta.getStorageUrl().getBytes();
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

        SdkIterable<Page<FileMeta>> query = getTable().query(QueryEnhancedRequest.builder()
                .queryConditional(equalTo(Key.create(stringValue(tenant))))
                .build());

        // apply filtering here
        ArrayList<FileMeta> fileMetas = new ArrayList<>();
        query.forEach(action ->
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
        bind();
        createTable();

        SdkIterable<Page<FileMeta>> fileMetas = getTable().scan(ScanEnhancedRequest.builder().build());

        ArrayList<FileMeta> results = new ArrayList<>();
        fileMetas.forEach(action -> results.addAll(action.items()));
        return results;
    }

    /**
     * MappedTable object is used repeatedly execute operations against a specific table
     *
     * @return
     */
    private DynamoDbTable<FileMeta> getTable() {
        bind();
        if (fileMetaTable == null) {
            fileMetaTable = enhancedClient.table(getTableName(), Model.FILE_META_TABLE_SCHEMA);
        }
        return fileMetaTable;
    }

    private String getTableName() {
        return PREFIX + "."  + FileMeta.class.getSimpleName();
    }


    /**
     * Required because the Convertor factory does not create instances using the bean-factory
     * @return
     */
    public synchronized DynamoDbClient bind() {
        if (dynamoDbClient == null) {
            log.info("Binding to CDI Beans");
            BeanManager beanManager = CDI.current().getBeanManager();
            dynamoDbClient = (DynamoDbClient) beanManager.getBeans(DynamoDbClient.class).iterator().next().create(null);
            if (dynamoDbClient == null) {
                throw new RuntimeException("Failed to late-bind DynamoDBClient - check QueryFactoryConvertor config in application.[properties/yaml]");
            }
            PREFIX = ConfigProvider.getConfig().getValue("fluidity.prefix", String.class);
        }
        if (enhancedClient == null) {
            enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();
        }

        return dynamoDbClient;
    }


    /**
     * Testing only
     * @param client
     */
    public void setClient(DynamoDbClient client) {
        this.dynamoDbClient = client;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        dynamoDbClient.close();
    }
}
