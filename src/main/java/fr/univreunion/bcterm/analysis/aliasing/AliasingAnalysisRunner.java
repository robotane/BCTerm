package fr.univreunion.bcterm.analysis.aliasing;

import java.util.HashMap;
import java.util.Map;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;

public class AliasingAnalysisRunner implements AbstractAnalysisRunner {
    private static final Map<String, Map<BytecodeInstruction, AliasingState>> methodInstructionStates = new HashMap<>();
    private static Map<BytecodeInstruction, AliasingState> currentInstructionState = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        AliasPairAnalyzer.execute(instruction);
        AliasingState newState = AliasPairAnalyzer.getCurrentState().deepCopy();

        if (currentInstructionState.containsKey(instruction)) {
            System.out.println("Warning: Instruction already analyzed");
            AliasingState oldState = currentInstructionState.get(instruction);
            System.out.println("Before: " + oldState);
            System.out.println("After: " + newState);

            AliasingState lub = computeLUB(oldState, newState);
            System.out.println("LUB: " + lub);

            if (lub.getAliasPairs().equals(oldState.getAliasPairs())) {
                return false;
            }
        }
        currentInstructionState.put(instruction, newState);
        return true;
    }

    @Override
    public void setCurrentMethodCall(String methodCallId) {
        AliasPairAnalyzer.setCurrentMethodCall(methodCallId);
        methodInstructionStates.putIfAbsent(methodCallId, new HashMap<>());
        AliasingAnalysisRunner.currentInstructionState = methodInstructionStates.get(methodCallId);
    }

    @Override
    public void reset() {
        methodInstructionStates.clear();
        currentInstructionState = null;
        AliasPairAnalyzer.resetAll();
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
        return AliasPairAnalyzer.getDefiniteAliases();
    }

    @Override
    public Map<String, Map<BytecodeInstruction, AliasingState>> getMethodInstructionStates() {
        return methodInstructionStates;
    }

    @Override
    public String generateMethodCallId(String methodName) {
        int count = methodCallCounters.getOrDefault(methodName, 0) + 1;
        methodCallCounters.put(methodName, count);
        return methodName + "_aliasing_call" + count;
    }

    @Override
    public void addAnalysisResultToInstruction(BytecodeInstruction instruction, Object result) {
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_ALIAS_PAIRS, result);
    }
}
