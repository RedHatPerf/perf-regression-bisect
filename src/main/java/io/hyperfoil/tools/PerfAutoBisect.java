package io.hyperfoil.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hyperfoil.tools.tools.GitBisect;
import io.hyperfoil.tools.tools.QDupRunner;
import io.hyperfoil.tools.validate.Validator;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import static io.hyperfoil.tools.tools.Util.*;

class PerfAutoBisect {

    public static void main(String... args) {
        AeshRuntimeRunner.builder().command(Bisector.class).args(args).execute();
    }

    @CommandDefinition(name = "perfAutoBisect", description = "io.hyperfoil.tools.perfAutoBisect to a")
    public static class Bisector implements Command {

        private static final Logger logger = Logger.getLogger(PerfAutoBisect.class);

        @Option(shortName = 'c', name = "config", description = "Config File location")
        private String configPath;

        @Option(shortName = 'o', name = "--out", description = "Output file location")
        private File outputFile;
        private static final Queue<Runnable> actions = new LinkedList<>();
        static GitBisect bisect;
        static QDupRunner runner;

        static Validator validator;

        private String badCommitID;

        public String getBadCommitID() {
            return badCommitID;
        }

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws InterruptedException {
            try {
                logger.infov("Reading config from: {0}", configPath);

                File configFile = new File(configPath);
                if (!configFile.exists()) {
                    logger.errorv("Config file does not exist: {0}", configPath);
                    return CommandResult.FAILURE;
                }

                initialize(configFile);

                //loop until we run out of actions to perform
                while (!actions.isEmpty()) {
                    actions.remove().run();
                }

                return CommandResult.SUCCESS;
            } catch (Exception e) {
                logger.errorv("Error occurred: {0}", e.getMessage());
                return CommandResult.FAILURE;
            }
        }

        void initialize(File configFile) {

            try {
                JsonNode json = new ObjectMapper().readTree(configFile);

                //Initialize git repo
                bisect = GitBisect.Builder.instance()
                        .remoteRepo(getJsonScalar(json, "/project/repoUrl"))
                        .badCommit(getJsonScalar(json, "/project/badCommit"))
                        .goodCommit(getJsonScalar(json, "/project/goodCommit"))
                        .localDir(createTempPath("perfbisect-"))
                        .build();

                actions.add(() -> bisect.checkoutRepo());
                actions.add(() -> bisect.checkoutCommit(getJsonScalar(json, "/project/badCommit")));
                actions.add(() -> bisect.initializeBisect());

                //Initialize qDup
                runner = QDupRunner.Builder.instance()
                        .remoteRepo(getJsonScalar(json, "/qDup/repoUrl"))
                        .branch(getJsonScalar(json, "/qDup/branch"))
                        .scriptFile(getJsonScalar(json, "/qDup/scriptFile"))
                        .params(getJsonMap(json, "/qDup/params"))
                        .credentials(getJsonMap(json, "/qDup/credentials"))
                        .commitParam(getJsonScalar(json, "/qDup/commitParam"))
                        .localDir(createTempPath("perf-qDup-"))
                        .build();

                actions.add(() -> runner.checkoutScriptRepo());


                Path tempDirWithPrefix = createTempPath("qDup-");
                actions.add(() -> runner.runIteration(tempDirWithPrefix, null));
                actions.add(() -> checkResult(tempDirWithPrefix));


                //Initialize validator
                validator = Validator.buildValidator(getJsonNode(json, "/validator"));


            } catch (IOException ioe) {
                logger.errorv("Could not parse file: {0}; {1}", configFile, ioe.getMessage());
            }
        }

        private void checkResult(Path tempDirWithPrefix) {
            actions.add(() -> logger.info("Comparing result"));

            GitBisect.CommitType type = validator.validateResult(tempDirWithPrefix) ? GitBisect.CommitType.GOOD : GitBisect.CommitType.BAD;

            bisect.markResult(bisect.getCurrentCommitID(), type);

            if (!bisect.isComplete()) {// run qDup script with next bisected git commit
                Path newTempDirWithPrefix = createTempPath("qDup-");
                actions.add(() -> runner.runIteration(newTempDirWithPrefix, "-S", runner.config.commitParam.concat("=").concat(bisect.getCurrentCommitID())));
                actions.add(() -> checkResult(newTempDirWithPrefix));
                tempDirWithPrefix = null;

            } else { //found bad commit!
                actions.add(() -> logger.infov("Found Bad commitID: {0}", bisect.getBadCommit()));
                badCommitID = bisect.getBadCommit();
                writeResultToFile();
            }
        }

        private void writeResultToFile() {
            if (outputFile != null) {
                if (!outputFile.exists()) {
                    try {
                        outputFile.createNewFile();
                        FileWriter myWriter = new FileWriter(outputFile);
                        myWriter.write(badCommitID);
                        myWriter.close();
                    } catch (IOException ioException) {
                        logger.errorv("Could not create output file: {0} ({1})", outputFile.getAbsolutePath(), ioException.getMessage());
                    }
                }


            }
        }

    }
}
