#!/bin/bash -v
echo " 1. AWS account with cloud-formation role and credentials installed: ~/.aws/credentials"
echo " 2. AWS2 CLI installed: see: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2-linux-mac.html"
echo " 3. AWS SAM installed:  https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install-mac.html "
echo " 4. Configure the S3 runtime and tenant buckets in env.sh (it must be created already)"
. ./env.sh

cd ../services

echo pwd
echo ""
echo "Step 1 - Build artifacts"
echo "========================"
./mvnw -Dmaven.test.skip=true clean install

echo "Step 2 - Package on AWS"
echo "========================"
echo "Uploading to S3 Bucket -> $S3_BUCKET"
sam package --template-file sam.jvm.yaml --output-template-file packaged.yaml --s3-bucket $S3_BUCKET

echo "Step 3 - Deploy the stack the AWS"
echo "========================"
sam deploy --template-file packaged.yaml --capabilities CAPABILITY_IAM --stack-name $STACK_NAME

echo "Step 4 - Review stack"
echo "========================"
aws2 cloudformation describe-stacks --stack-name $STACK_NAME

echo "========================"
echo "Step 5 - Validate the REST API"
echo "========================"
echo "Open browser at the 'OutputValue' URL above AND append 'query/id'"
echo "The page should display 'io.fluidity.query.QueryResource"
echo ""
echo "See uploader/README for other commands"
