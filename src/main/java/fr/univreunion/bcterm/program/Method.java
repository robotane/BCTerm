package fr.univreunion.bcterm.program;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.MemoryGraphGenerator;

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
    private String methodCallId;

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
        this(name, Constants.DEFAULT_METHOD_SIGNATURE, cfg, program);
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

    public Set<JVMState> execute(JVMState initialState, AbstractAnalysisRunner analysisRunner) {
        this.methodCallId = analysisRunner.generateMethodCallId(name);

        analysisRunner.setCurrentMethodCall(this.methodCallId);

        System.out.println("\nExecuting method " + name + " with " + analysisRunner.getName());
        System.out.println("---------------------------------");

        BasicBlock startBlock = cfg.getBlocks().get(0);
        Set<JVMState> finalStates = new HashSet<>();
        Set<BasicBlock> visitedInPath = new HashSet<>();

        executeRecursive(startBlock, initialState, finalStates, visitedInPath, analysisRunner);

        System.out.println("\nMethod " + name + " execution completed");
        System.out.println("Found " + finalStates.size() + " final states");
        System.out.println("---------------------------------");

        return finalStates;
    }

    private void executeRecursive(BasicBlock currentBlock, JVMState currentState,
            Set<JVMState> finalStates, Set<BasicBlock> visitedInPath, AbstractAnalysisRunner analysisRunner) {

        visitedInPath.add(currentBlock);

        System.out.println("Executing block " + currentBlock.getId() + " in method " + name);

        JVMState stateAfterBlock = executeBlock(currentBlock, new JVMState(currentState), analysisRunner);

        if (stateAfterBlock == null) {
            System.out.println("Execution of block " + currentBlock.getId() + " failed in method " + name);
            visitedInPath.remove(currentBlock);
            return;
        }

        Set<BasicBlock> nextBlocks = getBlockSuccessors(currentBlock);

        if (nextBlocks.isEmpty()) {
            System.out.println("End of path at block " + currentBlock.getId() + " in method " + name);
            finalStates.add(stateAfterBlock.deepCopy());
        } else {
            for (BasicBlock nextBlock : nextBlocks) {
                executeRecursive(nextBlock, stateAfterBlock, finalStates,
                        new HashSet<>(visitedInPath), analysisRunner);
            }
        }

        // Remove the block from visited path before going back up
        visitedInPath.remove(currentBlock);
    }

    private JVMState executeBlock(BasicBlock block, JVMState state, AbstractAnalysisRunner analysisRunner) {
        if (block == null || state == null) {
            return null;
        }

        List<BytecodeInstruction> instructions = block.getInstructions();
        if (instructions == null) {
            return null;
        }

        boolean noLoop = true;

        for (BytecodeInstruction instruction : instructions) {
            int localVarsCount = state.getLocalVariablesSize();
            int stackSize = state.getStackSize();
            Object analyzeResult = analysisRunner.getCurrentAnalysisResults();
            JVMState oldState = state.deepCopy();

            String instructionLabel = instruction.getLabel();

            if (instruction instanceof CallInstruction) {
                instruction.setAnalysisRunner(analysisRunner);
            }

            boolean result = instruction.execute(state);

            if (instruction instanceof CallInstruction) {
                analysisRunner.setCurrentMethodCall(methodCallId);
                instruction.setAnalysisRunner(analysisRunner);
            }

            if (result) {
                noLoop = analysisRunner.analyze(instruction);
                if (!noLoop) {
                    System.out.println("  Analysis failed");
                } else {
                    System.out.println("  " + instruction + " " + instructionLabel);
                }
            } else {
                System.out.println("  Instruction execution failed");
                System.out.println("  " + instruction + " " + instructionLabel);
            }

            instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_LOCAL_VARS_COUNT, localVarsCount);
            instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_STACK_SIZE, stackSize);
            analysisRunner.addAnalysisResultToInstruction(instruction, analyzeResult);

            if (Constants.ENABLE_FILE_GENERATION) {
                String analysisName = analysisRunner.getName().replaceAll("\\s+", "_").toLowerCase();
                String programName = program.getName();
                String memoryGraphPath = programName + File.separator + analysisName + File.separator
                        + Constants.MEMORY_GRAPH_PREFIX + this.methodCallId;

                MemoryGraphGenerator.generateMemoryGraph(oldState, memoryGraphPath, instruction,
                        Constants.MEMORY_GRAPH_SHOW_OBJECTS, Constants.MEMORY_GRAPH_SHOW_PRIMITIVES);
            }

            if (Objects.equals(name, "size")) {
                System.out.println();
            }

            if (!result || !noLoop) {
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
            dot.append("digraph ").append(cleanedMethodName).append(" {\n");
            dot.append("  label=\"").append(name.replace("\"", "\\\"")).append("\";\n");
            dot.append("  node [shape=box];\n");
        } else {
            dot.append("  subgraph cluster_").append(methodIndex).append(" {\n");
            dot.append("    label=\"").append(name.replace("\"", "\\\"")).append("\";\n");
            dot.append("    color=black;\n");
            dot.append("    style=rounded;\n");
        }

        String cfgDot = cfg.toTrimmedDOT(labelKey);
        String[] lines = cfgDot.split("\n");
        for (String line : lines) {
            String modifiedLine = line.replaceAll("block(\\d+)", cleanedMethodName + "_block$1");
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
        String programName = program.getName();
        String programDir = Constants.GENERATED_DIR + File.separator + programName;

        try {
            File generatedDir = new File(programDir);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            File dotFile = new File(generatedDir, filename + Constants.DOT_FILE_EXTENSION);
            File outputFile = new File(generatedDir, filename + "." + extension);

            try (FileWriter writer = new FileWriter(dotFile)) {
                writer.write(dot);
                System.out.println("Generated " + dotFile + " successfully.");
            } catch (IOException e) {
                System.err.println("Error generating dot file: " + e.getMessage());
            }

            ProcessBuilder pb = new ProcessBuilder(
                    Constants.DOT_COMMAND,
                    Constants.DOT_TYPE_FLAG_PREFIX + extension,
                    dotFile.getAbsolutePath(),
                    Constants.DOT_OUTPUT_FLAG,
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
        saveToFile(labelKey, Constants.DEFAULT_GRAPH_EXTENSION);
    }

    /**
     * Saves the method's control flow graph as a PNG file with default settings.
     * Uses an empty label key and PNG as the default output format.
     */
    public void saveToFile() {
        saveToFile("", Constants.DEFAULT_GRAPH_EXTENSION);
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

    public BasicBlock getStartBlock() {
        return this.cfg.getBlocks().get(0);
    }
}
