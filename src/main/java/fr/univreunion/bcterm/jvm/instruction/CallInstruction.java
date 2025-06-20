package fr.univreunion.bcterm.jvm.instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import fr.univreunion.bcterm.analysis.AbstractAnalysisRunner;
import fr.univreunion.bcterm.analysis.aliasing.AliasPair;
import fr.univreunion.bcterm.analysis.aliasing.AliasPairAnalyzer;
import fr.univreunion.bcterm.analysis.aliasing.AliasingAnalysisRunner;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicVariableAnalyzer;
import fr.univreunion.bcterm.analysis.cyclicity.CyclicityAnalysisRunner;
import fr.univreunion.bcterm.analysis.sharing.SharingAnalysisRunner;
import fr.univreunion.bcterm.analysis.sharing.SharingPair;
import fr.univreunion.bcterm.analysis.sharing.SharingPairAnalyzer;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.program.Method;
import fr.univreunion.bcterm.program.Program;
import fr.univreunion.bcterm.util.Logger;

/**
 * call κ1.m(t1, ..., tp) : t, ..., κn.m(t1, ..., tp) : t.
 *
 * Pops the topmost p + 1 values (the actual parameters) a0, a1, ..., ap from
 * the stack.
 * Value a0 is called receiver of the call and must be a reference to an object
 * o or null.
 * In the latter case, the computation stops.
 *
 * Otherwise, a lookup procedure is started from the class κ of o upwards along
 * the superclass chain,
 * looking for a method called m, expecting p formal parameters of type t1, ...,
 * tp, respectively,
 * and returning a value of type t.
 *
 * It is guaranteed that such a method is found in a class belonging to the set
 * {κ1, ..., κn}.
 * That method is run from a state having an empty stack and a set of local
 * variables bound to a0, a1, ..., ap.
 */
public class CallInstruction extends BytecodeInstruction {
    private static final java.util.logging.Logger logger = Logger.getLogger(CallInstruction.class);
    private String methodName;
    private String signature;
    private List<String> implementationClasses; // Remplace className
    private Program program;
    private AbstractAnalysisRunner analysisRunner;
    private Stack<String> methodCallStack = new Stack<>();

    /**
     * Constructs a CallInstruction by parsing a call string containing method
     * implementations.
     * 
     * Parses the call string to extract class names, method names, and signatures.
     * Supports multiple method implementations with different classes.
     * 
     * @param callString A string representing method call implementations in the
     *                   format
     *                   "ClassName.methodName(params):returnType"
     */
    public CallInstruction(String callString) {
        implementationClasses = new ArrayList<>();

        List<String> implementations = parseImplementations(callString);

        for (String impl : implementations) {
            // Expected format: "ClassName.methodName(params):returnType"
            int dotIndex = impl.indexOf('.');
            int parenOpenIndex = impl.indexOf('(');

            if (dotIndex == -1 || parenOpenIndex == -1) {
                logger.warning(() -> "Warning: Invalid implementation format: " + impl);
                continue;
            }

            String currentClass = impl.substring(0, dotIndex);
            implementationClasses.add(currentClass);

            if (methodName == null) {
                // Extract method name from the first implementation
                methodName = impl.substring(dotIndex + 1, parenOpenIndex);

                // Extract signature (everything from opening parenthesis onwards)
                int signatureStart = parenOpenIndex;
                signature = impl.substring(signatureStart);
            }
        }
    }

    /**
     * Parses a string containing multiple method implementations into a list of
     * individual implementations.
     * 
     * Example:
     * Input: "ArrayList.add(Object):boolean,LinkedList.add(Object):boolean"
     * Output: ["ArrayList.add(Object):boolean", "LinkedList.add(Object):boolean"]
     * 
     * @param callString String containing one or more method implementations
     * @return List of individual implementation strings
     */
    private List<String> parseImplementations(String callString) {
        List<String> implementations = new ArrayList<>();
        StringBuilder currentImpl = new StringBuilder();
        int parenLevel = 0;

        for (int i = 0; i < callString.length(); i++) {
            char c = callString.charAt(i);

            if (c == '(') {
                parenLevel++;
                currentImpl.append(c);
            } else if (c == ')') {
                parenLevel--;
                currentImpl.append(c);
            } else if (c == ',' && parenLevel == 0) {
                // This comma is an implementation separator
                implementations.add(currentImpl.toString().trim());
                currentImpl = new StringBuilder();
            } else {
                currentImpl.append(c);
            }
        }

        // Add the last implementation
        if (currentImpl.length() > 0) {
            implementations.add(currentImpl.toString().trim());
        }

        return implementations;
    }

    public void setProgram(Program programme) {
        this.program = programme;
    }

    @Override
    public void setAnalysisRunner(AbstractAnalysisRunner analysisRunner) {
        this.analysisRunner = analysisRunner;
    }

    @Override
    public boolean execute(JVMState initialState) {
        if (program == null) {
            logger.severe("Error: Program not defined for CallInstruction");
            return false;
        }

        Map<String, Object> signatureInfo = parseSignature(this.signature);
        @SuppressWarnings("unchecked")
        List<String> parameters = (List<String>) signatureInfo.get("parameters");
        int paramCount = parameters.size();

        if (initialState.getStackSize() < paramCount + 1) {
            logger.severe("Error: Not enough parameters on stack");
            return false;
        }
        int initialStackSize = initialState.getStackSize();

        Value[] paramValues = new Value[paramCount + 1];
        for (int i = paramCount; i >= 0; i--) {
            paramValues[i] = initialState.popStack();
        }

        if (paramValues[0] == Value.NULL) {
            logger.severe("Error: Null receiver for method call");
            return false;
        }

        boolean methodFound = false;
        for (String methName : program.getMethodNames()) {
            if (methName.equals(this.methodName)) {
                Method method = program.getMethod(methName);
                if (method != null && method.getSignature().equals(this.signature)) {
                    methodFound = true;
                    break;
                }
            }
        }

        if (!methodFound) {
            logger.severe(() -> "Error: Method not found in program: " + this.methodName + this.signature);
            return false;
        }

        JVMState methodState = new JVMState(initialState.getMemory());

        for (int i = 0; i <= paramCount; i++) {
            methodState.setLocalVariable(i, paramValues[i]);
        }

        // Propagate analysis relationships from stack variables to local variables
        List<Object> propagatedResults = propagateAnalysisRelationships(paramCount, initialStackSize);
        String methodCallId = null;
        if (analysisRunner != null) {
            methodCallId = analysisRunner.generateMethodCallId(this.methodName);
            analysisRunner.setCurrentMethodCall(methodCallId);
            if (!propagatedResults.isEmpty()) {
                for (Object result : propagatedResults) {
                    if (result instanceof AliasPair && analysisRunner instanceof AliasingAnalysisRunner) {
                        AliasPair pair = (AliasPair) result;
                        AliasPairAnalyzer.getCurrentState().addAliasPair(pair.getVar1(), pair.getVar2());
                    } else if (result instanceof SharingPair && analysisRunner instanceof SharingAnalysisRunner) {
                        SharingPair pair = (SharingPair) result;
                        SharingPairAnalyzer.getCurrentState().addSharingPair(pair.getVar1(), pair.getVar2());
                    } else if (result instanceof String && analysisRunner instanceof CyclicityAnalysisRunner) {
                        String nonCyclicVar = (String) result;
                        CyclicVariableAnalyzer.getCurrentState().addPossiblyCyclic(nonCyclicVar);
                    }
                }
            }
        }

        Set<JVMState> finalStates = program.execute(this.methodName, methodState, analysisRunner, methodCallId);

        if (finalStates != null && !finalStates.isEmpty()) {
            JVMState finalState = finalStates.iterator().next();

            if (finalState.getStackSize() > 0) {
                initialState.pushStack(finalState.popStack());
            }
            return true;
        }

        return false;
    }

    /**
     * Parses a method signature of the form "(t1, ..., tp) : t" to extract
     * parameters and return type.
     * 
     * Examples:
     * - For signature "(int) : void": returns parameters=["int"], returnType="void"
     * - For signature "(String, int) : boolean": returns parameters=["String",
     * "int"], returnType="boolean"
     * 
     * @param signature The method signature to parse (e.g. "(int) : void",
     *                  "(String, int) : boolean")
     * @return A map containing "parameters" (List<String>) and "returnType"
     *         (String)
     */
    public Map<String, Object> parseSignature(String signature) {
        Map<String, Object> result = new HashMap<>();
        List<String> parameters = new ArrayList<>();

        // Find the positions of key delimiters
        int openParenIndex = signature.indexOf('(');
        int closeParenIndex = signature.indexOf(')');
        int colonIndex = signature.indexOf(':');

        if (openParenIndex == -1 || closeParenIndex == -1 || colonIndex == -1) {
            logger.warning(() -> "Error: Invalid signature format: " + signature);
            result.put("parameters", parameters);
            result.put("returnType", "void");
            return result;
        }

        // Extract parameters part
        String paramsSection = signature.substring(openParenIndex + 1, closeParenIndex).trim();

        // If there are parameters, split and process them
        if (!paramsSection.isEmpty()) {
            String[] paramArray = paramsSection.split(",");
            for (String param : paramArray) {
                parameters.add(param.trim());
            }
        }

        // Extract return type
        String returnType = signature.substring(colonIndex + 1).trim();

        result.put("parameters", parameters);
        result.put("returnType", returnType);

        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("call ");

        boolean isFirst = true;
        for (String cls : implementationClasses) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append(cls).append(".").append(methodName).append(signature);
            isFirst = false;
        }

        return sb.toString();
    }

    /**
     * Returns the number of parameters in the method signature.
     *
     * @return the count of parameters parsed from the method signature
     */
    public int getParameterCount() {
        Map<String, Object> signatureInfo = parseSignature(this.signature);

        @SuppressWarnings("unchecked")
        List<String> parameters = (List<String>) signatureInfo.get("parameters");

        return parameters.size();
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        Map<String, Object> signatureInfo = parseSignature(this.signature);
        return (String) signatureInfo.get("returnType");
    }

    /**
     * Propagates analysis relationships from stack variables (sk) to local
     * variables (li).
     * If two stack variables were related by analysis, the corresponding local
     * variables
     * will also be marked as related.
     * 
     * @return List of propagated results to be applied to the called method
     */
    private List<Object> propagateAnalysisRelationships(int paramCount, int initialStackSize) {
        List<Object> propagatedResults = new ArrayList<>();

        if (analysisRunner == null) {
            return propagatedResults;
        }

        Object analysisResults = analysisRunner.getCurrentAnalysisResults();
        if (analysisResults == null) {
            return propagatedResults;
        }

        // Check relationships between all pairs of stack parameters
        for (int i = 0; i <= paramCount; i++) {

            if (analysisRunner instanceof CyclicityAnalysisRunner) {
                int stackPos1 = initialStackSize - i;
                String stackVar1 = "s" + stackPos1;
                String localVar1 = "l" + i;
                // For non-cyclicity, we propagate cyclic properties
                if (CyclicVariableAnalyzer.getCurrentState().isPossiblyCyclic(stackVar1)) {
                    propagatedResults.add(localVar1);
                }
                continue;
            }
            for (int j = i + 1; j <= paramCount; j++) {
                int stackPos1 = initialStackSize - i;
                int stackPos2 = initialStackSize - j;
                String stackVar1 = "s" + stackPos1;
                String stackVar2 = "s" + stackPos2;
                String localVar1 = "l" + i;
                String localVar2 = "l" + j;
                // Check if stack variables are related and create local relationship if so
                Object localRelationship = createRelationshipIfExists(stackVar1, stackVar2, localVar1, localVar2);
                if (localRelationship != null) {
                    propagatedResults.add(localRelationship);
                }
            }
        }

        return propagatedResults;
    }

    /**
     * Checks if two stack variables are related and creates the corresponding local
     * variable relationship.
     * 
     * @return The relationship object if stack variables are related, null
     *         otherwise
     */
    private Object createRelationshipIfExists(String stackVar1, String stackVar2, String localVar1, String localVar2) {
        if (analysisRunner instanceof AliasingAnalysisRunner) {
            if (AliasPairAnalyzer.getCurrentState().areAliases(stackVar1, stackVar2)) {
                return new AliasPair(localVar1, localVar2);
            }
        } else if (analysisRunner instanceof SharingAnalysisRunner) {
            if (SharingPairAnalyzer.getCurrentState().mayShare(stackVar1, stackVar2)) {
                return new SharingPair(localVar1, localVar2);
            }
        }
        return null;
    }

    public void pushMethodCallStack(String calleddId) {
        methodCallStack.push(calleddId);
    }

    public String popMethodCallStack() {
        return methodCallStack.pop();
    }

}