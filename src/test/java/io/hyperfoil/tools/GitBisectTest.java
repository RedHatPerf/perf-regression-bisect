package io.hyperfoil.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GitBisectTest {

    static  PerfAutoBisect.Bisector perfAutoBisect;

    @BeforeAll
    public static void initialize(){
        String configPath = GitBisectTest.class.getClassLoader().getResource("test_config.json").getPath();
        File configFile = new File(configPath);
        perfAutoBisect = new PerfAutoBisect.Bisector();
        perfAutoBisect.initialize(configFile);
    }

    @Test
    public void testGitBisect(){

        perfAutoBisect.bisect.checkoutRepo();
        perfAutoBisect.bisect.initializeBisect();

        String bisectID = perfAutoBisect.bisect.getCurrentCommitID();

        assertNotNull(bisectID);

        assertEquals("547b8e2cb1cb0bd6184611bc1c84e90f652328e7", bisectID);

    }
}
;