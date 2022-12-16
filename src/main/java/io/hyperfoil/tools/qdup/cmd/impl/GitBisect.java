package io.hyperfoil.tools.qdup.cmd.impl;


import io.hyperfoil.tools.qdup.cmd.Cmd;
import io.hyperfoil.tools.qdup.cmd.Context;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.UUID;

public class GitBisect {

    private static final String STATE_VARIABLE = "gitBisect";
    private static final String STATE_GIT_DIR = "gitDir";
    private static final String STATE_BAD_COMMIT = "badCommit";
    private static final String STATE_GOOD_COMMIT = "goodCommit";
    private static final String STATE_CUR_COMMIT = "curCommit";
    private static final String COMPLETE_SIGNAL = "completeSignal";


    protected static Git git;

    public enum CommitType {
        GOOD,
        BAD
    }

    protected static String calcBisectCommit(String goodCommithash, String badCommitHash) {
        if (goodCommithash == null || badCommitHash == null) {
            return null;
        } else if (goodCommithash.equals(badCommitHash)) {
            return null;
        } else {
            try {

                LinkedList<String> commitIDs = new LinkedList<>();

                ObjectId goodCommitRef = git.getRepository().resolve(goodCommithash);
                ObjectId badCommitRef = git.getRepository().resolve(badCommitHash);

                git.log().addRange(goodCommitRef, badCommitRef).call().forEach(revCommit -> {
                    if( !revCommit.getName().equals(badCommitHash) && !revCommit.getName().equals(goodCommithash)) {
                        commitIDs.add(revCommit.getName());
                    }
                });

                int commitSize = commitIDs.size();

                if (commitSize == 0) {
                    return null;
                } else {
                    return commitIDs.get( commitSize / 2);
                }

            } catch (GitAPIException | IOException e) {
                return null;
            }
        }
    }



    public static class GitBisectInitCmd extends Cmd {

        private final String remoteRepo;
        private final String badCommitHash;
        private final String goodCommitHash;

        private final UUID uuid;

        public GitBisectInitCmd(String remoteRepo,
                                String badCommitHash,
                                String goodCommitID) {
            this.remoteRepo = remoteRepo;
            this.badCommitHash = badCommitHash;
            this.goodCommitHash = goodCommitID;

            this.uuid = UUID.randomUUID();

        }


        @Override
        public void run(String input, Context context) {

            String remoteRepo = Cmd.populateStateVariables(this.remoteRepo,this,context);
            String badCommitHash = Cmd.populateStateVariables(this.badCommitHash,this,context);
            String goodCommitHash = Cmd.populateStateVariables(this.goodCommitHash,this,context);

            try {
                File gitLocalDir = context.getScratchDir(uuid.toString());
                gitLocalDir.mkdirs();
                git = Git.cloneRepository()
                        .setURI(remoteRepo)
                        .setDirectory(gitLocalDir)
                        .call();

                Json gitBisectConfig = new Json();
                gitBisectConfig.set(STATE_GIT_DIR, gitLocalDir.getAbsolutePath());
                gitBisectConfig.set(STATE_BAD_COMMIT, badCommitHash);
                gitBisectConfig.set(STATE_GOOD_COMMIT, goodCommitHash);
                gitBisectConfig.set(STATE_CUR_COMMIT, "");

                //IDK if this is a good idea -
                //but seeing as user does not access qDup state, seems reasonable to share between commands
                context.getState().set(STATE_VARIABLE, gitBisectConfig);

                context.next(null);
            } catch (GitAPIException e) {
                String errMsg = String.format("Failed to checkout git repo: %s\n%s", remoteRepo, e.getMessage());
                context.error(errMsg);
                context.abort(false);
            }

        }

        @Override
        public String getLogOutput(String output, Context context) {
            return "git-bisect-init: " + Cmd.populateStateVariables(this.remoteRepo,this,context);
        }

        @Override
        public Cmd copy() {
            return new GitBisectInitCmd(remoteRepo, badCommitHash, goodCommitHash);
        }

        private String getRemoteRepo() {
            return this.remoteRepo;
        }

        private String getBadCommitHash() {
            return this.badCommitHash;
        }

        private String getGoodCommitHash() {
            return this.goodCommitHash;
        }


        public static void extendParse(Parser parser) {
            parser.addCmd(
                    GitBisect.GitBisectInitCmd.class,
                    "git-bisect-init",
                    (cmd) -> {
                        LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
                        LinkedHashMap<Object, Object> opts = new LinkedHashMap<>();
                        map.put("git-bisect-init", opts);
                        opts.put("remote-repo", cmd.getRemoteRepo());
                        opts.put("bad-commit-hash", cmd.getBadCommitHash());
                        opts.put("good-commit-hash", cmd.getGoodCommitHash());
                        return map;
                    },
                    (str, prefix, suffix) -> {
                        if (str == null || str.isEmpty()) {
                            throw new YAMLException("git-bisect-init command cannot be empty");
                        }
                        String[] split = str.split(" ");
                        if (split.length != 3) {
                            throw new YAMLException("git-bisect-init command expecting 3 params");
                        }
                        //TODO: clean this up
                        GitBisect.GitBisectInitCmd newCommand = new GitBisect.GitBisectInitCmd(split[0], split[1], split[2]);
                        return newCommand;
                    },
                    (json) -> {
                        validateNonEmptyValue(json, "remote-repo");
                        validateNonEmptyValue(json, "bad-commit-hash");
                        validateNonEmptyValue(json, "good-commit-hash");

                        GitBisectInitCmd bisectInitCmd = new GitBisectInitCmd(json.getString("remote-repo"),
                                json.getString("bad-commit-hash"), json.getString("good-commit-hash"));
                        return bisectInitCmd;
                    },
                    "remote-repo", "bad-commit-hash", "good-commit-hash"
            );

        }

        public static void validateNonEmptyValue(Json json, String key) throws YAMLException {
            if (!json.has(key) || json.getString(key, "").isEmpty()) {
                throw new YAMLException("git-bisect-init requires a non-empty " + key + " ");
            }
        }

    }


    public static class GitBisectCmd extends Cmd {

        public GitBisectCmd() {
        }


        @Override
        public void run(String input, Context context) {

            if( !this.hasWith(COMPLETE_SIGNAL)) {
                context.error("No 'doneSignal' defined");
                context.abort(false);
            }

            Json gitBisectConfig = (Json) context.getState().get(STATE_VARIABLE);

            String bisectCommit = calcBisectCommit(gitBisectConfig.getString(STATE_GOOD_COMMIT), gitBisectConfig.getString(STATE_BAD_COMMIT));

            if ( bisectCommit != null ) {
                gitBisectConfig.set(STATE_CUR_COMMIT, bisectCommit);

                Json result = new Json();
                result.set("commitHash", bisectCommit);

                context.next(result.toString());
            } else {
                context.log("GitBisectUpdateCmd: Bad commit: " + gitBisectConfig.getString(STATE_BAD_COMMIT));
                context.getCoordinator().signal(this.getWith(COMPLETE_SIGNAL).toString());
                context.skip(input);
            }

        }

        @Override
        public String toString(){return "git-bisect";}
        @Override
        public String getLogOutput(String output, Context context) {
            return "git-bisect";
        }

        @Override
        public Cmd copy() {
            return new GitBisectCmd();
        }


        public static void extendParse(Parser parser) {
            parser.addCmd(
                    GitBisectCmd.class,
                    "git-bisect",
                    true,
                    (cmd) -> "",
                    (str, prefix, suffix) -> new GitBisectCmd(),
                    (json) -> new GitBisectCmd()
            );

        }
    }


    public static class GitBisectUpdateCmd extends Cmd {

        public GitBisectUpdateCmd() {
        }

        @Override
        public void run(String input, Context context) {

            if ( input != null ) {
                Boolean checkedVal = Boolean.parseBoolean(input);

                Json gitBisectConfig = (Json) context.getState().get(STATE_VARIABLE);

                if ( checkedVal ) {
                    gitBisectConfig.set(STATE_GOOD_COMMIT, gitBisectConfig.get(STATE_CUR_COMMIT));
                } else {
                    gitBisectConfig.set(STATE_BAD_COMMIT, gitBisectConfig.get(STATE_CUR_COMMIT));
                }
                context.next(null);
            } else {
                context.error("no input for git-bisect-update");
                context.abort(false);
            }
        }

        @Override
        public String toString() {
            return "git-bisect-update";
        }

        @Override
        public String getLogOutput(String output, Context context) {
            return "git-bisect-update";
        }

        @Override
        public Cmd copy() {
            return new GitBisectUpdateCmd();
        }


        public static void extendParse(Parser parser) {
            parser.addCmd(
                    GitBisectUpdateCmd.class,
                    "git-bisect-update",
                    true,
                    (cmd) -> "",
                    (str, prefix, suffix) -> new GitBisectUpdateCmd(),
                    (json) -> new GitBisectUpdateCmd()
            );

        }
    }

}
