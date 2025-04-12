package fr.univreunion.bcterm.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;

/**
 * Represents a Control Flow Graph (CFG) for program analysis.
 * Contains a list of basic blocks and their control flow edges.
 */
public class CFG {
    private final List<BasicBlock> blocks = new ArrayList<>();
    private final Map<BasicBlock, Set<BasicBlock>> edges = new HashMap<>();

    /**
     * Adds a basic block to the Control Flow Graph.
     *
     * @param block the basic block to be added to the graph
     */
    public void addBlock(BasicBlock block) {
        blocks.add(block);
    }

    /**
     * Adds a control flow edge between two basic blocks in the Control Flow Graph.
     *
     * @param from the source basic block
     * @param to   the destination basic block
     */
    public void addEdge(BasicBlock from, BasicBlock to) {
        if (!edges.containsKey(from)) {
            edges.put(from, new HashSet<>());
        }
        edges.get(from).add(to);
    }

    public List<BasicBlock> getBlocks() {
        return this.blocks;
    }

    public Map<BasicBlock, Set<BasicBlock>> getEdges() {
        return this.edges;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BasicBlock block : blocks) {
            sb.append("Block ").append(block.getId()).append(":\n");
            for (BytecodeInstruction instr : block.getInstructions()) {
                sb.append("  ").append(instr).append("\n");
            }
            sb.append("Successors: ");
            for (BasicBlock succ : edges.get(block)) {
                sb.append("Block ").append(succ.getId()).append(" ");
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

}
