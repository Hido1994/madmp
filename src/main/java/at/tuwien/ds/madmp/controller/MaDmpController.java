package at.tuwien.ds.madmp.controller;

import java.io.IOException;
import java.io.InputStream;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class MaDmpController {
    @Value("${api.key}")
    private String apiKey;

    private RestTemplate restTemplate;

    private static Logger LOG = LoggerFactory.getLogger(MaDmpController.class);

    public MaDmpController() {
        restTemplate = new RestTemplate();

    }

    @PostMapping("/madmp/{invocationId}/{datasetId}")
    ResponseEntity<String> madmpProcess(@PathVariable String invocationId,
                        @PathVariable String datasetId) throws IOException {
        LOG.info("String maDmp process {} / {}", invocationId, datasetId);

        try {
            InputStream schemaInputStream = new ClassPathResource("schema.json").getInputStream();

            JSONObject jsonSchema = new JSONObject(
                new JSONTokener(schemaInputStream));


            String url
                = "http://localhost:8080/api/datasets/" + datasetId + "?key=" + apiKey;
            ResponseEntity<String> response
                = restTemplate.getForEntity(url, String.class);

            JSONObject dataset=new JSONObject(response.getBody());

            LOG.info("Retrieved from {} dataset: {}",url, dataset.toString());

            JSONArray filesInDataset = dataset.getJSONObject("data").getJSONObject(
                "latestVersion").getJSONArray("files");

            Integer fileId = null;
            for (int i = 0; i < filesInDataset.length(); i++) {
                JSONObject file = filesInDataset.getJSONObject(i);
                if (file.getString("label").equals("madmp.json")) {
                    fileId = file.getJSONObject("dataFile").getInt("id");
                }
            }

            if (fileId != null) {
                url = "http://localhost:8080/api/access/datafile/" + fileId + "?key=" + apiKey;
                response
                    = restTemplate.getForEntity(url, String.class);

                JSONObject maDmpJson=new JSONObject(response.getBody());

                LOG.info("Retrieved from {} maDMP: {}", url, maDmpJson);

                SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(jsonSchema)
                    .draftV7Support()
                    .build();

                Schema schema = loader.load(jsonSchema);
                schema.validate(maDmpJson);

                LOG.info("Validation successful");
            } else {
                LOG.error("No maDMP found!");
            }

/*
            url = "http://localhost:8080/api/workflows/" + invocationId ;
            response
                = restTemplate.postForEntity(url, "OK", String.class);
*/

/*            LOG.info(response.getBody());*/
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (JSONException e) {
            LOG.error("An error occured while reading maDmp or schema", e);
            throw new MaDmpException("An error occured while reading maDmp or schema", e);
        } catch (ValidationException e) {
            LOG.error("An error occured while validating maDmp or schema", e);
            for (ValidationException validationException : e.getCausingExceptions()) {
                LOG.error("Violation found: " + validationException.getErrorMessage());
            }
            throw new MaDmpException("An error occured while validating maDmp or schema", e);
        }

    }

    @GetMapping("/hello")
    String helloWorld() throws IOException {
        return "Hello World";
    }
}
