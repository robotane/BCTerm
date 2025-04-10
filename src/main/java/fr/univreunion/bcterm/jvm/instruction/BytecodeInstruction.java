package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMState;

public abstract class BytecodeInstruction {

    /** Label to store information about the instruction */
    private String label;

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
        return label;
    }

    /**
     * Sets the label for this bytecode instruction.
     * 
     * @param label The label string to associate with this instruction
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
