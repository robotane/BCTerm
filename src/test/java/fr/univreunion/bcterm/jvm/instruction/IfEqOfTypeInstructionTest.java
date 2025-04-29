package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
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
     * Test ifeq instruction with integer value 0.
     */
    public void testIfEqWithIntegerZero() {
        // Push integer 0 onto the stack
        state.pushStack(new IntegerValue(0));

        // Create an ifeq instruction expecting integer 0
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (value was 0)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with non-zero integer value.
     */
    public void testIfEqWithNonZeroInteger() {
        // Push integer 42 onto the stack
        state.pushStack(new IntegerValue(42));

        // Create an ifeq instruction expecting integer 0
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was not successful (value was not 0)
        assertFalse(result);

        // Check that the stack still has the value (it wasn't popped since condition
        // wasn't met)
        assertEquals(1, state.getStackSize());
        Value topValue = state.peekStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(42, ((IntegerValue) topValue).getValue().intValue());
    }

    /**
     * Test ifeq instruction with null reference.
     */
    public void testIfEqWithNullReference() {
        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Create an ifeq instruction expecting null for a reference type
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new LocationValue(0, "java.lang.Object"));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (value was null)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifeq instruction with non-null reference.
     */
    public void testIfEqWithNonNullReference() {
        // Push a non-null reference onto the stack
        LocationValue locationValue = new LocationValue(12345, "java.lang.String");
        state.pushStack(locationValue);

        // Create an ifeq instruction expecting null for a reference type
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new LocationValue(0, "java.lang.Object"));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was not successful (value was not null)
        assertFalse(result);

        // Check that the stack still has the value (it wasn't popped since condition
        // wasn't met)
        assertEquals(1, state.getStackSize());
        assertEquals(locationValue, state.peekStack());
    }

    /**
     * Test ifeq instruction with empty stack.
     */
    public void testIfEqWithEmptyStack() {
        // Create an ifeq instruction
        IfEqOfTypeInstruction instruction = new IfEqOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction on empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (stack was empty)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test toString method of IfEqOfTypeInstruction.
     */
    public void testToString() {
        // Test toString() for integer type
        IfEqOfTypeInstruction intInstruction = new IfEqOfTypeInstruction(new IntegerValue(0));
        assertEquals("ifeq of type 0", intInstruction.toString());

        // Test toString() for reference type with type name
        IfEqOfTypeInstruction refInstruction = new IfEqOfTypeInstruction(new LocationValue(0, "java.lang.String"));
        assertEquals("ifeq of type java.lang.String", refInstruction.toString());

        // Test toString() for reference type without type name
        IfEqOfTypeInstruction refWithoutTypeInstruction = new IfEqOfTypeInstruction(new LocationValue(0));
        assertTrue(refWithoutTypeInstruction.toString().startsWith("ifeq of type loc@"));
    }
}
