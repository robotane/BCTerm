package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.NullValue;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for IfNeOfTypeInstruction class.
 */
public class IfNeOfTypeInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test ifne instruction with integer value 0.
     * Should continue execution (return true) when value is 0.
     */
    public void testIfNeWithIntegerZero() {
        // Create expected value (0)
        IntegerValue expectedValue = new IntegerValue(0);

        // Create ifne instruction to check against 0
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(expectedValue);

        // Push 0 onto the stack
        state.pushStack(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution should continue (value is 0, so it's equal to expected)
        assertTrue(result);

        // Check that the stack is now empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with non-zero integer value.
     * Should stop execution (return false) when value is not 0.
     */
    public void testIfNeWithNonZeroInteger() {
        // Create expected value (0)
        IntegerValue expectedValue = new IntegerValue(0);

        // Create ifne instruction to check against 0
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(expectedValue);

        // Push non-zero value onto the stack
        state.pushStack(new IntegerValue(42));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution should stop (value is not 0)
        assertFalse(result);

        // Check that the stack is now empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with null value.
     * Should stop execution (return false) when value is null.
     */
    public void testIfNeWithNullValue() {
        // Create expected value (null)
        NullValue expectedValue = (NullValue) Value.NULL;

        // Create ifne instruction to check against null
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(expectedValue);

        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution should stop (value is null)
        assertFalse(result);

        // Check that the stack is now empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with non-null reference value.
     * Should continue execution (return true) when value is not null.
     */
    public void testIfNeWithNonNullReference() {
        // Create expected value (null)
        NullValue expectedValue = (NullValue) Value.NULL;

        // Create ifne instruction to check against null
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(expectedValue);

        // Push a non-null reference onto the stack (using IntegerValue as an example)
        state.pushStack(new IntegerValue(1));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution should continue (value is not null)
        assertTrue(result);

        // Check that the stack is now empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with empty stack.
     * Should fail (return false) when stack is empty.
     */
    public void testIfNeWithEmptyStack() {
        // Create ifne instruction
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction on empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed
        assertFalse(result);

        // Stack should still be empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test toString method of IfNeOfTypeInstruction.
     */
    public void testToString() {
        // Test toString() for integer value
        IfNeOfTypeInstruction intInstruction = new IfNeOfTypeInstruction(new IntegerValue(0));
        assertEquals("ifne of type 0", intInstruction.toString());

        // Test toString() for null value
        IfNeOfTypeInstruction nullInstruction = new IfNeOfTypeInstruction(Value.NULL);
        assertEquals("ifne of type null", nullInstruction.toString());
    }
}
