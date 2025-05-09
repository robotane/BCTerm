package fr.univreunion.bcterm.jvm.instruction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.aliasing.AliasPair;
import fr.univreunion.bcterm.analysis.aliasing.AliasPairAnalyzer;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicVariable;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicVariableAnalyzer;
import fr.univreunion.bcterm.analysis.sharing.SharingPair;
import fr.univreunion.bcterm.analysis.sharing.SharingPairAnalyzer;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.util.Constants;

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
        String localVarsAndStack = getLabelFor(Constants.ANALYSIS_RESULT_LOCAL_VARS_AND_STACK);
        if (!localVarsAndStack.isEmpty()) {
            builtLabel.append(localVarsAndStack);
        }

        String sharingPairs = getLabelFor(Constants.ANALYSIS_RESULT_SHARING_PAIRS);
        if (!sharingPairs.isEmpty()) {
            if (builtLabel.length() > 0) {
                builtLabel.append(",");
            }
            builtLabel.append(sharingPairs);
        }

        String cyclicVars = getLabelFor(Constants.ANALYSIS_RESULT_CYCLIC_VARS);
        if (!cyclicVars.isEmpty()) {
            if (builtLabel.length() > 0) {
                builtLabel.append(",");
            }
            builtLabel.append(cyclicVars);
        }

        String aliasPairs = getLabelFor(Constants.ANALYSIS_RESULT_ALIAS_PAIRS);
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
    @SuppressWarnings("unchecked")
    public String getLabelFor(String key) {

        if (!key.equals(Constants.ANALYSIS_RESULT_LOCAL_VARS_AND_STACK) &&
                !key.equals(Constants.ANALYSIS_RESULT_ALL) &&
                !analysisResults.containsKey(key)) {
            return "";
        }

        Object value = analysisResults.get(key);

        if (!key.equals(Constants.ANALYSIS_RESULT_LOCAL_VARS_AND_STACK) &&
                !key.equals(Constants.ANALYSIS_RESULT_ALL) &&
                value == null) {
            return "";
        }

        switch (key) {
            case Constants.ANALYSIS_RESULT_SHARING_PAIRS:
                return SharingPairAnalyzer.formatForLabel((Set<SharingPair>) value);
            case Constants.ANALYSIS_RESULT_CYCLIC_VARS:
                return CyclicVariableAnalyzer.formatForLabel((Set<CyclicVariable>) value);
            case Constants.ANALYSIS_RESULT_ALIAS_PAIRS:
                return AliasPairAnalyzer.formatForLabel((Set<AliasPair>) value);
            case Constants.ANALYSIS_RESULT_LOCAL_VARS_AND_STACK:
                StringBuilder builtLabel = new StringBuilder();
                if (analysisResults.containsKey(Constants.ANALYSIS_RESULT_LOCAL_VARS_COUNT)) {
                    builtLabel.append(analysisResults.get(Constants.ANALYSIS_RESULT_LOCAL_VARS_COUNT).toString());
                }
                if (analysisResults.containsKey(Constants.ANALYSIS_RESULT_STACK_SIZE)) {
                    if (builtLabel.length() > 0)
                        builtLabel.append(",");
                    builtLabel.append(analysisResults.get(Constants.ANALYSIS_RESULT_STACK_SIZE).toString());
                }
                return builtLabel.toString();
            case Constants.ANALYSIS_RESULT_LOCAL_VARS_COUNT:
                return analysisResults.get(Constants.ANALYSIS_RESULT_LOCAL_VARS_COUNT).toString();
            case Constants.ANALYSIS_RESULT_STACK_SIZE:
                return analysisResults.get(Constants.ANALYSIS_RESULT_STACK_SIZE).toString();
            case Constants.ANALYSIS_RESULT_ALL:
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
