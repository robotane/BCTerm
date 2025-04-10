package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for AddInstruction class.
 */
public class AddInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test add instruction with two integer values on the stack.
     */
    public void testAddWithTwoIntegers() {
        // Push two integer values onto the stack
        state.pushStack(new IntegerValue(10));
        state.pushStack(new IntegerValue(20));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the sum of the two integers
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(30, topValue.getValue());
    }

    /**
     * Test add instruction with negative integers.
     */
    public void testAddWithNegativeIntegers() {
        // Push two integer values onto the stack, one negative
        state.pushStack(new IntegerValue(-10));
        state.pushStack(new IntegerValue(20));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the sum of the two integers
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(10, topValue.getValue());
    }

    /**
     * Test add instruction with both negative integers.
     */
    public void testAddWithBothNegativeIntegers() {
        // Push two negative integer values onto the stack
        state.pushStack(new IntegerValue(-10));
        state.pushStack(new IntegerValue(-20));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the sum of the two integers
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(-30, topValue.getValue());
    }

    /**
     * Test add instruction with zero.
     */
    public void testAddWithZero() {
        // Push an integer and zero onto the stack
        state.pushStack(new IntegerValue(42));
        state.pushStack(new IntegerValue(0));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is unchanged (42 + 0 = 42)
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(42, topValue.getValue());
    }

    /**
     * Test add instruction with only one value on the stack.
     */
    public void testAddWithOneValueOnStack() {
        // Push only one value onto the stack
        state.pushStack(new IntegerValue(10));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (not enough values on stack)
        assertFalse(result);

        // Check that the stack still has one value
        assertEquals(1, state.getStackSize());

        // Check that the value on the stack is unchanged
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(10, topValue.getValue());
    }

    /**
     * Test add instruction with an empty stack.
     */
    public void testAddWithEmptyStack() {
        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (empty stack)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test add instruction with non-integer values.
     */
    public void testAddWithNonIntegerValues() {
        // Push a non-integer value (null) and an integer onto the stack
        state.pushStack(Value.NULL);
        state.pushStack(new IntegerValue(10));

        // Create an add instruction
        AddInstruction instruction = new AddInstruction();

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (incompatible types)
        assertFalse(result);

        // Check that the stack still has the original values
        assertEquals(2, state.getStackSize());

        // Check that the values on the stack are unchanged and in the original order
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(10, topValue.getValue());

        Value secondValue = state.popStack();
        assertEquals(Value.NULL, secondValue);
    }

    /**
     * Test toString method of AddInstruction.
     */
    public void testToString() {
        AddInstruction instruction = new AddInstruction();
        assertEquals("add", instruction.toString());
    }
}
