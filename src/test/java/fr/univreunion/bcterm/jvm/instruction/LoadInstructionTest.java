package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for LoadInstruction class.
 */
public class LoadInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();

        // Set up some local variables for testing
        state.setLocalVariable(0, new IntegerValue(10));
        state.setLocalVariable(1, new IntegerValue(20));
        state.setLocalVariable(2, Value.NULL);
    }

    /**
     * Test load instruction with a valid local variable index.
     */
    public void testLoadWithValidIndex() {
        // Create a load instruction for local variable 0
        LoadInstruction instruction = new LoadInstruction(0);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the value of local variable 0
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(10, topValue.getValue());
    }

    /**
     * Test load instruction with another valid local variable index.
     */
    public void testLoadWithAnotherValidIndex() {
        // Create a load instruction for local variable 1
        LoadInstruction instruction = new LoadInstruction(1);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the value of local variable 1
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(20, topValue.getValue());
    }

    /**
     * Test load instruction with a null local variable.
     */
    public void testLoadWithNullLocalVariable() {
        // Create a load instruction for local variable 2 (which is null)
        LoadInstruction instruction = new LoadInstruction(2);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is null
        Value topValue = state.popStack();
        assertEquals(Value.NULL, topValue);
    }

    /**
     * Test load instruction with an invalid (negative) index.
     */
    public void testLoadWithNegativeIndex() {
        // Create a load instruction with a negative index
        LoadInstruction instruction = new LoadInstruction(-1);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test load instruction with an out-of-bounds index.
     */
    public void testLoadWithOutOfBoundsIndex() {
        // Create a load instruction with an index beyond the size of local variables
        LoadInstruction instruction = new LoadInstruction(10);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test multiple load instructions in sequence.
     */
    public void testMultipleLoadInstructions() {
        // Create and execute first load instruction (load 0)
        LoadInstruction instruction1 = new LoadInstruction(0);
        instruction1.execute(state);

        // Create and execute second load instruction (load 1)
        LoadInstruction instruction2 = new LoadInstruction(1);
        instruction2.execute(state);

        // Check that the stack has two elements
        assertEquals(2, state.getStackSize());

        // Check the values on the stack (LIFO order)
        Value value2 = state.popStack();
        assertTrue(value2 instanceof IntegerValue);
        assertEquals(20, value2.getValue());

        Value value1 = state.popStack();
        assertTrue(value1 instanceof IntegerValue);
        assertEquals(10, value1.getValue());
    }

    /**
     * Test toString method of LoadInstruction.
     */
    public void testToString() {
        LoadInstruction instruction = new LoadInstruction(5);
        assertEquals("load 5", instruction.toString());
    }
}
