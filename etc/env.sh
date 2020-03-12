#!/bin/bash

# 1. Create and configured this bucket with CORS and Policy files before any deployment. Note edit the policy-S3 arn
## application code
export S3_BUCKET=precognito-runtime
export STACK_NAME=precognito-faas-runtime

## uploaded user data (will be prefixed with precongnito-dev or precongnito-prod when it is automatically created)
export S3_TENANT_BUCKET=tenant-userstore


# 2. Configure after the backend is deployed and the REST API is known
export SERVICE_API=https://ocdmbpn3mf.execute-api.eu-west-2.amazonaws.com/Prod/

