package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.NullValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * getfield f. Pops the topmost value l of the stack, which must be a reference
 * to an object
 * o or null, and pushes at its place o(f). If l is null, the computation stops
 */
public class GetFieldInstruction extends BytecodeInstruction {
    private final String fieldName;

    /**
     * Constructs a GetFieldInstruction with the specified field name.
     * 
     * @param fieldName the name of the field to get
     */
    public GetFieldInstruction(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean execute(JVMState state) {
        if (state.getStackSize() > 0) {
            Value value = state.peekStack();

            // Check if value is null
            if (value instanceof NullValue) {
                return false; // Stop execution
            }

            // Check if value is a LocationValue
            if (value instanceof LocationValue) {
                LocationValue LocationValue = (LocationValue) value.getValue();
                JVMObject object = state.getObject(LocationValue);

                if (object != null) {
                    // Get the field value o(f) from the object and push it onto the stack
                    state.popStack(); // Actually remove the object reference from the stack
                    Value fieldValue = object.getField(fieldName);
                    state.pushStack(fieldValue);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "getfield " + fieldName;
    }
}
