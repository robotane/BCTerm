package fr.univreunion.bcterm.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.ConstInstruction;
import junit.framework.TestCase;

/**
 * Test case for Method class.
 */
public class MethodTest extends TestCase {

    private Program program;
    private CFG cfg;
    private BasicBlock block1;
    private BasicBlock block2;

    @Override
    protected void setUp() {
        // Create a program
        program = new Program("TestProgram");

        // Create a CFG with two basic blocks
        cfg = new CFG();

        // Create instructions for block1
        List<BytecodeInstruction> instructions1 = new ArrayList<>();
        instructions1.add(new ConstInstruction(10));
        instructions1.add(new ConstInstruction(20));

        // Create instructions for block2
        List<BytecodeInstruction> instructions2 = new ArrayList<>();
        instructions2.add(new ConstInstruction(30));
        instructions2.add(new ConstInstruction(40));

        // Create basic blocks
        block1 = new BasicBlock(1, instructions1);
        block2 = new BasicBlock(2, instructions2);

        // Add blocks to CFG
        cfg.addBlock(block1);
        cfg.addBlock(block2);

        // Add edge from block1 to block2
        cfg.addEdge(block1, block2);
    }

    /**
     * Test method creation with full constructor.
     */
    public void testMethodCreationWithFullConstructor() {
        // Create a method with full constructor
        Method method = new Method("testMethod", "(int,int):void", cfg, program);

        // Check method properties
        assertEquals("testMethod", method.getName());
        assertEquals("(int,int):void", method.getSignature());
        assertSame(cfg, method.getCfg());
        assertSame(program, method.getProgram());
    }

    /**
     * Test method creation with simplified constructor.
     */
    public void testMethodCreationWithSimplifiedConstructor() {
        // Create a method with simplified constructor
        Method method = new Method("testMethod", cfg, program);

        // Check method properties
        assertEquals("testMethod", method.getName());
        assertEquals("():void", method.getSignature()); // Default signature
        assertSame(cfg, method.getCfg());
        assertSame(program, method.getProgram());
    }

    /**
     * Test adding method to program.
     */
    public void testAddMethodToProgram() {
        // Create a method
        Method method = new Method("testMethod", cfg, program);

        // Add method to program
        program.addMethod(method);

        // Check that method is in program
        assertTrue(program.getMethodNames().contains("testMethod"));
        assertSame(method, program.getMethod("testMethod"));

        // Check that program is set as main method (since it's the first method added)
        assertEquals("testMethod", program.getMainMethodName());
    }

    /**
     * Test setting program for method.
     */
    public void testSetProgram() {
        // Create a method
        Method method = new Method("testMethod", cfg, program);

        // Create a new program
        Program newProgram = new Program("NewProgram");

        // Set new program for method
        method.setProgram(newProgram);

        // Check that program is updated
        assertSame(newProgram, method.getProgram());
    }

    /**
     * Test getting block successors.
     */
    public void testGetBlockSuccessors() {
        // Create a method
        Method method = new Method("testMethod", cfg, program);

        // Get successors of block1
        Set<BasicBlock> successors = method.getBlockSuccessors(block1);

        // assertTrue(successors.contains(block2));
        assertEquals(1, successors.size());
    }

    /**
     * Test toString method.
     */
    public void testToString() {
        // Create a method and add it to program
        Method method = new Method("testMethod", "(int):void", cfg, program);
        program.addMethod(method);
        program.setMainMethodName("testMethod");

        // Get string representation
        String str = method.toString();

        // Check that string contains expected information
        assertTrue(str.contains("Main Method"));
        assertTrue(str.contains("testMethod"));
        assertTrue(str.contains("(int):void"));
        assertTrue(str.contains("Block 1"));
        assertTrue(str.contains("Block 2"));
        assertTrue(str.contains("const 10"));
        assertTrue(str.contains("const 20"));
        assertTrue(str.contains("const 30"));
        assertTrue(str.contains("const 40"));
    }

    /**
     * Test method with multiple blocks and complex edges.
     */
    public void testMethodWithComplexCFG() {
        // Create a new CFG
        CFG complexCfg = new CFG();

        // Create three blocks
        List<BytecodeInstruction> instrs1 = new ArrayList<>();
        instrs1.add(new ConstInstruction(1));
        BasicBlock b1 = new BasicBlock(1, instrs1);

        List<BytecodeInstruction> instrs2 = new ArrayList<>();
        instrs2.add(new ConstInstruction(2));
        BasicBlock b2 = new BasicBlock(2, instrs2);

        List<BytecodeInstruction> instrs3 = new ArrayList<>();
        instrs3.add(new ConstInstruction(3));
        BasicBlock b3 = new BasicBlock(3, instrs3);

        // Add blocks to CFG
        complexCfg.addBlock(b1);
        complexCfg.addBlock(b2);
        complexCfg.addBlock(b3);

        // Add edges: b1 -> b2, b1 -> b3, b2 -> b3
        complexCfg.addEdge(b1, b2);
        complexCfg.addEdge(b1, b3);
        complexCfg.addEdge(b2, b3);

        // Create method with complex CFG
        Method method = new Method("complexMethod", complexCfg, program);

        // Check that CFG has correct structure
        assertEquals(3, complexCfg.getBlocks().size());
        assertTrue(complexCfg.getEdges().get(b1).contains(b2));
        assertTrue(complexCfg.getEdges().get(b1).contains(b3));
        assertTrue(complexCfg.getEdges().get(b2).contains(b3));

        // Add method to program
        program.addMethod(method);

        // Check that method is in program
        assertTrue(program.getMethodNames().contains("complexMethod"));
    }
}
