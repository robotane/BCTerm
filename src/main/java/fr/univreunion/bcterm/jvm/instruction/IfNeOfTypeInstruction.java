package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.NullValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * ifne of type t. Pops the topmost element of the stack and checks if it is not
 * 0 (when t is int)
 * or not null (when t is a class). If this is the case, the computation stops
 */
public class IfNeOfTypeInstruction extends BytecodeInstruction {
    private final Value expectedValue;

    /**
     * Constructs an IfNeOfTypeInstruction with the expected value to compare
     * against.
     * 
     * @param expectedValue The value to check against (0 for int, null for class)
     */
    public IfNeOfTypeInstruction(Value expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() > 0) {
            // Get the top value without removing it first
            Value value = state.peekStack();

            boolean conditionMet;
            if (expectedValue instanceof IntegerValue) {
                // Check if value is an integer and not equals 0
                conditionMet = value instanceof IntegerValue &&
                        ((IntegerValue) value).getValue() != 0;
            } else {
                // Check if value is not null
                conditionMet = !(value instanceof NullValue);
            }

            // Only pop the stack if the condition is met
            if (conditionMet) {
                state.popStack();
                return true;
            } else {
                // Don't modify the stack if the condition is not met
                return false;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (expectedValue instanceof LocationValue) {
            String typeName = ((LocationValue) expectedValue).getTypeName();
            return "ifne of type " + (typeName != null ? typeName : expectedValue);
        }
        return "ifne of type " + expectedValue;
    }

}
