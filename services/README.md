#Services

##Description

## Building

> mvn clean package

### Building the GraalVM Native image

> https://quarkus.io/guides/building-native-image

### Install Graalvm (OSX)
* > brew cask install graalvm/tap/graalvm-ce-java8 (wait for a bit...)

1. From the CLI - set the new JVM Home and export the GU Tools<br>
> export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java8-20.0.0/Contents/Home
<br>> export PATH=/Library/Java/JavaVirtualMachines/graalvm-ce-java8-20.0.0/Contents/Home/bin:"$PATH"

1. Install the graalvm native image
<br>>gu install native-image

### Test GraalVM
1. TEST
> ./mvnw -Dnative.image.path=/Library/Java/JavaVirtualMachines/graalvm-ce-java8-20.0.0  verify -Pnative


### Build the the app native image
<br>>./mvnw -Dmaven.test.skip=true package -Pnative
<br>
1. Build the image for AWS Lamda deployment
<br>>./mvnw clean install -Dmaven.test.skip=true -Pnative -Dnative-image.docker-build=true




## Running the Services locally or deploy as Lambda

> Changes to the 'search' module are avaiable to the  'services' module by running 'mvn clean install'
 
* Local test mode - using in-memory services

> mvn -Dquarkus-profile=test  clean quarkus:dev
 
* Local DEV mode - connected to AWS services (S3, DynamoDB)

> mvn clean quarkus:dev


From Intellij open the index.html by clicking the browser icon in the top RH corner. The application will run against  the local instance (port 8080).
See precognito.js for backend-service configuration. 


* Deploy as Lambda - look in etc/deploy-backend.sh
