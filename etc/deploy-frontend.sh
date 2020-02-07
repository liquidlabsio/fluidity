#!/bin/bash
echo " 1. The REST API must be available and configured in env.sh"
echo " 2. This script copies the web module to a temporary directory and configures the endpoint REST endpoint"
echo " 3. It then copies it to the configured S3 bucket (which should be configured for static website hosting)"
. ./env.sh

export TEMP_DIR=temp-web-package

rm -rf $TEMP_DIR
mkdir $TEMP_DIR


echo pwd
echo ""
echo "Step 0 - Applying bucket policy"
echo "========================"


sed "s/EXAMPLE-BUCKET.*/"${S3_BUCKET}"\/*\"/" s3-policy.json > $TEMP_DIR/policy.json

aws2 s3api put-bucket-policy --bucket $S3_BUCKET --policy file://$TEMP_DIR/policy.json


echo pwd
echo ""
echo "Step 1 - Copy artifacts"
echo "========================"

rm -rf $TEMP_DIR
mkdir $TEMP_DIR

cp -R ../web/src/main/webapp/* $TEMP_DIR

cd $TEMP_DIR/js

echo "Step 2 - Configuring REST API in js/backend.js"
echo "========================"

mv precognito.js precognito.js.BAK
echo "SERVICES_URL = '$SERVICES_API'" > precognito.js
sed '1d' precognito.js.BAK >> precognito.js


echo "Step 3 - Configuring TENANT_S3 BUCKET in js/backend.js"
echo "========================"

mv precognito.js precognito.js.BAK.2
sed "s/DEFAULT_TENANT=.*/"DEFAULT_TENANT=\"${S3_TENANT_BUCKET}"\"/" precognito.js.BAK.2 > precognito.js


echo "Step 4 - Publishing to S3 Bucket:" + $S3_BUCKET
echo "========================"
aws2 s3 cp . s3://$S3_BUCKET --recursive

export REGION=`aws2 configure get region`

echo "====================================="
echo "Point your browser to http://"$S3_BUCKET".s3-website."$REGION".amazonaws.com/index.html"
echo "DONE!"
