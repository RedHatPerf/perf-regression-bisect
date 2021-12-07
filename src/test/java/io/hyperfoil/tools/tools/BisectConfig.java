package io.hyperfoil.tools.tools;


//side-step package private access :(
public class BisectConfig {

    public static GitBisect.bisectConfig getConfig(GitBisect gitBisect){
        return gitBisect.config;
    }
}
