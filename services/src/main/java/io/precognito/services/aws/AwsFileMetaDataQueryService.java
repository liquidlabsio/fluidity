package io.precognito.services.aws;

import io.precognito.services.query.FileMeta;
import io.precognito.services.query.FileMetaDataQueryService;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static io.precognito.services.aws.Model.FILE_META_TABLE_SCHEMA;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.QueryConditional.equalTo;

@ApplicationScoped
public class AwsFileMetaDataQueryService implements FileMetaDataQueryService {

    private final Logger log = LoggerFactory.getLogger(AwsFileMetaDataQueryService.class);

    @Inject
    DynamoDbClient dynamoDbClient;

    @ConfigProperty(name = "precognito.prefix", defaultValue = "precognito.")
    String PREFIX;

    private MappedDatabase database;
    private MappedTable<FileMeta> fileMetaTable;


    public AwsFileMetaDataQueryService() {
        log.info("Created");
    }

    private boolean created = false;
    synchronized public void createTable(){
        if (!created && !tableExists()) {
            created = true;
            log.info("Creating table:" + getTableName());
            ProvisionedThroughput.Builder provisionedThroughput = ProvisionedThroughput.builder().readCapacityUnits(10L).writeCapacityUnits(10L);
            CreateTable<FileMeta> fileMetaCreateTable = CreateTable.of(provisionedThroughput.build()).toBuilder().build();
            MappedTable<FileMeta> table = getTable();
            table.execute(fileMetaCreateTable);
            // cannot use the table while it is being created - triggers errors when querying
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
    public void put(FileMeta fileMeta) {
        getTable().execute(PutItem.of(fileMeta));
    }

    @Override
    public FileMeta find(String tenant, String filename) {
        return getTable().execute(GetItem.of(Model.getKey(tenant, filename)));
    }

    @Override
    public byte[] get(String tenant, String filename) {
        FileMeta execute = getTable().execute(GetItem.of(Model.getKey(tenant, filename)));
        // TODO: fix content getting - content should be from the storage projection view - not here - it needs to be injected and delegated
        return execute.getStorageUrl().getBytes();
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
        bind();
        createTable();

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
            fileMetaTable = getDatabase().table(getTableName(), FILE_META_TABLE_SCHEMA);
        }
        return fileMetaTable;
    }

    private String getTableName() {
        return PREFIX + "."  + FileMeta.class.getSimpleName();
    }

    private MappedDatabase getDatabase() {

        if (database == null) {
            database = MappedDatabase.builder()
                    .dynamoDbClient(bind())
                    .build();
        }
        return database;
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
            PREFIX = ConfigProvider.getConfig().getValue("precognito.prefix", String.class);
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
}
