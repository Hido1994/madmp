# IN PROGRESS - Data Stewardship Exercise 2

# Introduction
This can be used as extension to Dataverse. It provides to functionalities to support maDMP in Dataverse.
One feature to import data from an maDMP and a second to export datasets as JSON in a RDA DMP Common Standard compliant syntax.

# Prerequisite
To use the project there must be a running instance of dataverse available.
Also the workflow has to be modified over the api so that dataverse talk to this application.

# MaDMP - Workflow
The first feature is ....

![Workflow](https://github.com/Hido1994/madmp/blob/master/docs/workflow.png?raw=true "MaDMP - Workflow process")

## Installation & Configuration
* Create workflow
```
curl -X POST -H "Content-Type: application/json" \
 -d '{"name":"maDMP workflow","steps":[{"provider":":internal","stepType":"http/sr","parameters":{"url":"http://localhost:8081/madmp/${invocationId}/${dataset.id}","method":"POST","contentType":"text/plain","body":"","expectedResponse":"OK.*"}}]}' \
 http://localhost:8080/api/admin/workflows/
```

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

```
curl -X POST -H 'Content-type: application/json' \
-d '{"displayName":"maDMP Export","description":"Export as maDMP block.","type":"explore","contentType":"application/json","toolUrl":"http://localhost:8081/madmp/ext","toolParameters":{"queryParameters":[{"fileid":"{fileId}"},{"key":"{apiToken}"}]}}' http://localhost:8080/api/admin/externalTools
```


## Execution

