package fr.univreunion.bcterm.analysis.aliasing;

import java.util.HashMap;
import java.util.Map;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

public class AliasingAnalysisRunner implements AbstractAnalysisRunner {
    private static final java.util.logging.Logger logger = Logger.getLogger(AliasingAnalysisRunner.class);
    private static final Map<String, Map<BytecodeInstruction, AliasingState>> methodInstructionStates = new HashMap<>();
    private static Map<BytecodeInstruction, AliasingState> currentInstructionState = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        AliasingState newState = AliasPairAnalyzer.getCurrentState().deepCopy();
        AliasPairAnalyzer.execute(instruction);

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
        return methodName + "_call" + count;
    }

    @Override
    public void addAnalysisResultToInstruction(BytecodeInstruction instruction, Object result) {
        instruction.addAnalysisResult(Constants.ANALYSIS_RESULT_ALIAS_PAIRS, result);
    }
}
