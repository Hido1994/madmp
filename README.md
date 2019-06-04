# IN PROGRESS - Data Stewardship Exercise 2

# Introduction
This project was created for the course Data Stewardship on the TU Wien in SS2019.
It serves as extension to Dataverse and therefore is dependent on a running instance of dataverse.
When running this project it provides two functionalities/extensions to dataverse:
* Import metadata from an maDMP 
* Export files as JSON in a RDA DMP Common Standard compliant syntax

# Prerequisite
As mentioned in the Introduction the project serves as extension to Dataverse and therefore is dependent on a running instance of dataverse.
Also some configuration (e.g. creation of workflow and registratino of application as external tool) to dataverse has to be done manually over the api of dataverse (The configuration steps are explained in the next section).  
Further the following software has to be installed to compile and run the project:
* Java 8 or higher (Java 8 is also required by dataverse)
* Maven

## Running Dataverse
Because one of the prerequisite is a running dataverse instance this section contains some steps to execute dataverse.  
This section can be skipped if an running dataverse instance is already available.
* To install dataverse locally you can follow the instructions on http://guides.dataverse.org/en/latest/developers/dev-environment.html
* It also would be possible to run dataverse containerized in docker. To do this the following steps have to be done
  * TODO steps for dataverse-docker

# Running the project
* First you have to download the sources from https://github.com/Hido1994/madmp
* After that you can configure the application as described in the next section 
* After the configuration is finished the project can be compiled
```
mvn clean install
```
* After that a jar-file is generated in the target/ directory which can simply be excuted with the java command
```
java -jar [executable-name].jar
```

Hint: It would also be possible to simply run the code in and IDE.

# Configuration
To configure dataverse and our application to communicate with each other we have to modify some configurations on both sides.

## Dataverse
To configure dataverse to establish the connection with our application we use the api.

### Workflow
* Create Workflow
```
export DATAVERSE_ADDRESS="http://localhost:8080"

curl -X POST -H "Content-Type: application/json" \
 -d '{"name":"maDMP workflow","steps":[{"provider":":internal","stepType":"http/sr","parameters":{"url":"http://localhost:8081/madmp/${invocationId}/${dataset.id}","method":"POST","contentType":"text/plain","body":"","expectedResponse":"OK.*"}}]}' \
 ${DATAVERSE_ADDRESS}/api/admin/workflows/
```

* Set Workflow


### External tool
* Register external tool
```
curl -X POST -H 'Content-type: application/json' \
-d '{"displayName":"maDMP Export","description":"Export as maDMP block.","type":"explore","contentType":"application/json","toolUrl":"http://localhost:8081/madmp/ext","toolParameters":{"queryParameters":[{"fileid":"{fileId}"},{"datasetid":"{datasetId}"}]}}' ${DATAVERSE_ADDRESS}/api/admin/externalTools
```

## application.properties
To configure this application there can be found an application.properties file under /src/main/resources.
The following configurations are important:

TODO - table with configuration parameters


# MaDMP - Workflow
The first feature is ....

![Workflow](https://github.com/Hido1994/madmp/blob/master/docs/workflow.png?raw=true "MaDMP - Workflow process")

## Installation & Configuration
* Set workflow

* Configurations in application.properties


## Execution
* Build project  
* Run project  
* Create dataverse  
* Create dataset  
* Upload maDMP  
* Upload other files of the project  
* Publish the dataset  

# MaDMP - Export
The second feature is ....

![Extension](https://github.com/Hido1994/madmp/blob/master/docs/extension.png?raw=true "MaDMP - Export process")

## Installation & Configuration



## Execution

