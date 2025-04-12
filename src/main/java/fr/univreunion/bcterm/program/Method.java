package fr.univreunion.bcterm.program;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;

/**
 * Represents a method in a program with its control flow graph and associated
 * metadata.
 * Tracks method details such as name, signature, and block successors for
 * bytecode analysis.
 * Provides methods to access method properties and generate a string
 * representation of the method's structure.
 */
public class Method {
    private final String name;
    private final String signature;
    private final CFG cfg;
    private Program program; // TDOO: may be final ?
    private final Map<BasicBlock, Set<BasicBlock>> blockSuccessors;

    /**
     * Constructs a Method with a specific name, signature, control flow graph, and
     * program context.
     * Initializes the block successors map by creating an empty set for each basic
     * block
     * and populating it with the successor blocks from the CFG's edge structure.
     *
     * @param name      The name of the method
     * @param signature The method's signature defining its parameter and return
     *                  types
     * @param cfg       The control flow graph representing the method's structure
     * @param program   The program containing this method
     */
    public Method(String name, String signature, CFG cfg, Program program) {
        this.name = name;
        this.signature = signature;
        this.cfg = cfg;
        this.program = program;
        this.blockSuccessors = new HashMap<>();

        for (BasicBlock block : cfg.getBlocks()) {
            blockSuccessors.put(block, new HashSet<>());
        }

        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : cfg.getEdges().entrySet()) {
            BasicBlock source = entry.getKey();
            Set<BasicBlock> targets = entry.getValue();

            for (BasicBlock target : targets) {
                blockSuccessors.get(source).add(target);
            }
        }
    }

    /**
     * Constructs a Method with a default void signature.
     * Delegates to the full constructor with a default "(void):void" signature.
     *
     * @param name    The name of the method
     * @param cfg     The control flow graph representing the method's structure
     * @param program The program containing this method
     */
    public Method(String name, CFG cfg, Program program) {
        this(name, "(void):void", cfg, program);
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public CFG getCfg() {
        return cfg;
    }

    public Program getProgram() {
        return program;
    }

    public Set<BasicBlock> getBlockSuccessors(BasicBlock block) {
        return blockSuccessors.getOrDefault(block, new HashSet<>());
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (program.getMainMethodName().equals(name))
            sb.append("Main ");
        sb.append("Method ");
        sb.append(": ").append(name);
        sb.append(" ").append(signature).append("\n");

        for (BasicBlock block : cfg.getBlocks()) {
            sb.append("  Block ").append(block.getId()).append(": { ");

            List<BytecodeInstruction> instructions = block.getInstructions();
            for (int i = 0; i < instructions.size(); i++) {
                sb.append(instructions.get(i));
                if (i < instructions.size() - 1) {
                    sb.append(" :: ");
                }
            }
            sb.append(" } ");
            Set<BasicBlock> successors = blockSuccessors.get(block);
            if (!successors.isEmpty()) {
                sb.append("-> ");
                boolean first = true;
                for (BasicBlock successor : successors) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append("Block ").append(successor.getId());
                    first = false;
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

}
