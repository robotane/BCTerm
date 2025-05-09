package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * add. Pops the topmost two values from the stack and pushes their sum instead.
 */
public class AddInstruction extends BytecodeInstruction {

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() < 2) {
            return false;
        }

        Value value2 = state.popStack();
        Value value1 = state.popStack();

        if (value1 instanceof IntegerValue && value2 instanceof IntegerValue) {
            Integer int1 = (Integer) value1.getValue();
            Integer int2 = (Integer) value2.getValue();

            Integer sum = int1 + int2;

            state.pushStack(new IntegerValue(sum));
            return true;
        } else {
            state.pushStack(value1);
            state.pushStack(value2);
            return false;
        }
    }

    @Override
    public String toString() {
        return "add";
    }
}
