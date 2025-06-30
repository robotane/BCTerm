package fr.univreunion.bcterm.program;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.analysis.AbstractAnalysisEngine;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.util.Constants;
import fr.univreunion.bcterm.util.Logger;

/**
 * Represents a program containing a collection of methods with a designated
 * main method.
 * 
 * This class manages a program's methods, allowing addition, retrieval, and
 * tracking
 * of methods, including identifying a primary (main) method.
 */
public class Program {
    private static final java.util.logging.Logger logger = Logger.getLogger(Program.class);
    private final String name;
    private final Map<String, Method> methods;
    private String mainMethodName;

    /**
     * Constructs a new Program with the specified name and an empty collection of
     * methods.
     *
     * @param programName the name of the program
     */
    public Program(String programName) {
        this(programName, new HashMap<>(), null);
    }

    /**
     * Constructs a new Program with the specified name, methods, and main method
     * name.
     *
     * @param programName    the name of the program
     * @param methods        a map of method names to Method objects representing
     *                       the program's methods
     * @param mainMethodName the name of the main method for this program
     */
    public Program(String programName, Map<String, Method> methods, String mainMethodName) {
        this.name = programName;
        this.methods = methods;
        this.mainMethodName = mainMethodName;
    }

    /**
     * Constructs a new Program with the specified name and an initial method which
     * will be the main method by default.
     *
     * @param programName the name of the program
     * @param method      the initial method to be added to the program
     */
    public Program(String programName, Method method) {
        this(programName);
        this.addMethod(method);
    }

    public final String getName() {
        return name;
    }

    public Map<String, Method> getMethods() {
        return methods;
    }

    public String getMainMethodName() {
        return mainMethodName;
    }

    /**
     * Retrieves a method from the program by its name.
     *
     * @param methodName the name of the method to retrieve
     * @return the Method object corresponding to the given method name
     * @throws IllegalArgumentException if the method does not exist in the program
     */
    public Method getMethod(String methodName) {
        if (!methods.containsKey(methodName)) {
            throw new IllegalArgumentException("Method " + methodName + " does not exist in this program");
        }
        return methods.get(methodName);
    }

    public Set<String> getMethodNames() {
        return methods.keySet();
    }

    /**
     * Adds a method to the program, setting the program reference and potentially
     * setting it as the main method if no main method is currently defined.
     *
     * @param method the method to be added to the program
     */
    public final void addMethod(Method method) {
        method.setProgram(this); // TODO: check if this is necessary, could the program of a method be changed?
        methods.put(method.getName(), method);

        if (this.mainMethodName == null) {
            this.mainMethodName = method.getName();
        }
    }

    /**
     * Adds a method to the program with a specified name, signature, and control
     * flow graph.
     *
     * @param methodName the name of the method to be added
     * @param signature  the method's signature
     * @param methodCFG  the control flow graph representing the method's structure
     */
    public final void addMethod(String methodName, String signature, CFG methodCFG) {
        Method method = new Method(methodName, signature, methodCFG, this);
        this.addMethod(method);
    }

    /**
     * Adds a method to the program with a specified name and control flow graph.
     *
     * @param methodName the name of the method to be added
     * @param methodCFG  the control flow graph representing the method's structure
     */
    public final void addMethod(String methodName, CFG methodCFG) {
        Method method = new Method(methodName, methodCFG, this);
        this.addMethod(method);
    }

    /**
     * Sets the main method of the program by its name.
     *
     * @param methodName the name of the method to be set as the main method
     * @throws IllegalArgumentException if the specified method does not exist in
     *                                  the program
     */
    public final void setMainMethodName(String methodName) {
        if (!methods.containsKey(methodName)) {
            throw new IllegalArgumentException("Method " + methodName + " does not exist in this program");
        }
        this.mainMethodName = methodName;
    }

    /**
     * Executes a specific method of the program with the given interpreter.
     *
     * @param methodName   The name of the method to execute
     * @param initialState The initial JVM state
     * @param interpreter  The interpreter to use for analysis
     * @return The set of final JVM states after execution
     * @throws IllegalArgumentException if the specified method does not exist in
     *                                  the program
     */
    public Set<JVMState> execute(String methodName, JVMState initialState, AbstractAnalysisEngine interpreter) {
        return execute(methodName, initialState, interpreter, null);
    }

    public Set<JVMState> execute(String methodName, JVMState initialState, AbstractAnalysisEngine interpreter,
            String methodCallId) {
        if (!methods.containsKey(methodName)) {
            throw new IllegalArgumentException("Method " + methodName + " does not exist in this program");
        }

        return methods.get(methodName).execute(initialState, interpreter, methodCallId);
    }

    /**
     * Executes the main method of the program with the given interpreter.
     *
     * @param initialState The initial JVM state
     * @param interpreter  The interpreter to use for analysis
     * @return The set of final JVM states after execution
     */
    public Set<JVMState> execute(JVMState initialState, AbstractAnalysisEngine interpreter) {
        return execute(mainMethodName, initialState, interpreter, null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Program: ").append(name).append("\n");

        for (Method method : methods.values()) {
            sb.append(method.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Converts the program to a DOT graph representation.
     *
     * @param labelKey a key used for labeling nodes in the graph
     * @return a string containing the DOT graph description of the program
     */
    public String toDOT(String labelKey) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph Program {\n");
        dot.append("  label=\"").append(this.name).append("\"\n");

        // Add the global graph attributes
        dot.append("  node [shape=").append(Constants.DOT_NODE_SHAPE).append(" fontname=\"monospace\"];\n");
        dot.append("  edge [color=blue];\n");

        // Create a subgraph for each method
        int methodIndex = 0;
        for (Method method : this.methods.values()) {
            dot.append(method.toDOT(methodIndex++, labelKey));
        }

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Saves the program's DOT graph representation to a file.
     *
     * @param labelKey  the key used for labeling nodes in the graph
     * @param extension the file extension for the output graph image (e.g., "png",
     *                  "svg", pdf", etc.)
     */
    public void saveToFile(String labelKey, String extension) {
        String dot = toDOT(labelKey);
        String filename = this.name;
        String programDir = Constants.GENERATED_DIR + File.separator + this.name;

        try {
            File generatedDir = new File(programDir);
            if (!generatedDir.exists()) {
                generatedDir.mkdirs();
            }

            File dotFile = new File(generatedDir, filename + Constants.DOT_FILE_EXTENSION);
            File outputFile = new File(generatedDir, filename + "." + extension);
            try (FileWriter writer = new FileWriter(dotFile)) {
                writer.write(dot);
                logger.info(() -> "Generated " + dotFile + " successfully.");
            } catch (IOException e) {
                logger.severe(() -> "Error generating dot file: " + e.getMessage());
            }

            ProcessBuilder pb = new ProcessBuilder(
                    Constants.DOT_COMMAND,
                    Constants.DOT_TYPE_FLAG_PREFIX + extension,
                    dotFile.getAbsolutePath(),
                    Constants.DOT_OUTPUT_FLAG,
                    outputFile.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();

            logger.info(() -> "Generated " + outputFile + " successfully.");

        } catch (IOException | InterruptedException e) {
            logger.severe(() -> "Error generating image. Make sure GraphViz is installed.\n" + e.getMessage());
        }
    }

    /**
     * Saves the program's DOT graph representation to a file as a PNG image.
     *
     * @param labelKey the key used for labeling nodes in the graph
     */
    public void saveToFile(String labelKey) {
        saveToFile(labelKey, Constants.DEFAULT_GRAPH_EXTENSION);
    }

    /**
     * Saves the program's DOT graph representation to a file as a PNG image with an
     * empty label key.
     */
    public void saveToFile() {
        saveToFile("");
    }

}
