package fr.univreunion.bcterm.util;

/**
 * Centralized constants management for BCTerm.
 * This class contains all constants extracted from the codebase.
 */
public class Constants {

    // Default method signature
    public static final String DEFAULT_METHOD_SIGNATURE = "():void";

    // Analysis result keys
    public static final String ANALYSIS_RESULT_LOCAL_VARS_COUNT = "localVarsCount";
    public static final String ANALYSIS_RESULT_STACK_SIZE = "stackSize";
    public static final String ANALYSIS_RESULT_SHARING_PAIRS = "sharingPairs";
    public static final String ANALYSIS_RESULT_LOCAL_VARS_AND_STACK = "localVarsAndStack";
    public static final String ANALYSIS_RESULT_CYCLIC_VARS = "cyclicVars";
    public static final String ANALYSIS_RESULT_ALIAS_PAIRS = "aliasPairs";
    public static final String ANALYSIS_RESULT_ALL = "all";

    // File generation constants
    public static final boolean ENABLE_FILE_GENERATION = true;

    // Memory graph related constants
    public static final String GENERATED_DIR = "generated";
    public static final String MEMORY_GRAPH_PREFIX = "memoryGraph_";
    public static final boolean MEMORY_GRAPH_SHOW_OBJECTS = true;
    public static final boolean MEMORY_GRAPH_SHOW_PRIMITIVES = true;

    // Graph visualization constants
    public static final String DOT_FILE_EXTENSION = ".dot";
    public static final String DEFAULT_GRAPH_EXTENSION = "png";
    public static final String DOT_NODE_SHAPE = "box";
    public static final String DOT_GRAPH_STYLE = "rounded";

    // DOT graph generation commands
    public static final String DOT_COMMAND = "dot";
    public static final String DOT_OUTPUT_FLAG = "-o";
    public static final String DOT_TYPE_FLAG_PREFIX = "-T";

    private Constants() {
    }
}