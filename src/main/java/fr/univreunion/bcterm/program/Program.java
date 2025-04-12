package fr.univreunion.bcterm.program;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public final void addMethod(String methodName, String signature, CFG methodCFG) {
        Method method = new Method(methodName, signature, methodCFG, this);
        this.addMethod(method);
    }

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
}
