package io.hyperfoil.tools.validate;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;


public class ScalarFileLimitValidatorTest {

    private static final String validator_config= "{\"filePath\": \"localhost/result.out\", \"limit\": \"68000.0\"}";

    static private Validator validator;

    @BeforeAll
    static void setup(){
    }

    @Test
    void testFileValidator(){

        URL result1Url =  ScalarFileLimitValidatorTest.class.getClassLoader().getResource("scalarFiles/result_1.out") ;
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("filePath", result1Url.getPath());
        node.put("limit", "68000.0");



    }
}
