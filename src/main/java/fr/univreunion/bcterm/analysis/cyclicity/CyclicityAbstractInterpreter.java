package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.Set;

import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisEngine;
import fr.univreunion.bcterm.analysis.sharing.SharingState;

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
 * Analyzer for tracking and managing cyclicity relationships between variables
 * during bytecode analysis.
 * 
 * This class provides static methods to track cyclicity states across method
 * calls,
 * analyze bytecode instructions for potential cyclicity changes, and query
 * cyclicity
 * relationships.
 * 
 * The analyzer maintains a mapping of method calls to their current cyclicity
 * states,
 * allowing for precise tracking of variable cyclicity during bytecode
 * interpretation.
 */
public class CyclicityAbstractInterpreter {

    public static String formatForLabel(Set<CyclicVariable> cyclicVariables) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (CyclicVariable cyclicVar : cyclicVariables) {
            if (!first) {
                sb.append(",");
            }
            sb.append(cyclicVar.getVariableName());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static CyclicityState execute(BytecodeInstruction instruction, CyclicityState state,
            String currentMethodCall) {
        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction, state);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction, state);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction, state);
        } else if (instruction instanceof NewInstruction) {
            handleNewInstruction((NewInstruction) instruction, state);
        } else if (instruction instanceof PutFieldInstruction) {
            handlePutFieldInstruction((PutFieldInstruction) instruction, state, currentMethodCall);
        } else if (instruction instanceof CallInstruction) {
            handleCallInstruction((CallInstruction) instruction, state, currentMethodCall);
        } else if (instruction instanceof GetFieldInstruction) {
            handleGetFieldInstruction((GetFieldInstruction) instruction, state);
        } else if (instruction instanceof ConstInstruction) {
            handleConstInstruction((ConstInstruction) instruction, state);
        } else if (instruction instanceof AddInstruction) {
            handleAddInstruction((AddInstruction) instruction, state);
        } else if (instruction instanceof IfEqOfTypeInstruction) {
            handleIfEqOfTypeInstruction((IfEqOfTypeInstruction) instruction, state);
        } else if (instruction instanceof IfNeOfTypeInstruction) {
            handleIfNeOfTypeInstruction((IfNeOfTypeInstruction) instruction, state);
        }

        return state;
    }

    private static void handleLoadInstruction(LoadInstruction instruction, CyclicityState state) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = "s" + state.getStackSize();

        state.pushToStack(stackVar);

        if (state.isPossiblyCyclic(localVar)) {
            state.addPossiblyCyclic(stackVar);
        }
    }

    private static void handleStoreInstruction(StoreInstruction instruction, CyclicityState state) {
        int localIndex = instruction.getIndex();
        String localVar = "l" + localIndex;
        String stackVar = state.popFromStack();

        state.removePossiblyCyclic(localVar);
        if (state.isPossiblyCyclic(stackVar)) {
            state.addPossiblyCyclic(localVar);
        }
        state.removePossiblyCyclic(stackVar);
    }

    private static void handleDupInstruction(DupInstruction instruction, CyclicityState state) {
        String topVar = state.peekStack();
        String newStackVar = "s" + state.getStackSize();

        state.pushToStack(newStackVar);

        if (state.isPossiblyCyclic(topVar)) {
            state.addPossiblyCyclic(newStackVar);
        }
    }

    private static void handleNewInstruction(NewInstruction instruction, CyclicityState state) {
        String newObjectVar = "s" + state.getStackSize();
        state.pushToStack(newObjectVar);
    }

    private static void handlePutFieldInstruction(PutFieldInstruction instruction, CyclicityState state,
            String currentMethodCall) {
        String valueVar = state.popFromStack();
        String objectVar = state.popFromStack();


        SharingState sharingState = SharingAnalysisEngine.getMethodInstructionStates(currentMethodCall).get(instruction);

        boolean valueIsCyclic = state.isPossiblyCyclic(valueVar);
        boolean objectAndValueShare = sharingState != null && sharingState.mayShare(objectVar, valueVar);

        if (valueIsCyclic || objectAndValueShare) {
            if (sharingState != null) {
                Set<String> variablesSharingWithObject = sharingState.getSharingVarsWith(objectVar);
                for (String var : variablesSharingWithObject) {
                    state.addPossiblyCyclic(var);
                }
            }
        }

        state.removePossiblyCyclic(valueVar);
        state.removePossiblyCyclic(objectVar);
    }

    private static void handleCallInstruction(CallInstruction instruction, CyclicityState state,
            String currentMethodCall) {
        int paramCount = instruction.getParameterCount();
        java.util.List<String> params = new java.util.ArrayList<>();

        for (int i = 0; i < paramCount + 1; i++) {
            if (state.isStackEmpty())
                break;
            params.add(state.popFromStack());
        }

        SharingState sharingState = SharingAnalysisEngine.getMethodInstructionStates(currentMethodCall)
                .get(instruction);

        Set<String> allSharingVariables = new java.util.HashSet<>();
        if (sharingState != null) {
            for (String paramVar : params) {
                allSharingVariables.addAll(sharingState.getSharingVarsWith(paramVar));
            }
        }

         for (String var : allSharingVariables) {
             state.addPossiblyCyclic(var);
         }

        for (String paramVar : params) {
            state.removePossiblyCyclic(paramVar);
        }

        if (!instruction.getReturnType().equals("void")) {
            String resultVar = "s" + state.getStackSize();
            state.pushToStack(resultVar);

            state.addPossiblyCyclic(resultVar);
        }
    }

    private static void handleGetFieldInstruction(GetFieldInstruction instruction, CyclicityState state) {
        String objectVar = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        if (state.isPossiblyCyclic(objectVar)) {
            state.addPossiblyCyclic(resultVar);
        }
        state.removePossiblyCyclic(objectVar);
    }

    private static void handleConstInstruction(ConstInstruction instruction, CyclicityState state) {
        String constVar = "s" + state.getStackSize();
        state.pushToStack(constVar);
    }

    private static void handleAddInstruction(AddInstruction instruction, CyclicityState state) {
        String op2 = state.popFromStack();
        String op1 = state.popFromStack();
        String resultVar = "s" + state.getStackSize();

        state.pushToStack(resultVar);

        state.removePossiblyCyclic(op1);
        state.removePossiblyCyclic(op2);
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction instruction, CyclicityState state) {
        String topVar = state.popFromStack();
        state.removePossiblyCyclic(topVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction instruction, CyclicityState state) {
        String topVar = state.popFromStack();
        state.removePossiblyCyclic(topVar);
    }
}