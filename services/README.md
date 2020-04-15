#Services

##Description

## Building and Running with the Java runtime

    $mvn clean package
> Changes to the 'search' module are available to the  'services' module by running 'mvn clean install'
 
* Local test mode - using in-memory services

        $ mvn -Dquarkus-profile=test  clean quarkus:dev
    
* Local DEV mode - connected to AWS services (S3, DynamoDB)

        $ mvn clean quarkus:dev
    

From Intellij open the index.html by clicking the browser icon in the top RH corner. The application will run against  the local instance (port 8080).
See fluidity.js for backend-service configuration. 


### Building the GraalVM Native image

**Note: Native tests are always run using the prod profile.**

> https://quarkus.io/guides/building-native-image

### Install Graalvm (OSX)

    $ brew cask install graalvm/tap/graalvm-ce-java11 (wait for a bit...and then a bit more...)

From the CLI - set the new JVM Home and export the GU Tools<br>

    $ export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.0.0/Contents/Home
    $ export JAVA_HOME=$GRAALVM_HOME
    $ export PATH=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.0.0/Contents/Home/bin:"$PATH"

Install the graalvm native image

> $ gu install native-image


### Install GraalVM (Ubuntu 18)

Download from https://github.com/graalvm/graalvm-ce-builds/releases

    $ tar xvf graalvm-ce-java11-linux-amd64-20.0.0.tar.gz
    $ mv graalvm-ce-java11-20.0.0 /usr/lib/jvm/
    $ sudo mv graalvm-ce-java11-20.0.0 /usr/lib/jvm/
    $ cd /usr/lib/jvm
    $ ln -s graalvm-ce-java11-20.0.0/ graalvm-11
    $ sudo ln -s graalvm-ce-java11-20.0.0/ graalvm-11
    $ sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/graalvm-11/bin/java 4
 
 Results:
 
    $ sudo update-alternatives --config java
       Selection    Path                                            Priority   Status
    ------------------------------------------------------------
    * 0            /usr/lib/jvm/java-11-openjdk-amd64/bin/java      1111      auto mode
    1            /usr/lib/jvm/graalvm-11/bin/java                 4         manual mode
    2            /usr/lib/jvm/java-11-openjdk-amd64/bin/java      1111      manual mode
    3            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1081      manual mode
  
 Choose '2' and export env variables as follows
 

    $ export GRAALVM_HOME=/usr/lib/jvm/graalvm-11 
    $ export JAVA_HOME=$GRAALVM_HOME
    $ export PATH=$GRAALVM_HOME/bin/:$PATH

### Test GraalVM

    $ ./mvnw -Dnative.image.path=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.0.0  verify -Pnative


### Building application artifacts

#### Graalvm built for your OS

    $ ./mvnw package -Dmaven.test.skip=true -Pnative
    $ ./target/services-0.1-SNAPSHOT-runner


#### Docker

    $ ./mvnw package -Dmaven.test.skip=true -Pnative -Dquarkus.native.container-runtime=docker

 
#### GraalVM in Docker

    $ ./mvnw package -Dmaven.test.skip=true -Pnative -Dquarkus.native.container-runtime=docker

Ubuntu: If you get the error `/usr/bin/ld: cannot find -lz` then install `sudo apt-get install libz-dev`     

The produced executable will be a 64 bit Linux executable which is then placed into a Docker.image

    $ docker build -f src/main/docker/Dockerfile.native -t quarkus-quickstart/getting-started .


Run it with

    $ docker run -i --rm -p 8080:8080 quarkus-quickstart/getting-started
    
#### For AWS Lamda

    $ ./mvnw clean install -Dmaven.test.skip=true -Pnative -Dnative-image.docker-build=true

Note: I needed to run from the Docker Quick start terminal to avoid 'cannot connect to docker' errors. 


* Deploy as Lambda - look in etc/deploy-backend.sh
