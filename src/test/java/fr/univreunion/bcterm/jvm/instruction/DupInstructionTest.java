package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for DupInstruction class.
 */
public class DupInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test dup instruction with a non-empty stack.
     */
    public void testDupWithNonEmptyStack() {
        // Push a value onto the stack
        IntegerValue value = new IntegerValue(42);
        state.pushStack(value);

        // Create a dup instruction
        DupInstruction instruction = new DupInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has two elements
        assertEquals(2, state.getStackSize());

        // Check that the top two values on the stack are the same
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(42, topValue.getValue());

        Value secondValue = state.popStack();
        assertTrue(secondValue instanceof IntegerValue);
        assertEquals(42, secondValue.getValue());
    }

    /**
     * Test dup instruction with an empty stack.
     */
    public void testDupWithEmptyStack() {
        // Create a dup instruction
        DupInstruction instruction = new DupInstruction();

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (can't dup from an empty stack)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test dup instruction with multiple values on the stack.
     */
    public void testDupWithMultipleValuesOnStack() {
        // Push multiple values onto the stack
        state.pushStack(new IntegerValue(10));
        state.pushStack(new IntegerValue(20));

        // Create a dup instruction
        DupInstruction instruction = new DupInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has three elements
        assertEquals(3, state.getStackSize());

        // Check the values on the stack
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(20, topValue.getValue());

        Value secondValue = state.popStack();
        assertTrue(secondValue instanceof IntegerValue);
        assertEquals(20, secondValue.getValue());

        Value thirdValue = state.popStack();
        assertTrue(thirdValue instanceof IntegerValue);
        assertEquals(10, thirdValue.getValue());
    }

    /**
     * Test toString method of DupInstruction.
     */
    public void testToString() {
        DupInstruction instruction = new DupInstruction();
        assertEquals("dup", instruction.toString());
    }

    /**
     * Test multiple dup instructions in sequence.
     */
    public void testMultipleDupInstructions() {
        // Push a value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create and execute first dup instruction
        DupInstruction instruction1 = new DupInstruction();
        instruction1.execute(state);

        // Create and execute second dup instruction
        DupInstruction instruction2 = new DupInstruction();
        instruction2.execute(state);

        // Check stack size
        assertEquals(3, state.getStackSize());

        // Check values
        for (int i = 0; i < 3; i++) {
            Value value = state.popStack();
            assertTrue(value instanceof IntegerValue);
            assertEquals(42, value.getValue());
        }
    }
}
