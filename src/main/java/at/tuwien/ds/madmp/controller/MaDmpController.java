package at.tuwien.ds.madmp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Value("${workflow.madmp.delete}")
    private Boolean deleteMaDmp;

    @Value("${workflow.madmp.filename}")
    private String maDmpFilename;

    private JSONObject jsonSchema;

    public MaDmpController() {
        LOG.info("Setup Controller");
        restTemplate = new RestTemplate();

        try {
            LOG.info("Load schema from classpath");
            InputStream schemaInputStream = new ClassPathResource("schema.json").getInputStream();
            jsonSchema = new JSONObject(
                new JSONTokener(schemaInputStream));
        } catch (IOException e) {
            LOG.error("Failed to retrieve schema from classpath", e);
            throw new MaDmpException("Failed to retrieve schema from classpath", e);
        }

    }

    @GetMapping("/madmp/ext")
    String export(){
        LOG.info("External Tool called");
        return "Hello World external Tool";
    }

    @PostMapping("/madmp/{invocationId}/{datasetId}")
    void madmpProcess(@PathVariable String invocationId,
                      @PathVariable String datasetId,
                      HttpServletResponse httpServletResponse) throws IOException {
        LOG.info("String maDmp process - invocationId: {}, datasetId {}", invocationId, datasetId);

        try {
            String url
                = dataverseAddress + "/api/datasets/" + datasetId + "?key=" + apiKey;
            ResponseEntity<String> response
                = restTemplate.getForEntity(url, String.class);
            JSONObject dataset = new JSONObject(response.getBody());

            LOG.debug("Retrieved from {} dataset: {}", url, dataset.toString());

            JSONArray filesInDataset = dataset.getJSONObject("data").getJSONObject(
                "latestVersion").getJSONArray("files");
            Integer fileId = null;
            for (int i = 0; i < filesInDataset.length(); i++) {
                JSONObject file = filesInDataset.getJSONObject(i);
                if (file.getString("label").equals(maDmpFilename)) {
                    fileId = file.getJSONObject("dataFile").getInt("id");
                }
            }

            if (fileId != null) {
                LOG.info("madmp.json found in dataset");

                url = dataverseAddress + "/api/access/datafile/" + fileId + "?key=" + apiKey;
                response
                    = restTemplate.getForEntity(url, String.class);
                JSONObject maDmpJson = new JSONObject(response.getBody());
                LOG.debug("Retrieved from {} maDMP: {}", url, maDmpJson);

                SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(jsonSchema)
                    .draftV7Support()
                    .build();
                Schema schema = loader.load(jsonSchema);
                schema.validate(maDmpJson);
                LOG.info("Validation successful");

                //Metadata update according madmp
                LOG.info("Updating metadata");

                JSONArray metadataFieldsArray = new JSONArray();

                JSONObject dmpObject = maDmpJson.getJSONObject("dmp");

                String title = dmpObject.getString("title");
                String description = dmpObject.getString("description");
                if (title != null) {
                    metadataFieldsArray.put(new JSONObject().put("typeName", "title")
                        .put("value", title));
                }
                if (description != null) {
                    metadataFieldsArray.put(new JSONObject().put("typeName", "dsDescription")
                        .put("value", new JSONArray()
                            .put(new JSONObject()
                                .put("dsDescriptionValue", new JSONObject()
                                        .put("typeName", "dsDescriptionValue")
                                        .put("value", description)))));
                }


                JSONObject metadataFields = new JSONObject().put("fields",
                    metadataFieldsArray);


                url = dataverseAddress + "/api/datasets/" + datasetId + "/editMetadata" +
                    "?key=" + apiKey + "&replace=true";
                LOG.debug("Update dataset metadata over {}: {}", url, metadataFields.toString());
                restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(metadataFields.toString()), String.class);

                if (deleteMaDmp) {
                    LOG.info("Deleting maDMP file.");
                    //TODO: delete dmp file
                }
            } else {
                LOG.info("No maDMP found!");
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
        } finally {
            //Send response
            PrintWriter writer = httpServletResponse.getWriter();
            writer.println("OK");
            writer.flush();

            String url = dataverseAddress + "/api/workflows/" + invocationId;
            restTemplate.postForEntity(url, "FAILURE",
                String.class);   //Send failure for testing
            LOG.debug("Publishing dataset over {}", url);
        }
    }
}
