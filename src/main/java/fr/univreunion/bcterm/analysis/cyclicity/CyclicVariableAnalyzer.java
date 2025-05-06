package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * Analyzes cyclic variables within a JVM state, tracking and detecting cyclic
 * references
 * across local variables, operand stack, and object fields.
 *
 * This utility class provides methods to:
 * - Set and manage the current method call context
 * - Detect cyclic variables in a JVM state
 * - Store and retrieve cyclic variables for different method calls
 *
 * The analysis identifies variables that form circular references within the
 * program's memory state.
 */
public class CyclicVariableAnalyzer {
    private static final Map<String, Set<CyclicVariable>> methodCallCyclicVars = new HashMap<>();
    private static String currentMethodCall = null;

    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;
        methodCallCyclicVars.putIfAbsent(methodCallId, new HashSet<>());
    }

    public static String getCurrentMethodCall() {
        return currentMethodCall;
    }

    public static void resetAll() {
        methodCallCyclicVars.clear();
        currentMethodCall = null;
    }

    private static Set<CyclicVariable> getCurrentMethodCallCyclicVars() {
        if (currentMethodCall == null) {
            return new HashSet<>();
        }
        return methodCallCyclicVars.getOrDefault(currentMethodCall, new HashSet<>());
    }

    public static Set<CyclicVariable> getCyclicVariables() {
        return new HashSet<>(getCurrentMethodCallCyclicVars());
    }

    public static Set<CyclicVariable> analyze(JVMState state) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method call set for cyclic variables analysis");
            return new HashSet<>();
        }

        Set<CyclicVariable> cyclicVars = detectCyclicVariables(state);

        methodCallCyclicVars.put(currentMethodCall, cyclicVars);

        return cyclicVars;
    }

    private static Set<CyclicVariable> detectCyclicVariables(JVMState state) {
        Set<CyclicVariable> result = new HashSet<>();

        // Check local variables
        for (int i = 0; i < state.getLocalVariablesSize(); i++) {
            Value value = state.getLocalVariable(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue location = (LocationValue) value;
                if (hasCycle(state, location, new HashSet<>())) {
                    result.add(new CyclicVariable("l" + i));
                }
            }
        }

        // Check operand stack
        for (int i = 0; i < state.getStackSize(); i++) {
            Value value = state.getStackElement(i);
            if (value != null && value instanceof LocationValue) {
                LocationValue location = (LocationValue) value;
                if (hasCycle(state, location, new HashSet<>())) {
                    result.add(new CyclicVariable("s" + i));
                }
            }
        }

        return result;
    }

    private static boolean hasCycle(JVMState state, LocationValue location, Set<Long> visited) {
        long address = location.getAddress();

        if (visited.contains(address)) {
            return true;
        }

        visited.add(address);

        JVMObject object = state.getObject(location);
        if (object == null) {
            return false;
        }

        Map<String, Value> fields = object.getFields();
        for (Value fieldValue : fields.values()) {
            if (fieldValue != null && fieldValue instanceof LocationValue) {
                LocationValue fieldLocation = (LocationValue) fieldValue;
                if (hasCycle(state, fieldLocation, new HashSet<>(visited))) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String formatForLabel(Set<CyclicVariable> cyclicVars) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (CyclicVariable var : cyclicVars) {
            if (!first) {
                sb.append(",");
            }
            sb.append(var.getVariableName());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
