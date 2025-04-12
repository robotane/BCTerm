package fr.univreunion.bcterm.program;

import java.util.List;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;

/**
 * Represents a basic block in a bytecode program, containing a sequence of
 * bytecode instructions.
 * A basic block is a linear sequence of instructions with no branches except at
 * the end.
 */
public class BasicBlock {
    private final int id;
    private final List<BytecodeInstruction> instructions;

    /**
     * Constructs a new basic block with the given ID and instructions.
     * 
     * @param id           The unique identifier for this basic block
     * @param instructions The list of bytecode instructions in this block
     */
    public BasicBlock(int id, List<BytecodeInstruction> instructions) {
        this.id = id;
        this.instructions = instructions;
    }

    public int getId() {
        return id;
    }

    public List<BytecodeInstruction> getInstructions() {
        return instructions;
    }

}
