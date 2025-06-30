package fr.univreunion.bcterm.analysis.sharing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisEngine;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

public class SharingAnalysisEngine implements AbstractAnalysisEngine {
    private static final java.util.logging.Logger logger = Logger.getLogger(SharingAnalysisEngine.class);
    private static final Map<String, Map<BytecodeInstruction, SharingState>> methodInstructionStates = new HashMap<>();
    private static final Map<String, SharingState> methodStates = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    private String currentMethodCall = null;
    private SharingState currentState = null;
    private static Map<BytecodeInstruction, SharingState> currentInstructionState = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        SharingState newState = getCurrentState().copy();
        SharingAbstractInterpreter.execute(instruction, currentState, currentMethodCall);

        if (currentInstructionState.containsKey(instruction)) {
            logger.warning("Warning: Instruction already analyzed");
            SharingState oldState = currentInstructionState.get(instruction);
            logger.info(() -> "Before: " + oldState);
            logger.info(() -> "After: " + newState);

            SharingState lub = computeLUB(oldState, newState);
            logger.info(() -> "LUB: " + lub);

            if (lub.getSharingPairs().equals(oldState.getSharingPairs())) {
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
        methodStates.putIfAbsent(methodCallId, new SharingState());
        SharingAnalysisEngine.currentInstructionState = methodInstructionStates.get(methodCallId);

        currentState = methodStates.get(methodCallId);
    }

    public String getCurrentMethodCall() {
        return currentMethodCall;
    }

    @Override
    public SharingState getCurrentState() {
        if (currentState == null) {
            return new SharingState();
        }
        return currentState;
    }

    public Set<SharingPair> getSharingPairs() {
        return new HashSet<>(getCurrentState().getSharingPairs());
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
        return "Sharing Analysis";
    }

    private static SharingState computeLUB(SharingState state1, SharingState state2) {
        return state1.union(state2);
    }

    @Override
    public Object getCurrentAnalysisResults() {
        return getSharingPairs();
    }

    @Override
    public Map<String, Map<BytecodeInstruction, SharingState>> getMethodInstructionStates() {
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
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_SHARING_PAIRS, result);
    }

    public static Map<BytecodeInstruction, SharingState> getMethodInstructionStates(String methodCallId) {
        return methodInstructionStates.get(methodCallId);
    }

    @Override
    public Map<BytecodeInstruction, SharingState> getCurrentInstructionState() {
        return currentInstructionState;
    }
}
