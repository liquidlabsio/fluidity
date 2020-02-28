#Services

##Description

## Running

> Changes to the 'search' module are avaiable to the  'services' module by running 'mvn clean install'
 
* Local test mode - using in-memory services

> mvn -Dquarkus-profile=test  clean quarkus:dev
 
* Local DEV mode - connected to AWS services (S3, DynamoDB)

> mvn clean quarkus:dev


From Intellij open the index.html by clicking the browser icon in the top RH corner. The application will run against  the local instance (port 8080).
See precognito.js for backend-service configuration. 
