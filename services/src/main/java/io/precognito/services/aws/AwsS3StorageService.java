package io.precognito.services.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import io.precognito.services.query.FileMeta;
import io.precognito.services.storage.Storage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AwsS3StorageService implements Storage {

    private static final long LIMIT = (long) (FileUtils.ONE_MB * 4.5);
    private final Logger log = LoggerFactory.getLogger(AwsS3StorageService.class);

    @ConfigProperty(name = "precognito.prefix", defaultValue = "precognito.")
    private String PREFIX;


    public AwsS3StorageService(){
        log.info("Created");
    }

    /**
     * Doesnt actually removed external entities, but lists them instead
     * @param region
     * @param tenant
     * @param storageId
     * @param includeFileMask
     * @return
     */
    @Override
    public List<FileMeta> removeByStorageId(String region, String tenant, String storageId, String includeFileMask) {
        return importFromStorage(region, tenant, storageId, includeFileMask, "");
    }

    @Override
    public List<FileMeta> importFromStorage(String region, String tenant, String storageId, String includeFileMask, String tags) {
        String bucketName = storageId;
        AmazonS3 s3Client = getAmazonS3Client(region);

        ObjectListing objectListing = s3Client.listObjects(bucketName, "");
        List<FileMeta> fileMetas = objectListing.getObjectSummaries().stream().filter(item -> item.getKey().contains(includeFileMask) || includeFileMask.equals("*")).map(objSummary ->
        {
            FileMeta fileMeta = new FileMeta(tenant,
                    objSummary.getBucketName(),
                    objSummary.getETag(),
                    objSummary.getKey(),
                    new byte[0],
                    objSummary.getLastModified().getTime() - (24 * 60 * 60 * 1000),
                    objSummary.getLastModified().getTime());
            fileMeta.setSize(objSummary.getSize());
            fileMeta.setStorageUrl(String.format("s3://%s/%s", bucketName, objSummary.getKey()));
            fileMeta.setTags(tags + " " + getExtensions(objSummary.getKey()));
            return fileMeta;
        })
                .collect(Collectors.toList());
        return fileMetas;
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

        log.info("uploading:" + upload + " bucket:" + upload.getTenant());

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata("tags", upload.getTags().toString());
        objectMetadata.addUserMetadata("tenant", upload.tenant);
        objectMetadata.addUserMetadata("length", "" + upload.fileContent.length);


        File file = createTempFile(upload.fileContent);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            AmazonS3 s3Client = getAmazonS3Client(region);


            if (!s3Client.doesBucketExistV2(bucketName)) {
                log.info("Bucket:{} doesnt exist, creating", bucketName);
                Bucket bucket = s3Client.createBucket(bucketName);
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
            for (int i = 1; filePosition < upload.fileContent.length; i++) {
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

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            log.debug("ETags:" + partETags);
            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, filePath,
                    initResponse.getUploadId(), partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = s3Client.completeMultipartUpload(compRequest);

//            upload.storageUrl = completeMultipartUploadResult.getLocation();
            upload.setStorageUrl(String.format("s3://%s/%s", bucketName, filePath));

        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            log.error("AmazonServiceException S3 Upload failed to process:{}", upload, e);
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.error("SdkClientException S3 not responding:{}", upload, e);
        } finally {
            file.delete();
        }

        return upload;
    }

    private String getBucketName(String tenant) {
        return (PREFIX + "-" + tenant).toLowerCase();
    }

    private static AmazonS3 getAmazonS3Client(String region) {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
//                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    synchronized private void bind() {
        if (PREFIX == null) {
            PREFIX = ConfigProvider.getConfig().getValue("precognito.prefix", String.class);
        }
    }

    /**
     * TODO: Seek to offset/compression-handling[on-off]/direct-download etd
     * @param region
     * @param storageUrl
     * @return
     */
    @Override
    public byte[] get(String region, String storageUrl) {
        bind();
        try {
            URI uri = new URI(storageUrl);
            String bucket = uri.getHost();
            String filename = uri.getPath().substring(1);

            AmazonS3 s3Client = getAmazonS3Client(region);
            S3Object s3object = s3Client.getObject(bucket, filename);
            S3ObjectInputStream inputStream = s3object.getObjectContent();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            IOUtils.copyLarge(inputStream, baos, 0, LIMIT);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to retrieve {}", storageUrl, e);
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getSignedDownloadURL(String region, String storageUrl) {
        bind();

        try {
            URI uri = new URI(storageUrl);
            String bucket = uri.getHost();
            String filename = uri.getPath().substring(1);

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