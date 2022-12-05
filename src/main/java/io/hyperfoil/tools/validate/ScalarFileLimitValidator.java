package io.hyperfoil.tools.validate;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.file.Path;

public class ScalarFileLimitValidator extends Validator {
    private static final Logger logger = Logger.getLogger(ScalarFileLimitValidator.class);

    private String resultFilePath;
    private Float limit;

    public ScalarFileLimitValidator(JsonNode validatorConfig) {

        //TODO:: proper error handling
        this.resultFilePath = validatorConfig.get("filePath").asText();
        this.limit = Float.parseFloat(validatorConfig.get("limit").asText());
    }

    @Override
    public boolean validateResult(Path resultPath) {
        File resultFile = resultPath.resolve(resultFilePath).toFile();
        logger.infov("Checking: {0}", resultFile.getAbsolutePath());

        try (BufferedReader reader =new BufferedReader(new FileReader(resultFile)))
        {
            String text = reader.readLine();
            logger.infov("Value is : {0}", text);

            Float result = Float.parseFloat(text);
            if(result <= limit){
                return true;
            }
            else {
                return false;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return false;
    }
}
