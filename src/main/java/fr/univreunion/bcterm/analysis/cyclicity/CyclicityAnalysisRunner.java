package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.HashMap;
import java.util.Map;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

public class CyclicityAnalysisRunner implements AbstractAnalysisRunner {
    private static final java.util.logging.Logger logger = Logger.getLogger(CyclicityAnalysisRunner.class);
    private static final Map<String, Map<BytecodeInstruction, CyclicityState>> methodInstructionStates = new HashMap<>();
    private static Map<BytecodeInstruction, CyclicityState> currentInstructionState = new HashMap<>();
    private static final Map<String, Integer> methodCallCounters = new HashMap<>();

    @Override
    public boolean analyze(BytecodeInstruction instruction) {
        CyclicityState newState = CyclicVariableAnalyzer.getCurrentState().deepCopy();
        CyclicVariableAnalyzer.analyze(instruction);

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
        CyclicVariableAnalyzer.setCurrentMethodCall(methodCallId);
        methodInstructionStates.putIfAbsent(methodCallId, new HashMap<>());
        CyclicityAnalysisRunner.currentInstructionState = methodInstructionStates.get(methodCallId);
    }

    @Override
    public void reset() {
        methodInstructionStates.clear();
        currentInstructionState = null;
        CyclicVariableAnalyzer.resetAll();
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
        return CyclicVariableAnalyzer.getPossiblyCyclicVariables();
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
}