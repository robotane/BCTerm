package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * load i. Pushes on top of the stack the value of local variable i
 */
public class LoadInstruction extends BytecodeInstruction {
    private final int index;

    /**
     * Creates a new load instruction for the given local variable index.
     * 
     * @param index The index of the local variable to load
     */
    public LoadInstruction(int index) {
        this.index = index;
    }

    @Override
    public boolean execute(JVMState state) {
        if (index < 0 || index >= state.getLocalVariablesSize()) {
            return false;
        }
        Value value = state.getLocalVariable(index);
        state.pushStack(value);
        return true;
    }

    @Override
    public String toString() {
        return "load " + index;
    }

    public int getIndex() {
        return index;
    }
}
