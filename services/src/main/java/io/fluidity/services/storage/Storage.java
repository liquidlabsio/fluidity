/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.services.storage;

import io.fluidity.search.StorageInputStream;
import io.fluidity.services.Lifecycle;
import io.fluidity.services.query.FileMeta;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface Storage extends Lifecycle {
    /**
     * Store and Capture the Storage URL
     * @param region
     * @param upload
     * @return
     */
    FileMeta upload(String region, FileMeta upload);

    byte[] get(String region, String storageUrl, int offset);

    String getBucketName(String tenant);

    List<FileMeta> importFromStorage(String region, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags, String timeFormat);

    String getSignedDownloadURL(String region, String storageUrl);

    StorageInputStream getInputStream(String region, String tenant, String storageUrl);

    OutputStream getOutputStream(String region, String tenant, String filePathUrl, int daysRetention, long lastModified);

    Map<String, StorageInputStream> getInputStreams(String region, String tenant, String prefix, String filepathSuffix, long fromTime);

    void stop();

    /**
     * MANDATORY: List in UTF-binary order
     *
     * @param region
     * @param tenant
     * @param prefix
     * @param processor
     */
    void listBucketAndProcess(String region, String tenant, String prefix, Processor processor);

    interface Processor {
        void process(String region, String itemUrl, String itemName, long modified, long size);
    }
}
