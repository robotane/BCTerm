package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.NullValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * ifeq of type t. Pops the topmost element of the stack and checks if it is 0
 * (when t is int)
 * or null (when t is a class). If this is not the case, the computation stops
 */
public class IfEqOfTypeInstruction extends BytecodeInstruction {
    private final Value expectedValue;

    /**
     * Constructs an ifeq of type instruction with the expected value to compare
     * against.
     * 
     * @param expectedValue The value to compare against (0 for int, null for
     *                      reference types)
     */
    public IfEqOfTypeInstruction(Value expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() > 0) {
            Value value = state.popStack();

            if (this.expectedValue instanceof IntegerValue) {
                // Check if value is an integer and equals 0
                return value instanceof IntegerValue && ((IntegerValue) value).getValue() == 0;
            } else {
                // Check if value is null
                return value instanceof NullValue;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (expectedValue instanceof LocationValue) {
            String typeName = ((LocationValue) expectedValue).getTypeName();
            return "ifeq of type " + (typeName != null ? typeName : expectedValue);
        }
        return "ifeq of type " + expectedValue;
    }

}
