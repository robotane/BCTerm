package fr.univreunion.bcterm.analysis.sharing;

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
 * Analyzer for tracking and managing sharing relationships between variables
 * during bytecode analysis.
 * 
 * This class provides static methods to track sharing states across method
 * calls,
 * analyze bytecode instructions for potential sharing changes, and query
 * sharing
 * relationships.
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
public class SharingAbstractInterpreter {

    public static String formatForLabel(Set<SharingPair> sharingPairs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (SharingPair pair : sharingPairs) {
            if (!first) {
                sb.append(",");
            }
            sb.append(pair.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static SharingState execute(BytecodeInstruction instruction, SharingState state, String currentMethodCall) {
        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction, state);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction, state, currentMethodCall);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction, state);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction, state, currentMethodCall);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction, state);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction, state);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction, state);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction, state, currentMethodCall);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction, state);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction, state);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction, state);
        }

        return state;
    }

    private static void handleLoadInstruction(LoadInstruction instruction, SharingState state) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + state.getStackSize();

        state.pushToStack(stackVar);
        state.addSharingPair(localVar, stackVar);
        state.computeTransitiveClosure();
    }

    private static void handleStoreInstruction(StoreInstruction instruction, SharingState state,
            String currentMethodCall) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = state.popFromStack();

        state.removeSharingPairsFor(localVar);

        state.addSharingPair(localVar, stackVar);
        state.computeTransitiveClosure();

        state.removeSharingPairsFor(stackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction instruction, SharingState state) {
        String objectVar = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        state.addSharingPair(objectVar, resultVar);
        state.computeTransitiveClosure();
    }

    private static void handlePutFieldInstruction(PutFieldInstruction instruction, SharingState state,
            String currentMethodCall) {
        String valueVar = state.popFromStack();
        String objectVar = state.popFromStack();

        state.addSharingPair(objectVar, valueVar);
        state.computeTransitiveClosure();

        state.removeSharingPairsFor(valueVar);
        state.removeSharingPairsFor(objectVar);
    }

    private static void handleNewInstruction(NewInstruction instruction, SharingState state) {
        String newObjectVar = "s" + state.getStackSize();
        state.pushToStack(newObjectVar);
    }

    private static void handleConstInstruction(ConstInstruction instruction, SharingState state) {
        String constVar = "s" + state.getStackSize();
        state.pushToStack(constVar);
    }

    private static void handleCallInstruction(CallInstruction instruction, SharingState state) {
        int paramCount = instruction.getParameterCount();
        for (int i = 0; i < paramCount + 1; i++) {
            String paramVar = state.popFromStack();
            state.removeSharingPairsFor(paramVar);
        }

        if (!instruction.getReturnType().equals("void")) {
            String resultVar = "s" + state.getStackSize();
            state.pushToStack(resultVar);
        }
    }

    private static void handleDupInstruction(DupInstruction instruction, SharingState state, String currentMethodCall) {
        String topVar = state.peekStack();
        String newStackVar = "s" + state.getStackSize();
        state.pushToStack(newStackVar);
        state.addSharingPair(topVar, newStackVar);
        state.computeTransitiveClosure();
    }

    private static void handleAddInstruction(AddInstruction instruction, SharingState state) {
        String op2 = state.popFromStack();
        String op1 = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        state.removeSharingPairsFor(op1);
        state.removeSharingPairsFor(op2);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction instruction, SharingState state) {
        String topVar = state.popFromStack();
        state.removeSharingPairsFor(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction instruction, SharingState state) {
        String topVar = state.popFromStack();
        state.removeSharingPairsFor(topVar);
    }
}
