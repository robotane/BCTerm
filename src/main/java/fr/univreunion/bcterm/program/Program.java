package fr.univreunion.bcterm.program;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.univreunion.bcterm.jvm.state.JVMState;

/**
 * Represents a program containing a collection of methods with a designated
 * main method.
 * 
 * This class manages a program's methods, allowing addition, retrieval, and
 * tracking
 * of methods, including identifying a primary (main) method.
 */
public class Program {
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

    public Set<JVMState> execute(String methodName, JVMState initialState) {
        if (!methods.containsKey(methodName)) {
            throw new IllegalArgumentException("Method " + methodName + " does not exist in this program");
        }

        return methods.get(methodName).execute(initialState);
    }

    public Set<JVMState> execute(JVMState initialState) {
        return execute(mainMethodName, initialState);
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
        dot.append("  node [shape=box fontname=\"monospace\"];\n");
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
        try {
            // Write DOT content to temporary file
            File dotFile = new File(filename + ".dot");
            File outputFile = new File(filename + "." + extension);
            try (FileWriter writer = new FileWriter(dotFile)) {
                writer.write(dot);
                System.out.println("Generated " + dotFile + " successfully.");
            } catch (IOException e) {
                System.err.println("Error generating dot file: " + e.getMessage());
            }

            // Generate output using dot command
            ProcessBuilder pb = new ProcessBuilder("dot", "-T" + extension, dotFile.getAbsolutePath(), "-o",
                    outputFile.getAbsolutePath());
            Process process = pb.start();
            process.waitFor();

            System.out.println("Generated " + outputFile + " successfully.");

        } catch (IOException | InterruptedException e) {
            System.err.println("Error generating image. Make sure GraphViz is installed.\n" + e.getMessage());
        }
    }

    /**
     * Saves the program's DOT graph representation to a file as a PNG image.
     *
     * @param labelKey the key used for labeling nodes in the graph
     */
    public void saveToFile(String labelKey) {
        saveToFile(labelKey, "png");
    }

    /**
     * Saves the program's DOT graph representation to a file as a PNG image with an
     * empty label key.
     */
    public void saveToFile() {
        saveToFile("");
    }

}
