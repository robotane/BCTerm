package fr.univreunion.bcterm.analysis.aliasing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisEngine;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

public class AliasingAnalysisEngine implements AbstractAnalysisEngine {
    private static final java.util.logging.Logger logger = Logger.getLogger(AliasingAnalysisEngine.class);
    private static final Map<String, Map<BytecodeInstruction, AliasingState>> methodInstructionStates = new HashMap<>();
    private static final Map<String, AliasingState> methodStates = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    private String currentMethodCall = null;
    private AliasingState currentState = null;
    private static Map<BytecodeInstruction, AliasingState> currentInstructionState = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        AliasingState newState = getCurrentState().copy();
        AliasingAbstractInterpreter.execute(instruction, currentState, currentMethodCall);

        if (currentInstructionState.containsKey(instruction)) {
            logger.warning(() -> "Warning: Instruction already analyzed");
            AliasingState oldState = currentInstructionState.get(instruction);
            logger.info(() -> "Before: " + oldState);
            logger.info(() -> "After: " + newState);

            AliasingState lub = computeLUB(oldState, newState);
            logger.info(() -> "LUB: " + lub);

            if (lub.getAliasPairs().equals(oldState.getAliasPairs())) {
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
        methodStates.putIfAbsent(methodCallId, new AliasingState());
        AliasingAnalysisEngine.currentInstructionState = methodInstructionStates.get(methodCallId);

        currentState = methodStates.get(methodCallId);
    }

    public String getCurrentMethodCall() {
        return currentMethodCall;
    }

    @Override
    public AliasingState getCurrentState() {
        if (currentState == null) {
            return new AliasingState();
        }
        return currentState;
    }

    public Set<AliasPair> getDefiniteAliases() {
        return new HashSet<>(getCurrentState().getAliasPairs());
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
        return "Aliasing Analysis";
    }

    private static AliasingState computeLUB(AliasingState state1, AliasingState state2) {
        return state1.union(state2);
    }

    @Override
    public Object getCurrentAnalysisResults() {
        return getDefiniteAliases();
    }

    @Override
    public Map<String, Map<BytecodeInstruction, AliasingState>> getMethodInstructionStates() {
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
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_ALIAS_PAIRS, result);
    }

    @Override
    public Map<BytecodeInstruction, AliasingState> getCurrentInstructionState() {
        return currentInstructionState;
    }
}