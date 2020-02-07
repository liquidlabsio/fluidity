
![Image description](img/precog-logo.png)

# What is it?

Cloud native, serverless machine learning, serverless search engine using the latest technologies to run at minimum cost on the Cloud. 
Data processing is expensive; this tool is meant to be cheap and simple. It provides basic 
functionality that most people need. 
* upload data to storage
* import existing storage data
* then enrich it with meta data (i.e. human readable tags)
* find it by the tags and view the raw data
* search, like a distributed grep

Because it processes data on a storage layer, it is considered a batch system. 
Storage chunks are immutable, and periodically written. It is not realtime (more like slow-time). 
If you want streaming realtime correlations and alerts etc then look elsewhere. That is an upstream problem.
 (although there are plans to explore what that looks like using Apache Kafka and Confluent Cloud) 

## Technology
1. Quarkus - a Java container providing the best of breed tools/frameworks from the Java ecosystem. They compile down to GraalVM is - Java-native microkernel without the traditional Java overhead
2. Serverless functions. Using the AWS-Lambda extension, API calls are delegated through the APIGateway (proxy) to call onto a Serverless function instance.
3. Cloud native storage: S3, GFS etc
4. Cloud native DB (AWS:DynamoDB)
5. Static web-tier from storage layer (the front end is hosts on S3 etc) and calls to the REST API
 that is part of the APIGateway that delegates to the Serverless function. Much of this call complexity is handled by Quarkus extensions (i.e. lambda-http, resteasy etc)

# Why another logging tool?

Because you can never have enough! 
Seriously though, Precognito runs in your VPC and its b. This means you own the total 
cost of infrastructure. It is designed to run serverless in the most optimial 
way possible; mostly running in the free tier on supported cloud providers
 (currently AWS). When not in use you only pay for S3/GFS storage (which is relatively cheap). 
 When executing tasks, Serverless functions are executed to handle the work, and then they shutdown.
 
 

# How do I get data into it?
Data can be uploaded, or better yet it already exists in Cloud Storage (S3, CFG etc). To import external data you will need to look at Flume or logstash/beats - then make them write to S3.


# Building

#### Dev Mode on local machine against AWS
* $ cd uploader
* $ ./mvnw compile quarkus:dev
* Open index.html in a browser from Intellij (backend.js defaults to http://localhost:8080 which quarkus is running the REST endpoint)
* Browse to 


#### AWS Deployment

Notes and limits: 
- If you want data to load quickly to the browser then enable CORS on each S3 bucket and 
Precognito will use a signed URL to download directly from S3 (see etc/s3-cors.xml). 
If not enabled, then a signed download be tried, and upon failure revert to using a Lambda.
- The largest browsable file is 100MB before the browser starts to choke on memory (about 250k lines of text) 
The same file will gzip down to 3.6MB and can be handled by a Function call. AWS Function download limits are limited by lambda return limits -  5MB   

Prerequisites: 
- Install AWS SAM: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html
- Install the AWS cli. I used version 2.0 - i.e. all cli commands are `> aws2 --stuff--`
- Create an S3 bucket for the Precognito cloud runtime (lambda-code, front-end website) deployment (put that name into env.sh) <YOUR_S3_BUCKET>
- Configure the S3 precognito-runtime bucket for static website hosting
- AWS account for the SAM user such that Cloudformation etc is available. Brute force unsecure json as follows: <br>
    **IAM -> User (x) -> inline-policy ** <BR>
    IAMFullAccess,   AmazonS3FullAccess,  AmazonDynamoDBFullAccess  
    AWSCloudFormationFullAccess,  AmazonAPIGatewayAdministrator, AWSLambdaFullAccess

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "iam:*",
                "apigateway:*",
                "lambda:*",
                "cloudformation:*",
                "dynamodb:*"
            ],
            "Resource": "*"
        }
    ]
}
```

#### Scripted
1. $ cd precognito/etc
1. edit env.sh with S3 bucket, tenant information etc
1. $ ./deploy-backend.sh
1. edit env.sh with APIGateway address
1. $ ./deploy-frontend.sh 
1. S3 cp generally fails so change to ./temp-web-package and 'aws2 s3 cp . s3://<BUCKET> --recurvise' files again
1. Point your browser to the URL in this message 'Point your browser to:'
1. Username:anything@precognito.io pwd: 'secret'

##### Troubleshooting
1. Check the deployment user has correct permissions - error message will indicate so
1. Check the aws2 s3 cp to the runtime bucket worked. If it partially failed the browser debug console will show resources failed to load or endpoints are not correct (failed to connect to address)
1. Rollback using the cloudformation console

#### Manual

Reference: https://quarkus.io/guides/amazon-lambda-http


**Notes:**
This approach uses the AWS Gateway Proxy (i.e. the Gateway passes through everything). Quarkus has a HTTP handler embedded in the Lambda the calls on the RX Rest endpoint locally.
1. Create the precognito fat jar <br>
 `$ mvn clean install` ->
 `target/precognito-0.1-SNAPSHOT-runner.jar`<br>
1. Package the deployment:<br>
 `$ sam package --template-file sam.jvm.yaml --output-template-file packaged.yaml --s3-bucket <YOUR_S3_BUCKET>`
1. Deploy it using any stack-name<br>
`$ sam deploy --template-file packaged.yaml --capabilities CAPABILITY_IAM --stack-name <YOUR_STACK_NAME>`<br>
The output will display the API-Gateway URL for the front end

1. List deployments looking for your stack (and others)<br>
`$ aws2 cloudformation list-stacks`<br>
> to delete: 
> `$ aws2 cloudformation delete-stack --stack-name <YOUR_STACK_NAME>`

```json
 { "StackSummaries": [
        {
            "StackId": "arn:aws:cloudformation:eu-west-2:001814218767:stack/precognito-faas/6a5712b0-3d2f-11ea-8738-021679f87d94",
            "StackName": "precognito-faas",
            "TemplateDescription": "AWS Serverless Quarkus HTTP - liquidlabs::precognito",
            "CreationTime": "2020-01-22T15:54:04.530000+00:00",
            "LastUpdatedTime": "2020-01-22T15:54:09.927000+00:00",
            "StackStatus": "CREATE_COMPLETE",
            "DriftInformation": {
                "StackDriftStatus": "NOT_CHECKED"
            }
        }
    ]
}
```
1. Note the API-Gateway-Proxy REST URL<br>
`$ aws2 cloudformation describe-stacks --stack-name precognito-faas`
```json
{
    "Stacks": [
        {
            "StackId": "arn:aws:cloudformation:eu-west-2:001814218767:stack/precognito-faas/6a5712b0-3d2f-11ea-8738-021679f87d94",
            "StackName": "precognito-faas",
            "ChangeSetId": "arn:aws:cloudformation:eu-west-2:001814218767:changeSet/samcli-deploy1579708443/2f0ce821-122f-40b5-ad6f-26861bc64daf",
            "Description": "Serverless log analysis- liquidlabs::precognito",
            "CreationTime": "2020-01-22T15:54:04.530000+00:00",
            "LastUpdatedTime": "2020-01-22T15:54:09.927000+00:00",
            "RollbackConfiguration": {},
            "StackStatus": "CREATE_COMPLETE",
            "DisableRollback": false,
            "NotificationARNs": [],
            "Capabilities": [
                "CAPABILITY_IAM"
            ],
            "Outputs": [
                {
                    "OutputKey": "PrecognitoApi",
                    "OutputValue": "https://3dwlmgnks6.execute-api.eu-west-2.amazonaws.com/Prod/",
                    "Description": "URL for application",
                    "ExportName": "PrecognitoApi"
                }
            ],
            "Tags": [],
            "EnableTerminationProtection": false,
            "DriftInformation": {
                "StackDriftStatus": "NOT_CHECKED"
            }
        }
    ]
}
```
1. From above see that the API-Gateway Proxy is available on <br>
`https://3dwlmgnks6.execute-api.eu-west-2.amazonaws.com/Prod/`
1. Check the API Gateway proxies requests to the Lambda function - **Note - append** `query/id` to the url. It will suffer a cold start, but subsequent invocations will be much quicker
 
```
Browser URL: https://3dwlmgnks6.execute-api.eu-west-2.amazonaws.com/Prod/query/id
Response:    io.precognito.services.query.QueryResource
```
1. Configure the User interface URL to point to the API-Gateway address