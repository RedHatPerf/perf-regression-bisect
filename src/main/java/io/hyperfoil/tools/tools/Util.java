package io.hyperfoil.tools.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Util {
    private static final Logger logger = Logger.getLogger(Util.class);

    public static Path createTempPath(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (NullPointerException | IllegalArgumentException | UnsupportedOperationException | IOException | SecurityException exception) {
            logger.error("Could not create temporary directory");
            System.exit(1);
        }
        return null;
    }

    public static Map<String, String> getJsonMap(JsonNode json, String pathExpr) {
        JsonNode node = getNode(json, pathExpr);
        Map<String, String> mapping = new HashMap<>();
        validJsonNode(() -> !(node instanceof ObjectNode), String.format("Missing Param Object at JSON expr: %s", pathExpr));
        node.fieldNames().forEachRemaining(fieldName -> mapping.put(fieldName, node.get(fieldName).textValue()));
        return mapping;
    }

    public static String getJsonScalar(JsonNode json, String pathExpr) {
        return getNode(json, pathExpr).asText();
    }

    public static JsonNode getJsonNode(JsonNode json, String pathExpr){
        return getNode(json, pathExpr);
    }

    private static JsonNode getNode(JsonNode jsonNode, String pathExpr){
        JsonNode node = jsonNode.at(pathExpr);
        validJsonNode(() -> node.isMissingNode(), String.format("Could not evaluate JSON expr: %s", pathExpr));
        return node;
    }



    private static void validJsonNode(Supplier<Boolean> validationFunction, String msg){
        if(validationFunction.get()){
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

}
