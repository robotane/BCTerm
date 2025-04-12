package fr.univreunion.bcterm.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.ConstInstruction;
import junit.framework.TestCase;

/**
 * Test case for Program class.
 */
public class ProgramTest extends TestCase {

    private CFG createSimpleCFG() {
        CFG cfg = new CFG();

        // Create a simple basic block with one instruction
        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(new ConstInstruction(42));
        BasicBlock block = new BasicBlock(1, instructions);

        cfg.addBlock(block);
        return cfg;
    }

    /**
     * Test program creation with name only.
     */
    public void testProgramCreationWithNameOnly() {
        // Create a program with just a name
        Program program = new Program("TestProgram");

        // Check program properties
        assertEquals("TestProgram", program.getName());
        assertTrue(program.getMethods().isEmpty());
        assertNull(program.getMainMethodName());
    }

    /**
     * Test program creation with name and initial method.
     */
    public void testProgramCreationWithInitialMethod() {
        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Create a method
        Method method = new Method("initialMethod", cfg, null);

        // Create a program with name and initial method
        Program program = new Program("TestProgram", method);

        // Check program properties
        assertEquals("TestProgram", program.getName());
        assertEquals(1, program.getMethods().size());
        assertTrue(program.getMethods().containsKey("initialMethod"));
        assertEquals("initialMethod", program.getMainMethodName());

        // Check that method's program reference is set
        assertSame(program, method.getProgram());
    }

    /**
     * Test program creation with name, methods map, and main method name.
     */
    public void testProgramCreationWithMethodsMap() {
        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Create methods
        Method method1 = new Method("method1", cfg, null);
        Method method2 = new Method("method2", cfg, null);

        // Create methods map
        Map<String, Method> methods = new HashMap<>();
        methods.put("method1", method1);
        methods.put("method2", method2);

        // Create a program with name, methods map, and main method name
        Program program = new Program("TestProgram", methods, "method2");

        // Check program properties
        assertEquals("TestProgram", program.getName());
        assertEquals(2, program.getMethods().size());
        assertTrue(program.getMethods().containsKey("method1"));
        assertTrue(program.getMethods().containsKey("method2"));
        assertEquals("method2", program.getMainMethodName());
    }

    /**
     * Test adding methods to program.
     */
    public void testAddMethod() {
        // Create a program
        Program program = new Program("TestProgram");

        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Add a method using Method object
        Method method1 = new Method("method1", cfg, null);
        program.addMethod(method1);

        // Add a method using name and CFG
        program.addMethod("method2", cfg);

        // Add a method using name, signature, and CFG
        program.addMethod("method3", "(int):void", cfg);

        // Check that methods were added
        assertEquals(3, program.getMethods().size());
        assertTrue(program.getMethods().containsKey("method1"));
        assertTrue(program.getMethods().containsKey("method2"));
        assertTrue(program.getMethods().containsKey("method3"));

        // Check that first method added is set as main method
        assertEquals("method1", program.getMainMethodName());

        // Check that method's program reference is set
        assertSame(program, method1.getProgram());
        assertSame(program, program.getMethod("method2").getProgram());
        assertSame(program, program.getMethod("method3").getProgram());
    }

    /**
     * Test getting method by name.
     */
    public void testGetMethod() {
        // Create a program
        Program program = new Program("TestProgram");

        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Add methods
        Method method1 = new Method("method1", cfg, null);
        program.addMethod(method1);
        program.addMethod("method2", cfg);

        // Get methods by name
        Method retrievedMethod1 = program.getMethod("method1");
        Method retrievedMethod2 = program.getMethod("method2");

        // Check that correct methods were retrieved
        assertSame(method1, retrievedMethod1);
        assertEquals("method2", retrievedMethod2.getName());

        // Try to get non-existent method
        try {
            program.getMethod("nonExistentMethod");
            fail("Should throw IllegalArgumentException for non-existent method");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    /**
     * Test getting method names.
     */
    public void testGetMethodNames() {
        // Create a program
        Program program = new Program("TestProgram");

        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Add methods
        program.addMethod("method1", cfg);
        program.addMethod("method2", cfg);
        program.addMethod("method3", cfg);

        // Get method names
        assertEquals(3, program.getMethodNames().size());
        assertTrue(program.getMethodNames().contains("method1"));
        assertTrue(program.getMethodNames().contains("method2"));
        assertTrue(program.getMethodNames().contains("method3"));
    }

    /**
     * Test setting main method name.
     */
    public void testSetMainMethodName() {
        // Create a program
        Program program = new Program("TestProgram");

        // Create a CFG
        CFG cfg = createSimpleCFG();

        // Add methods
        program.addMethod("method1", cfg);
        program.addMethod("method2", cfg);

        // Check initial main method
        assertEquals("method1", program.getMainMethodName());

        // Set main method to method2
        program.setMainMethodName("method2");
        assertEquals("method2", program.getMainMethodName());

        // Try to set main method to non-existent method
        try {
            program.setMainMethodName("nonExistentMethod");
            fail("Should throw IllegalArgumentException for non-existent method");
        } catch (IllegalArgumentException e) {
            // Expected exception
        }
    }

    /**
     * Test toString method.
     */
    public void testToString() {
        // Create a program
        Program program = new Program("TestProgram");

        // Create CFGs
        CFG cfg1 = createSimpleCFG();
        CFG cfg2 = new CFG();

        // Create a more complex basic block for cfg2
        List<BytecodeInstruction> instructions = new ArrayList<>();
        instructions.add(new ConstInstruction(10));
        instructions.add(new ConstInstruction(20));
        BasicBlock block = new BasicBlock(1, instructions);
        cfg2.addBlock(block);

        // Add methods
        program.addMethod("method1", "(void):int", cfg1);
        program.addMethod("method2", "(int,int):void", cfg2);

        // Set main method
        program.setMainMethodName("method2");

        // Get string representation
        String str = program.toString();

        // Check that string contains expected information
        assertTrue(str.contains("Program: TestProgram"));
        assertTrue(str.contains("Method : method1"));
        assertTrue(str.contains("(void):int"));
        assertTrue(str.contains("Main Method : method2"));
        assertTrue(str.contains("(int,int):void"));
        assertTrue(str.contains("const 42"));
        assertTrue(str.contains("const 10"));
        assertTrue(str.contains("const 20"));
    }
}
