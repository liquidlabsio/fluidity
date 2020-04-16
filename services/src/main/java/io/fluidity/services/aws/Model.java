package io.fluidity.services.aws;

import io.fluidity.services.query.FileMeta;
import io.fluidity.services.query.FileMeta.Fields;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.*;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.longNumberAttribute;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.stringAttribute;

/**
 * From https://github.com/aws/aws-sdk-java-v2/tree/d6b9a7e7e06448ca73a3b7c462e4f76fc3c97102/services-custom/dynamodb-enhanced
 */
public class Model {

    static final TableSchema<FileMeta> FILE_META_TABLE_SCHEMA =
            StaticTableSchema.builder(FileMeta.class)
                    .newItemSupplier(FileMeta::new)       // Tells the mapper how to make new objects when reading items
                    .attributes(
                            stringAttribute(Fields.tenant.name(), FileMeta::getTenant, FileMeta::setTenant)
                                    .as(primaryPartitionKey())                                                  // primary partition key
                            , stringAttribute(Fields.filename.name(), FileMeta::getFilename, FileMeta::setFilename)
                                    .as(primarySortKey())                                                       // primary sort key
                            , stringAttribute(Fields.resource.name(), FileMeta::getResource, FileMeta::setResource)
                                    .as(secondaryPartitionKey("files_by_resource"))                             // GSI partition key
                            , longNumberAttribute(Fields.toTime.name(), FileMeta::getToTime, FileMeta::setToTime)
                                    .as(secondarySortKey("files_by_time"), secondarySortKey("files_by_resource"))    // Sort key for both the LSI and the GSI
                            , stringAttribute(Fields.storageUrl.name(), FileMeta::getStorageUrl, FileMeta::setStorageUrl)
                            , longNumberAttribute(Fields.fromTime.name(), FileMeta::getFromTime, FileMeta::setFromTime)
                            , longNumberAttribute(Fields.size.name(), FileMeta::getSize, FileMeta::setSize)
                            , stringAttribute(Fields.tags.name(), FileMeta::getTags, FileMeta::setTags)
                            , stringAttribute(Fields.timeFormat.name(), FileMeta::getTimeFormat, FileMeta::setTimeFormat)
                    )
                    .build();

    /**
     * Must match the key in the schema above
     * @param tenant
     * @param filename
     * @return
     */
    public static Key getKey(String tenant, String filename) {
        return Key.create(stringValue(tenant), stringValue(filename));
    }
}
