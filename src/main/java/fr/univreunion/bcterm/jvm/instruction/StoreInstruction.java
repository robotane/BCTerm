package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * store i. Pops the topmost value from the stack and writes it into local
 * variable i
 */
public class StoreInstruction extends BytecodeInstruction {
    private final int index;

    /**
     * Constructs a StoreInstruction with the specified local variable index.
     *
     * @param index the index of the local variable where the value will be stored
     */
    public StoreInstruction(int index) {
        this.index = index;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() <= 0 || index < 0 || index > state.getLocalVariablesSize()) {
            return false;
        }
        Value value = state.popStack();
        state.setLocalVariable(index, value);
        return true;
    }

    @Override
    public String toString() {
        return "store " + index;
    }

    public int getIndex() {
        return index;
    }

}
