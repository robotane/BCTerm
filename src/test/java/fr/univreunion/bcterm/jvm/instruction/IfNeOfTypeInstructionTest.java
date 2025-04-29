package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
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
     */
    public void testIfNeWithIntegerZero() {
        // Push integer 0 onto the stack
        state.pushStack(new IntegerValue(0));

        // Create an ifne instruction expecting integer 0
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (value was 0, and we want to stop if it's not
        // non-zero)
        assertFalse(result);

        // The stack should still have the value since the condition wasn't met
        assertEquals(1, state.getStackSize());
        Value topValue = state.peekStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(0, ((IntegerValue) topValue).getValue().intValue());
    }

    /**
     * Test ifne instruction with non-zero integer value.
     */
    public void testIfNeWithNonZeroInteger() {
        // Push integer 42 onto the stack
        state.pushStack(new IntegerValue(42));

        // Create an ifne instruction expecting integer 0
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (value was non-zero)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with null reference.
     */
    public void testIfNeWithNullReference() {
        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Create an ifne instruction expecting null for a reference type
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new LocationValue(0, "java.lang.Object"));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was not successful (value was null, and we want to
        // continue if it's not non-null)
        assertFalse(result);

        // Check that the stack still has the null value (it wasn't popped since
        // condition wasn't met)
        assertEquals(1, state.getStackSize());
        assertEquals(Value.NULL, state.peekStack());
    }

    /**
     * Test ifne instruction with non-null reference.
     */
    public void testIfNeWithNonNullReference() {
        // Push a non-null reference onto the stack
        LocationValue locationValue = new LocationValue(12345, "java.lang.String");
        state.pushStack(locationValue);

        // Create an ifne instruction expecting null for a reference type
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new LocationValue(0, "java.lang.Object"));

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (value was non-null, and we want to
        // continue if it's non-null)
        assertTrue(result);

        // Check that the stack is now empty (value was popped)
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test ifne instruction with empty stack.
     */
    public void testIfNeWithEmptyStack() {
        // Create an ifne instruction
        IfNeOfTypeInstruction instruction = new IfNeOfTypeInstruction(new IntegerValue(0));

        // Execute the instruction on empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (stack was empty)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test toString method of IfNeOfTypeInstruction.
     */
    public void testToString() {
        // Test toString() for integer type
        IfNeOfTypeInstruction intInstruction = new IfNeOfTypeInstruction(new IntegerValue(0));
        assertEquals("ifne of type 0", intInstruction.toString());

        // Test toString() for reference type with type name
        IfNeOfTypeInstruction refInstruction = new IfNeOfTypeInstruction(new LocationValue(0, "java.lang.String"));
        assertEquals("ifne of type java.lang.String", refInstruction.toString());

        // Test toString() for reference type without type name
        IfNeOfTypeInstruction refWithoutTypeInstruction = new IfNeOfTypeInstruction(new LocationValue(0));
        assertTrue(refWithoutTypeInstruction.toString().startsWith("ifne of type loc@"));
    }
}
