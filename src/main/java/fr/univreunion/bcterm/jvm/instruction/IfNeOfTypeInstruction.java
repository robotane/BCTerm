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
            Value value = state.popStack();

            if (this.expectedValue instanceof IntegerValue) {
                // Check if value is an integer and not equals 0
                return !(value instanceof IntegerValue && (Integer) value.getValue() != 0);
            } else {
                // Check if value is not null
                return !(value instanceof NullValue);
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
