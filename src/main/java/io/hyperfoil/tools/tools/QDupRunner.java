package io.hyperfoil.tools.tools;

import io.hyperfoil.tools.qdup.QDup;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QDupRunner {

    public static class qDupConfig {
        public Path localDir;
        public String remoteRepo;
        public String branch;
        public String scriptFile;
        public List<String> utilityScripts = new ArrayList<>();
        public Map<String, String> params = new HashMap<>();

        public String user;
        public String password;
        public String commitParam;
    }

    static Logger logger = Logger.getLogger(QDupRunner.class);

    public qDupConfig config;

    public QDupRunner(qDupConfig config) {
        this.config = config;
    }

    public void runIteration(Path tempDirWithPrefix, String... args) {
        logger.info("Running iteration");
        run(this.config, tempDirWithPrefix, args);
    }

    public void checkoutScriptRepo() {
        logger.infov("Checking out qDup Script Repo: {0}, in: {1}", config.remoteRepo, config.localDir.toString());
        try {
            Git.cloneRepository()
                    .setURI(this.config.remoteRepo)
                    .setDirectory(this.config.localDir.toFile())
                    .call();

        } catch (GitAPIException e) {
            String errMsg = String.format("Failed to checkout git repo: %s\n%s", this.config.remoteRepo, e.getMessage());
            logger.errorv(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    public static class Builder {
        private static final QDupRunner.qDupConfig config = new QDupRunner.qDupConfig();

        public static QDupRunner.Builder instance() {
            return new QDupRunner.Builder();
        }

        public Builder remoteRepo(String repoUrl) {
            config.remoteRepo = repoUrl;
            return this;
        }

        public Builder branch(String branch) {
            config.branch = branch;
            return this;
        }

        public QDupRunner build() {
            return new QDupRunner(this.config);
        }

        public Builder params(Map params) {
            config.params.putAll(params);
            return this;
        }

        public Builder credentials(Map<String, String> credentials) {
            if (credentials.containsKey("user")) {
                config.user = credentials.get("user");
            } else {
                logger.warn("Credentials not set: user");
            }
            if (credentials.containsKey("pword")) {
                config.password = credentials.get("pword");
            } else {
                logger.warn("Credentials not set: pword");
            }
            return this;
        }

        public Builder localDir(Path tempDirectory) {
            this.config.localDir = tempDirectory;
            return this;
        }

        public Builder scriptFile(String scriptFile) {
            this.config.scriptFile = scriptFile;
            return this;
        }

        public Builder commitParam(String commitParam) {
            this.config.commitParam = commitParam;
            return this;
        }
    }

    private void run(qDupConfig config, Path tempDirWithPrefix, String... args) {

        String projectPath = this.config.localDir.toString();

        if (projectPath.equals("")) {
            System.err.println("Could not determine project directory");
            System.exit(1);
        }

        String[] qDupBaseArgs = {"-B"
                , tempDirWithPrefix.toString()
                , projectPath.concat("/").concat(config.scriptFile)
        };

        String[] qDupArgs = ArrayUtils.addAll(qDupBaseArgs, args);

        QDup.main(qDupArgs);
    }
}
