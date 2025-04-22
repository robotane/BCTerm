package fr.univreunion.bcterm.jvm.instruction;

import java.util.HashMap;
import java.util.Map;

import fr.univreunion.bcterm.jvm.state.JVMState;

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
     * Gets the label associated with this bytecode instruction.
     * 
     * @return The label string, or null if no label is set
     */
    public String getLabel() {
        // If a custom label is set, return it
        if (this.label != null) {
            return this.label;
        }

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
    }

    /**
     * Sets the label for this bytecode instruction.
     * 
     * @param label The label string to associate with this instruction
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelFor(String labelKey) {
        return getLabel();
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
