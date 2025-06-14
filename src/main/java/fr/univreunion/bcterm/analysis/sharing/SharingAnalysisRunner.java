package fr.univreunion.bcterm.analysis.sharing;

import java.util.HashMap;
import java.util.Map;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;

public class SharingAnalysisRunner implements AbstractAnalysisRunner {
    private static final Map<String, Map<BytecodeInstruction, SharingState>> methodInstructionStates = new HashMap<>();
    private static Map<BytecodeInstruction, SharingState> currentInstructionState = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        SharingPairAnalyzer.analyze(instruction);
        SharingState newState = SharingPairAnalyzer.getCurrentState().copy();

        if (currentInstructionState.containsKey(instruction)) {
            System.out.println("Warning: Instruction already analyzed");
            SharingState oldState = currentInstructionState.get(instruction);
            System.out.println("Before: " + oldState);
            System.out.println("After: " + newState);

            SharingState lub = computeLUB(oldState, newState);
            System.out.println("LUB: " + lub);

            if (lub.getSharingPairs().equals(oldState.getSharingPairs())) {
                return false;
            }
        }
        currentInstructionState.put(instruction, newState);
        return true;
    }

    @Override
    public void setCurrentMethodCall(String methodCallId) {
        methodInstructionStates.putIfAbsent(methodCallId, new HashMap<>());
        SharingAnalysisRunner.currentInstructionState = methodInstructionStates.get(methodCallId);
        SharingPairAnalyzer.setCurrentMethodCall(methodCallId);
    }

    @Override
    public void reset() {
        methodInstructionStates.clear();
        currentInstructionState = null;
        SharingPairAnalyzer.resetAll();
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
        return SharingPairAnalyzer.getSharingPairs();
    }

    @Override
    public Map<String, Map<BytecodeInstruction, SharingState>> getMethodInstructionStates() {
        return methodInstructionStates;
    }

    @Override
    public String generateMethodCallId(String methodName) {
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);
        return methodName + "_sharing_call" + count;
    }

    @Override
    public void addAnalysisResultToInstruction(BytecodeInstruction instruction, Object result) {
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_SHARING_PAIRS, result);
    }
}
