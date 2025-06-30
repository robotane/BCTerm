package fr.univreunion.bcterm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisEngine;
import fr.univreunion.bcterm.analysis.aliasing.AliasPair;
import fr.univreunion.bcterm.analysis.aliasing.AliasingAnalysisEngine;
import fr.univreunion.bcterm.analysis.aliasing.AliasingState;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicVariable;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityAnalysisEngine;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityState;
import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisEngine;
import fr.univreunion.bcterm.analysis.sharing.SharingPair;
import fr.univreunion.bcterm.analysis.sharing.SharingState;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.state.IntegerValue;
import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * Class responsible for generating memory graphs to visualize
 * the JVM state, sharing relationships, and aliases.
 */
public class MemoryGraphGenerator {
    private static final java.util.logging.Logger logger = Logger.getLogger(MemoryGraphGenerator.class);

    /**
     * Generates a memory graph visualization for a given JVM state at a specific
     * bytecode instruction.
     *
     * @param state          The current JVM state to visualize
     * @param outputPath     Path where the DOT graph file will be written
     * @param instruction    The bytecode instruction being processed
     * @param showAllObjects Flag to determine whether to show all objects or only
     *                       referenced ones
     * @param showAnalysis   Flag to include additional analysis results in the
     *                       graph
     * @return true if the memory graph was successfully generated, false otherwise
     */
    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction,
            AbstractAnalysisEngine analysisEngine, boolean showAllObjects, boolean showAnalysis) {
        try {
            Map<String, Set<Long>> pointsToGraph = buildPointsToGraph(state);

            StringBuilder dotContent = new StringBuilder();

            // Create generated directory if it doesn't exist
            File generatedDir = new File(Constants.GENERATED_DIR);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            String dotFilePath = new File(generatedDir, outputPath + ".dot").getPath();
            long timestamp = System.nanoTime();

            initializeDotFile(dotContent, dotFilePath);

            String anchorNodeId = createAnchorNode(dotContent, timestamp);

            String subgraphId = "cluster_" + timestamp;
            dotContent.append(" subgraph ").append(subgraphId).append(" {\n");
            dotContent.append(" ").append(anchorNodeId).append(" [style=invis];\n");
            dotContent.append(" label=\"").append(instruction.toString()).append("\";\n");

            addLocalVariables(dotContent, state, timestamp);

            addStack(dotContent, state, timestamp);

            Set<Long> addedObjectAddresses = new HashSet<>();

            addMemoryObjects(dotContent, state, timestamp, showAllObjects, pointsToGraph, addedObjectAddresses);

            addEdgesFromVariablesToObjects(dotContent, state, timestamp, addedObjectAddresses);

            addEdgesBetweenObjects(dotContent, state, timestamp, addedObjectAddresses);

            if (showAnalysis) {
                addAnalysisResults(instruction, analysisEngine, dotContent, timestamp, true);
            }

            dotContent.append(" }\n");

            finalizeDotFile(dotContent, dotFilePath);

            try (FileWriter writer = new FileWriter(dotFilePath)) {
                writer.write(dotContent.toString());
            }

            return true;

        } catch (IOException | NumberFormatException e) {
            logger.severe(() -> "Error generating memory graph: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Constructs a memory graph by mapping variables to their memory addresses.
     * 
     * Traverses local variables and stack elements, identifying location values
     * and building a graph that tracks the memory addresses each variable can
     * reach.
     * 
     * Example:
     * For a JVM state with:
     * - Local variable l0 pointing to address 1, which has a field pointing to
     * address 2
     * - Stack element s0 pointing to address 3
     * The resulting graph would be:
     * {
     * "l0" -> {1, 2},
     * "s0" -> {3}
     * }
     * 
     * @param state The JVM state containing local variables and stack elements to
     *              analyze
     * @return A map of variable names to sets of memory addresses they reference
     */
    private static Map<String, Set<Long>> buildPointsToGraph(JVMState state) {
        Map<String, Set<Long>> pointsToGraph = new HashMap<>();

        // Process local variables
        for (int i = 0; i < state.getLocalVariablesSize(); i++) {
            Value value = state.getLocalVariable(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                String varName = "l" + i;
                long address = locationValue.getAddress();
                pointsToGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(address);

                // Add objects accessible through fields
                addReachableObjects(varName, address, pointsToGraph, state, new HashSet<>());
            }
        }

        // Process stack elements
        for (int i = 0; i < state.getStackSize(); i++) {
            Value value = state.getStackElement(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                String varName = "s" + i;
                long address = locationValue.getAddress();
                pointsToGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(address);

                // Add objects accessible through fields
                addReachableObjects(varName, address, pointsToGraph, state, new HashSet<>());
            }
        }

        return pointsToGraph;
    }

    /**
     * Recursively explores and tracks reachable memory objects from a given
     * address.
     *
     * @param varName     The name of the variable being analyzed
     * @param address     The starting memory address to explore
     * @param memoryGraph A mapping of variable names to their reachable object
     *                    addresses
     * @param state       The current JVM state containing object field
     *                    information
     * @param visited     A set of already visited memory addresses to prevent
     *                    infinite recursion
     */
    private static void addReachableObjects(String varName, long address,
            Map<String, Set<Long>> memoryGraph, JVMState state,
            Set<Long> visited) {
        if (visited.contains(address)) {
            return;
        }
        visited.add(address);

        Map<String, Value> objectFields = state.getObjectFields(address);
        if (objectFields != null) {
            for (String fieldName : objectFields.keySet()) {
                Value fieldValue = objectFields.get(fieldName);
                if (fieldValue != null && fieldValue instanceof LocationValue) {
                    LocationValue locationValue = (LocationValue) fieldValue;
                    long fieldAddress = locationValue.getAddress();
                    memoryGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(fieldAddress);

                    addReachableObjects(varName, fieldAddress, memoryGraph, state, visited);
                }
            }
        }
    }

    private static void initializeDotFile(StringBuilder dotContent, String dotFilePath) throws IOException {
        File dotFile = new File(dotFilePath);

        // Create parent directories if they don't exist
        if (!dotFile.getParentFile().exists()) {
            dotFile.getParentFile().mkdirs();
        }

        if (!dotFile.exists()) {
            dotContent.append("digraph MemoryGraph {\n");
            dotContent.append(" node [shape=box, style=filled, fillcolor=lightblue];\n");
            dotContent.append(" rankdir=LR;\n\n");

            dotContent.append(" // Invisible anchor subgraph\n");
            dotContent.append(" subgraph cluster_anchor {\n");
            dotContent.append(" style=invis;\n");
            dotContent.append(" anchor [style=invis, shape=point, width=0, height=0];\n");
            dotContent.append(" }\n\n");

            dotContent.append(" // Starting point for subgraph ordering\n");
            dotContent.append(" anchor_start [style=invis, shape=point];\n\n");
        } else {
            // Read existing content
            try (BufferedReader reader = new BufferedReader(new FileReader(dotFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("}")) {
                        break;
                    }
                    dotContent.append(line).append("\n");
                }
            }
        }
    }

    private static String createAnchorNode(StringBuilder dotContent, long timestamp) {
        String anchorNodeId = "anchor_" + timestamp;
        dotContent.append(" ").append(anchorNodeId).append(" [style=invis, shape=point];\n");

        // Add ordering constraint with previous subgraph
        dotContent.append(" anchor_start -> ").append(anchorNodeId).append(" [style=invis];\n");
        dotContent.append(" anchor_start = ").append(anchorNodeId).append(";\n\n");

        return anchorNodeId;
    }

    private static void addLocalVariables(StringBuilder dotContent, JVMState state, long timestamp) {
        dotContent.append(" subgraph cluster_locals_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Local Variables\";\n");
        dotContent.append(" style=filled;\n");
        dotContent.append(" color=lightgrey;\n");
        dotContent.append(" node [style=filled, fillcolor=lightgreen];\n");

        boolean hasVariables = false;
        // Add all local variables
        for (int i = 0; i < state.getLocalVariablesSize(); i++) {
            Value value = state.getLocalVariable(i);
            if (value != null) {
                hasVariables = true;
                String varId = "l" + i + "_" + timestamp;
                String varLabel = formatVariableLabel("l" + i, value);
                dotContent.append(" \"").append(varId).append("\" [label=\"").append(varLabel).append("\"];\n");
            }
        }

        // Add a placeholder node if there are no variables
        if (!hasVariables) {
            String emptyId = "empty_locals_" + timestamp;
            dotContent.append(" \"").append(emptyId)
                    .append("\" [label=\"No local variables\", style=dashed, fillcolor=white];\n");
        }

        dotContent.append(" }\n\n");
    }

    private static void addStack(StringBuilder dotContent, JVMState state, long timestamp) {
        dotContent.append(" subgraph cluster_stack_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Stack\";\n");
        dotContent.append(" style=filled;\n");
        dotContent.append(" color=lightgrey;\n");
        dotContent.append(" node [style=filled, fillcolor=lightpink];\n");

        boolean hasStackElements = false;
        // Add all stack elements
        for (int i = 0; i < state.getStackSize(); i++) {
            Value value = state.getStackElement(i);
            if (value != null) {
                hasStackElements = true;
                String stackId = "s" + i + "_" + timestamp;
                String stackLabel = formatVariableLabel("s" + i, value);
                dotContent.append(" \"").append(stackId).append("\" [label=\"").append(stackLabel).append("\"];\n");
            }
        }

        // Add a placeholder node if the stack is empty
        if (!hasStackElements) {
            String emptyId = "empty_stack_" + timestamp;
            dotContent.append(" \"").append(emptyId)
                    .append("\" [label=\"Empty stack\", style=dashed, fillcolor=white];\n");
        }

        dotContent.append(" }\n\n");
    }

    private static String formatVariableLabel(String prefix, Value value) {
        if (value == null || value == Value.NULL) {
            return prefix + " = null";
        } else if (value instanceof IntegerValue) {
            return prefix + " = " + value.toString();
        } else if (value instanceof LocationValue) {
            return prefix + " = @" + ((LocationValue) value).getAddress();
        }
        return prefix + " = " + value.toString();
    }

    private static void addMemoryObjects(StringBuilder dotContent, JVMState state, long timestamp,
            boolean showAllObjects, Map<String, Set<Long>> pointsToGraph, Set<Long> addedObjectAddresses) {
        dotContent.append(" subgraph cluster_memory_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Memory\";\n");
        dotContent.append(" style=filled;\n");
        dotContent.append(" color=lightgrey;\n");
        dotContent.append(" node [style=filled, fillcolor=lightyellow];\n");

        boolean hasObjects;
        if (showAllObjects) {
            hasObjects = addAllMemoryObjects(dotContent, state, timestamp, addedObjectAddresses);
        } else {
            hasObjects = addAccessibleMemoryObjects(dotContent, state, timestamp, pointsToGraph, addedObjectAddresses);
        }

        // Add a placeholder node if there are no objects in memory
        if (!hasObjects) {
            String emptyId = "empty_memory_" + timestamp;
            dotContent.append(" \"").append(emptyId)
                    .append("\" [label=\"Empty memory\", style=dashed, fillcolor=white];\n");
        }

        dotContent.append(" }\n\n");
    }

    private static boolean addAllMemoryObjects(StringBuilder dotContent, JVMState state, long timestamp,
            Set<Long> addedObjectAddresses) {
        Map<LocationValue, JVMObject> memory = state.getMemory();
        if (memory.isEmpty()) {
            return false;
        }

        for (Map.Entry<LocationValue, JVMObject> entry : memory.entrySet()) {
            LocationValue location = entry.getKey();
            JVMObject obj = entry.getValue();
            long address = location.getAddress();
            addedObjectAddresses.add(address);

            String objId = "obj" + address + "_" + timestamp;
            String objLabel = formatObjectLabel(obj, address);

            dotContent.append(" \"").append(objId).append("\" [label=\"").append(objLabel).append("\"];\n");
        }
        return true;
    }

    private static boolean addAccessibleMemoryObjects(StringBuilder dotContent, JVMState state, long timestamp,
            Map<String, Set<Long>> pointsToGraph, Set<Long> addedObjectAddresses) {
        Set<Long> allAddresses = new HashSet<>();
        for (Set<Long> addresses : pointsToGraph.values()) {
            allAddresses.addAll(addresses);
        }

        if (allAddresses.isEmpty()) {
            return false;
        }

        boolean addedAny = false;
        for (Long address : allAddresses) {
            JVMObject obj = state.getObject(new LocationValue(address));
            if (obj != null) {
                addedObjectAddresses.add(address);
                addedAny = true;

                String objId = "obj" + address + "_" + timestamp;
                String objLabel = formatObjectLabel(obj, address);

                dotContent.append(" \"").append(objId).append("\" [label=\"").append(objLabel).append("\"];\n");
            }
        }
        return addedAny;
    }

    private static String formatObjectLabel(JVMObject obj, long address) {
        StringBuilder label = new StringBuilder("Object@").append(address).append("\\n").append(obj.getClassTag());

        // Add field values to the label
        Map<String, Value> fields = obj.getFields();
        if (fields != null && !fields.isEmpty()) {
            label.append("\\n---\\n");
            for (Map.Entry<String, Value> field : fields.entrySet()) {
                Value fieldValue = field.getValue();
                String fieldValueStr = formatFieldValue(fieldValue);
                label.append(field.getKey()).append(" = ").append(fieldValueStr).append("\\n");
            }
        }

        return label.toString();
    }

    private static String formatFieldValue(Value fieldValue) {
        if (fieldValue == null || fieldValue == Value.NULL) {
            return "null";
        } else if (fieldValue instanceof IntegerValue) {
            return fieldValue.getValue().toString();
        } else if (fieldValue instanceof LocationValue) {
            return "@" + ((LocationValue) fieldValue).getAddress();
        } else {
            return fieldValue.toString();
        }
    }

    private static void addEdgesFromVariablesToObjects(StringBuilder dotContent, JVMState state, long timestamp,
            Set<Long> addedObjectAddresses) {
        // For local variables
        for (int i = 0; i < state.getLocalVariablesSize(); i++) {
            Value value = state.getLocalVariable(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                long address = locationValue.getAddress();

                // Ensure the object is added to the graph
                addObject(dotContent, state, timestamp, addedObjectAddresses, address);

                dotContent.append(" \"l").append(i).append("_").append(timestamp)
                        .append("\" -> \"obj").append(address).append("_").append(timestamp).append("\";\n");
            }
        }

        // For stack elements
        for (int i = 0; i < state.getStackSize(); i++) {
            Value value = state.getStackElement(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                long address = locationValue.getAddress();

                addObject(dotContent, state, timestamp, addedObjectAddresses, address);

                dotContent.append(" \"s").append(i).append("_").append(timestamp)
                        .append("\" -> \"obj").append(address).append("_").append(timestamp).append("\";\n");
            }
        }
    }

    private static void addObject(StringBuilder dotContent, JVMState state, long timestamp,
            Set<Long> addedObjectAddresses, long address) {
        if (!addedObjectAddresses.contains(address)) {
            JVMObject obj = state.getObject(new LocationValue(address));
            if (obj != null) {
                addedObjectAddresses.add(address);

                String objLabel = formatObjectLabel(obj, address);

                dotContent.append(" \"obj").append(address).append("_").append(timestamp)
                        .append("\" [label=\"").append(objLabel).append("\", fillcolor=lightyellow];\n");
            }
        }
    }

    private static void addEdgesBetweenObjects(StringBuilder dotContent, JVMState state, long timestamp,
            Set<Long> addedObjectAddresses) {
        for (Long address : new HashSet<>(addedObjectAddresses)) {
            JVMObject obj = state.getObject(new LocationValue(address));
            if (obj != null) {
                Map<String, Value> fields = obj.getFields();
                if (fields != null) {
                    for (Map.Entry<String, Value> entry : fields.entrySet()) {
                        Value fieldValue = entry.getValue();
                        if (fieldValue != null && fieldValue instanceof LocationValue) {
                            LocationValue locationValue = (LocationValue) fieldValue;
                            long fieldAddress = locationValue.getAddress();

                            addObject(dotContent, state, timestamp, addedObjectAddresses, fieldAddress);

                            dotContent.append(" \"obj").append(address).append("_").append(timestamp)
                                    .append("\" -> \"obj").append(fieldAddress).append("_").append(timestamp)
                                    .append("\" [label=\"").append(entry.getKey()).append("\"];\n");
                        }
                    }
                }
            }
        }
    }

    private static void addAnalysisResults(BytecodeInstruction instruction, AbstractAnalysisEngine analysisEngine,
            StringBuilder dotContent, long timestamp, boolean finalResult) {

        // Get analysis results from the analysis runner's current instruction state
        Object analysisState = null;
        if (analysisEngine != null) {
            Map<BytecodeInstruction, ?> currentInstructionState = analysisEngine.getCurrentInstructionState();
            if (currentInstructionState != null) {
                analysisState = currentInstructionState.get(instruction);
            }
        }

        String sharingNodeId = addSharingPairs(instruction, analysisEngine, dotContent, timestamp, finalResult);

        String aliasNodeId = addAliasPairs(instruction, analysisEngine, dotContent, timestamp, finalResult);

        String cyclicNodeId = addCyclicVariables(instruction, analysisEngine, dotContent, timestamp, finalResult);

        if (sharingNodeId != null && aliasNodeId != null) {
            dotContent.append(" // Invisible edge for horizontal alignment\n");
            dotContent.append(" \"").append(sharingNodeId).append("\" -> \"")
                    .append(aliasNodeId).append("\" [style=invis, weight=10];\n");
        }

        if (aliasNodeId != null && cyclicNodeId != null) {
            dotContent.append(" // Invisible edge for horizontal alignment\n");
            dotContent.append(" \"").append(aliasNodeId).append("\" -> \"")
                    .append(cyclicNodeId).append("\" [style=invis, weight=10];\n");
        }
    }

    private static String addSharingPairs(BytecodeInstruction instruction, AbstractAnalysisEngine analysisEngine,
            StringBuilder dotContent, long timestamp, boolean finalResult) {

        Set<SharingPair> sharingPairs = null;

        // Try to get from analysis runner first
        if (analysisEngine instanceof SharingAnalysisEngine) {
            Map<BytecodeInstruction, ?> currentInstructionState = analysisEngine.getCurrentInstructionState();
            if (currentInstructionState != null) {
                Object state = currentInstructionState.get(instruction);
                if (state instanceof SharingState) {
                    SharingState sharingState = (SharingState) state;
                    sharingPairs = sharingState.getSharingPairs();
                }
            }
        }

        // Fallback to instruction analysis results
        if (sharingPairs == null) {
            @SuppressWarnings("unchecked")
            Set<SharingPair> instructionSharingPairs = (Set<SharingPair>) instruction
                    .getAnalysisResult(Constants.ANALYSIS_RESULT_SHARING_PAIRS);
            sharingPairs = instructionSharingPairs;
        }

        dotContent.append("\n // Sharing pairs\n");
        dotContent.append(" subgraph cluster_sharing_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Sharing Pairs\";\n");
        dotContent.append(" node [shape=ellipse, style=filled, fillcolor=lightyellow];\n");

        String sharingNodeId = null;
        if (sharingPairs != null && !sharingPairs.isEmpty()) {
            int pairId = 0;
            for (SharingPair pair : sharingPairs) {
                String pairNodeId = "pair" + pairId++ + "_" + timestamp;
                sharingNodeId = pairNodeId;
                dotContent.append(" \"").append(pairNodeId).append("\" [label=\"")
                        .append(pair.getVar1()).append(" ↔ ").append(pair.getVar2()).append("\"];\n");
            }
        } else {
            sharingNodeId = "empty_sharing_" + timestamp;
            dotContent.append(" \"").append(sharingNodeId)
                    .append("\" [label=\"No sharing\", style=dashed, fillcolor=white];\n");
        }
        dotContent.append(" }\n");

        return sharingNodeId;
    }

    private static String addAliasPairs(BytecodeInstruction instruction, AbstractAnalysisEngine analysisEngine,
            StringBuilder dotContent, long timestamp, boolean finalResult) {

        Set<AliasPair> aliasPairs = null;

        // Try to get from analysis runner first
        if (analysisEngine instanceof AliasingAnalysisEngine) {
            Map<BytecodeInstruction, ?> currentInstructionState = analysisEngine.getCurrentInstructionState();
            if (currentInstructionState != null) {
                Object state = currentInstructionState.get(instruction);
                if (state instanceof AliasingState) {
                    AliasingState aliasingState = (AliasingState) state;
                    aliasPairs = aliasingState.getAliasPairs();
                }
            }
        }

        // Fallback to instruction analysis results
        if (aliasPairs == null) {
            @SuppressWarnings("unchecked")
            Set<AliasPair> instructionAliasPairs = (Set<AliasPair>) instruction
                    .getAnalysisResult(Constants.ANALYSIS_RESULT_ALIAS_PAIRS);
            aliasPairs = instructionAliasPairs;
        }

        dotContent.append("\n    // Alias pairs\n");
        dotContent.append("    subgraph cluster_aliases_").append(timestamp).append(" {\n");
        dotContent.append("      label=\"Definite Aliases\";\n");
        dotContent.append("      node [shape=ellipse, style=filled, fillcolor=lightcyan];\n");

        String aliasNodeId = null;
        if (aliasPairs != null && !aliasPairs.isEmpty()) {
            int pairId = 0;
            for (AliasPair pair : aliasPairs) {
                String pairNodeId = "alias" + pairId + "_" + timestamp;
                aliasNodeId = pairNodeId;
                dotContent.append("      \"").append(pairNodeId).append("\" [label=\"")
                        .append(pair.getVar1()).append(" = ").append(pair.getVar2()).append("\"];\n");
                pairId++;
            }
        } else {
            aliasNodeId = "empty_alias_" + timestamp;
            dotContent.append("      \"").append(aliasNodeId)
                    .append("\" [label=\"No aliases\", style=dashed, fillcolor=white];\n");
        }
        dotContent.append("    }\n");

        return aliasNodeId;
    }

    private static String addCyclicVariables(BytecodeInstruction instruction, AbstractAnalysisEngine analysisEngine,
            StringBuilder dotContent, long timestamp, boolean finalResult) {

        Set<CyclicVariable> cyclicVars = null;

        // Try to get from analysis runner first
        if (analysisEngine instanceof CyclicityAnalysisEngine) {
            Map<BytecodeInstruction, ?> currentInstructionState = analysisEngine.getCurrentInstructionState();
            if (currentInstructionState != null) {
                Object state = currentInstructionState.get(instruction);
                if (state instanceof CyclicityState) {
                    CyclicityState cyclicityState = (CyclicityState) state;
                    cyclicVars = cyclicityState.getPossiblyCyclicVariables();
                }
            }
        }

        // Fallback to instruction analysis results
        if (cyclicVars == null) {
            @SuppressWarnings("unchecked")
            Set<CyclicVariable> instructionCyclicVars = (Set<CyclicVariable>) instruction
                    .getAnalysisResult(Constants.ANALYSIS_RESULT_CYCLIC_VARS);
            cyclicVars = instructionCyclicVars;
        }

        dotContent.append("\n // Cyclic variables\n");
        dotContent.append(" subgraph cluster_cyclic_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Cyclic Variables\";\n");
        dotContent.append(" node [shape=ellipse];\n");

        String cyclicNodeId = null;
        if (cyclicVars != null && !cyclicVars.isEmpty()) {
            int varId = 0;
            for (CyclicVariable var : cyclicVars) {
                String varNodeId = "cyclic" + varId++ + "_" + timestamp;
                cyclicNodeId = varNodeId;

                // Determine color based on variable name prefix
                String varName = var.getVariableName();
                String fillColor = "white"; // Default color

                if (varName.startsWith("l")) {
                    fillColor = "lightgreen"; // Local variable color
                } else if (varName.startsWith("s")) {
                    fillColor = "lightpink"; // Stack variable color
                }

                dotContent.append(" \"").append(varNodeId).append("\" [label=\"")
                        .append(varName).append("\", style=filled, fillcolor=").append(fillColor).append("];\n");
            }
        } else {
            cyclicNodeId = "empty_cyclic_" + timestamp;
            dotContent.append(" \"").append(cyclicNodeId)
                    .append("\" [label=\"No cycle\", style=dashed, fillcolor=white];\n");
        }
        dotContent.append(" }\n");

        return cyclicNodeId;
    }

    private static void finalizeDotFile(StringBuilder dotContent, String dotFilePath) throws IOException {
        File dotFile = new File(dotFilePath);
        if (!dotFile.exists()) {
            dotContent.append("}\n");
        } else {
            dotContent.append("}\n");
        }
    }

    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction,
            boolean showAllObjects) {
        return generateMemoryGraph(state, outputPath, instruction, null, showAllObjects, false);
    }

    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction) {
        return generateMemoryGraph(state, outputPath, instruction, null, true, false);
    }

    public static boolean generateFinalMemoryGraph(JVMState state, String outputPath,
            boolean showAllObjects, boolean showAnalysis) {
        try {
            Map<String, Set<Long>> pointsToGraph = buildPointsToGraph(state);

            StringBuilder dotContent = new StringBuilder();

            File generatedDir = new File(Constants.GENERATED_DIR);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            String dotFilePath = new File(generatedDir, outputPath + ".dot").getPath();
            long timestamp = System.currentTimeMillis();

            initializeDotFile(dotContent, dotFilePath);

            String anchorNodeId = createAnchorNode(dotContent, timestamp);

            String subgraphId = "cluster_final_" + timestamp;
            dotContent.append(" subgraph ").append(subgraphId).append(" {\n");
            dotContent.append(" ").append(anchorNodeId).append(" [style=invis];\n");
            dotContent.append(" label=\"Final State\";\n");
            dotContent.append(" color=red;\n");
            dotContent.append(" penwidth=3;\n");

            addLocalVariables(dotContent, state, timestamp);

            addStack(dotContent, state, timestamp);

            Set<Long> addedObjectAddresses = new HashSet<>();

            addMemoryObjects(dotContent, state, timestamp, showAllObjects, pointsToGraph, addedObjectAddresses);

            addEdgesFromVariablesToObjects(dotContent, state, timestamp, addedObjectAddresses);

            addEdgesBetweenObjects(dotContent, state, timestamp, addedObjectAddresses);

            if (showAnalysis) {
                addAnalysisResults(null, null, dotContent, timestamp, true);
            }

            dotContent.append(" }\n");

            finalizeDotFile(dotContent, dotFilePath);

            try (FileWriter writer = new FileWriter(dotFilePath)) {
                writer.write(dotContent.toString());
            }

            return true;

        } catch (IOException | NumberFormatException e) {
            logger.severe(() -> "Error generating final memory graph: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates images from all DOT files in a directory and its subdirectories.
     * 
     * @param baseDirectory The base directory to search for DOT files
     * @return true if all conversions were successful, false otherwise
     */
    public static boolean generateImagesFromDotFiles(String baseDirectory) {
        return generateImagesFromDotFiles(baseDirectory, Constants.DEFAULT_GRAPH_EXTENSION);
    }

    /**
     * Generates images from all DOT files in a directory and its subdirectories.
     * 
     * @param baseDirectory The base directory to search for DOT files
     * @param extension     The output image format (png, svg, pdf, etc.)
     * @return true if all conversions were successful, false otherwise
     */
    public static boolean generateImagesFromDotFiles(String baseDirectory, String extension) {
        try {
            File baseDir = new File(baseDirectory);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                logger.severe(() -> "Base directory does not exist: " + baseDirectory);
                return false;
            }

            return processDirectoryRecursively(baseDir, extension);
        } catch (Exception e) {
            logger.severe(() -> "Error generating images from DOT files: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recursively processes a directory to find and convert DOT files.
     * 
     * @param directory The directory to process
     * @param extension The output image format
     * @return true if all conversions in this directory were successful
     */
    private static boolean processDirectoryRecursively(File directory, String extension) {
        boolean allSuccessful = true;

        File[] files = directory.listFiles();
        if (files == null) {
            return true;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively process subdirectories
                if (!processDirectoryRecursively(file, extension)) {
                    allSuccessful = false;
                }
            } else if (file.getName().endsWith(Constants.DOT_FILE_EXTENSION)) {
                // Convert DOT file to image
                if (!convertDotToImage(file, extension)) {
                    allSuccessful = false;
                }
            }
        }

        return allSuccessful;
    }

    /**
     * Converts a single DOT file to an image.
     * 
     * @param dotFile   The DOT file to convert
     * @param extension The output image format
     * @return true if conversion was successful, false otherwise
     */
    private static boolean convertDotToImage(File dotFile, String extension) {
        try {
            String baseName = dotFile.getName().substring(0,
                    dotFile.getName().length() - Constants.DOT_FILE_EXTENSION.length());
            File outputFile = new File(dotFile.getParent(), baseName + "." + extension);

            ProcessBuilder pb = new ProcessBuilder(
                    Constants.DOT_COMMAND,
                    Constants.DOT_TYPE_FLAG_PREFIX + extension,
                    dotFile.getAbsolutePath(),
                    Constants.DOT_OUTPUT_FLAG,
                    outputFile.getAbsolutePath());

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info(() -> "Generated " + outputFile.getName() + " successfully in " + dotFile.getParent());
                return true;
            } else {
                logger.severe(
                        () -> "Error generating image from " + dotFile.getName() + " (exit code: " + exitCode + ")");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.severe(() -> "Error generating image from " + dotFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
}