#!/bin/bash

# 7 days retention
declare -r retention="7"
for L in $(aws2 logs describe-log-groups     --log-group-name-prefix '/aws/lambda/fluidity' --output text)
do
   aws2 logs  put-retention-policy --log-group-name ${L} \
   --retention-in-days ${retention}
done