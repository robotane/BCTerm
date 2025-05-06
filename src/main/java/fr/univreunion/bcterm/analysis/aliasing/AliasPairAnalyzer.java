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
import fr.univreunion.bcterm.jvm.state.JVMState;

public class AliasPairAnalyzer {

    private static final Map<String, Set<AliasPair>> methodCallAliases = new HashMap<>();

    private static String currentMethodCall = null;

    public static void setCurrentMethodCall(String methodCallId) {
        currentMethodCall = methodCallId;
        methodCallAliases.putIfAbsent(methodCallId, new HashSet<>());
    }

    public static String getCurrentMethodCall() {
        return currentMethodCall;
    }

    public static void resetAll() {
        methodCallAliases.clear();
        currentMethodCall = null;
    }

    private static Set<AliasPair> getCurrentMethodCallAliases() {
        if (currentMethodCall == null) {
            return new HashSet<>();
        }
        return methodCallAliases.getOrDefault(currentMethodCall, new HashSet<>());
    }

    public static Set<AliasPair> getDefiniteAliases() {
        return new HashSet<>(getCurrentMethodCallAliases());
    }

    private static void addDefiniteAlias(String var1, String var2) {
        if (currentMethodCall == null || var1.equals(var2)) {
            return;
        }

        Set<AliasPair> aliases = methodCallAliases.get(currentMethodCall);
        aliases.add(new AliasPair(var1, var2));
        computeTransitiveClosure();
    }

    private static void removeAliasesFor(String var) {
        if (currentMethodCall == null) {
            return;
        }

        Set<AliasPair> aliases = methodCallAliases.get(currentMethodCall);
        aliases.removeIf(pair -> pair.getVar1().equals(var) || pair.getVar2().equals(var));
    }

    private static void computeTransitiveClosure() {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return;
        }

        Set<AliasPair> aliases = getCurrentMethodCallAliases();
        Set<AliasPair> newAliases = new HashSet<>();
        boolean changed;

        do {
            changed = false;
            for (AliasPair pair1 : aliases) {
                for (AliasPair pair2 : aliases) {
                    AliasPair newPair = null;

                    // Case 1: (a,b) and (b,c) -> (a,c)
                    if (pair1.getVar2().equals(pair2.getVar1())) {
                        newPair = new AliasPair(pair1.getVar1(), pair2.getVar2());
                    }

                    // Case 2: (a,b) and (c,b) -> (a,c)
                    else if (pair1.getVar2().equals(pair2.getVar2())) {
                        newPair = new AliasPair(pair1.getVar1(), pair2.getVar1());
                    }

                    // Case 3: (b,a) and (b,c) -> (a,c)
                    else if (pair1.getVar1().equals(pair2.getVar1())) {
                        newPair = new AliasPair(pair1.getVar2(), pair2.getVar2());
                    }

                    // Case 4: (b,a) and (c,b) -> (c,a)
                    else if (pair1.getVar1().equals(pair2.getVar2())) {
                        newPair = new AliasPair(pair1.getVar2(), pair2.getVar1());
                    }

                    if (newPair != null && !aliases.contains(newPair) && !newPair.getVar1().equals(newPair.getVar2())) {
                        newAliases.add(newPair);
                        changed = true;
                    }
                }
            }

            aliases.addAll(newAliases);
            newAliases.clear();
        } while (changed);
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

    public static void analyze(BytecodeInstruction instruction, JVMState state) {
        if (currentMethodCall == null) {
            System.out.println("Warning: No current method set for alias analysis");
            return;
        }

        // System.out.println("Before analysis: " +
        // formatForLabel(getDefiniteAliases()));

        // Instructions that mainly add aliases
        if (instruction instanceof LoadInstruction) {
            handleLoadInstruction((LoadInstruction) instruction, state);
        } else if (instruction instanceof DupInstruction) {
            handleDupInstruction((DupInstruction) instruction, state);
        } else if (instruction instanceof StoreInstruction) {
            handleStoreInstruction((StoreInstruction) instruction, state);
        }

        // Instructions that mainly remove aliases
        else if (instruction instanceof GetFieldInstruction) {
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

        // System.out.println("After analysis: " +
        // formatForLabel(getDefiniteAliases()));
    }

    private static void handleLoadInstruction(LoadInstruction loadInst, JVMState state) {
        int localIndex = loadInst.getIndex();
        int futureStackIndex = state.getStackSize() - 1;
        String localVar = "l" + localIndex;
        String futureStackVar = "s" + futureStackIndex;

        addDefiniteAlias(localVar, futureStackVar);
    }

    private static void handleStoreInstruction(StoreInstruction storeInst, JVMState state) {
        int currentStackIndex = state.getStackSize();
        String currentStackVar = "s" + currentStackIndex;
        int localIndex = storeInst.getIndex();
        String localVar = "l" + localIndex;
        addDefiniteAlias(currentStackVar, localVar);

        removeAliasesFor(currentStackVar);
    }

    private static void handleDupInstruction(DupInstruction dupInst, JVMState state) {
        int currentStackIndex = state.getStackSize() - 2;
        int futureStackIndex = state.getStackSize() - 1;
        String currentStackVar = "s" + currentStackIndex;
        String futureStackVar = "s" + futureStackIndex;

        addDefiniteAlias(currentStackVar, futureStackVar);
    }

    private static void handleGetFieldInstruction(GetFieldInstruction getFieldInst, JVMState state) {
        String stackVar = "s" + (state.getStackSize() - 1);

        removeAliasesFor(stackVar);
    }

    private static void handlePutFieldInstruction(PutFieldInstruction putFieldInst, JVMState state) {
        String objectRef = "s" + (state.getStackSize() - 1);
        String value = "s" + (state.getStackSize());
        removeAliasesFor(objectRef);
        removeAliasesFor(value);
    }

    private static void handleAddInstruction(AddInstruction addInst, JVMState state) {
        String resultVar = "s" + (state.getStackSize() - 1);
        removeAliasesFor(resultVar);

        String topVar = "s" + (state.getStackSize());
        removeAliasesFor(topVar);
    }

    private static void handleConstInstruction(ConstInstruction constInst, JVMState state) {
        return;
    }

    private static void handleNewInstruction(NewInstruction newInst, JVMState state) {
        return;
    }

    private static void handleIfEqOfTypeInstruction(IfEqOfTypeInstruction ifEqInst, JVMState state) {
        String topStackVar = "s" + (state.getStackSize());

        removeAliasesFor(topStackVar);
    }

    private static void handleIfNeOfTypeInstruction(IfNeOfTypeInstruction ifNeInst, JVMState state) {
        String topStackVar = "s" + (state.getStackSize());

        removeAliasesFor(topStackVar);
    }

    private static void handleCallInstruction(CallInstruction callInst, JVMState state) {
        int paramCount = callInst.getParameterCount();

        for (int i = 0; i < paramCount + 1; i++) {
            int stackIndex = state.getStackSize() + i;
            String stackVar = "s" + stackIndex;
            removeAliasesFor(stackVar);
        }
    }
}
