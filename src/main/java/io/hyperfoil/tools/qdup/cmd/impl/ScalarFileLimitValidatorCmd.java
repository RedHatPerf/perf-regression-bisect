package io.hyperfoil.tools.qdup.cmd.impl;

import io.hyperfoil.tools.qdup.cmd.Cmd;
import io.hyperfoil.tools.qdup.cmd.Context;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.util.LinkedHashMap;

public class ScalarFileLimitValidatorCmd extends Cmd {

    private final String filename;
    private final Float limit;

    public ScalarFileLimitValidatorCmd(String filename,
                                       String limit) {
        this.filename = filename;
        this.limit = Float.parseFloat(limit);

    }


    @Override
    public void run(String input, Context context) {
        if ( input != null ) {
            File outputFile = new File(input.concat(File.separator).concat(filename));
            if(!outputFile.exists()){
                context.error("File does not exist: " + outputFile.getAbsolutePath());
                context.abort(false);
            } else {

                Boolean passed = false;

                try (BufferedReader br = new BufferedReader(new FileReader(outputFile))) {
                    String text = br.readLine(); // TODO: for this POC, we are only interested in scalar value in first line only
                    Float scalarVal = Float.parseFloat(text);
                    if ( scalarVal < this.limit) {
                        passed = Boolean.TRUE;
                    }
                    context.log("File: " + outputFile.getAbsolutePath() + "; value: " + scalarVal + "; limit: " + this.limit + "; PASSED: " + passed);
                } catch (FileNotFoundException e) {
                    context.error("File does not exist: " + outputFile.getAbsolutePath());
                    context.abort(false);
                } catch (IOException e) {
                    context.error("IOException: " + outputFile.getAbsolutePath());
                    context.abort(false);
                }

                context.next(passed.toString());

            }

        } else {
            context.error("Expecting a file path, got nothing");
            context.abort(false);
        }

    }

    @Override
    public String toString() {
        return "scalar-file-limit-validator: " + this.getFilename() + "; " + this.limit;
    }

    @Override
    public String getLogOutput(String output, Context context) {
        return "scalar-file-limit-validator: " + this.getFilename();
    }

    @Override
    public Cmd copy() {
        return new ScalarFileLimitValidatorCmd(filename, limit.toString());
    }

    public String getFilename() {
        return filename;
    }

    public String getLimit() {
        return limit.toString();
    }


    public static void extendParse(Parser parser) {
        parser.addCmd(
                ScalarFileLimitValidatorCmd.class,
                "scalar-file-limit-validator",
                (cmd) -> {
                    LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
                    LinkedHashMap<Object, Object> opts = new LinkedHashMap<>();
                    map.put("scalar-file-limit-validator", opts);
                    opts.put("filename", cmd.getFilename());
                    opts.put("limit", cmd.getLimit());
                    return map;
                },
                (str, prefix, suffix) -> {
                    if (str == null || str.isEmpty()) {
                        throw new YAMLException("scalar-file-limit-validator command cannot be empty");
                    }
                    String[] split = str.split(" ");
                    if (split.length != 2) {
                        throw new YAMLException("scalar-file-limit-validator command expecting 2 params");
                    }
                    //TODO: clean this up
                    ScalarFileLimitValidatorCmd newCommand = new ScalarFileLimitValidatorCmd(split[0], split[1]);
                    return newCommand;
                },
                (json) -> {
                    validateNonEmptyValue(json, "filename");
                    validateNonEmptyValue(json, "limit");

                    ScalarFileLimitValidatorCmd scalarFileLimitValidatorCmd = new ScalarFileLimitValidatorCmd(json.getString("filename"),
                            json.getString("limit"));
                    return scalarFileLimitValidatorCmd;
                },
                "filename", "limit"
        );

    }

    public static void validateNonEmptyValue(Json json, String key) throws YAMLException {
        if (!json.has(key) || json.getString(key, "").isEmpty()) {
            throw new YAMLException("git-bisect-init requires a non-empty " + key + " ");
        }
    }

}
