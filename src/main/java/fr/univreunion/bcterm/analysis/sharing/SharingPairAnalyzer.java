package fr.univreunion.bcterm.analysis.sharing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.instruction.AddInstruction;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.jvm.instruction.CallInstruction;
import fr.univreunion.bcterm.jvm.instruction.ConstInstruction;
import fr.univreunion.bcterm.jvm.instruction.DupInstruction;
import fr.univreunion.bcterm.jvm.instruction.GetFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfEqOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.IfNeOfTypeInstruction;
import fr.univreunion.bcterm.jvm.instruction.LoadInstruction;
import fr.univreunion.bcterm.jvm.instruction.NewInstruction;
import fr.univreunion.bcterm.jvm.instruction.PutFieldInstruction;
import fr.univreunion.bcterm.jvm.instruction.StoreInstruction;

/**
 * Analyzes sharing relationships between variables in a JVM state.
 * 
 * Two variables are considered to share if they can reach a common memory
 * location, either directly or transitively through object references. For
 * example:
 * 
 * Case 1 (No sharing):
 * When two variables point to completely disjoint data structures,
 * they do not share any memory locations.
 *
 * Case 2 (Sharing):
 * When one variable can reach memory locations reachable by another variable
 * (e.g., through field references like sh2 == sh1.next), they share memory.
 * 
 * This implementation is based on the pair-sharing analysis approach described
 * in:
 * "Pair-Sharing Analysis of Object-Oriented Programs" by Stefano Secci and
 * Fausto Spoto,
 * which formalizes the abstract domain for pair-sharing and provides a
 * compositional
 * abstract semantics for static analysis.
 */
public class SharingPairAnalyzer {

    private static final Map<String, SharingState> methodStates = new HashMap<>();
    private static String currentMethodCall = null;
    private static SharingState currentState = null;
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    /**
     * Sets the current method call being analyzed.
     * This should be called at the beginning of a method execution.
     *
     * @param methodCallId The unique identifier for this method call
     */
    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;

        methodStates.putIfAbsent(methodCallId, new SharingState());
        currentState = methodStates.get(methodCallId);
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
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);

        return methodName + "_call" + count;
    }

    public static Set<SharingPair> getCurrentSharingPairs() {
        return new HashSet<>(currentState.getSharingPairs());
    }

    /**
     * Resets the sharing analysis state for all methods.
     * Should be called at the beginning of a new program execution.
     */
    public static void resetAll() {
        methodStates.clear();
        methodCallCounters.clear();
        currentMethodCall = null;
        currentState = null;
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
        if (currentState == null) {
            return new HashSet<>();
        }
        return new HashSet<>(currentState.getSharingPairs());
    }

    /**
     * Gets the sharing pairs for the current method.
     *
     * @return The set of sharing pairs for the current method
     */
    public static Set<SharingPair> getSharingPairs() {
        if (currentState == null) {
            return new HashSet<>();
        }
        return currentState.getSharingPairs();
    }

    /**
     * Analyzes a bytecode instruction for sharing effects.
     * 
     * @param instruction The bytecode instruction to analyze
     */
    public static void analyze(BytecodeInstruction instruction) {
        if (currentMethodCall == null || currentState == null) {
            System.out.println("Warning: No current method call set for sharing analysis");
            return;
        }

        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction);
        }
    }

    private static void handleLoadInstruction(LoadInstruction instruction) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + currentState.getStackSize();

        currentState.pushToStack(stackVar);
        currentState.addSharingPair(localVar, stackVar);

        currentState.computeTransitiveClosure();
    }

    private static void handleStoreInstruction(StoreInstruction instruction) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = currentState.popFromStack();

        currentState.removeSharingPairsFor(localVar);
        currentState.addSharingPair(localVar, stackVar);

        currentState.computeTransitiveClosure();
        currentState.removeSharingPairsFor(stackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction instruction) {
        String objectVar = currentState.popFromStack();
        String resultVar = "s" + currentState.getStackSize();

        currentState.pushToStack(resultVar);

        currentState.addSharingPair(objectVar, resultVar);

        currentState.computeTransitiveClosure();
    }

    private static void handlePutFieldInstruction(PutFieldInstruction instruction) {
        String valueVar = currentState.popFromStack();
        String objectVar = currentState.popFromStack();

        currentState.addSharingPair(objectVar, valueVar);

        currentState.computeTransitiveClosure();

        currentState.removeSharingPairsFor(valueVar);
        currentState.removeSharingPairsFor(objectVar);
    }

    private static void handleNewInstruction(NewInstruction instruction) {
        String newObjectVar = "s" + currentState.getStackSize();
        currentState.pushToStack(newObjectVar);
    }

    private static void handleConstInstruction(ConstInstruction instruction) {
        String constVar = "s" + currentState.getStackSize();
        currentState.pushToStack(constVar);
    }

    private static void handleCallInstruction(CallInstruction instruction) {
        int paramCount = instruction.getParameterCount();
        for (int i = 0; i < paramCount + 1; i++) {
            String paramVar = currentState.popFromStack();
            currentState.removeSharingPairsFor(paramVar);
        }

        if (!instruction.getReturnType().equals("void")) {
            String resultVar = "s" + currentState.getStackSize();
            currentState.pushToStack(resultVar);
        }
    }

    private static void handleDupInstruction(DupInstruction instruction) {
        String topVar = currentState.peekStack();
        String newStackVar = "s" + currentState.getStackSize();

        currentState.pushToStack(newStackVar);

        currentState.addSharingPair(topVar, newStackVar);

        Set<String> sharingWithTop = currentState.getSharingVarsWith(topVar);
        for (String var : sharingWithTop) {
            currentState.addSharingPair(newStackVar, var);
        }
    }

    private static void handleAddInstruction(AddInstruction instruction) {
        String op2 = currentState.popFromStack();
        String op1 = currentState.popFromStack();
        String resultVar = "s" + currentState.getStackSize();

        currentState.pushToStack(resultVar);

        currentState.removeSharingPairsFor(op1);
        currentState.removeSharingPairsFor(op2);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction instruction) {
        String topVar = currentState.popFromStack();
        currentState.removeSharingPairsFor(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction instruction) {
        String topVar = currentState.popFromStack();
        currentState.removeSharingPairsFor(topVar);
    }

    /**
     * Retrieves all variables that may share memory with the given variable.
     * 
     * @param var the variable to find sharing variables for
     * @return a set of variables that may share memory with the input variable,
     *         or an empty set if no current state is available
     */
    public static Set<String> getSharingVarsWith(String var) {
        if (currentState == null) {
            return new HashSet<>();
        }
        return currentState.getSharingVarsWith(var);
    }

    /**
     * Determines whether two variables may potentially share memory.
     * 
     * @param var1 the first variable to check for potential memory sharing
     * @param var2 the second variable to check for potential memory sharing
     * @return true if the variables may share memory, false otherwise or if no
     *         current state is available
     */
    public static boolean mayShare(String var1, String var2) {
        if (currentState == null) {
            return false;
        }
        return currentState.mayShare(var1, var2);
    }

}
