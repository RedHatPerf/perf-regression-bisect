package io.hyperfoil.tools.tools;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;

public class GitBisect {

    Logger logger = Logger.getLogger(GitBisect.class);

    private static Git git;

    public enum CommitType {
        GOOD,
        BAD
    }

    final bisectConfig config;
    private String badCommit;
    private String goodCommit;
    private String currentCommit;

    private static boolean complete = false;

    public GitBisect(bisectConfig config) {
        this.config = config;
    }

    public static boolean isComplete() {
        return complete;
    }

    public void checkoutRepo() {
        logger.infov("Checking out repo: {0}, in: {1}", this.config.remoteRepo, this.config.localDir.toString());
        try {
            git = Git.cloneRepository()
                    .setURI(this.config.remoteRepo)
                    .setDirectory(this.config.localDir.toFile())
                    .call();

        } catch (GitAPIException e) {
            String errMsg = String.format("Failed to checkout git repo: %s\n%s", this.config.remoteRepo, e.getMessage());
            logger.errorv(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    public void checkoutCommit(String commitHash) {
        logger.infov("Checking out commit: {0}", commitHash);
        try {
            git.checkout().setName(commitHash).call();
        } catch (GitAPIException e) {
            String errMsg = String.format("Failed to checkout comm: %s\n%s", commitHash, e.getMessage());
            logger.errorv(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    public void initializeBisect() {
        logger.infov("Setting up git bisect: (good) {0}}; (bad) {1}}", this.config.goodCommitID, this.config.badCommitID);
        markCommit(this.config.goodCommitID, CommitType.GOOD);
        markCommit(this.config.badCommitID, CommitType.BAD);
    }

    public void markResult(String commitID, CommitType type){
        markCommit(commitID, type);
        calcBisectCommit();
    }

    public void markCommit(String commitID, CommitType type) {
        switch (type) {
            case BAD -> markBadCommit(commitID);
            case GOOD -> markGoodCommit(commitID);
            default -> logger.warn("markCommit: Commit type not set!");
        }
    }

    public String getBadCommit() {
        return badCommit;
    }

    public String getGoodCommit() {
        return goodCommit;
    }

    public String getCurrentCommitID() {
        if (this.currentCommit == null) {
            calcBisectCommit();
        }
        return this.currentCommit;
    }

    private void calcBisectCommit() {
        logger.infov("Calculating bisect commit between:  {0} and {1}", this.goodCommit, this.badCommit);
        if (this.goodCommit == null || this.badCommit == null) {
            logger.error("Bad and Good commit ID's have not been set!");
        } else if (this.goodCommit.equals(this.badCommit)) {
            logger.warn("Bad and Good commit ID's are the same. Can not bisect!");
            this.currentCommit = this.goodCommit;
            complete = true;
        } else {
            try {

                LinkedList<String> commitIDs = new LinkedList<>();

                ObjectId goodCommitRef = git.getRepository().resolve(this.goodCommit);
                ObjectId badCommitRef = git.getRepository().resolve(this.badCommit);

                git.log().addRange(goodCommitRef, badCommitRef).call().forEach(revCommit -> {
                    if( !revCommit.getName().equals(this.badCommit) && !revCommit.getName().equals(this.goodCommit)) {
                        commitIDs.add(revCommit.getName());
                    }
                });

                int commitSize = commitIDs.size();

                if (commitSize == 0) {
                    logger.infov("No commits found. Bad commit: {0}", this.badCommit);
                    complete = true;
                    this.currentCommit = null;
                } else {
                    this.currentCommit = commitIDs.get( commitSize / 2);
                }

                logger.infov("Bisect commitID: {0}", this.currentCommit);

            } catch (GitAPIException | IOException e) {
                logger.errorv("Error occurred querying git repo: {0}", e.getMessage());
            }
        }
    }

    private void markBadCommit(String commitID) {
        logger.infov("Set BAD commit: {0}", commitID);
        this.badCommit = commitID;
    }

    private void markGoodCommit(String commitID) {
        logger.infov("Set GOOD commit: {0}", commitID);
        this.goodCommit = commitID;
    }

    public static class bisectConfig {
        public Path localDir;
        public String remoteRepo;
        public String badCommitID;
        public String goodCommitID;
    }

    public static class Builder {

        private static final bisectConfig config = new bisectConfig();

        public static Builder instance() {
            return new Builder();
        }

        public Builder remoteRepo(String repo) {
            this.config.remoteRepo = repo;
            return this;
        }

        public Builder badCommit(String commidID) {
            this.config.badCommitID = commidID;
            return this;
        }

        public Builder goodCommit(String commidID) {
            this.config.goodCommitID = commidID;
            return this;
        }

        public GitBisect build() {
            return new GitBisect(this.config);
        }

        public Builder localDir(Path tempDirectory) {
            this.config.localDir = tempDirectory;
            return this;
        }
    }

}
