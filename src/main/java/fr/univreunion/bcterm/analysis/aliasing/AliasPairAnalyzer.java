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

    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;
        methodStates.putIfAbsent(methodCallId, new AliasingState());
        currentState = methodStates.get(methodCallId);
    }

    public static String getCurrentMethodCall() {
        return currentMethodCall;
    }

    public static void resetAll() {
        methodStates.clear();
        currentMethodCall = null;
        currentState = null;
    }

    private static AliasingState getCurrentState() {
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

    public static void analyze(BytecodeInstruction instruction) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return;
        }

        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction);
        }
    }

    private static void handleLoadInstruction(LoadInstruction loadInst) {
        int localIndex = loadInst.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + currentState.getStackSize();

        currentState.pushToStack(stackVar);
        currentState.addAliasPair(localVar, stackVar);
    }

    private static void handleStoreInstruction(StoreInstruction storeInst) {
        int localIndex = storeInst.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = currentState.popFromStack();

        currentState.removeAliasesFor(localVar);
        currentState.addAliasPair(stackVar, localVar);

        currentState.removeAliasesFor(stackVar);
    }

    private static void handleDupInstruction(DupInstruction dupInst) {
        String topVar = currentState.peekStack();
        String newStackVar = "s" + currentState.getStackSize();

        currentState.pushToStack(newStackVar);

        currentState.addAliasPair(topVar, newStackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction getFieldInst) {
        String objectVar = currentState.popFromStack();
        String resultVar = "s" + currentState.getStackSize();

        currentState.pushToStack(resultVar);

        currentState.removeAliasesFor(resultVar);
    }

    private static void handlePutFieldInstruction(PutFieldInstruction putFieldInst) {
        String valueVar = currentState.popFromStack();
        String objectVar = currentState.popFromStack();

        currentState.removeAliasesFor(objectVar);
        currentState.removeAliasesFor(valueVar);
    }

    private static void handleAddInstruction(AddInstruction addInst) {
        String op2 = currentState.popFromStack();
        String op1 = currentState.popFromStack();

        String resultVar = "s" + currentState.getStackSize();
        currentState.pushToStack(resultVar);

        currentState.removeAliasesFor(op1);
        currentState.removeAliasesFor(op2);
    }

    private static void handleConstInstruction(ConstInstruction constInst) {
        String constVar = "s" + currentState.getStackSize();

        currentState.pushToStack(constVar);
    }

    private static void handleNewInstruction(NewInstruction newInst) {
        String newObjectVar = "s" + currentState.getStackSize();

        currentState.pushToStack(newObjectVar);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction ifEqInst) {
        String topVar = currentState.popFromStack();

        currentState.removeAliasesFor(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction ifNeInst) {
        String topVar = currentState.popFromStack();

        currentState.removeAliasesFor(topVar);
    }

    private static void handleCallInstruction(CallInstruction callInst) {
        int paramCount = callInst.getParameterCount();
        for (int i = 0; i < paramCount + 1; i++) {
            String paramVar = currentState.popFromStack();
            currentState.removeAliasesFor(paramVar);
        }
        if (!callInst.getReturnType().equals("void")) {
            String resultVar = "s" + currentState.getStackSize();
            currentState.pushToStack(resultVar);
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
}
