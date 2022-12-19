package io.hyperfoil.tools.qdup.cmd.impl;

import io.hyperfoil.tools.qdup.QDup;
import io.hyperfoil.tools.qdup.cmd.Cmd;
import io.hyperfoil.tools.qdup.cmd.Context;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.*;
import java.util.stream.Collectors;

public class QdupProcessCmd extends Cmd {

    private final String scriptUrl;
    private final String commitParamExpr;
    private final String commitValueExpr;
    private final String overrideStateExpr;

    public QdupProcessCmd(String scriptUrl,
                          String commitParam,
                          String commitValue,
                          String overrideStateExpr) {
        this.scriptUrl = scriptUrl;
        this.commitParamExpr = commitParam;
        this.commitValueExpr = commitValue;
        this.overrideStateExpr = overrideStateExpr;
    }

    @Override
    public void run(String input, Context context) {

        String scriptUrl = Cmd.populateStateVariables(this.scriptUrl, this, context);
        String commitParam = Cmd.populateStateVariables(this.commitParamExpr, this, context);
        String commitValue = Cmd.populateStateVariables(this.commitValueExpr, this, context);
        String[] paramsArr = new String[]{};
        if ( this.overrideStateExpr != null) {
            String params = Cmd.populateStateVariables(this.overrideStateExpr, this, context);
            //TODO:: validate parsed json
            Json paramsJson = Json.fromString(params);

            List<String> populateParams = paramsJson.stream()
                    .map(entry -> entry.getKey().toString() + "=" + entry.getValue().toString())
                    .collect(Collectors.toList());
            paramsArr = new String[populateParams.size() * 2];
            for(int i = 0; i < populateParams.size() * 2; i=i+2){
                paramsArr[i] = "-S";
                paramsArr[i+1] = populateParams.get(Math.floorDiv(i, 2));
            }
        }

        String[] qDupArgs = {
                scriptUrl
                , "-S"
                , commitParam+"="+commitValue
        };

        String[] args = Arrays.copyOf(qDupArgs, qDupArgs.length + paramsArr.length);
        System.arraycopy(paramsArr, 0,args,  qDupArgs.length, paramsArr.length);

        QDup toRun = new QDup(args);

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
        return new QdupProcessCmd(scriptUrl, commitParamExpr, commitValueExpr, overrideStateExpr);
    }

    public String getScriptUrl() {
        return scriptUrl;
    }

    public String getCommitParamExpr() {
        return commitParamExpr;
    }

    public String getCommitValueExpr() {
        return commitValueExpr;
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
                    opts.put("commitParam", cmd.getCommitParamExpr());
                    opts.put("commitValue", cmd.getCommitValueExpr());
                    return map;
                },
                (str, prefix, suffix) -> {
                    throw new YAMLException("qdup-process string processing not supported");
                },
                (json) -> {
                    validateNonEmptyValue(json, "scriptUrl");
                    validateNonEmptyValue(json, "commitParam");
                    validateNonEmptyValue(json, "commitValue");

                    String scriptUrl = json.getString("scriptUrl");
                    String commitParam = json.getString("commitParam");
                    String commitValue = json.getString("commitValue");
                    String params = json.getString("overrideState", null);

                    QdupProcessCmd bisectInitCmd = new QdupProcessCmd(scriptUrl,
                            commitParam, commitValue,
                            params
                    );
                    return bisectInitCmd;
                },
                "scriptUrl", "commitParam", "commitValue", "overrideState"
        );

    }

    public static void validateNonEmptyValue(Json json, String key) throws YAMLException {
        if (!json.has(key) || json.getString(key, "").isEmpty()) {
            throw new YAMLException("qdup-process requires a non-empty " + key + " ");
        }
    }

}
