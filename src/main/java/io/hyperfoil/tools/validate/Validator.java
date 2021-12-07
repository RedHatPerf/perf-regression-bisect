package io.hyperfoil.tools.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static io.hyperfoil.tools.tools.Util.getJsonNode;

public abstract class Validator {

    private static Logger logger = Logger.getLogger(Validator.class);

    public abstract boolean validateResult(Path resultPath);

    public static Validator buildValidator(JsonNode validatorNode) {

        if (!(validatorNode instanceof ObjectNode)) {

            String errMsg = "Validator should be defined as an Object type";
            logger.errorv(errMsg);
            throw new RuntimeException(errMsg);
        }

        List<String> validators = new LinkedList<>();
        ((ObjectNode)validatorNode).fieldNames().forEachRemaining(fieldName -> validators.add(fieldName));
        if (validators.size() > 1 || validators.size() == 0) {
            String errMsg = "Only one validator should be defined";
            logger.errorv(errMsg);
            throw new RuntimeException(errMsg);
        }
        String validatorType = validators.get(0);

        JsonNode validatorConfig = getJsonNode(validatorNode, "/".concat(validatorType));

        switch (validatorType) {
            case "ScalarFileLimitValidator":
                return new ScalarFileLimitValidator(validatorConfig);
            default:
                throw new RuntimeException(String.format("Could not build validator: %s", validatorType));
        }
    }
}
