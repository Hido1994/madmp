package at.tuwien.ds.madmp.controller;

import at.tuwien.ds.madmp.exception.MaDmpException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class MaDmpController {
    private RestTemplate restTemplate;

    private static Logger LOG = LoggerFactory.getLogger(MaDmpController.class);

    public MaDmpController() {
        restTemplate = new RestTemplate();

    }

    @GetMapping("/madmp")
    String madmpProcess() throws IOException {
        LOG.info("String maDmp process");

        try {
            InputStream schemaInputStream = new ClassPathResource("schema.json").getInputStream();

            JSONObject jsonSchema = new JSONObject(
                    new JSONTokener(schemaInputStream));

            InputStream maDmpInputStream = new ClassPathResource("dmp.json").getInputStream();

            JSONObject maDmpJson = new JSONObject(
                    new JSONTokener(maDmpInputStream));

            SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(jsonSchema)
                    .draftV7Support()
                    .build();

            Schema schema = loader.load(jsonSchema);
            schema.validate(maDmpJson);


            String url
                    = "http://localhost:8080/hello";
            ResponseEntity<String> response
                    = restTemplate.getForEntity(url, String.class);

            LOG.info("Validation successful");


            return response.getBody();
        } catch (JSONException e) {
            LOG.error("An error occured while reading maDmp or schema", e);
            throw new MaDmpException("An error occured while reading maDmp or schema", e);
        } catch (ValidationException e) {
            LOG.error("An error occured while validating maDmp or schema", e);
            for(ValidationException validationException:e.getCausingExceptions()){
                LOG.error("Violation found: "+validationException.getErrorMessage());
            }
            throw new MaDmpException("An error occured while validating maDmp or schema", e);
        }

    }

    @GetMapping("/hello")
    String helloWorld() throws IOException {
        return "Hello World";
    }
}
