package io.hyperfoil.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.schema.ValidationMessage;
import io.hyperfoil.tools.qdup.QDup;
import io.hyperfoil.tools.qdup.cmd.impl.GitBisect;
import io.hyperfoil.tools.qdup.cmd.impl.QdupProcessCmd;
import io.hyperfoil.tools.qdup.cmd.impl.ScalarFileLimitValidatorCmd;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.schema.Validator;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.logging.Logger;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static io.hyperfoil.tools.Utils.copyFile;

class PerfAutoBisect {

    public static void main(String... args) {
        AeshRuntimeRunner.builder().command(Bisector.class).args(args).execute();
    }

    @CommandDefinition(name = "perfAutoBisect", description = "Perform Git Bisect with performance tests to identify which git commit introduced a performance regression")
    public static class Bisector implements Command {

        private static final Logger logger = Logger.getLogger(PerfAutoBisect.class);

        @Option(shortName = 'c', name = "config", description = "Config File location", required = true)
        private File configFile;

        @Option(shortName = 'o', name = "--out", description = "Output file location", required = true)
        private File outputFile;

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            try {
                logger.infov("Reading config from: {0}", configFile);

                if (!configFile.exists()) {
                    logger.errorv("Config file does not exist: {0}", configFile);
                    return CommandResult.FAILURE;
                }

                //create temp directory
                Path tempDirWithPrefix = Files.createTempDirectory("perf-bisect-");
                Path bisectYml = tempDirWithPrefix.resolve("perfBisect.qdup.yaml");

                String confInp;
                try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("perfBisect.qdup.yaml");
                     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))
                ){
                    //load config into memory
                    confInp = bufferedReader.lines().collect(Collectors.joining());
                } catch (IOException e) {
                    logger.errorv("Could not copy the workload defintion"); //needs to be more user-friendly
                    return CommandResult.FAILURE;
                }

                //validate input json
                Validator validator = new Validator();
                try {
                    Set<ValidationMessage> errors =  validator.validate(confInp);
                    if ( errors.size() > 0) {
                        logger.errorv("The following errors occurred validating input json:");
                        errors.forEach(err -> logger.error(err.getMessage()));
                        return CommandResult.FAILURE;
                    }
                } catch (JsonProcessingException jpe){
                    logger.errorv("Could not parse input json");
                    return CommandResult.FAILURE;
                }

                String curUser = System.getProperty("user.name");
                String curHost = InetAddress.getLocalHost().getHostName();

                //build custom qDup args
                String[] args = new String[]{
                        "-B" ,
                        tempDirWithPrefix.toAbsolutePath().toString(),
                        bisectYml.toAbsolutePath().toString(),
                        "-S" ,
                        "INPUT=" + confInp,
                        //TODO: remove user/host override when we have local host runtime available
                        "-S" ,
                        "USER=" + curUser,
                        "-S",
                        "HOST=" + curHost
                };

                //new qDup instance
                QDup qDup = new QDup(args);

                //register custom qDup commands with parser
                Parser parser = qDup.getYamlParser();
                GitBisect.GitBisectInitCmd.extendParse(parser);
                GitBisect.GitBisectCmd.extendParse(parser);
                GitBisect.GitBisectUpdateCmd.extendParse(parser);
                ScalarFileLimitValidatorCmd.extendParse(parser);
                QdupProcessCmd.extendParse(parser);

                //execute qDup
                if( qDup.run() ){
                    //complete correctly
                    //TODO:: get output file
                    File resultFile = new File(qDup.getOutputPath() + File.separator + curHost + File.separator + "result.json");
                    if ( resultFile.exists()) {
                        copyFile(resultFile, outputFile);

                        return CommandResult.SUCCESS;
                    } else {
                        logger.errorv("Could not find result file");
                        return CommandResult.FAILURE;
                    }
                } else {
                    //QDUP failed to run bisect
                    return CommandResult.FAILURE;
                }
            } catch (Exception e) {
                logger.errorv("Error occurred: {0}", e.getMessage());
                return CommandResult.FAILURE;
            }
        }

    }

}
