package io.hyperfoil.tools;


import org.aesh.AeshRuntimeRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerfAutoBisectTest {

    static PerfAutoBisect perfAutoBisect;

    @BeforeAll
    public static void initialize(){
        perfAutoBisect = new PerfAutoBisect();
    }

    @Test
    public void runAutoBisect(){

        URL configURL = PerfAutoBisectTest.class.getClassLoader().getResource("test_config.json");

        String configFile = configURL.getPath();

        String[] args= {"-c", configFile};

        AeshRuntimeRunner.builder().command(PerfAutoBisect.Bisector.class).args(args).execute();

        //TODO: need Aesh release to be able to reason about the result of the command
//        assertEquals(CommandResult.SUCCESS, result);

        //ATM can not pass already instantiated Command Class into AeshRuntimeRunner to reason about state of command after it has been run
        //assertEquals("d4b9d6a49972817b4acd8dfce00872aafe1721e3", perfAutoBisect.getBadCommitID());


    }

}
