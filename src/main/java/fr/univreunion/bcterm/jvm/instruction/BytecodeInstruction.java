package fr.univreunion.bcterm.jvm.instruction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.sharing.SharingPair;
import fr.univreunion.bcterm.analysis.sharing.SharingPairAnalyzer;
import fr.univreunion.bcterm.jvm.state.JVMState;

/**
 * Represents an abstract bytecode instruction in the JVM with analysis and
 * labeling capabilities.
 * 
 * This class provides a framework for executing bytecode instructions and
 * managing
 * analysis results and labels. Subclasses must implement the specific execution
 * logic for different types of bytecode instructions.
 * 
 * The class supports dynamic label generation based on various analysis
 * results,
 * such as local variables, stack information, sharing pairs, and more.
 */
public abstract class BytecodeInstruction {

    /** Label to store information about the instruction */
    private String label;

    // Map to store analysis results
    private final Map<String, Object> analysisResults = new HashMap<>();

    /**
     * Executes this bytecode instruction on the given JVM state.
     * 
     * @param state The current JVM state to execute the instruction on
     * @return true if execution should continue to next instruction, false if
     *         execution should stop
     */
    public abstract boolean execute(JVMState state);

    /**
     * Generates a label for the bytecode instruction based on various analysis
     * results.
     * 
     * If a custom label is set, it returns that label. Otherwise, it constructs
     * a label by combining local variables, stack information, sharing pairs,
     * cyclic variables, and alias pairs.
     * 
     * @return A string representation of the instruction's label
     */
    public String getLabel() {
        // If a custom label is set, return it
        if (label != null) {
            return label;
        }

        StringBuilder builtLabel = new StringBuilder();
        String localVarsAndStack = getLabelFor("localVarsAndStack");
        if (!localVarsAndStack.isEmpty()) {
            builtLabel.append(localVarsAndStack);
        }

        String sharingPairs = getLabelFor("sharingPairs");
        if (!sharingPairs.isEmpty()) {
            if (builtLabel.length() > 0) {
                builtLabel.append(",");
            }
            builtLabel.append(sharingPairs);
        }

        String cyclicVars = getLabelFor("cyclicVars");
        if (!cyclicVars.isEmpty()) {
            if (builtLabel.length() > 0) {
                builtLabel.append(",");
            }
            builtLabel.append(cyclicVars);
        }

        String aliasPairs = getLabelFor("aliasPairs");
        if (!aliasPairs.isEmpty()) {
            if (builtLabel.length() > 0) {
                builtLabel.append(",");
            }
            builtLabel.append(aliasPairs);
        }
        return builtLabel.toString();
    }

    /**
     * Sets the label for this bytecode instruction.
     * 
     * @param label The label string to associate with this instruction
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets a label string for a specific analysis result key.
     * 
     * @param key The key to get the label for. Possible values:
     *            - "sharingPairs": Returns formatted sharing pairs
     *            - "cyclicVars": Returns formated cyclic variables
     *            - "aliasPairs": Returns formated alias pairs
     *            - "localVarsAndStack": Returns concatenated local vars count and
     *            stack size
     *            - "localVarsCount": Returns the number of local variables
     *            - "stackSize": Returns the stack size
     *            - "all": Returns the complete label
     * @return The label string for the given key, or empty string if key is invalid
     *         or has no value
     */
    public String getLabelFor(String key) {

        if (!key.equals("localVarsAndStack") && !key.equals("all") && !analysisResults.containsKey(key)) {
            return "";
        }

        Object value = analysisResults.get(key);

        if (!key.equals("localVarsAndStack") && !key.equals("all") && value == null) {
            return "";
        }

        switch (key) {
            case "sharingPairs":
                return SharingPairAnalyzer.formatForLabel((Set<SharingPair>) value);
            case "cyclicVars":
                return "{}"; // Not implemented yet
            case "aliasPairs":
                return "{}"; // Not implemented yet
            case "localVarsAndStack":
                StringBuilder builtLabel = new StringBuilder();
                if (analysisResults.containsKey("localVarsCount")) {
                    builtLabel.append(analysisResults.get("localVarsCount").toString());
                }
                if (analysisResults.containsKey("stackSize")) {
                    if (builtLabel.length() > 0)
                        builtLabel.append(",");
                    builtLabel.append(analysisResults.get("stackSize").toString());
                }
                return builtLabel.toString();
            case "localVarsCount":
                return analysisResults.get("localVarsCount").toString();
            case "stackSize":
                return analysisResults.get("stackSize").toString();
            case "all":
                return getLabel();
            default:
                return value.toString();
        }
    }

    public void addAnalysisResult(String key, Object value) {
        analysisResults.put(key, value);
    }

    public Object getAnalysisResult(String key) {
        return analysisResults.get(key);
    }

    public Map<String, Object> getAllAnalysisResults() {
        return new HashMap<>(analysisResults);
    }

    public void clearAnalysisResults() {
        analysisResults.clear();
    }
}
