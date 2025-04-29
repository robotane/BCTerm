package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for PutFieldInstruction class.
 */
public class PutFieldInstructionTest extends TestCase {

    private JVMState state;
    private JVMObject testObject;
    private LocationValue locationValue;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();

        // Create a test object
        testObject = new JVMObject("TestClass");

        // Allocate the object in memory
        locationValue = state.allocateNewObject(testObject);
    }

    /**
     * Test putfield instruction with a valid object reference and value.
     */
    public void testPutFieldWithValidObject() {
        // Push the object reference onto the stack
        state.pushStack(locationValue);

        // Push the value to store onto the stack
        IntegerValue valueToStore = new IntegerValue(42);
        state.pushStack(valueToStore);

        // Create a putfield instruction for the "age" field
        PutFieldInstruction instruction = new PutFieldInstruction("age");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack is now empty (both values were popped)
        assertEquals(0, state.getStackSize());

        // Check that the field was set in the object
        Value fieldValue = testObject.getField("age");
        assertNotNull(fieldValue);
        assertTrue(fieldValue instanceof IntegerValue);
        assertEquals(42, fieldValue.getValue());
    }

    /**
     * Test putfield instruction with a null object reference.
     */
    public void testPutFieldWithNullReference() {
        // Push null reference onto the stack
        state.pushStack(Value.NULL);

        // Push the value to store onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a putfield instruction
        PutFieldInstruction instruction = new PutFieldInstruction("anyField");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (null reference)
        assertFalse(result);

        // Check that the stack still has both values (they weren't popped since
        // execution failed)
        assertEquals(2, state.getStackSize());

        // Verify the values are still on the stack in the correct order
        Value topValue = state.popStack();
        assertTrue(topValue instanceof IntegerValue);
        assertEquals(42, ((IntegerValue) topValue).getValue().intValue());

        assertEquals(Value.NULL, state.popStack());
    }

    /**
     * Test putfield instruction with an empty stack.
     */
    public void testPutFieldWithEmptyStack() {
        // Create a putfield instruction
        PutFieldInstruction instruction = new PutFieldInstruction("anyField");

        // Execute the instruction on an empty stack
        boolean result = instruction.execute(state);

        // Check that execution failed (empty stack)
        assertFalse(result);

        // Check that the stack is still empty
        assertEquals(0, state.getStackSize());
    }

    /**
     * Test putfield instruction with only one value on the stack.
     */
    public void testPutFieldWithOneValueOnStack() {
        // Push only one value onto the stack
        state.pushStack(new IntegerValue(42));

        // Create a putfield instruction
        PutFieldInstruction instruction = new PutFieldInstruction("anyField");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution failed (not enough values on stack)
        assertFalse(result);

        // Check that the stack still has one value
        assertEquals(1, state.getStackSize());
    }

    /**
     * Test putfield instruction with multiple fields.
     */
    public void testPutFieldWithMultipleFields() {
        // Set first field
        state.pushStack(locationValue);
        state.pushStack(new IntegerValue(25));
        PutFieldInstruction ageInstruction = new PutFieldInstruction("age");
        ageInstruction.execute(state);

        // Set second field
        state.pushStack(locationValue);
        state.pushStack(new IntegerValue(100));
        PutFieldInstruction scoreInstruction = new PutFieldInstruction("score");
        scoreInstruction.execute(state);

        // Check that both fields were set correctly
        Value ageValue = testObject.getField("age");
        assertNotNull(ageValue);
        assertTrue(ageValue instanceof IntegerValue);
        assertEquals(25, ageValue.getValue());

        Value scoreValue = testObject.getField("score");
        assertNotNull(scoreValue);
        assertTrue(scoreValue instanceof IntegerValue);
        assertEquals(100, scoreValue.getValue());

        // Chek that the object has the correct number of fields
        assertEquals(2, testObject.getFields().size());
    }

    /**
     * Test putfield instruction overwriting an existing field.
     */
    public void testPutFieldOverwritingExistingField() {
        // Set initial field value
        state.pushStack(locationValue);
        state.pushStack(new IntegerValue(25));
        PutFieldInstruction initialInstruction = new PutFieldInstruction("age");
        initialInstruction.execute(state);

        // Overwrite the field
        state.pushStack(locationValue);
        state.pushStack(new IntegerValue(30));
        PutFieldInstruction overwriteInstruction = new PutFieldInstruction("age");
        overwriteInstruction.execute(state);

        // Check that the field was updated
        Value ageValue = testObject.getField("age");
        assertNotNull(ageValue);
        assertTrue(ageValue instanceof IntegerValue);
        assertEquals(30, ageValue.getValue());
    }

    /**
     * Test toString method of PutFieldInstruction.
     */
    public void testToString() {
        PutFieldInstruction instruction = new PutFieldInstruction("testField");
        assertEquals("putfield testField", instruction.toString());
    }
}
