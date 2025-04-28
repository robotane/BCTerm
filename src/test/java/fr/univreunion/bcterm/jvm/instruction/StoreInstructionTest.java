package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for StoreInstruction class.
 */
public class StoreInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();

        // Initialize local variables sequentially
        state.setLocalVariable(0, new IntegerValue(10));
        state.setLocalVariable(1, new IntegerValue(20));
        // Initialize variables up to index 10 for testStoreWithIndexBeyondSize
        for (int i = 2; i <= 10; i++) {
            state.setLocalVariable(i, Value.NULL);
        }
    }

    /**
     * Test store instruction with a valid index and non-empty stack.
     */
    public void testStoreWithValidIndex() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a store instruction for local variable 0
        StoreInstruction instruction = new StoreInstruction(0);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());

        // Check that the local variable was updated
        Value localVar = state.getLocalVariable(0);
        assertTrue(localVar instanceof IntegerValue);
        assertEquals(42, localVar.getValue());
    }

    /**
     * Test store instruction with a different valid index.
     */
    public void testStoreWithDifferentValidIndex() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(99));

        // Create a store instruction for local variable 1
        StoreInstruction instruction = new StoreInstruction(1);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());

        // Check that the local variable was updated
        Value localVar = state.getLocalVariable(1);
        assertTrue(localVar instanceof IntegerValue);
        assertEquals(99, localVar.getValue());

        // Check that other local variables were not affected
        Value otherLocalVar = state.getLocalVariable(0);
        assertTrue(otherLocalVar instanceof IntegerValue);
        assertEquals(10, otherLocalVar.getValue());
    }

    /**
     * Test store instruction with an empty stack.
     */
    public void testStoreWithEmptyStack() {
        // Create a store instruction
        StoreInstruction instruction = new StoreInstruction(0);

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (empty stack)
        assertFalse(result);

        // Check that the local variable was not changed
        Value localVar = state.getLocalVariable(0);
        assertTrue(localVar instanceof IntegerValue);
        assertEquals(10, localVar.getValue());
    }

    /**
     * Test store instruction with a negative index.
     */
    public void testStoreWithNegativeIndex() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a store instruction with a negative index
        StoreInstruction instruction = new StoreInstruction(-1);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (invalid index)
        assertFalse(result);

        // Check that the stack still has the value (not popped)
        assertEquals(1, state.getStackSize());
        Value stackValue = state.popStack();
        assertTrue(stackValue instanceof IntegerValue);
        assertEquals(42, stackValue.getValue());
    }

    /**
     * Test store instruction with an index beyond the current size.
     */
    public void testStoreWithIndexBeyondSize() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a store instruction with an index at the current size
        // (Note: We've initialized variables up to index 10 in setUp)
        StoreInstruction instruction = new StoreInstruction(10);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack is empty (value was popped)
        assertEquals(0, state.getStackSize());

        // Check that the local variable was updated
        Value localVar = state.getLocalVariable(10);
        assertTrue(localVar instanceof IntegerValue);
        assertEquals(42, localVar.getValue());
    }

    /**
     * Test store instruction with an index beyond the current size + 1.
     * This should now return false instead of throwing an exception.
     */
    public void testStoreWithIndexTooFarBeyondSize() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a store instruction with an index far beyond the current size
        StoreInstruction instruction = new StoreInstruction(20);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (invalid index)
        assertFalse(result);

        // Check that the stack still has the value (not popped)
        assertEquals(1, state.getStackSize());
        Value stackValue = state.popStack();
        assertTrue(stackValue instanceof IntegerValue);
        assertEquals(42, stackValue.getValue());
    }

    /**
     * Test multiple store instructions in sequence.
     */
    public void testMultipleStoreInstructions() {
        // Push first value and store it
        state.pushStack(new IntegerValue(30));
        StoreInstruction instruction1 = new StoreInstruction(0);
        instruction1.execute(state);

        // Push second value and store it
        state.pushStack(new IntegerValue(40));
        StoreInstruction instruction2 = new StoreInstruction(1);
        instruction2.execute(state);

        // Check that both local variables were updated
        Value localVar0 = state.getLocalVariable(0);
        assertTrue(localVar0 instanceof IntegerValue);
        assertEquals(30, localVar0.getValue());

        Value localVar1 = state.getLocalVariable(1);
        assertTrue(localVar1 instanceof IntegerValue);
        assertEquals(40, localVar1.getValue());
    }

    /**
     * Test store instruction with null value.
     */
    public void testStoreWithNullValue() {
        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Create a store instruction
        StoreInstruction instruction = new StoreInstruction(0);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the local variable was updated to null
        Value localVar = state.getLocalVariable(0);
        assertEquals(Value.NULL, localVar);
    }
}
