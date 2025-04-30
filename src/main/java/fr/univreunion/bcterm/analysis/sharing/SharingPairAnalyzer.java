package fr.univreunion.bcterm.analysis.sharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
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

    // Map from method call ID to set of sharing pairs for that method call
    private static final Map<String, Set<SharingPair>> methodCallSharingPairs = new HashMap<>();

    // Current method call being analyzed
    private static String currentMethodCall = null;

    // Counter for method calls
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    /**
     * Sets the current method call being analyzed.
     * This should be called at the beginning of a method execution.
     *
     * @param methodCallId The unique identifier for this method call
     */
    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;
        // Initialize the sharing pairs set for this method call if it doesn't exist
        methodCallSharingPairs.putIfAbsent(methodCallId, new HashSet<>());
    }

    /**
     * Gets the current method call being analyzed.
     *
     * @return The identifier of the current method call
     */
    public static String getCurrentMethodCall() {
        return currentMethodCall;
    }

    /**
     * Gets a unique identifier for a method call
     * 
     * @param methodName The name of the method
     * @return A unique identifier for this method call
     */
    public static String getNextMethodCallId(String methodName) {
        // Get the current count for this method and increment it
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);

        // Return a unique identifier combining method name and call count
        return methodName + "_call" + count;
    }

    /**
     * Resets the sharing analysis state for all methods.
     * Should be called at the beginning of a new program execution.
     */
    public static void resetAll() {
        methodCallSharingPairs.clear();
        methodCallCounters.clear();
        currentMethodCall = null;
    }

    /**
     * Resets the sharing analysis state for a specific method.
     *
     * @param methodName The name of the method to reset
     */
    public static void resetMethod(String methodName) {
        methodCallSharingPairs.put(methodName, new HashSet<>());
    }

    /**
     * Analyzes the JVM state to determine pairs of variables that might share.
     *
     * @param state       The JVM state to analyze
     * @param instruction The current bytecode instruction
     * @return The set of variable pairs that might share
     */
    public static Set<SharingPair> analyze(JVMState state, BytecodeInstruction instruction) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method call set for sharing analysis");
            return new HashSet<>();
        }

        // Build accessibility graph
        Map<String, Set<Long>> pointsToGraph = buildPointsToGraph(state);

        // Determine sharing pairs
        Set<SharingPair> sharingPairs = computeSharingPairs(pointsToGraph);

        // Store results for current method
        methodCallSharingPairs.put(currentMethodCall, sharingPairs);

        return sharingPairs;
    }

    /**
     * Analyzes the sharing relationships between variables and objects in a JVM
     * state.
     * 
     * @param state The JVM state to analyze for object sharing
     * @return A set of sharing pairs representing variables that point to the same
     *         or related memory addresses
     */
    public static Set<SharingPair> analyze(JVMState state) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method call set for sharing analysis");
            return new HashSet<>();
        }
        Map<String, Set<Long>> memoryGraph = buildPointsToGraph(state);

        Set<SharingPair> sharingPairs = computeSharingPairs(memoryGraph);
        methodCallSharingPairs.put(currentMethodCall, sharingPairs);

        return sharingPairs;
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
    public static Map<String, Set<Long>> buildPointsToGraph(JVMState state) {
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
     * Gets the sharing pairs for a specific method.
     *
     * @param methodName The name of the method
     * @return The set of sharing pairs for this method
     */
    public static Set<SharingPair> getSharingPairsForMethod(String methodName) {
        return new HashSet<>(methodCallSharingPairs.getOrDefault(methodName, new HashSet<>()));
    }

    /**
     * Gets the sharing pairs for the current method.
     *
     * @return The set of sharing pairs for the current method
     */
    public static Set<SharingPair> getCurrentSharingPairs() {
        return getSharingPairsForMethod(currentMethodCall);
    }
}
