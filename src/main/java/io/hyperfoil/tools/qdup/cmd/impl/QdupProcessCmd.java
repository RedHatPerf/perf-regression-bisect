package io.hyperfoil.tools.qdup.cmd.impl;

import io.hyperfoil.tools.qdup.QDup;
import io.hyperfoil.tools.qdup.cmd.Cmd;
import io.hyperfoil.tools.qdup.cmd.Context;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.LinkedHashMap;

public class QdupProcessCmd extends Cmd {

    private final String scriptUrl;
    private final String commitParam;
    private final String commitValue;

    public QdupProcessCmd(String scriptUrl,
                          String commitParam,
                          String commitValue) {
        this.scriptUrl = scriptUrl;
        this.commitParam = commitParam;
        this.commitValue = commitValue;
    }

    @Override
    public void run(String input, Context context) {

        String scriptUrl = Cmd.populateStateVariables(this.scriptUrl, this, context);
        String commitParam = Cmd.populateStateVariables(this.commitParam, this, context);
        String commitValue = Cmd.populateStateVariables(this.commitValue, this, context);


        String[] qDupArgs = {
                scriptUrl
                , "-S"
                , commitParam+"="+commitValue
        };


        QDup toRun = new QDup(qDupArgs);

        boolean ok = toRun.run();

        if( ok ) {
            context.next(toRun.getOutputPath());
        } else {
            context.error("qDup process did not complete correctly");
            context.abort(false);
        }


    }
    @Override
    public String toString() {
        return "qdup-process: " + this.scriptUrl;
    }
    @Override
    public String getLogOutput(String output, Context context) {
        return "qdup-process: " + Cmd.populateStateVariables(this.scriptUrl, this,context);
    }

    @Override
    public Cmd copy() {
        return new QdupProcessCmd(scriptUrl, commitParam, commitValue);
    }

    public String getScriptUrl() {
        return scriptUrl;
    }

    public String getCommitParam() {
        return commitParam;
    }

    public String getCommitValue() {
        return commitValue;
    }

    public static void extendParse(Parser parser) {
        parser.addCmd(
                QdupProcessCmd.class,
                "qdup-process",
                (cmd) -> {
                    LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
                    LinkedHashMap<Object, Object> opts = new LinkedHashMap<>();
                    map.put("qdup-process", opts);
                    opts.put("scriptUrl", cmd.getScriptUrl());
                    opts.put("commitParam", cmd.getCommitParam());
                    opts.put("commitValue", cmd.getCommitValue());
                    return map;
                },
                (str, prefix, suffix) -> {
                    if (str == null || str.isEmpty()) {
                        throw new YAMLException("qdup-process command cannot be empty");
                    }
                    String[] split = str.split(" ");
                    if (split.length != 5) {
                        throw new YAMLException("qdup-process command expecting 5 params");
                    }
                    //TODO: clean this up
                    QdupProcessCmd newCommand = new QdupProcessCmd(split[0], split[1], split[2]);
                    return newCommand;
                },
                (json) -> {
                    validateNonEmptyValue(json, "scriptUrl");
                    validateNonEmptyValue(json, "commitParam");
                    validateNonEmptyValue(json, "commitValue");

                    QdupProcessCmd bisectInitCmd = new QdupProcessCmd(json.getString("scriptUrl"),
                            json.getString("commitParam"), json.getString("commitValue")
                    );
                    return bisectInitCmd;
                },
                "scriptUrl", "commitParam", "commitValue"
        );

    }

    public static void validateNonEmptyValue(Json json, String key) throws YAMLException {
        if (!json.has(key) || json.getString(key, "").isEmpty()) {
            throw new YAMLException("qdup-process requires a non-empty " + key + " ");
        }
    }

}
