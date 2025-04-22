package fr.univreunion.bcterm.program;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.state.JVMState;

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
     * Delegates to the full constructor with a default "():void" signature.
     *
     * @param name    The name of the method
     * @param cfg     The control flow graph representing the method's structure
     * @param program The program containing this method
     */
    public Method(String name, CFG cfg, Program program) {
        this(name, "():void", cfg, program);
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

    public Set<JVMState> execute(JVMState initialState) {
        // Method execution ...
        System.out.println("\nExecuting method " + name);
        System.out.println("---------------------------------");

        // Start with the first block of the method
        BasicBlock startBlock = cfg.getBlocks().get(0);

        // Set to store final states
        Set<JVMState> finalStates = new HashSet<>();

        // Set to track blocks already visited in current path
        Set<BasicBlock> visitedInPath = new HashSet<>();

        // Execute recursively from the start block
        executeRecursive(startBlock, new JVMState(initialState), finalStates, visitedInPath);

        System.out.println("\nMethod " + name + " execution completed");
        System.out.println("Found " + finalStates.size() + " final states");
        System.out.println("---------------------------------");

        return finalStates;
    }

    private void executeRecursive(BasicBlock currentBlock, JVMState currentState,
            Set<JVMState> finalStates, Set<BasicBlock> visitedInPath) {
        // Check if we are in a cycle
        if (visitedInPath.contains(currentBlock)) {
            System.out.println("Cycle detected at block " + currentBlock.getId() + " in method " + name);
            // We can choose to add the current state as final state or not
            finalStates.add(new JVMState(currentState));
            return;
        }

        // Mark the block as visited in the current path
        visitedInPath.add(currentBlock);

        System.out.println("Executing block " + currentBlock.getId() + " in method " + name);

        // Execute instructions of the current block
        JVMState stateAfterBlock = executeBlock(currentBlock, new JVMState(currentState));

        // If execution failed, stop this path
        if (stateAfterBlock == null) {
            System.out.println("Execution of block " + currentBlock.getId() + " failed in method " + name);
            visitedInPath.remove(currentBlock);
            return;
        }

        // Get successors of the current block
        Set<BasicBlock> nextBlocks = getBlockSuccessors(currentBlock);

        // If no successors, add current state as final state
        if (nextBlocks.isEmpty()) {
            System.out.println("End of path at block " + currentBlock.getId() + " in method " + name);
            finalStates.add(stateAfterBlock);
        } else {
            // Execute recursively for each successor
            for (BasicBlock nextBlock : nextBlocks) {
                System.out.println("Following path from block " + currentBlock.getId() +
                        " to block " + nextBlock.getId() + " in method " + name);
                executeRecursive(nextBlock, stateAfterBlock, finalStates,
                        new HashSet<>(visitedInPath));
            }
        }

        // Remove the block from visited path before going back up
        visitedInPath.remove(currentBlock);
    }

    private JVMState executeBlock(BasicBlock block, JVMState state) {
        if (block == null || state == null) {
            return null;
        }

        // System.out.println(state.toDetailedString());

        List<BytecodeInstruction> instructions = block.getInstructions();

        // Execute each instruction in the block
        for (BytecodeInstruction instruction : instructions) {
            instruction.addAnalysisResult("localVarsCount", state.getLocalVariablesSize());
            instruction.addAnalysisResult("stackSize", state.getStackSize());

            // Display the state before execution with sharing and cyclicity information
            System.out.println(
                    "  " + instruction + (instruction.getLabel() != null ? " [" + instruction.getLabel() + "]" : ""));

            // If the instruction is a CallInstruction, provide a reference to the program
            if (instruction instanceof CallInstruction) {
                ((CallInstruction) instruction).setProgram(program);
            }

            boolean result = instruction.execute(state); // Execute the instruction

            // If execution failed, stop block execution
            if (!result) {
                System.out.println("  Instruction execution failed");
                // System.out.println(state.toDetailedString());
                return null;
            }
        }

        return state;
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

    /**
     * Generates a DOT graph representation of the method's control flow graph.
     *
     * @param methodIndex The index of the method in a larger graph, or -1 to create
     *                    a standalone graph
     * @param labelKey    A key used to customize node labels in the graph
     * @return A string containing the DOT graph representation of the method's
     *         control flow
     */
    public String toDOT(int methodIndex, String labelKey) {
        StringBuilder dot = new StringBuilder();
        String cleanedMethodName = cleanString(name);

        if (methodIndex < 0) {
            // Create a digraph
            dot.append("digraph ").append(cleanedMethodName).append(" {\n");
            dot.append("  label=\"").append(name.replace("\"", "\\\"")).append("\";\n");
            dot.append("  node [shape=box];\n");
        } else {
            // Create a subgraph
            dot.append("  subgraph cluster_").append(methodIndex).append(" {\n");
            dot.append("    label=\"").append(name.replace("\"", "\\\"")).append("\";\n");
            dot.append("    color=black;\n");
            dot.append("    style=rounded;\n");
        }

        // Get the trimmed DOT representation of the CFG
        String cfgDot = cfg.toTrimedDOT(labelKey);
        String[] lines = cfgDot.split("\n");
        for (String line : lines) {
            String modifiedLine = line.replaceAll("block(\\d+)", cleanedMethodName + "_block$1");
            // Add appropriate indentation
            dot.append(methodIndex < 0 ? "" : "  ").append(modifiedLine).append("\n");
        }

        dot.append(methodIndex < 0 ? "}\n" : "  }\n");

        return dot.toString();
    }

    /**
     * Saves the method's control flow graph as a DOT file and generates a graph
     * visualization.
     *
     * @param labelKey  A key used to customize node labels in the graph
     * @param extension The file extension and output format for the generated graph
     *                  (e.g., "png", "svg")
     */
    public void saveToFile(String labelKey, String extension) {
        String dot = toDOT(-1, labelKey);
        String filename = cleanString(name);

        try {
            // Write DOT content to a file
            File dotFile = new File(filename + ".dot");
            File outputFile = new File(filename + "." + extension);

            try (FileWriter writer = new FileWriter(dotFile)) {
                writer.write(dot);
                System.out.println("Generated " + dotFile + " successfully.");
            } catch (IOException e) {
                System.err.println("Error generating dot file: " + e.getMessage());
            }

            // Generate output using dot command
            ProcessBuilder pb = new ProcessBuilder("dot", "-T" + extension, dotFile.getAbsolutePath(), "-o",
                    outputFile.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();

            System.out.println("Generated " + outputFile + " successfully.");

        } catch (IOException | InterruptedException e) {
            System.err.println("Error generating image. Make sure GraphViz is installed.\n" + e.getMessage());
        }
    }

    /**
     * Saves the method's control flow graph as a PNG file using the specified label
     * key.
     *
     * @param labelKey A key used to customize node labels in the graph
     */
    public void saveToFile(String labelKey) {
        saveToFile(labelKey, "png");
    }

    /**
     * Saves the method's control flow graph as a PNG file with default settings.
     * Uses an empty label key and PNG as the default output format.
     */
    public void saveToFile() {
        saveToFile("", "png");
    }

    /**
     * Sanitizes a string by replacing any characters that are not alphanumeric or
     * underscore
     * with an underscore.
     *
     * @param input The input string to be cleaned
     * @return A sanitized version of the input string with invalid characters
     *         replaced
     */
    private String cleanString(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "_");
    }

}
