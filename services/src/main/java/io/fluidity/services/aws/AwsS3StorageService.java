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

package io.fluidity.services.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import io.fluidity.services.query.FileMeta;
import io.fluidity.services.storage.Storage;
import io.fluidity.util.DateUtil;
import io.fluidity.util.LazyFileInputStream;
import io.fluidity.util.UriUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AwsS3StorageService implements Storage {

    private static final long LIMIT = (long) (FileUtils.ONE_MB * 4.5);
    public static final int MAX_KEYS = 100000;
    public static final int S3_REQ_THREADS = 100;
    public static final int CONNECTION_POOL_SIZE = 200;
    public static final int SCAN_COUNT_LIMIT_FOR_COST = 10000;

    private final Logger log = LoggerFactory.getLogger(AwsS3StorageService.class);

    @ConfigProperty(name = "fluidity.prefix", defaultValue = "fluidity-")
    String PREFIX;


    public AwsS3StorageService() {
        log.info("Created");
        if (PREFIX == null) {
            PREFIX = ConfigProvider.getConfig().getValue("fluidity.prefix", String.class);
        }
        log.info("Created: PREFIX: {}", PREFIX);
    }

    @Override
    public void listBucketAndProcess(String region, String tenant, String prefix, Processor processor) {
        String bucketName = getBucketName(tenant);
        AmazonS3 s3Client = getAmazonS3Client(region);
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result objectListing = s3Client.listObjectsV2(bucketName, prefix);

        objectListing.getObjectSummaries().stream().forEach(item -> processor.process(region, item.getKey(), item.getKey(), item.getLastModified().getTime()));

        while (objectListing.isTruncated()) {
            req.setContinuationToken(objectListing.getNextContinuationToken());
            objectListing = s3Client.listObjectsV2(req);
            objectListing.getObjectSummaries().stream().forEach(item -> processor.process(region, item.getKey(), item.getKey(), item.getLastModified().getTime()));
        }
    }

    /**
     * TODO: should be using callback to prevent OOM
     *
     * @param region
     * @param tenant
     * @param storageId
     * @param includeFileMask
     * @param tags
     * @return
     */
    @Override
    public List<FileMeta> importFromStorage(String region, String tenant, String storageId, String prefix, int ageDays, String includeFileMask, String tags, String timeFormat) {

        String filePrefix = prefix.equals("*") ? "" : prefix;
        long sinceTimeMs = ageDays == 0 ? 0 : System.currentTimeMillis() - ageDays * DateUtil.DAY;

        log.info("Importing from:{} mask:{}", storageId, includeFileMask);
        String bucketName = storageId;
        AmazonS3 s3Client = getAmazonS3Client(region);

        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);

        int scanCount = 0;
        ListObjectsV2Result objectListing = s3Client.listObjectsV2(bucketName, filePrefix);
        ArrayList<FileMeta> results = new ArrayList<>();
        results.addAll(addSummaries(tenant, includeFileMask, tags, bucketName, objectListing, sinceTimeMs, timeFormat));
        while (objectListing.isTruncated() && results.size() < 200000 && scanCount++ < SCAN_COUNT_LIMIT_FOR_COST) {
            req.setContinuationToken(objectListing.getNextContinuationToken());
            objectListing = s3Client.listObjectsV2(req);
            results.addAll(addSummaries(tenant, includeFileMask, tags, bucketName, objectListing, sinceTimeMs, timeFormat));
        }

        log.info("Import finished, total:{} scanCount:{} - if scan limit is hit then specify a prefix", results.size(), scanCount);

        if (scanCount >= SCAN_COUNT_LIMIT_FOR_COST) {
            log.warn("Too many scans - specify a prefix to improve accuracy and reduce cost");
        }
        return results.stream().distinct().collect(Collectors.toList());
    }

    private List<FileMeta> addSummaries(String tenant, String includeFileMask, String tags, String bucketName, ListObjectsV2Result objectListing, long sinceTimeMs, String timeFormat) {

        List<FileMeta> results = objectListing.getObjectSummaries().stream().filter(
                item -> (item.getKey().contains(includeFileMask) || includeFileMask.equals("*")) &&
                        item.getLastModified().getTime() > sinceTimeMs)
                .map(objSummary ->
                {
                    FileMeta fileMeta = new FileMeta(tenant,
                            objSummary.getBucketName(),
                            objSummary.getETag(),
                            objSummary.getKey(),
                            new byte[0],
                            inferFakeStartTimeFromSize(objSummary.getSize(), objSummary.getLastModified().getTime()),
                            objSummary.getLastModified().getTime(), timeFormat);
                    fileMeta.setSize(objSummary.getSize());
                    fileMeta.setStorageUrl(String.format("storage://%s/%s", bucketName, objSummary.getKey()));
                    fileMeta.setTags(tags + " " + getExtensions(objSummary.getKey()));
                    return fileMeta;
                })
                .collect(Collectors.toList());


        if (results.size() > 0) log.info("Import progress:{}", results.size());
        return results.stream().distinct().collect(Collectors.toList());
    }

    private long inferFakeStartTimeFromSize(long size, long lastModified) {
        if (size < 4096) return lastModified - DateUtil.HOUR;
        int fudgeLineLength = 256;
        int fudgeLineCount = (int) (size / fudgeLineLength);
        long fudgedTimeIntervalPerLineMs = 1000;
        long startTimeOffset = fudgedTimeIntervalPerLineMs * fudgeLineCount;
        if (startTimeOffset < DateUtil.HOUR) startTimeOffset = DateUtil.HOUR;
        return lastModified - startTimeOffset;
    }

    private String getExtensions(String filename) {
        if (filename.contains(".")) return Arrays.toString(filename.split("."));
        return "";
    }


    @Override
    public FileMeta upload(final String region, final FileMeta upload) {
        bind();

        String bucketName = getBucketName(upload.getTenant());
        String filePath = upload.resource + "/" + upload.filename;

        log.info("uploading:" + upload + " bucket:" + bucketName);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata("tags", upload.getTags());
        objectMetadata.addUserMetadata("tenant", upload.tenant);
        objectMetadata.addUserMetadata("length", "" + upload.fileContent.length);
        upload.setStorageUrl(writeToS3(region, upload.fileContent, bucketName, filePath, objectMetadata, 0));

        return upload;
    }

    private String writeToS3(String region, byte[] fileContent, String bucketName, String filePath, ObjectMetadata objectMetadata, int daysRetention) {
        File file = createTempFile(fileContent);
        return writeFileToS3(region, file, bucketName, filePath, objectMetadata, daysRetention);
    }

    private String writeFileToS3(String region, File file, String bucketName, String filePath, ObjectMetadata objectMetadata, int daysRetention) {

        if (daysRetention > 0) {
            objectMetadata.setExpirationTime(new Date(System.currentTimeMillis() - DateUtil.DAY * daysRetention));
        }

        log.debug("Write:{} {} length:{}", bucketName, filePath, file.length());
        if (file.length() == 0) {
            log.warn("Attempted to write empty file to S3:{}", filePath);
            return "empty-file";
        }
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        /**
         * Cannot write to S3 with '/' header
         */
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        try {
            AmazonS3 s3Client = getAmazonS3Client(region);


            if (!s3Client.doesBucketExistV2(bucketName)) {
                log.info("Bucket:{} doesnt exist, creating", bucketName);
                s3Client.createBucket(bucketName);
            }

            // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<>();

            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, filePath, objectMetadata);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);


            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < file.length(); i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName)
                        .withKey(filePath)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);


                log.debug("UploadPart:{} {} {}", filePath, i, filePosition);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            log.debug("Complete - ETags:{} File.length:{}", partETags, file.length());
            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, filePath,
                    initResponse.getUploadId(), partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = s3Client.completeMultipartUpload(compRequest);

//            upload.storageUrl = completeMultipartUploadResult.getLocation();


        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            log.error("AmazonServiceException S3 Upload failed to process:{}", filePath, e);
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.error("SdkClientException S3 not responding:{}", filePath, e);
        } finally {
            file.delete();
        }
        return String.format("storage://%s/%s", bucketName, filePath);
    }

    public String getBucketName(String tenant) {
        return (PREFIX + "-" + tenant).toLowerCase();
    }

    private static AmazonS3 getAmazonS3Client(String region) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxConnections(CONNECTION_POOL_SIZE);
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    synchronized private void bind() {
        if (PREFIX == null) {
            PREFIX = ConfigProvider.getConfig().getValue("fluidity.prefix", String.class);
        }
    }

    /**
     * TODO: Seek to offset/compression-handling[on-off]/direct-download etd
     *
     * @param region
     * @param storageUrl
     * @param offset
     * @return
     */
    @Override
    public byte[] get(String region, String storageUrl, int offset) {
        bind();
        try {
            String[] hostnameAndPath = UriUtil.getHostnameAndPath(storageUrl);
            String bucket = hostnameAndPath[0];
            String filename = hostnameAndPath[1];

            AmazonS3 s3Client = getAmazonS3Client(region);
            S3Object s3object = s3Client.getObject(bucket, filename);
            S3ObjectInputStream inputStream = s3object.getObjectContent();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            IOUtils.copyLarge(inputStream, baos, offset, LIMIT);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to retrieve {}", storageUrl, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream(String region, String tenant, String storageUrl) {
        bind();
        try {
            String[] hostnameAndPath = UriUtil.getHostnameAndPath(storageUrl);
            String bucket = hostnameAndPath[0];
            String filename = hostnameAndPath[1];

            AmazonS3 s3Client = getAmazonS3Client(region);
            S3Object s3object = s3Client.getObject(bucket, filename);
            return copyToLocalTempFs(s3object.getObjectContent());

        } catch (Exception e) {
            log.error("Failed to retrieve {}", storageUrl, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, InputStream> getInputStreams(String region, String tenant, String filePathPrefix, String filenameExtension, long fromTime) {
        String bucketName = getBucketName(tenant);
        AmazonS3 s3Client = getAmazonS3Client(region);

        long start = System.currentTimeMillis();

        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(filePathPrefix).withMaxKeys(MAX_KEYS);

        ListObjectsV2Result objectListing = s3Client.listObjectsV2(req);
        Map<String, InputStream> results = new HashMap<>();
        results.putAll(getInputStreamsFromS3(s3Client, filenameExtension, objectListing, fromTime));
        while (objectListing.isTruncated()) {
            objectListing = s3Client.listObjectsV2(req);
            results.putAll(getInputStreamsFromS3(s3Client, filenameExtension, objectListing, fromTime));
            req.setContinuationToken(objectListing.getNextContinuationToken());
        }
        log.info("getInputStreams Elapsed:{}", (System.currentTimeMillis() - start));
        return results;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private Map<String, InputStream> getInputStreamsFromS3(final AmazonS3 s3Client, String filenameExtension, final ListObjectsV2Result objectListing, final long fromTime) {

        Map<String, InputStream> results = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(S3_REQ_THREADS);

        objectListing.getObjectSummaries().stream()
                .filter(objSummary -> objSummary.getKey().endsWith(filenameExtension) && objSummary.getLastModified().getTime() > fromTime)
                .forEach(
                        objSummary -> executorService.submit(() -> {
                            results.put(objSummary.getKey(), copyToLocalTempFs(s3Client.getObject(objSummary.getBucketName(), objSummary.getKey())
                                    .getObjectContent()));
                        })
                );

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("getInputStreamsFromS3 Count:" + results.size());

        return results;
    }

    /**
     * AWS Recommend grabbing the content as quickly as possible
     *
     * @param objectContent
     * @return
     */
    private InputStream copyToLocalTempFs(S3ObjectInputStream objectContent) {
        try {
            File temp = File.createTempFile("fluidity", "s3");

            FileOutputStream fos = new FileOutputStream(temp);
            IOUtils.copyLarge(objectContent, fos);
            fos.flush();
            fos.close();
            objectContent.close();

            return new BufferedInputStream(new LazyFileInputStream(temp)) {
                @Override
                public void close() {
                    temp.delete();
                }
            };
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        throw new RuntimeException("Failed to localise S3 data");
    }

    @Override
    public OutputStream getOutputStream(String region, String tenant, String filenameUrl, int daysRetention) {
        try {
            File toS3 = File.createTempFile("S3OutStream", "tmp");
            return new BufferedOutputStream(new FileOutputStream(toS3)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                        if (toS3.length() > 0) {
                            ObjectMetadata objectMetadata = new ObjectMetadata();
                            objectMetadata.addUserMetadata("tenant", tenant);
                            objectMetadata.addUserMetadata("length", "" + toS3.length());
                            writeFileToS3(region, toS3, getBucketName(tenant), filenameUrl, objectMetadata, daysRetention);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        toS3.delete();
                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        bind();

        try {
            String[] hostnameAndPath = UriUtil.getHostnameAndPath(storageUrl);
            String bucket = hostnameAndPath[0];
            String filename = hostnameAndPath[1];

            AmazonS3 s3Client = getAmazonS3Client(region);

            // Set the pre-signed URL to expire after one hour.
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60;
            expiration.setTime(expTimeMillis);

            // Generate the pre-signed URL.
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucket, filename)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);
            return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();

        } catch (Exception e) {
            log.error("Failed to retrieve {}", storageUrl, e);
            throw new RuntimeException(e);
        }
    }

    private File createTempFile(byte[] filecontent) {
        try {
            File tempFile = File.createTempFile("test", ".tmp");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(filecontent);
            fos.flush();
            fos.close();
            return tempFile;
        } catch (IOException e) {
            log.error("Failed to created temp file:{}", e);
            e.printStackTrace();
        }
        throw new RuntimeException("Failed to create temp file");
    }


    public static void main(String[] args) throws IOException {
        Regions clientRegion = Regions.DEFAULT_REGION;
        String bucketName = "*** Bucket name ***";
        String keyName = "*** Key name ***";
        String filePath = "*** Path to file to upload ***";

        File file = new File(filePath);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            AmazonS3 s3Client = getAmazonS3Client(clientRegion.getName());

            // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<PartETag>();

            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName)
                        .withKey(keyName)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
                    initResponse.getUploadId(), partETags);
            s3Client.completeMultipartUpload(compRequest);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
    }


}