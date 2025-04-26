package fr.univreunion.bcterm.jvm.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A formal model of the JVM state as described by the triple < l || s || m >
 * where l is the array of local variables, s is the operand stack, and m is the
 * memory/heap.
 */
public class JVMState {

    /** The local variables array (l). */
    private final ArrayList<Value> localVariables;

    /** The operand stack (s). */
    private final Stack<Value> operandStack;

    /** The memory/heap (m). */
    private final Map<LocationValue, JVMObject> memory;

    /**
     * Constructs a new JVMState with empty local variables, operand stack, and
     * memory.
     */
    public JVMState() {
        this.localVariables = new ArrayList<>();
        this.operandStack = new Stack<>();
        this.memory = new HashMap<>();
    }

    /**
     * Constructs a new JVMState with empty local variables, operand stack, and the
     * provided memory.
     * 
     * @param initialMemory The initial memory state to use
     */
    public JVMState(Map<LocationValue, JVMObject> initialMemory) {
        this.localVariables = new ArrayList<>();
        this.operandStack = new Stack<>();
        this.memory = initialMemory;
    }

    /**
     * Creates a copy of the provided JVMState.
     * 
     * @param initialState The JVMState to copy
     */
    public JVMState(JVMState initialState) {
        this.localVariables = new ArrayList<>(initialState.localVariables);
        this.operandStack = new Stack<>();
        this.operandStack.addAll(initialState.operandStack);

        this.memory = new HashMap<>(initialState.memory);
    }

    /**
     * Returns a string representation of this JVM state in the format < l || s || m
     * >
     * where l is the array of local variables, s is the operand stack, and m is the
     * heap.
     *
     * @return A string representation of this JVM state
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Format local variables (l)
        sb.append("<[");
        for (int i = 0; i < localVariables.size(); i++) {
            if (i > 0)
                sb.append(", ");
            Value value = localVariables.get(i);
            // sb.append("l").append(i).append("=");
            sb.append(value != null ? value : "null");
        }
        sb.append("] || ");

        // Format operand stack (s)
        sb.append("[");
        for (int i = operandStack.size() - 1; i >= 0; i--) {
            if (i < operandStack.size() - 1)
                sb.append("::");
            // sb.append("s").append(i).append("=");
            sb.append(operandStack.get(i));
        }
        sb.append("] || ");

        // Format heap (m)
        sb.append("{");
        int i = 0;
        for (LocationValue location : memory.keySet()) {
            if (i > 0)
                sb.append(", ");
            sb.append(location).append(" -> ");
            sb.append(memory.get(location));
            i++;
        }
        sb.append("}>");

        return sb.toString();
    }

    /**
     * Returns a detailed string representation of the JVM state with formatting.
     * 
     * @return A formatted string showing local variables, operand stack, and memory
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("JVM State:\n");

        // Local variables
        sb.append("  Local Variables (l):\n");
        for (int i = 0; i < localVariables.size(); i++) {
            Value value = localVariables.get(i);
            sb.append("    l").append(i).append(" = ");
            sb.append(value != null ? value : "null").append("\n");
        }

        // Operand stack
        sb.append("  Stack (s):\n");
        if (operandStack.isEmpty()) {
            sb.append("    <empty>\n");
        } else {
            for (int i = 0; i < operandStack.size(); i++) {
                sb.append("    s").append(i).append(" = ");
                sb.append(operandStack.get(i)).append("\n");
            }
        }

        // Heap
        sb.append("  Heap (m):\n");
        if (memory.isEmpty()) {
            sb.append("    <empty>\n");
        } else {
            for (LocationValue location : memory.keySet()) {
                sb.append("    ").append(location).append(" -> ");
                sb.append(memory.get(location)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Gets the value of a local variable at the specified index.
     * 
     * @param index The index of the local variable
     * @return The Value stored at the specified index
     */
    public Value getLocalVariable(int index) {
        return localVariables.get(index);
    }

    /**
     * Sets the value of a local variable at the specified index.
     * If the index is beyond the current size, the list is expanded with null
     * values
     * to accommodate the new index.
     * 
     * @param index The index where to set the value
     * @param value The Value to set
     */
    public void setLocalVariable(int index, Value value) {
        // Expand the list if needed to reach the index
        while (index >= localVariables.size()) {
            localVariables.add(Value.NULL);
        }
        // Now set the value at the specified index
        localVariables.set(index, value);
    }

    /**
     * Removes and returns the top value from the operand stack.
     * 
     * @return The Value from the top of the stack
     */
    public Value popStack() {
        return operandStack.pop();
    }

    /**
     * Returns the top value from the operand stack without removing it.
     * 
     * @return The Value at the top of the stack
     */
    public Value peekStack() {
        return operandStack.peek();
    }

    /**
     * Pushes a value onto the top of the operand stack.
     * 
     * @param value The Value to push onto the stack
     */
    public void pushStack(Value value) {
        operandStack.push(value);
    }

    /**
     * Retrieves an object from memory at the specified location.
     * 
     * @param LocationValue The location value where the object is stored
     * @return The JVMObject at the specified location
     */
    public JVMObject getObject(LocationValue LocationValue) {
        return memory.get(LocationValue);
    }

    /**
     * Retrieves the fields of a JVM object at the specified memory address.
     * 
     * @param address The memory address of the object
     * @return A map of field names to their corresponding values, or an empty map
     *         if the object is not found
     */
    public Map<String, Value> getObjectFields(long address) {
        LocationValue locationValue = new LocationValue(address);
        JVMObject object = memory.get(locationValue);
        if (object != null) {
            return object.getFields();
        }
        return new HashMap<>();
    }

    /**
     * Allocates an object in memory at the specified location.
     * 
     * @param LocationValue The location value where to store the object
     * @param object        The JVMObject to store
     */
    public void allocateObject(LocationValue LocationValue, JVMObject object) {
        memory.put(LocationValue, object);
    }

    /**
     * Allocates a new object in memory and generates a new location value for it.
     * 
     * @param object The JVMObject to allocate in memory
     * @return A new LocationValue representing where the object is stored
     */
    public LocationValue allocateNewObject(JVMObject object) {
        // Generate a new LocationValue
        int nextId = memory.size() + 1;
        LocationValue LocationValue = new LocationValue(nextId, object.getClassTag());

        // Allocate the object
        allocateObject(LocationValue, object);

        return LocationValue;
    }

    /**
     * Gets the current size of the operand stack.
     * 
     * @return The number of elements in the operand stack
     */
    public int getStackSize() {
        return operandStack.size();
    }

    /**
     * Gets the current number of local variables.
     * 
     * @return The number of local variables
     */
    public int getLocalVariablesSize() {
        return localVariables.size();
    }

    /**
     * Gets the element at the specified position in the operand stack.
     * 
     * @param i The index of the element to retrieve
     * @return The Value at the specified position in the operand stack
     */
    public Value getStackElement(int i) {
        return operandStack.get(i);
    }

    /**
     * Gets the memory map containing all allocated objects.
     * 
     * @return A map of location values to JVM objects representing the memory state
     */
    public Map<LocationValue, JVMObject> getMemory() {
        return memory;
    }

    /**
     * Creates a deep copy of the current JVMState instance.
     * 
     * This method performs a complete deep copy of the JVM state, including:
     * - Local variables
     * - Operand stack
     * - Memory map with objects
     * 
     * Each component is copied by creating new instances of values, locations, and
     * objects.
     * 
     * @return A new JVMState instance that is a deep copy of the current state
     */
    public JVMState deepCopy() {
        JVMState copy = new JVMState();

        for (Value value : this.localVariables) {
            copy.localVariables.add(value != null ? value.deepCopy() : Value.NULL);
        }

        for (Value value : this.operandStack) {
            copy.operandStack.push(value != null ? value.deepCopy() : Value.NULL);
        }

        for (Map.Entry<LocationValue, JVMObject> entry : this.memory.entrySet()) {
            LocationValue newLocation = (LocationValue) entry.getKey().deepCopy();

            JVMObject copiedObject = entry.getValue().deepCopy();

            copy.memory.put(newLocation, copiedObject);
        }

        return copy;
    }
}
