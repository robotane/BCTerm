package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for GetFieldInstruction class.
 */
public class GetFieldInstructionTest extends TestCase {

    private JVMState state;
    private JVMObject testObject;
    private LocationValue locationValue;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();

        // Create a test object with fields
        testObject = new JVMObject("TestClass");
        testObject.setField("age", new IntegerValue(25));
        testObject.setField("score", new IntegerValue(100));

        // Allocate the object in memory
        locationValue = state.allocateNewObject(testObject);
    }

    /**
     * Test getfield instruction with a valid object reference and field.
     */
    public void testGetFieldWithValidObject() {
        // Push the location value onto the stack
        state.pushStack(locationValue);

        // Create a getfield instruction for the "age" field
        GetFieldInstruction instruction = new GetFieldInstruction("age");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack still has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is the field value (25)
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(25, topValue.getValue());
    }

    /**
     * Test getfield instruction with a null reference.
     */
    public void testGetFieldWithNullReference() {
        // Push null onto the stack
        state.pushStack(Value.NULL);

        // Create a getfield instruction
        GetFieldInstruction instruction = new GetFieldInstruction("anyField");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (null reference)
        assertFalse(result);

        // Check that the stack still has the null value (it wasn't popped)
        assertEquals(1, state.getStackSize());
        assertEquals(Value.NULL, state.peekStack());
    }

    /**
     * Test getfield instruction with a non-existent field.
     */
    public void testGetFieldWithNonExistentField() {
        // Push the location value onto the stack
        state.pushStack(locationValue);

        // Create a getfield instruction for a non-existent field
        GetFieldInstruction instruction = new GetFieldInstruction("nonExistentField");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful (even with non-existent field)
        assertTrue(result);

        // Check that the stack still has one element
        assertEquals(1, state.getStackSize());
    }

    /**
     * Test getfield instruction with an empty stack.
     */
    public void testGetFieldWithEmptyStack() {
        // Create a getfield instruction
        GetFieldInstruction instruction = new GetFieldInstruction("anyField");

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (empty stack)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test getfield instruction with multiple fields.
     */
    public void testGetFieldWithMultipleFields() {
        // Push the location value onto the stack
        state.pushStack(locationValue);

        // Create and execute getfield instruction for "age"
        GetFieldInstruction ageInstruction = new GetFieldInstruction("age");
        ageInstruction.execute(state);

        // Push the location value onto the stack again
        state.pushStack(locationValue);

        // Create and execute getfield instruction for "score"
        GetFieldInstruction scoreInstruction = new GetFieldInstruction("score");
        scoreInstruction.execute(state);

        // Check that the stack has two elements
        assertEquals(2, state.getStackSize());

        // Check the values on the stack (LIFO order)
        Value scoreValue = state.popStack();
        assertTrue(scoreValue instanceof IntegerValue);
        assertEquals(100, scoreValue.getValue());

        Value ageValue = state.popStack();
        assertTrue(ageValue instanceof IntegerValue);
        assertEquals(25, ageValue.getValue());
    }

    /**
     * Test toString method of GetFieldInstruction.
     */
    public void testToString() {
        GetFieldInstruction instruction = new GetFieldInstruction("testField");
        assertEquals("getfield testField", instruction.toString());
    }
}
