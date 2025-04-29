package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.NullValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * putfield f. Pops the topmost two values v (the top) and l (under v) from the
 * stack.
 * The value j must be a reference to an object o or null. Value v is stored
 * into o(f).
 * If l is null, the computation stops
 */
public class PutFieldInstruction extends BytecodeInstruction {

    private final String fieldName;

    /**
     * Constructs a putfield instruction for the given field name.
     *
     * @param fieldName the name of the field to store the value into
     */
    public PutFieldInstruction(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() >= 2) {
            // Get values without removing them
            Value v = state.getStackElement(state.getStackSize() - 1); // Value to store
            Value l = state.getStackElement(state.getStackSize() - 2); // Object reference

            // Check if l is null
            if (l instanceof NullValue) {
                return false; // Null pointer exception, stop computation
            }

            // Check if l is a LocationValue
            if (l instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) l;
                JVMObject object = state.getObject(locationValue);

                if (object != null) {
                    // Now we can modify the state
                    state.popStack(); // Remove the value to store
                    state.popStack(); // Remove the object reference
                    object.setField(fieldName, v);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "putfield " + fieldName;
    }
}
