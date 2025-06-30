package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisEngine;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

public class CyclicityAnalysisEngine implements AbstractAnalysisEngine {
    private static final java.util.logging.Logger logger = Logger.getLogger(CyclicityAnalysisEngine.class);
    private static final Map<String, Map<BytecodeInstruction, CyclicityState>> methodInstructionStates = new HashMap<>();
    private static final Map<String, CyclicityState> methodStates = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    private String currentMethodCall = null;
    private CyclicityState currentState = null;
    private static Map<BytecodeInstruction, CyclicityState> currentInstructionState = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        CyclicityState newState = getCurrentState().deepCopy();
        CyclicityAbstractInterpreter.execute(instruction, currentState, currentMethodCall);

        if (currentInstructionState.containsKey(instruction)) {
            logger.warning("Warning: Instruction already analyzed for cyclicity");
            CyclicityState oldState = currentInstructionState.get(instruction);
            logger.info(() -> "Before: " + oldState);
            logger.info(() -> "After: " + newState);

            CyclicityState lub = computeLUB(oldState, newState);
            logger.info(() -> "LUB: " + lub);

            if (lub.getPossiblyCyclicVariables().equals(oldState.getPossiblyCyclicVariables())) {
                return false;
            }
        }

        currentInstructionState.put(instruction, newState);
        return true;
    }

    @Override
    public void setCurrentMethodCall(String methodCallId) {
        this.currentMethodCall = methodCallId;
        methodInstructionStates.putIfAbsent(methodCallId, new HashMap<>());
        methodStates.putIfAbsent(methodCallId, new CyclicityState());
        CyclicityAnalysisEngine.currentInstructionState = methodInstructionStates.get(methodCallId);

        currentState = methodStates.get(methodCallId);
    }

    public String getCurrentMethodCall() {
        return currentMethodCall;
    }

    @Override
    public CyclicityState getCurrentState() {
        if (currentState == null) {
            return new CyclicityState();
        }
        return currentState;
    }

    public Set<CyclicVariable> getPossiblyCyclicVariables() {
        return new HashSet<>(getCurrentState().getPossiblyCyclicVariables());
    }

    public void resetAll() {
        methodStates.clear();
        currentMethodCall = null;
        currentState = null;
    }

    @Override
    public void reset() {
        methodInstructionStates.clear();
        methodStates.clear();
        currentInstructionState = null;
        currentMethodCall = null;
        currentState = null;
    }

    @Override
    public String getName() {
        return "Cyclicity Analysis";
    }

    private static CyclicityState computeLUB(CyclicityState state1, CyclicityState state2) {
        return state1.union(state2);
    }

    @Override
    public Object getCurrentAnalysisResults() {
        return getPossiblyCyclicVariables();
    }

    @Override
    public Map<String, Map<BytecodeInstruction, CyclicityState>> getMethodInstructionStates() {
        return methodInstructionStates;
    }

    @Override
    public String generateMethodCallId(String methodName) {
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);
        return methodName + "_call" + count;
    }

    @Override
    public void addAnalysisResultToInstruction(BytecodeInstruction instruction, Object result) {
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_CYCLIC_VARS, result);
    }

    public static Map<BytecodeInstruction, CyclicityState> getMethodInstructionStates(String methodCallId) {
        return methodInstructionStates.get(methodCallId);
    }

    @Override
    public Map<BytecodeInstruction, CyclicityState> getCurrentInstructionState() {
        return currentInstructionState;
    }
}