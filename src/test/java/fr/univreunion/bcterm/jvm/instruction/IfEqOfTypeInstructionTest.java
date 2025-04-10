package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for IfEqOfTypeInstruction class.
 */
public class IfEqOfTypeInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test ifeq instruction with integer 0 on stack.
     */
    public void testIfEqWithIntegerZero() {
        // Push integer 0 onto the stack
        state.pushStack(new IntegerValue(0));

        // Create an ifeq instruction expecting integer 0
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (condition met)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with non-zero integer on stack.
     */
    public void testIfEqWithNonZeroInteger() {
        // Push non-zero integer onto the stack
        state.pushStack(new IntegerValue(42));

        // Create an ifeq instruction expecting integer 0
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (condition not met)
        assertFalse(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with null on stack.
     */
    public void testIfEqWithNull() {
        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Create an ifeq instruction expecting null
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(Value.NULL);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (condition met)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with non-null reference on stack.
     */
    public void testIfEqWithNonNullReference() {
        // Create a location value (non-null reference)
        JVMObject object = new JVMObject("TestClass");
        LocationValue locationValue = state.allocateNewObject(object);

        // Push the location value onto the stack
        state.pushStack(locationValue);

        // Create an ifeq instruction expecting null
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(Value.NULL);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (condition not met)
        assertFalse(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with empty stack.
     */
    public void testIfEqWithEmptyStack() {
        // Create an ifeq instruction
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (empty stack)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with type mismatch.
     */
    public void testIfEqWithTypeMismatch() {
        // Push integer onto the stack
        state.pushStack(new IntegerValue(0));

        // Create an ifeq instruction expecting null
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(Value.NULL);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (type mismatch)
        assertFalse(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test toString method of IfEqOfTypeInstruction.
     */
    public void testToString() {
        // Test toString() for integer type
        IfEqOfTypeInstruction intInstruction = new IfEqOfTypeInstruction(new IntegerValue(0));
        assertEquals("ifeq of type 0", intInstruction.toString());

        // Test toString() for null type
        IfEqOfTypeInstruction nullInstruction = new IfEqOfTypeInstruction(Value.NULL);
        assertEquals("ifeq of type null", nullInstruction.toString());
    }
}
