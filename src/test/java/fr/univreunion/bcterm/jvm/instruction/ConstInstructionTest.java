package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for ConstInstruction class.
 */
public class ConstInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test const instruction with an integer value.
     */
    public void testConstWithIntegerValue() {
        // Create a const instruction with integer value 42
        ConstInstruction instruction = new ConstInstruction(42);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the integer 42
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(42, topValue.getValue());
    }

    /**
     * Test const instruction with a Value object.
     */
    public void testConstWithValueObject() {
        // Create a value object
        IntegerValue value = new IntegerValue(100);

        // Create a const instruction with the value object
        ConstInstruction instruction = new ConstInstruction(value);

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the integer 100
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(100, topValue.getValue());
    }

    /**
     * Test const instruction with null value.
     */
    public void testConstWithNullValue() {
        // Create a const instruction with null value
        ConstInstruction instruction = new ConstInstruction();

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
     * Test toString method of ConstInstruction.
     */
    public void testToString() {
        // Test toString() for integer value
        ConstInstruction intInstruction = new ConstInstruction(42);
        assertEquals("const 42", intInstruction.toString());

        // Test toString() for null value
        ConstInstruction nullInstruction = new ConstInstruction();
        assertEquals("const null", nullInstruction.toString());
    }

    /**
     * Test multiple const instructions in sequence.
     */
    public void testMultipleConstInstructions() {
        // Create and execute first instruction (push 10)
        ConstInstruction instruction1 = new ConstInstruction(10);
        instruction1.execute(state);

        // Create and execute second instruction (push 20)
        ConstInstruction instruction2 = new ConstInstruction(20);
        instruction2.execute(state);

        // Check stack size
        assertEquals(2, state.getStackSize());

        // Check values (LIFO order)
        Value value2 = state.popStack();
        assertTrue(value2 instanceof IntegerValue);
        assertEquals(20, value2.getValue());

        Value value1 = state.popStack();
        assertTrue(value1 instanceof IntegerValue);
        assertEquals(10, value1.getValue());
    }
}