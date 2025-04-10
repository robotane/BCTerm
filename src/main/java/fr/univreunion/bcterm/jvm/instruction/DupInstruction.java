package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * dup. Pushes on top of the stack its topmost element, which hence gets
 * duplicated
 */
public class DupInstruction extends BytecodeInstruction {

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() <= 0)
            return false;

        Value top = state.peekStack();
        state.pushStack(top);
        return true;
    }

    @Override
    public String toString() {
        return "dup";
    }
}
