package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * const c. Pushes on top of the stack the constant c, which can be an integer
 * or null
 */
public class ConstInstruction extends BytecodeInstruction {
    private final Value value;

    /**
     * Constructs a constant instruction with the given value.
     * 
     * @param value The value to be pushed onto the operand stack
     */
    public ConstInstruction(Value value) {
        this.value = value;
    }

    /**
     * Constructs a constant instruction with the given integer value.
     * 
     * @param intValue The integer value to be pushed onto the operand stack
     */
    public ConstInstruction(int intValue) {
        this.value = new IntegerValue(intValue);
    }

    /**
     * Constructs a constant instruction with a null value.
     */
    public ConstInstruction() {
        this.value = Value.NULL;
    }

    @Override
    public boolean execute(JVMState state) {
        state.pushStack(value);
        return true;
    }

    @Override
    public String toString() {
        return "const " + value;
    }

}
