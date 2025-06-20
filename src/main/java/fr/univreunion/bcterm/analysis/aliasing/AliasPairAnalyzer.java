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
import fr.univreunion.bcterm.util.Logger;

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
    private static final java.util.logging.Logger logger = Logger.getLogger(AliasPairAnalyzer.class);
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
        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction, state);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction, state);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction, state);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction, state);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction, state);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction, state);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction, state);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction, state);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction, state);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction, state);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction, state);
        }

        return state;
    }

    public static void execute(BytecodeInstruction instruction) {
        if (currentMethodCall == null) {
            logger.warning(() -> "Warning: No current method set for alias analysis");
            return;
        }

        AliasingState state = methodStates.get(currentMethodCall);

        execute(instruction, state);
    }

    private static void handleLoadInstruction(LoadInstruction instruction, AliasingState state) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + state.getStackSize();

        state.pushToStack(stackVar);
        state.addAliasPair(localVar, stackVar);
    }

    private static void handleStoreInstruction(StoreInstruction instruction, AliasingState state) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = state.popFromStack();

        state.removeAliasesFor(localVar);
        state.addAliasPair(stackVar, localVar);

        state.removeAliasesFor(stackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction instruction, AliasingState state) {
        String objectVar = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        state.removeAliasesFor(resultVar);
    }

    private static void handlePutFieldInstruction(PutFieldInstruction instruction, AliasingState state) {
        String valueVar = state.popFromStack();
        String objectVar = state.popFromStack();

        state.removeAliasesFor(objectVar);
        state.removeAliasesFor(valueVar);
    }

    private static void handleNewInstruction(NewInstruction instruction, AliasingState state) {
        String newObjectVar = "s" + state.getStackSize();

        state.pushToStack(newObjectVar);
    }

    private static void handleConstInstruction(ConstInstruction instruction, AliasingState state) {
        String constVar = "s" + state.getStackSize();

        state.pushToStack(constVar);
    }

    private static void handleCallInstruction(CallInstruction instruction, AliasingState state) {
        int paramCount = instruction.getParameterCount();
        for (int i = 0; i < paramCount + 1; i++) {
            String paramVar = state.popFromStack();
            state.removeAliasesFor(paramVar);
        }
        if (!instruction.getReturnType().equals("void")) {
            String resultVar = "s" + state.getStackSize();
            state.pushToStack(resultVar);
        }
    }

    private static void handleDupInstruction(DupInstruction instruction, AliasingState state) {
        String topVar = state.peekStack();
        String newStackVar = "s" + state.getStackSize();

        state.pushToStack(newStackVar);

        state.addAliasPair(topVar, newStackVar);
    }

    private static void handleAddInstruction(AddInstruction instruction, AliasingState state) {
        String op2 = state.popFromStack();
        String op1 = state.popFromStack();

        String resultVar = "s" + state.getStackSize();
        state.pushToStack(resultVar);

        state.removeAliasesFor(op1);
        state.removeAliasesFor(op2);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction instruction, AliasingState state) {
        String topVar = state.popFromStack();

        state.removeAliasesFor(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction instruction, AliasingState state) {
        String topVar = state.popFromStack();

        state.removeAliasesFor(topVar);
    }
}
