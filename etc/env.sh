#!/bin/bash

# Configured before any deployment
## application code
export S3_BUCKET=logscape-runtime
export STACK_NAME=logscape-faas-runtime

## uploaded user data (will be prefixed with loscape-dev or logscape-prod when it is automatically created)
export S3_TENANT_BUCKET=tenant-userstore


# Configure after the backend is deployed and the REST API is known
export SERVICES_API=https://orfwtb4h97.execute-api.eu-west-2.amazonaws.com/Prod
