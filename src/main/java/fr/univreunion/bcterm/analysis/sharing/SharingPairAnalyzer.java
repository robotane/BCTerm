package fr.univreunion.bcterm.analysis.sharing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * Analyzes sharing relationships between variables in a JVM state.
 * 
 * Two variables are considered to share if they can reach a common memory
 * location,
 * either directly or transitively through object references. For example:
 * 
 * Case 1 (No sharing):
 * When two variables point to completely disjoint data structures,
 * they do not share any memory locations.
 *
 * Case 2 (Sharing):
 * When one variable can reach memory locations reachable by another variable
 * (e.g., through field references like sh2 == sh1.next), they share memory.
 * 
 * The analysis helps in understanding data structure relationships,
 * detecting potential infinite recursion, and analyzing termination properties.
 */
public class SharingPairAnalyzer {

    /**
     * Analyzes the sharing relationships between variables and objects in a JVM
     * state.
     * 
     * @param state The JVM state to analyze for object sharing
     * @return A set of sharing pairs representing variables that point to the same
     *         or related memory addresses
     */
    public static Set<SharingPair> analyze(JVMState state) {
        Map<String, Set<Long>> memoryGraph = buildMemoryGraph(state);

        return computeSharingPairs(memoryGraph);
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
    private static Map<String, Set<Long>> buildMemoryGraph(JVMState state) {
        Map<String, Set<Long>> memoryGraph = new HashMap<>();

        // Build for local variables
        for (int i = 0; i < state.getLocalVariablesSize(); i++) {
            Value value = state.getLocalVariable(i);
            if (value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                String varName = "l" + i;
                long address = locationValue.getAddress();
                memoryGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(address);

                addReachableObjects(varName, address, memoryGraph, state, new HashSet<>());
            }
        }

        // Build for stack elements
        for (int i = 0; i < state.getStackSize(); i++) {
            Value value = state.getStackElement(i);
            if (value instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) value;
                String varName = "s" + i;
                long address = locationValue.getAddress();
                memoryGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(address);

                addReachableObjects(varName, address, memoryGraph, state, new HashSet<>());
            }
        }

        return memoryGraph;
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

        for (String fieldName : objectFields.keySet()) {
            Value fieldValue = objectFields.get(fieldName);
            if (fieldValue instanceof LocationValue) {
                LocationValue locationValue = (LocationValue) fieldValue;
                long fieldAddress = locationValue.getAddress();
                memoryGraph.computeIfAbsent(varName, k -> new HashSet<>()).add(fieldAddress);

                addReachableObjects(varName, fieldAddress, memoryGraph, state, visited);
            }
        }
    }

    /**
     * Computes sharing pairs of variables that point to the same memory addresses.
     *
     * @param memoryGraph A mapping of variable names to their memory addresses
     * @return A set of sharing pairs representing variables with overlapping memory
     *         references
     */
    private static Set<SharingPair> computeSharingPairs(Map<String, Set<Long>> memoryGraph) {
        Set<SharingPair> sharingPairs = new HashSet<>();
        List<String> variables = new ArrayList<>(memoryGraph.keySet());
        for (int i = 0; i < variables.size(); i++) {
            String var1 = variables.get(i);
            Set<Long> addresses1 = memoryGraph.get(var1);
            for (int j = i + 1; j < variables.size(); j++) {
                String var2 = variables.get(j);
                Set<Long> addresses2 = memoryGraph.get(var2);

                // Check if the two variables share any memory addresses
                if (!Collections.disjoint(addresses1, addresses2)) {
                    sharingPairs.add(new SharingPair(var1, var2));
                }
            }
        }
        return sharingPairs;

    }

    /**
     * Formats a set of sharing pairs into a string representation.
     *
     * @param pairs The set of sharing pairs to format
     * @return A string representation of the sharing pairs, enclosed in curly
     *         braces
     */
    public static String formatForLabel(Set<SharingPair> pairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (SharingPair pair : pairs) {
            if (!first) {
                sb.append(",");
            }
            sb.append(pair);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Generates a graphical representation of the memory graph in DOT format
     * and saves the result as a DOT file.
     *
     * This method creates a detailed visualization of the JVM state, including
     * variables, objects, and their relationships at a specific point during
     * bytecode instruction execution.
     *
     * @param state          The current JVM state to visualize
     * @param outputPath     The base path where the DOT file will be saved
     * @param instruction    The current bytecode instruction being analyzed
     * @param showAllObjects If true, includes all memory objects; if false,
     *                       only shows accessible objects
     * @return true if the DOT file generation was successful, false otherwise
     */
    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction,
            boolean showAllObjects) {
        try {
            // Build the accessibility graph
            Map<String, Set<Long>> pointsToGraph = buildMemoryGraph(state);

            // Generate DOT content
            StringBuilder dotContent = new StringBuilder();
            String dotFilePath = outputPath + ".dot";
            long timestamp = System.currentTimeMillis();

            // Create DOT file if it doesn't exist
            File dotFile = new File(dotFilePath);
            if (!dotFile.exists()) {
                dotContent.append("digraph MemoryGraph {\n");
                dotContent.append("  node [shape=box, style=filled, fillcolor=lightblue];\n");
                dotContent.append("  rankdir=LR;\n\n");

                // Create invisible subgraph as anchor point
                dotContent.append("  // Invisible anchor subgraph\n");
                dotContent.append("  subgraph cluster_anchor {\n");
                dotContent.append("    style=invis;\n");
                dotContent.append("    anchor [style=invis, shape=point, width=0, height=0];\n");
                dotContent.append("  }\n\n");

                // Define starting point for ordering
                dotContent.append("  // Starting point for subgraph ordering\n");
                dotContent.append("  anchor_start [style=invis, shape=point];\n\n");
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

            // Create anchor node for this subgraph
            String anchorNodeId = "anchor_" + timestamp;
            dotContent.append("  ").append(anchorNodeId).append(" [style=invis, shape=point];\n");

            // Add ordering constraint with previous subgraph
            dotContent.append("  anchor_start -> ").append(anchorNodeId).append(" [style=invis];\n");
            dotContent.append("  anchor_start = ").append(anchorNodeId).append(";\n\n");

            // Create new subgraph with unique timestamp
            String subgraphId = "cluster_" + timestamp;
            dotContent.append("  subgraph ").append(subgraphId).append(" {\n");
            // Include anchor node in subgraph to maintain order
            dotContent.append("    ").append(anchorNodeId).append(" [style=invis];\n");
            dotContent.append("    label=\"").append(instruction.toString()).append("\";\n");

            // Add nodes for variables
            for (String varName : pointsToGraph.keySet()) {
                String varLabel = varName.startsWith("l") ? "Local " + varName.substring(1)
                        : "Stack " + varName.substring(1);
                String uniqueVarId = varName + "_" + timestamp;
                String fillColor = varName.startsWith("l") ? "lightgreen" : "lightpink";
                dotContent.append("    \"").append(uniqueVarId).append("\" [label=\"").append(varLabel)
                        .append("\", fillcolor=").append(fillColor).append("];\n");
            }

            if (showAllObjects) {
                // Add all memory objects
                Map<LocationValue, JVMObject> memory = state.getMemory();
                for (Map.Entry<LocationValue, JVMObject> entry : memory.entrySet()) {
                    LocationValue location = entry.getKey();
                    JVMObject obj = entry.getValue();
                    String objLabel = "Object@" + location.getAddress() + "\\n" + obj.getClassTag();
                    String uniqueObjId = "obj" + location.getAddress() + "_" + timestamp;
                    dotContent.append("    \"").append(uniqueObjId).append("\" [label=\"").append(objLabel)
                            .append("\"];\n");

                    // Add object fields
                    Map<String, Value> fields = obj.getFields();
                    for (Map.Entry<String, Value> field : fields.entrySet()) {
                        Value fieldValue = field.getValue();
                        if (fieldValue != null && fieldValue instanceof LocationValue) {
                            LocationValue locValue = (LocationValue) fieldValue;
                            long fieldAddress = locValue.getAddress();
                            dotContent.append("    \"").append(uniqueObjId).append("\" -> \"obj")
                                    .append(fieldAddress).append("_").append(timestamp)
                                    .append("\" [label=\"").append(field.getKey()).append("\"];\n");
                        }
                    }
                }
            } else {
                // Add only accessible objects
                Set<Long> allAddresses = new HashSet<>();
                for (Set<Long> addresses : pointsToGraph.values()) {
                    allAddresses.addAll(addresses);
                }

                for (Long address : allAddresses) {
                    JVMObject obj = state.getObject(new LocationValue(address));
                    if (obj != null) {
                        String objLabel = "Object@" + address + "\\n" + obj.getClassTag();
                        String uniqueObjId = "obj" + address + "_" + timestamp;
                        dotContent.append("    \"").append(uniqueObjId).append("\" [label=\"").append(objLabel)
                                .append("\"];\n");

                        // Add object fields
                        Map<String, Value> fields = state.getObjectFields(address);
                        if (fields != null) {
                            for (Map.Entry<String, Value> field : fields.entrySet()) {
                                Value fieldValue = field.getValue();
                                if (fieldValue != null && fieldValue instanceof LocationValue) {
                                    LocationValue locValue = (LocationValue) fieldValue;
                                    long fieldAddress = locValue.getAddress();
                                    dotContent.append("    \"").append(uniqueObjId).append("\" -> \"obj")
                                            .append(fieldAddress).append("_").append(timestamp)
                                            .append("\" [label=\"").append(field.getKey()).append("\"];\n");
                                }
                            }
                        }
                    }
                }
            }

            // Add edges between variables and objects
            for (Map.Entry<String, Set<Long>> entry : pointsToGraph.entrySet()) {
                String varName = entry.getKey();
                for (Long address : entry.getValue()) {
                    Value value = varName.startsWith("l")
                            ? state.getLocalVariable(Integer.parseInt(varName.substring(1)))
                            : state.getStackElement(Integer.parseInt(varName.substring(1)));

                    if (value != null && value instanceof LocationValue) {
                        LocationValue locValue = (LocationValue) value;
                        if (locValue.getAddress() == address) {
                            dotContent.append("    \"").append(varName).append("_").append(timestamp)
                                    .append("\" -> \"obj").append(address).append("_").append(timestamp)
                                    .append("\";\n");
                        }
                    }
                }
            }

            // Add section for sharing pairs
            Set<SharingPair> sharingPairs = computeSharingPairs(pointsToGraph);
            if (!sharingPairs.isEmpty()) {
                dotContent.append("\n    // Sharing pairs\n");
                dotContent.append("    subgraph cluster_sharing_").append(timestamp).append(" {\n");
                dotContent.append("      label=\"Sharing Pairs\";\n");
                dotContent.append("      node [shape=ellipse, style=filled, fillcolor=lightyellow];\n");

                int pairId = 0;
                for (SharingPair pair : sharingPairs) {
                    String pairNodeId = "pair" + pairId++ + "_" + timestamp;
                    dotContent.append("      \"").append(pairNodeId).append("\" [label=\"")
                            .append(pair.getVar1()).append(" ↔ ").append(pair.getVar2()).append("\"];\n");
                }

                dotContent.append("    }\n");
            }

            dotContent.append("  }\n");

            // Close main graph
            dotContent.append("}\n");

            // Write DOT content to file
            try (java.io.FileWriter writer = new java.io.FileWriter(dotFilePath)) {
                writer.write(dotContent.toString());
            }

            // System.out.println("DOT file generated successfully: " + dotFilePath);
            return true;

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error generating memory graph: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates a memory graph with default settings.
     *
     * @param state       The current JVM state to analyze
     * @param outputPath  The file path where the memory graph will be saved
     * @param instruction The bytecode instruction being processed
     * @return {@code true} if the memory graph is successfully generated,
     *         {@code false} otherwise
     */
    public static boolean generateMemoryGraph(JVMState state, String outputPath, BytecodeInstruction instruction) {
        return generateMemoryGraph(state, outputPath, instruction, false);
    }
}
