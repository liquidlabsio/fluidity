package com.logscapeng.uploader.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.logscapeng.uploader.FileMeta;
import com.logscapeng.uploader.StorageUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AwsS3StorageUploaderService implements StorageUploader {

    private final Logger log = LoggerFactory.getLogger(AwsS3StorageUploaderService.class);




    public AwsS3StorageUploaderService(){
        log.info("CREATED:");
    }

    @Override
    public FileMeta upload(final FileMeta upload, final String region) {
        log.info("uploading:" + upload);

        Regions clientRegion = Regions.fromName(region);
        String bucketName = upload.getTenantWithDate();
        String filePath = upload.resource + "/" + upload.filename;

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata("tags", upload.getTags().toString());
        objectMetadata.addUserMetadata("tenant", upload.tenant);
        objectMetadata.addUserMetadata("length", "" + upload.fileContent.length);


        File file = createTempFile(upload.fileContent);
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new ProfileCredentialsProvider())
                    .build();


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

            upload.storageUrl = completeMultipartUploadResult.getLocation();

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

    @Override
    public byte[] get(String storageUrl) {
        throw new RuntimeException("Not implemented yet!");
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
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new ProfileCredentialsProvider())
                    .build();

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