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

    /**
     * Generates a trimmed DOT (graph description language) representation of the
     * Control Flow Graph.
     * 
     * @param labelKey the key used to retrieve labels for bytecode instructions
     * @return a DOT-formatted string representing the Control Flow Graph with
     *         aligned instructions and labels
     */
    public String toTrimedDOT(String labelKey) {
        StringBuilder dot = new StringBuilder();

        // Set default node and edge attributes
        dot.append("  node [shape=box fontname=\"monospace\"];\n");
        dot.append("  edge [color=blue];\n");

        // Add nodes
        for (BasicBlock block : blocks) {
            dot.append("  block").append(block.getId())
                    .append(" [label=\"");

            // Determine maximum instruction length for this block
            int maxInstrLength = 0;
            for (BytecodeInstruction instr : block.getInstructions()) {
                int length = instr.toString().replace("\"", "\\\"").length();
                if (length > maxInstrLength) {
                    maxInstrLength = length;
                }
            }

            maxInstrLength += 3; // Add some padding

            // Add instructions to node label with labels aligned consistently
            for (BytecodeInstruction instr : block.getInstructions()) {
                String instrStr = instr.toString().replace("\"", "\\\"");
                String label = instr.getLabelFor(labelKey);

                // Use %-s format for left alignment with consistent spacing
                String paddedInstr = String.format("%-" + maxInstrLength + "s %s\\l", instrStr, label);
                dot.append(paddedInstr);
            }
            dot.append("\"];\n");
        }

        // Add edges
        for (Map.Entry<BasicBlock, Set<BasicBlock>> entry : edges.entrySet()) {
            BasicBlock from = entry.getKey();
            for (BasicBlock to : entry.getValue()) {
                dot.append("  block").append(from.getId())
                        .append(" -> block").append(to.getId()).append(";\n");
            }
        }

        return dot.toString();
    }

    /**
     * Generates a complete DOT graph representation of the Control Flow Graph.
     *
     * @param labelKey the key used to retrieve labels for bytecode instructions
     * @return a complete DOT-formatted string representing the entire Control Flow
     *         Graph
     */
    public String toDOT(String labelKey) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph {\n");
        dot.append(toTrimedDOT(labelKey));
        dot.append("}\n");
        return dot.toString();
    }

}
