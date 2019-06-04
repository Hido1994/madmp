package at.tuwien.ds.madmp.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import at.tuwien.ds.madmp.exception.MaDmpException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class MaDmpController {
    private static Logger LOG = LoggerFactory.getLogger(MaDmpController.class);

    private RestTemplate restTemplate;

    @Value("${dataverse.address}")
    private String dataverseAddress;

    @Value("${dataverse.api.key}")
    private String apiKey;

    @Value("${workflow.madmp.filename}")
    private String maDmpFilename;

    private JSONObject jsonSchema;

    private JSONObject datasetTemplate;

    public MaDmpController() {
        LOG.info("Setup Controller");
        restTemplate = new RestTemplate();

        try {
            LOG.info("Load schema from classpath");
            InputStream schemaInputStream = new ClassPathResource("schema.json").getInputStream();
            jsonSchema = new JSONObject(
                new JSONTokener(schemaInputStream));

            InputStream datasetTemplateInputStream = new ClassPathResource(
                "dataset-template.json").getInputStream();
            datasetTemplate = new JSONObject(
                new JSONTokener(datasetTemplateInputStream));
        } catch (IOException e) {
            LOG.error("Failed to retrieve schema from classpath", e);
            throw new MaDmpException("Failed to retrieve schema from classpath", e);
        }

    }

    @GetMapping("/madmp/ext")
    ResponseEntity<InputStreamResource> export(@RequestParam(name = "fileid") String fileId,
                                               @RequestParam(name = "datasetid") String datasetId) {
        LOG.info("External Tool called - fileid: {}, datasetid {}", fileId, datasetId);

        String url = dataverseAddress + "/api/datasets/" + datasetId + "?key=" + apiKey;
        ResponseEntity<String> response
            = restTemplate.getForEntity(url, String.class);
        JSONObject metadataJson = new JSONObject(response.getBody());
        LOG.debug("Retrieved from {} maDMP: {}", url, metadataJson);

        JSONObject latestVersion = metadataJson.getJSONObject("data")
            .getJSONObject("latestVersion");
        JSONArray metadataFields = latestVersion
            .getJSONObject("metadataBlocks")
            .getJSONObject("citation")
            .getJSONArray("fields");

        String title = "";
        String description = "";
        String sensitiveData = "";
        String personalData = "";
        for (int i = 0; i < metadataFields.length(); i++) {
            JSONObject field = metadataFields.getJSONObject(i);
            if (field.getString("typeName").equals("title")) {
                title = field.getString("value");
            } else if (field.getString("typeName").equals("dsDescription")) {
                description = field.getJSONArray("value").getJSONObject(0)
                    .getJSONObject("dsDescriptionValue").getString("value");
            } else if (field.getString("typeName").equals("notesText")) {
                String noteText = field.getString("value");
                String[] split = noteText.split(",");
                if (split.length == 2 && split[0].contains("sensitive_data")
                    && split[1].contains("personal_data")) {
                    sensitiveData = split[0].replace("sensitive_data=", "").trim();
                    personalData = split[1].replace("personal_data=", "").trim();
                }
            }
        }

        JSONArray files = latestVersion
            .getJSONArray("files");

        JSONArray distributions = new JSONArray();

        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);
            String distributionTitle = file.getString("label");
            Boolean restricted = file.getBoolean("restricted");
            Integer fileSize = file.getJSONObject("dataFile").getInt("filesize");

            JSONObject distribution = new JSONObject();
            distribution.put("title", distributionTitle);
            distribution.put("data_access", restricted ?"closed":"open");
            distribution.put("byte_size", fileSize);

            String termOfUse = latestVersion.getString("termsOfUse");
            if (!termOfUse.isEmpty()) {
                String[] split = termOfUse.split(",");
                if (split.length == 2) {
                    distribution.put("license",
                        new JSONArray().put(new JSONObject()
                            .put("license_ref", split[0].trim())
                            .put("start_date", split[1].trim())));
                }
            }
            distributions.put(distribution);
        }

        //Build JSON
        JSONObject result = new JSONObject().put("title", title)
            .put("description", description)
            .put("type", "dataset")
            .put("personal_data", (!personalData.isEmpty() ? personalData : "unknown"))
            .put("sensitive_data", (!sensitiveData.isEmpty() ? sensitiveData : "unknown"))
            .put("distribution", distributions);


        InputStreamResource resource = new InputStreamResource(
            new ByteArrayInputStream(result.toString(2).getBytes()));

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"dataset-" + datasetId + ".json\"")
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .contentLength(result.toString(2).length())
            .body(resource);
    }

    @PostMapping("/madmp/{invocationId}/{datasetId}")
    void madmpProcess(@PathVariable String invocationId,
                      @PathVariable String datasetId,
                      HttpServletResponse httpServletResponse) throws IOException {
        LOG.info("String maDmp process - invocationId: {}, datasetId {}", invocationId, datasetId);


        JSONObject dataset = getDataset(datasetId);
        Integer fileId = findFileIdInDatasetByName(dataset, maDmpFilename);

        if (fileId != null) {
            try {
                LOG.info("maDMP found in dataset");

                JSONObject maDmpJson = getFileById(fileId);

                validateMaDmp(maDmpJson);

                //Create dataverse and datasets
                JSONObject dmpObject = maDmpJson.getJSONObject("dmp");
                String title = dmpObject.getString("title");
                String description = dmpObject.getString("description");
                JSONObject contact = dmpObject.getJSONObject("contact");

                String dataverseAlias = createDataverse(
                    dataset.getJSONObject("data").getString("publisher"), title,
                    description, contact.getString("mail"));


                JSONArray maDmpDatasets = dmpObject.getJSONArray("dataset");
                for (int i = 0; i < maDmpDatasets.length(); i++) {
                    createDataset(dataverseAlias, contact, maDmpDatasets.getJSONObject(i));
                }
            } catch (JSONException e) {
                LOG.error("An error occured while reading maDmp or schema", e);
                //Don't throw exception because uploading maDmp should be optional
                //throw new MaDmpException("An error occured while reading maDmp or schema", e);
            } catch (ValidationException e) {
                LOG.error("An error occured while validating maDmp or schema", e);
                for (ValidationException validationException : e.getCausingExceptions()) {
                    LOG.error("Violation found: " + validationException.getErrorMessage());
                }
                //throw new MaDmpException("An error occured while validating maDmp or
                // schema", e);
            }


            String url =
                dataverseAddress + "/api/datasets/" + datasetId + "/versions/:draft?key=" + apiKey;
            restTemplate.delete(url);


        } else {
            LOG.info("No maDMP found!");
        }

        //Send response
        PrintWriter writer = httpServletResponse.getWriter();
        writer.println("OK");
        writer.flush();

        //Unlock dataset
        String url = dataverseAddress + "/api/workflows/" + invocationId;
        restTemplate.postForEntity(url, "FAILURE",
            String.class);   //Send FAILURE for testing, change to OK in production to publish
        // dataset
        LOG.debug("Publishing dataset over {}", url);

    }


    private Integer createDataset(String dataverseAlias,
                                  JSONObject contact, JSONObject datasetMaDmp) {
        JSONObject datasetJson = new JSONObject(datasetTemplate.toString());
        JSONObject datasetVersion = datasetJson.getJSONObject("datasetVersion");
        JSONArray metadataFields = datasetVersion
            .getJSONObject("metadataBlocks")
            .getJSONObject("citation")
            .getJSONArray("fields");

        if (datasetMaDmp.has("distribution") && datasetMaDmp.getJSONArray(
            "distribution").getJSONObject(0).has("license")) {
            JSONObject license = datasetMaDmp
                .getJSONArray("distribution").getJSONObject(0)
                .getJSONArray("license").getJSONObject(0);

            datasetVersion.put("termsOfUse",
                license.getString("license_ref") + "," + license.getString("start_date"));
        }


        for (int i = 0; i < metadataFields.length(); i++) {
            JSONObject field = metadataFields.getJSONObject(i);
            switch (field.getString("typeName")) {
                case "title":
                    field.put("value", datasetMaDmp.getString("title"));
                    break;
                case "author":
                    field.getJSONArray("value").getJSONObject(0)
                        .getJSONObject("authorName")
                        .put("value", contact.getString("name"));
                    break;
                case "datasetContact":
                    JSONArray jsonArray = field.getJSONArray("value");
                    jsonArray.getJSONObject(0)
                        .getJSONObject("datasetContactEmail").put("value",
                        contact.getString("mail"));
                    jsonArray.getJSONObject(0)
                        .getJSONObject("datasetContactName").put("value",
                        contact.getString("name"));
                    break;
                case "dsDescription":
                    if (datasetMaDmp.has("description")) {
                        field.getJSONArray("value").getJSONObject(0)
                            .getJSONObject("dsDescriptionValue").put("value",
                            datasetMaDmp.getString("description"));
                    }
                    break;
                case "notesText":
                    field.put("value",
                        "sensitive_data=" + datasetMaDmp.getString("sensitive_data") +
                            ", personal_data=" + datasetMaDmp.getString("personal_data"));
                    break;
            }
        }
        //TODO: Add other metadata to datasets


        String url
            = dataverseAddress + "/api/dataverses/" + dataverseAlias + "/datasets?key=" + apiKey;

        LOG.debug("Create dataverse over {}: {}", url, datasetJson.toString());
        ResponseEntity<String> response = restTemplate.postForEntity(url, datasetJson.toString(),
            String.class);


        JSONObject responseJson = new JSONObject(response.getBody());
        Integer datasetId = responseJson.getJSONObject("data").getInt("id");
        LOG.info("Dataset created {}", datasetId);
        return datasetId;
    }

    private String createDataverse(String parentDataverse, String name, String description,
                                   String mail) {
        String alias = UUID.randomUUID().toString();
        JSONObject dataverseJson = new JSONObject()
            .put("name", name)
            .put("alias", alias)
            .put("description", description)
            .put("dataverseContacts", new JSONArray()
                .put(new JSONObject()
                    .put("contactEmail", mail)))
            .put("dataverseType", "UNCATEGORIZED");
        String url
            = dataverseAddress + "/api/dataverses/" + parentDataverse + "?key=" + apiKey;

        LOG.debug("Create dataverse over {}: {}", url, dataverseJson.toString());
        ResponseEntity<String> response = restTemplate.postForEntity(url, dataverseJson.toString(),
            String.class);

        LOG.info("Dataverse created {}", alias);
        return alias;
    }

    private JSONObject getDataset(String datasetId) {
        String url
            = dataverseAddress + "/api/datasets/" + datasetId + "?key=" + apiKey;
        ResponseEntity<String> response
            = restTemplate.getForEntity(url, String.class);

        LOG.debug("Retrieved from {} dataset: {}", url, response.getBody());
        return new JSONObject(response.getBody());
    }

    private Integer findFileIdInDatasetByName(JSONObject dataset, String filename) {
        JSONArray filesInDataset = dataset.getJSONObject("data").getJSONObject(
            "latestVersion").getJSONArray("files");

        LOG.info("Search for maDMP in dataset");
        for (int i = 0; i < filesInDataset.length(); i++) {
            JSONObject file = filesInDataset.getJSONObject(i);
            if (file.getString("label").equals(filename)) {
                return file.getJSONObject("dataFile").getInt("id");
            }
        }
        return null;
    }

    private JSONObject getFileById(Integer fileId) {
        String url = dataverseAddress + "/api/access/datafile/" + fileId + "?key=" + apiKey;
        ResponseEntity<String> response
            = restTemplate.getForEntity(url, String.class);
        JSONObject maDmpJson = new JSONObject(response.getBody());
        LOG.debug("Retrieved from {} maDMP: {}", url, maDmpJson);

        return maDmpJson;
    }

    private void validateMaDmp(JSONObject jsonToValidate) {
        SchemaLoader loader = SchemaLoader.builder()
            .schemaJson(jsonSchema)
            .draftV7Support()
            .build();
        Schema schema = loader.load(jsonSchema);
        schema.validate(jsonToValidate);
        LOG.info("Validation successful");
    }
}
