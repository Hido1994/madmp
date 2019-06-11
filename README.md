# Data Stewardship Exercise 2
This project was created for the course Data Stewardship on the TU Wien in SS2019.

# Introduction
This project serves as extension to Dataverse and therefore is dependent on a running instance of dataverse.
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
* It also would be possible to run dataverse containerized in docker.

# Running the project
* First you have to download the sources from https://github.com/Hido1994/madmp
* After that you can configure the application as described in the next section 
* After the configuration is finished the project can be compiled
```
mvn clean install
```
* After that a jar-file is generated in the `target/` directory which can simply be executed with the `java` command
```
java -jar [executable-name].jar
```

Hint: It would also be possible to simply run the code in an IDE.

# Configuration
To configure dataverse and our application to communicate with each other we have to modify some configurations on both sides.

## Dataverse
To configure dataverse to establish the connection with our application we use the api.  
The api is reachable over REST and therefore the `curl` command or a REST client can be used.

### Template
To simplify the process of creating the initial dataset for the researcher a template can be added to dataverse so the data is prefilled.  
TODO

### Workflow
To configure the workflow extension the following commands have to be called:
* Create the workflow  
```
export DATAVERSE_ADDRESS="http://localhost:8080"
curl -X POST -H "Content-Type: application/json" \
 -d '{"name":"maDMP workflow","steps":[{"provider":":internal","stepType":"http/sr","parameters":{"url":"http://localhost:8081/madmp/${invocationId}/${dataset.id}","method":"POST","contentType":"text/plain","body":"","expectedResponse":"OK.*"}}]}' \
 ${DATAVERSE_ADDRESS}/api/admin/workflows/
```

Hint: If the project is running on another port the url parameter has to be modified.

* Set workflow as default  
```
curl -X PUT -d '1' http://localhost:8080/api/admin/workflows/default/PrePublishDataset
```
Also it is possible to simply use a REST client.  
![SetWorkflow](https://github.com/Hido1994/madmp/blob/master/docs/images/set_workflow.png?raw=true "Set workflow")

After that the workflow should be registered and triggered before a Dataset is published.

### External tool
To configure the export functionality the application must simply be registered as external tool.

* Register external tool  
```
curl -X POST -H 'Content-type: application/json' \
-d '{"displayName":"maDMP Export","description":"Export as maDMP block.","type":"explore","contentType":"application/json","toolUrl":"http://localhost:8081/madmp/ext","toolParameters":{"queryParameters":[{"fileid":"{fileId}"},{"datasetid":"{datasetId}"}]}}' ${DATAVERSE_ADDRESS}/api/admin/externalTools
curl -X POST -H 'Content-type: text/x-python' \
-d '{"displayName":"maDMP Export","description":"Export as maDMP block.","type":"explore","contentType":"application/json","toolUrl":"http://localhost:8081/madmp/ext","toolParameters":{"queryParameters":[{"fileid":"{fileId}"},{"datasetid":"{datasetId}"}]}}' ${DATAVERSE_ADDRESS}/api/admin/externalTools
```

Hint: The externalTool registration is specific to a `content-type`, therefore the command has to be executed multible times to enable different `content-types`.

After that a button `Explore` appears in the list of files in a dataset for each file type which has been registered.


## application.properties
To configure this application there can be found an `application.properties` file under `/src/main/resources`.
The following configurations are important:

Parameter | Description | Default
--- | --- | ---
**dataverse.address** | Defines address of dataverse instance | http://localhost:8080
**dataverse.api.key** | Defines API-Key | 5f965eaf-...
**workflow.madmp.filename** | Defines filename of maDMP in dataset | dmp.json
**server.port** | Change server port | 8081
**logging.level.at.tuwien** | Change log-level | info

# MaDMP - Workflow
In this section the general process of the workflow feature is described.

## Process Diagram
![Workflow](https://github.com/Hido1994/madmp/blob/master/docs/images/workflow.png?raw=true "MaDMP - Workflow process")

## Execution
1. The researcher navigates to a dataverse and creates a placeholder dataset.  
![CreateDs](https://github.com/Hido1994/madmp/blob/master/docs/images/create_dataset.png?raw=true "Create dataset")
2. In the next step the researcher uploads the machine actionable DMP to the placeholder dataset.  
![UploadDmp](https://github.com/Hido1994/madmp/blob/master/docs/images/upload_dmp.png?raw=true "Upload DMP")
3. Then the researcher press the `Publish` button  
This triggers the `PrePublishDataset` event and therefore a http request is send to our application.  
4. The application validates the uploaded maDMP and creates a new dataverse and datasets for the project.  
![DVaDS](https://github.com/Hido1994/madmp/blob/master/docs/images/dataverse_and_datasets.png?raw=true "Dataverse view")  
![DSView](https://github.com/Hido1994/madmp/blob/master/docs/images/dataset.png?raw=true "Dataset view")    
Hint: The placeholder dataset and the maDMP is deleted.
5. Now the researcher can navigate to the single datasets and upload the files of the experiment.  
![RUF](https://github.com/Hido1994/madmp/blob/master/docs/images/researcher_file_upload.png?raw=true "Researcher uploads files")    

# MaDMP - Export
In this section the general process of the export feature is described.

## Process Diagram
![Extension](https://github.com/Hido1994/madmp/blob/master/docs/images/extension.png?raw=true "MaDMP - Export process")

## Execution
1. Through the registration as external tool a button `Explore` appears in the file list of the created datasets.
The researcher simply presses this button on the dataset which should be exported.
2. After dataverse triggers the application over a http request. 
The application than generates a JSON-File and trigger the download in the browser.  
![Export](https://github.com/Hido1994/madmp/blob/master/docs/images/trigger_download.png?raw=true "Trigger download")
3. The downloaded file contains a dataset block which simply can be added to a maDMP.  
![Exported](https://github.com/Hido1994/madmp/blob/master/docs/images/exported_json.png?raw=true "Exported JSON")

# DMP Files
* DMP and maDMP of David Hinterndorfer  
[DMP](https://github.com/Hido1994/madmp/blob/master/docs/dmp_dcc_01256409.pdf)  
[maDMP](https://github.com/Hido1994/madmp/blob/master/docs/exercise2_dmp_01526409.json)

* DMP and maDMP of Ahmad Alhirthani  
[DMP](https://github.com/Hido1994/madmp/blob/master/docs/dmp_dcc_11848870.pdf)  
[maDMP](https://github.com/Hido1994/madmp/blob/master/docs/exercise2_dmp_11848870.json)  

* Full maDMP for testing
[Full maDMP](https://github.com/Hido1994/madmp/blob/master/docs/testingmaDMP.json)  


* Schema used to validate
[Full schema](https://github.com/Hido1994/madmp/blob/master/src/main/resources/schema.json)  


