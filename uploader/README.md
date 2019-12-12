#Uploader

##Description

Uploads a given file to the configured S3 bucket and creates the associated meta data.
Parameters:
- filename
- tenant identifier (user-group etc)
- metadata key-values
- bucketname (defaults to tenant name)


##Metadata
metadata is attached to the S3 entry, including:
- Filename: file to upload
- Tag: comma delimited list of stings to identify characteristics
- Resource: the uri of the resource, might be hostname, devicename etc
- Tenant: who or which group owns the resource
 

##Dependencies

##Outputs
Writes to an S3 bucket using the following format:

 `tenant/today/resource/filepath-filename`


##Usage

##Other comments