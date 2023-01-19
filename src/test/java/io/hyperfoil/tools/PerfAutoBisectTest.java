package io.hyperfoil.tools;


import io.hyperfoil.tools.yaup.json.Json;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.CommandResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class PerfAutoBisectTest {

    static PerfAutoBisect.Bisector perfAutoBisect;

    @BeforeAll
    public static void initialize(){
        perfAutoBisect = new PerfAutoBisect.Bisector();
    }

    @Test
    public void runAutoBisect(){

        URL configURL = PerfAutoBisectTest.class.getClassLoader().getResource("test_config.json");

        String outputFile = "/tmp/perfBisecttestOutput.json";

        String configFile = configURL.getPath();

        String[] args= {
                "-c",
                configFile,
                "-o",
                outputFile
        };

        CommandResult result = AeshRuntimeRunner.builder().command(perfAutoBisect).args(args).execute();

        //TODO: need Aesh release to be able to reason about the result of the command
        assertEquals(CommandResult.SUCCESS, result);

        String json = null;
        try {
            json = new String(Files.readAllBytes(Paths.get(outputFile)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if( json != null ) {
            Json resultJson = Json.fromString(json);
            assertTrue(resultJson.has("bad-commit"));
          assertEquals("d4b9d6a49972817b4acd8dfce00872aafe1721e3", resultJson.getString("bad-commit"));
        } else {
            fail(String.format("Could not parse json file: %s", outputFile ));
        }


    }

}
