package com.logscapeng.uploader.aws;

import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.FileMeta.Fields;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.*;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.*;

/**
 * From https://github.com/aws/aws-sdk-java-v2/tree/d6b9a7e7e06448ca73a3b7c462e4f76fc3c97102/services-custom/dynamodb-enhanced
 */
public class Model {

    static final TableSchema<FileMeta> FILE_META_TABLE_SCHEMA =
            TableSchema.builder()
                    .newItemSupplier(FileMeta::new)       // Tells the mapper how to make new objects when reading items
                    .attributes(
                            string(Fields.tenant.name(), FileMeta::getTenant, FileMeta::setTenant)
                                    .as(primaryPartitionKey())                                                  // primary partition key
                            ,string(Fields.filename.name(), FileMeta::getFilename, FileMeta::setFilename)
                                     .as(primarySortKey())                                                       // primary sort key
                            ,string(Fields.resource.name(), FileMeta::getResource, FileMeta::setResource)
                                    .as(secondaryPartitionKey("files_by_resource"))                             // GSI partition key
                            ,longNumber(Fields.toTime.name(), FileMeta::getToTime, FileMeta::setToTime)
                                    .as(secondarySortKey("files_by_time"), secondarySortKey("files_by_resource"))    // Sort key for both the LSI and the GSI
                            ,string(Fields.storageUrl.name(), FileMeta::getStorageUrl, FileMeta::setStorageUrl)
                            ,longNumber(Fields.fromTime.name(), FileMeta::getFromTime, FileMeta::setFromTime)
                            ,string(Fields.tags.name(), FileMeta::getTags, FileMeta::setTags)
                    )
                    .build();

    /**
     * Must match the key in the schema above
     * @param tenant
     * @param filename
     * @return
     */
    public static Key getKey(String tenant, String filename) {
        return Key.of(stringValue(tenant), stringValue(filename));
    }
}
