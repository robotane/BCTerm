package fr.univreunion.bcterm.jvm.instruction;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import junit.framework.TestCase;

/**
 * Test case for NewInstruction class.
 */
public class NewInstructionTest extends TestCase {

    private JVMState state;

    @Override
    protected void setUp() {
        // Create a new empty JVM state for testing
        state = new JVMState();
    }

    /**
     * Test new instruction with a class name.
     */
    public void testNewWithClassName() {
        // Create a new instruction for class "TestClass"
        NewInstruction instruction = new NewInstruction("TestClass");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is a location value
        Value topValue = state.popStack();
        assertTrue(topValue instanceof LocationValue);

        // Check that the location value points to an object of the correct class
        LocationValue locationValue = (LocationValue) topValue;
        JVMObject object = state.getObject(locationValue);
        assertNotNull(object);
        assertEquals("TestClass", object.getClassTag());
    }

    /**
     * Test new instruction with a different class name.
     */
    public void testNewWithDifferentClassName() {
        // Create a new instruction for class "AnotherClass"
        NewInstruction instruction = new NewInstruction("AnotherClass");

        // Execute the instruction
        boolean result = instruction.execute(state);

        // Check that execution was successful
        assertTrue(result);

        // Check that the stack now has one element
        assertEquals(1, state.getStackSize());

        // Check that the value on top of the stack is a location value
        Value topValue = state.popStack();
        assertTrue(topValue instanceof LocationValue);

        // Check that the location value points to an object of the correct class
        LocationValue locationValue = (LocationValue) topValue;
        JVMObject object = state.getObject(locationValue);
        assertNotNull(object);
        assertEquals("AnotherClass", object.getClassTag());
    }

    /**
     * Test multiple new instructions in sequence.
     */
    public void testMultipleNewInstructions() {
        // Create and execute first new instruction
        NewInstruction instruction1 = new NewInstruction("Class1");
        instruction1.execute(state);

        // Create and execute second new instruction
        NewInstruction instruction2 = new NewInstruction("Class2");
        instruction2.execute(state);

        // Check that the stack has two elements
        assertEquals(2, state.getStackSize());

        // Check the values on the stack (LIFO order)
        Value value2 = state.popStack();
        assertTrue(value2 instanceof LocationValue);
        LocationValue locationValue2 = (LocationValue) value2;
        JVMObject object2 = state.getObject(locationValue2);
        assertNotNull(object2);
        assertEquals("Class2", object2.getClassTag());

        Value value1 = state.popStack();
        assertTrue(value1 instanceof LocationValue);
        LocationValue locationValue1 = (LocationValue) value1;
        JVMObject object1 = state.getObject(locationValue1);
        assertNotNull(object1);
        assertEquals("Class1", object1.getClassTag());
    }

    /**
     * Test that new instruction allocates unique objects.
     */
    public void testNewAllocatesUniqueObjects() {
        // Create and execute two new instructions for the same class
        NewInstruction instruction1 = new NewInstruction("SameClass");
        instruction1.execute(state);

        NewInstruction instruction2 = new NewInstruction("SameClass");
        instruction2.execute(state);

        // Check that the stack has two elements
        assertEquals(2, state.getStackSize());

        // Get the two location values
        Value value2 = state.popStack();
        Value value1 = state.popStack();

        // Check that they are different location values
        assertNotSame(value1, value2);

        // Check that they point to different objects
        LocationValue locationValue1 = (LocationValue) value1;
        LocationValue locationValue2 = (LocationValue) value2;

        JVMObject object1 = state.getObject(locationValue1);
        JVMObject object2 = state.getObject(locationValue2);

        assertNotSame(object1, object2);
    }

    /**
     * Test that new instruction creates an empty object.
     */
    public void testNewCreatesEmptyObject() {
        // Create and execute a new instruction
        NewInstruction instruction = new NewInstruction("EmptyClass");
        instruction.execute(state);

        // Get the object
        Value value = state.popStack();
        LocationValue locationValue = (LocationValue) value;
        JVMObject object = state.getObject(locationValue);
        // Check that the object has no fields initially
        assertTrue(object.getFields().isEmpty());
    }

    /**
     * Test toString method of NewInstruction.
     */
    public void testToString() {
        NewInstruction instruction = new NewInstruction("TestClass");
        assertEquals("new TestClass", instruction.toString());
    }
}
