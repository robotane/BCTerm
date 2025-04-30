package fr.univreunion.bcterm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.sharing.SharingPair;
import fr.univreunion.bcterm.analysis.sharing.SharingPairAnalyzer;
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
            boolean showAllObjects, boolean showAnalysis) {
        try {
            Map<String, Set<Long>> pointsToGraph = SharingPairAnalyzer.buildPointsToGraph(state);

            StringBuilder dotContent = new StringBuilder();

            // Create generated directory if it doesn't exist
            File generatedDir = new File(Constants.GENERATED_DIR);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            String dotFilePath = new File(generatedDir, outputPath + ".dot").getPath();
            long timestamp = System.currentTimeMillis();

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
                addAnalysisResults(dotContent, timestamp);
            }

            dotContent.append(" }\n");

            finalizeDotFile(dotContent, dotFilePath);

            try (FileWriter writer = new FileWriter(dotFilePath)) {
                writer.write(dotContent.toString());
            }

            return true;

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error generating memory graph: " + e.getMessage());
            e.printStackTrace();
            return false;
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

        boolean hasObjects = false;
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
                ensureObjectIsAdded(dotContent, state, timestamp, addedObjectAddresses, address);

                // Add the edge
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

                // Ensure the object is added to the graph
                ensureObjectIsAdded(dotContent, state, timestamp, addedObjectAddresses, address);

                // Add the edge
                dotContent.append(" \"s").append(i).append("_").append(timestamp)
                        .append("\" -> \"obj").append(address).append("_").append(timestamp).append("\";\n");
            }
        }
    }

    private static void ensureObjectIsAdded(StringBuilder dotContent, JVMState state, long timestamp,
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

                            // Ensure the referenced object is added to the graph
                            ensureObjectIsAdded(dotContent, state, timestamp, addedObjectAddresses, fieldAddress);

                            // Add the edge with the field name as label
                            dotContent.append(" \"obj").append(address).append("_").append(timestamp)
                                    .append("\" -> \"obj").append(fieldAddress).append("_").append(timestamp)
                                    .append("\" [label=\"").append(entry.getKey()).append("\"];\n");
                        }
                    }
                }
            }
        }
    }

    private static void addAnalysisResults(StringBuilder dotContent, long timestamp) {
        // Add sharing pairs
        String sharingNodeId = addSharingPairs(dotContent, timestamp);

        // For now, we'll just use a placeholder for alias pairs
        String aliasNodeId = addPlaceholderAliasPairs(dotContent, timestamp);

        // Add an invisible arrow between a sharing node and an alias node for layout
        if (sharingNodeId != null && aliasNodeId != null) {
            dotContent.append(" // Invisible edge for horizontal alignment\n");
            dotContent.append(" \"").append(sharingNodeId).append("\" -> \"")
                    .append(aliasNodeId).append("\" [style=invis, weight=10];\n");
        }
    }

    private static String addSharingPairs(StringBuilder dotContent, long timestamp) {
        Set<SharingPair> sharingPairs = SharingPairAnalyzer.getCurrentSharingPairs();

        // Always create the subgraph, even if there are no sharing pairs
        dotContent.append("\n // Sharing pairs\n");
        dotContent.append(" subgraph cluster_sharing_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Sharing Pairs\";\n");
        dotContent.append(" node [shape=ellipse, style=filled, fillcolor=lightyellow];\n");

        String sharingNodeId = null;
        if (!sharingPairs.isEmpty()) {
            int pairId = 0;
            for (SharingPair pair : sharingPairs) {
                String pairNodeId = "pair" + pairId++ + "_" + timestamp;
                sharingNodeId = pairNodeId;
                dotContent.append(" \"").append(pairNodeId).append("\" [label=\"")
                        .append(pair.getVar1()).append(" ↔ ").append(pair.getVar2()).append("\"];\n");
            }
        } else {
            // Add an invisible node so the subgraph is not empty
            sharingNodeId = "empty_sharing_" + timestamp;
            dotContent.append(" \"").append(sharingNodeId)
                    .append("\" [label=\"No sharing\", style=dashed, fillcolor=white];\n");
        }
        dotContent.append(" }\n");

        return sharingNodeId;
    }

    private static String addPlaceholderAliasPairs(StringBuilder dotContent, long timestamp) {
        // Create a placeholder for alias pairs
        dotContent.append("\n // Alias pairs (placeholder)\n");
        dotContent.append(" subgraph cluster_aliases_").append(timestamp).append(" {\n");
        dotContent.append(" label=\"Definite Aliases\";\n");
        dotContent.append(" node [shape=ellipse, style=filled, fillcolor=lightcyan];\n");

        String aliasNodeId = "empty_alias_" + timestamp;
        dotContent.append(" \"").append(aliasNodeId)
                .append("\" [label=\"No aliases\", style=dashed, fillcolor=white];\n");
        dotContent.append(" }\n");

        return aliasNodeId;
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
        return generateMemoryGraph(state, outputPath, instruction, showAllObjects, false);
    }

    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction) {
        return generateMemoryGraph(state, outputPath, instruction, true, false);
    }

    public static boolean generateFinalMemoryGraph(JVMState state, String outputPath,
            boolean showAllObjects, boolean showAnalysis) {
        try {
            // Build the accessibility graph
            Map<String, Set<Long>> pointsToGraph = SharingPairAnalyzer.buildPointsToGraph(state);

            // Generate DOT content
            StringBuilder dotContent = new StringBuilder();

            // Create generated directory if it doesn't exist
            File generatedDir = new File(Constants.GENERATED_DIR);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            String dotFilePath = new File(generatedDir, outputPath + ".dot").getPath();
            long timestamp = System.currentTimeMillis();

            // Initialize the DOT file
            initializeDotFile(dotContent, dotFilePath);

            // Create an anchor node for this subgraph
            String anchorNodeId = createAnchorNode(dotContent, timestamp);

            // Create a new subgraph with unique timestamp
            String subgraphId = "cluster_final_" + timestamp;
            dotContent.append(" subgraph ").append(subgraphId).append(" {\n");
            dotContent.append(" ").append(anchorNodeId).append(" [style=invis];\n");
            dotContent.append(" label=\"Final State\";\n");
            dotContent.append(" color=red;\n"); // Highlight that this is the final state
            dotContent.append(" penwidth=3;\n");

            // Add local variables
            addLocalVariables(dotContent, state, timestamp);

            // Add stack
            addStack(dotContent, state, timestamp);

            // Set to track addresses of objects already added
            Set<Long> addedObjectAddresses = new HashSet<>();

            // Add memory objects
            addMemoryObjects(dotContent, state, timestamp, showAllObjects, pointsToGraph, addedObjectAddresses);

            // Add edges between variables and objects
            addEdgesFromVariablesToObjects(dotContent, state, timestamp, addedObjectAddresses);

            // Add edges between objects
            addEdgesBetweenObjects(dotContent, state, timestamp, addedObjectAddresses);

            // Add analysis results if requested
            if (showAnalysis) {
                addAnalysisResults(dotContent, timestamp);
            }

            dotContent.append(" }\n");

            // Finalize the DOT file
            finalizeDotFile(dotContent, dotFilePath);

            // Write DOT content to file
            try (FileWriter writer = new FileWriter(dotFilePath)) {
                writer.write(dotContent.toString());
            }

            return true;

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error generating final memory graph: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}