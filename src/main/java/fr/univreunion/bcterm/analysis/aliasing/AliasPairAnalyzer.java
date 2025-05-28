package fr.univreunion.bcterm.analysis.aliasing;

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
 * Analyzer for tracking and managing alias relationships between variables
 * during bytecode analysis.
 * 
 * This class provides static methods to track aliasing states across method
 * calls,
 * analyze bytecode instructions for potential alias changes, and query alias
 * relationships.
 * 
 * The analyzer maintains a mapping of method calls to their current aliasing
 * states,
 * allowing for precise tracking of variable aliases during bytecode
 * interpretation.
 */
public class AliasPairAnalyzer {

    private static final Map<String, AliasingState> methodStates = new HashMap<>();
    private static String currentMethodCall = null;
    private static AliasingState currentState = null;

    // Added for interpreter functionality
    private static final Map<String, Map<BytecodeInstruction, AliasingState>> methodInstructionStates = new HashMap<>();
    private static Map<BytecodeInstruction, AliasingState> currentInstructionState = new HashMap<>();

    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;
        methodStates.putIfAbsent(methodCallId, new AliasingState());
        currentState = methodStates.get(methodCallId);

        // Added for interpreter functionality
        methodInstructionStates.putIfAbsent(methodCallId, new HashMap<>());
        currentInstructionState = methodInstructionStates.get(methodCallId);
    }

    public static String getCurrentMethodCall() {
        return currentMethodCall;
    }

    public static void resetAll() {
        methodStates.clear();
        currentMethodCall = null;
        currentState = null;

        // Added for interpreter functionality
        methodInstructionStates.clear();
        currentInstructionState = null;
    }

    public static AliasingState getCurrentState() {
        if (currentState == null) {
            return new AliasingState();
        }
        return currentState;
    }

    public static Set<AliasPair> getDefiniteAliases() {
        return new HashSet<>(getCurrentState().getAliasPairs());
    }

    public static String formatForLabel(Set<AliasPair> aliasPairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (AliasPair pair : aliasPairs) {
            if (!first) {
                sb.append(",");
            }
            sb.append(pair.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static AliasingState execute(BytecodeInstruction instruction, AliasingState state) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return state;
        }

        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction, state);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction, state);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction, state);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction, state);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction, state);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction, state);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction, state);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction, state);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction, state);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction, state);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction, state);
        }

        return state;
    }

    public static void execute(BytecodeInstruction instruction) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return;
        }

        AliasingState state = methodStates.get(currentMethodCall);

        execute(instruction, state);
    }

    /**
     * Analyzes a bytecode instruction and updates the aliasing state.
     * This method implements the interpreter functionality by tracking
     * instruction states and computing LUBs when needed.
     *
     * @param instruction The bytecode instruction to execute
     * @return true if the aliasing state changed, false otherwise
     */
    public static boolean analyze(BytecodeInstruction instruction) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return false;
        }

        AliasPairAnalyzer.execute(instruction);
        AliasingState newState = AliasPairAnalyzer.getCurrentState().deepCopy();

        if(currentInstructionState.containsKey(instruction)) {
            System.out.println("Warning: Instruction already analyzed");
            System.out.println("Before: "+currentInstructionState.get(instruction));
            AliasingState oldState = currentInstructionState.get(instruction);
            System.out.println("After: "+newState);

            AliasingState lub = computeLUB(oldState, newState);
            System.out.println("LUB: "+lub);
            if (lub.getAliasPairs().equals(oldState.getAliasPairs())) {
                return false;
            }
        }

        currentInstructionState.put(instruction, newState);
        return true;
    }

    /**
     * Computes the least upper bound (LUB) of two aliasing states.
     *
     * @param state1 The first aliasing state
     * @param state2 The second aliasing state
     * @return A new aliasing state representing the LUB of the input states
     */
    private static AliasingState computeLUB(AliasingState state1, AliasingState state2) {
        AliasingState result = state1.deepCopy();

        // Ajouter toutes les paires d'alias de state2 à result
        for (AliasPair pair : state2.getAliasPairs()) {
            result.addAliasPair(pair.getVar1(), pair.getVar2());
        }

        // Calculer la fermeture transitive pour assurer la cohérence
        result.computeTransitiveClosure();

        return result;
    }
    private static void handleLoadInstruction(LoadInstruction loadInst, AliasingState state) {
        int localIndex = loadInst.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + state.getStackSize();

        state.pushToStack(stackVar);
        state.addAliasPair(localVar, stackVar);
    }

    private static void handleStoreInstruction(StoreInstruction storeInst, AliasingState state) {
        int localIndex = storeInst.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = state.popFromStack();

        state.removeAliasesFor(localVar);
        state.addAliasPair(stackVar, localVar);

        state.removeAliasesFor(stackVar);
    }

    private static void handleDupInstruction(DupInstruction dupInst, AliasingState state) {
        String topVar = state.peekStack();
        String newStackVar = "s" + state.getStackSize();

        state.pushToStack(newStackVar);

        state.addAliasPair(topVar, newStackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction getFieldInst, AliasingState state) {
        String objectVar = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        state.removeAliasesFor(resultVar);
    }

    private static void handlePutFieldInstruction(PutFieldInstruction putFieldInst, AliasingState state) {
        String valueVar = state.popFromStack();
        String objectVar = state.popFromStack();

        state.removeAliasesFor(objectVar);
        state.removeAliasesFor(valueVar);
    }

    private static void handleAddInstruction(AddInstruction addInst, AliasingState state) {
        String op2 = state.popFromStack();
        String op1 = state.popFromStack();

        String resultVar = "s" + state.getStackSize();
        state.pushToStack(resultVar);

        state.removeAliasesFor(op1);
        state.removeAliasesFor(op2);
    }

    private static void handleConstInstruction(ConstInstruction constInst, AliasingState state) {
        String constVar = "s" + state.getStackSize();

        state.pushToStack(constVar);
    }

    private static void handleNewInstruction(NewInstruction newInst, AliasingState state) {
        String newObjectVar = "s" + state.getStackSize();

        state.pushToStack(newObjectVar);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction ifEqInst, AliasingState state) {
        String topVar = state.popFromStack();

        state.removeAliasesFor(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction ifNeInst, AliasingState state) {
        String topVar = state.popFromStack();

        state.removeAliasesFor(topVar);
    }

    private static void handleCallInstruction(CallInstruction callInst, AliasingState state) {
        int paramCount = callInst.getParameterCount();
        for (int i = 0; i < paramCount + 1; i++) {
            String paramVar = state.popFromStack();
            state.removeAliasesFor(paramVar);
        }
        if (!callInst.getReturnType().equals("void")) {
            String resultVar = "s" + state.getStackSize();
            state.pushToStack(resultVar);
        }
    }

    /**
     * Vérifie si deux variables sont des alias dans l'état courant.
     */
    public static boolean areAliases(String var1, String var2) {
        return getCurrentState().areAliases(var1, var2);
    }

    /**
     * Récupère toutes les variables qui sont des alias d'une variable donnée.
     */
    public static Set<String> getAliasesOf(String var) {
        Set<String> aliases = new HashSet<>();
        Set<AliasPair> aliasPairs = getDefiniteAliases();

        for (AliasPair pair : aliasPairs) {
            if (pair.getVar1().equals(var)) {
                aliases.add(pair.getVar2());
            } else if (pair.getVar2().equals(var)) {
                aliases.add(pair.getVar1());
            }
        }

        return aliases;
    }

    /**
     * Gets the aliasing state for a specific instruction.
     *
     * @param instruction The bytecode instruction
     * @return The aliasing state for the instruction, or null if not available
     */
    public static AliasingState getAliasingStateForInstruction(BytecodeInstruction instruction) {
        if (currentInstructionState == null) {
            return null;
        }
        return currentInstructionState.get(instruction);
    }

    /**
     * Gets all aliasing states for the current method.
     *
     * @return A map of instructions to their aliasing states
     */
    public static Map<BytecodeInstruction, AliasingState> getCurrentMethodAliasingStates() {
        if (currentInstructionState == null) {
            return new HashMap<>();
        }
        return new HashMap<>(currentInstructionState);
    }

    /**
     * Gets a unique identifier for a method call
     *
     * @param methodName The name of the method
     * @return A unique identifier for this method call
     */
    public static String getNextMethodCallId(String methodName) {
        // Using a static counter to generate unique IDs
        Map<String, Integer> methodCallCounters = new HashMap<>();
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);

        return methodName + "_call" + count;
    }
}
