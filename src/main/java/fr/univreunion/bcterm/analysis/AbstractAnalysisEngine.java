package fr.univreunion.bcterm.analysis;

import java.util.Map;

import fr.univreunion.bcterm.jvm.instruction.BytecodeInstruction;

public interface AbstractAnalysisEngine {
    /**
     * Sets the current method call context
     * 
     * @param methodCallId The method call identifier
     */
    void setCurrentMethodCall(String methodCallId);

    /**
     * Analyzes a bytecode instruction
     * 
     * @param instruction The instruction to analyze
     * @return true if analysis should continue, false to stop
     */
    boolean analyze(BytecodeInstruction instruction);

    /**
     * Gets the name of this interpreter
     * 
     * @return The interpreter name
     */
    String getName();

    /**
     * Resets the interpreter state
     */
    void reset();

    /**
     * Retrieves analysis results for a given instruction
     * 
     * @param instruction The instruction for which to retrieve analysis results
     * @return An object containing analysis results, type depends on implementation
     */
    Object getCurrentAnalysisResults();

    /**
     * Generates a unique identifier for a method call
     * 
     * @param methodName The method name
     * @return A unique identifier for this method call
     */
    String generateMethodCallId(String methodName);

    void addAnalysisResultToInstruction(BytecodeInstruction instruction, Object result);

    Object getMethodInstructionStates();

    Map<BytecodeInstruction, ?> getCurrentInstructionState();

    Object getCurrentState();
}