package fr.univreunion.bcterm.jvm.state;

import java.util.*;

/**
 * A formal model of the JVM state as described by the triple <l || s || μ>
 * where l is the array of local variables, s is the operand stack, and μ is the memory/heap.
 */
public class JVMState {
    // Local variables array (l)
    private final Value[] localVariables;
    // Operand stack (s) - grows leftwards
    private final Stack<Value> operandStack;

    // Memory/heap (m) - maps locations to objects
    private final Map<LocationValue, JVMObject> memory;

    /**
     * Creates a new JVM state with specified capacity for local variables and stack
     * @param localVarCount Number of local variables
     * @param maxStackSize Maximum stack size
     */
    public JVMState(int localVarCount, int maxStackSize) {
        this.localVariables = new Value[localVarCount];
        this.operandStack = new Stack<>();
        this.memory = new HashMap<>();
    }

    /**
     * Returns a string representation of this JVM state in the format <l || s || μ>
     * where l is the array of local variables, s is the operand stack, and μ is the memory/heap.
     *
     * @return A string representation of this JVM state
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Format local variables (l)
        sb.append("<[");
        for (int i = 0; i < localVariables.length; i++) {
            if (i > 0) sb.append(", ");
            Value value = localVariables[i];
            sb.append("l").append(i).append("=");
            sb.append(value != null ? value : "null");
        }
        sb.append("] || ");

        // Format operand stack (s)
        sb.append("[");
        for (int i = 0; i < operandStack.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("s").append(i).append("=");
            sb.append(operandStack.get(i));
        }
        sb.append("] || ");

        // Format memory/heap (m)
        sb.append("{");
        int count = 0;
        for (LocationValue location : memory.keySet()) {
            if (count > 0) sb.append(", ");
            sb.append(location).append(" -> ");
            sb.append(memory.get(location));
            count++;

            // Limit the number of memory entries shown to avoid excessive output
            if (count >= 5 && memory.size() > 6) {
                sb.append(", ... (").append(memory.size() - 5).append(" more)");
                break;
            }
        }
        sb.append("}>");

        return sb.toString();
    }

    /**
     * Returns a more detailed string representation of this JVM state
     * with each component on separate lines for better readability.
     *
     * @return A detailed string representation of this JVM state
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JVM State:\n");

        // Local variables
        sb.append("  Local Variables (l):\n");
        for (int i = 0; i < localVariables.length; i++) {
            Value value = localVariables[i];
            sb.append("    l").append(i).append(" = ");
            sb.append(value != null ? value : "null").append("\n");
        }

        // Operand stack
        sb.append("  Operand Stack (s):\n");
        if (operandStack.isEmpty()) {
            sb.append("    <empty>\n");
        } else {
            for (int i = operandStack.size() - 1; i >= 0; i--) {
                sb.append("    s").append(i).append(" = ");
                sb.append(operandStack.get(i)).append(i == 0 ? " (top)\n" : "\n");
            }
        }

        // Memory/heap
        sb.append("  Memory/Heap (m):\n");
        if (memory.isEmpty()) {
            sb.append("    <empty>\n");
        } else {
            for (Map.Entry<LocationValue, JVMObject> entry : memory.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(" -> ");
                sb.append(entry.getValue()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Allocates a new object in memory and returns its location.
     *
     * @param object The JVM object to allocate in memory
     * @return A new LocationValue representing the memory location of the allocated object
     */
    public LocationValue allocateNewObject(JVMObject object) {
        // Generate a new LocationValue
        int nextId = memory.size() + 1;
        LocationValue LocationValue = new LocationValue(nextId);

        // Allocate the object
        allocateObject(LocationValue, object);

        return LocationValue;
    }

    // Getters and setters

    public Value getLocalVariable(int index) {
        return localVariables[index];
    }

    public void setLocalVariable(int index, Value value) {
        localVariables[index] = value;
    }

    public Value popStack() {
        return operandStack.pop();
    }

    public void pushStack(Value value) {
        operandStack.push(value);
    }

    public JVMObject getObject(LocationValue LocationValue) {
        return memory.get(LocationValue);
    }

    public void allocateObject(LocationValue LocationValue, JVMObject object) {
        memory.put(LocationValue, object);
    }

    public int getStackSize() {
        return operandStack.size();
    }

    public int getLocalVarCount() {
        return localVariables.length;
    }
}
