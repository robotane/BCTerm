package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;

/**
 * new κ. Pushes on top of the stack a reference to a new object of class k
 */
public class NewInstruction extends BytecodeInstruction {
    private final String className;

    /**
     * Creates a new instruction to instantiate an object of the given class.
     * 
     * @param className the name of the class to instantiate
     */
    public NewInstruction(String className) {
        this.className = className;
    }

    @Override
    public boolean execute(JVMState state) {
        JVMObject newObject = new JVMObject(className);

        LocationValue location = state.allocateNewObject(newObject);

        state.pushStack(location);
        return true;
    }

    @Override
    public String toString() {
        return "new " + className;
    }
}
