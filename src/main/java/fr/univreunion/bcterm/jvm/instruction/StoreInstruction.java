package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * store i. Pops the topmost value from the stack and writes it into local
 * variable i
 */
public class StoreInstruction extends BytecodeInstruction {
    private final int index;

    public StoreInstruction(int index) {
        this.index = index;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() > 0) {
            if (index < 0) {
                return false; // Invalid index
            }
            Value value = state.popStack();
            state.setLocalVariable(index, value);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "store " + index;
    }

}
