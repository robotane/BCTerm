package fr.univreunion.bcterm;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;
import fr.univreunion.bcterm.util.Logger;

/**
 * Example demonstrating how to create and display a specific JVM state
 * Example 3 of "A Termination Analyser for Java Bytecode Based on Path-Length",
 * F. Spoto and F. Mesnard and É. Payet, 2010
 */
public class JVMStateExample {
    private static final java.util.logging.Logger logger = Logger.getLogger(JVMStateExample.class);

    public static void main(String[] args) {
        // Create a JVM state
        JVMState state = new JVMState();

        // Create locations
        LocationValue l1 = new LocationValue(1);
        LocationValue l2 = new LocationValue(2);
        LocationValue l3 = new LocationValue(3);
        LocationValue l4 = new LocationValue(4);
        LocationValue l5 = new LocationValue(5);

        // Set local variables [l1, l2, l4]
        state.setLocalVariable(0, l1);
        state.setLocalVariable(1, l2);
        state.setLocalVariable(2, l4);

        // Set operand stack l3::l2
        state.pushStack(l2);
        state.pushStack(l3);

        // Create objects
        JVMObject o1 = new JVMObject("k");
        o1.setField("next", l4);

        JVMObject o2 = new JVMObject("k");
        o2.setField("next", Value.NULL);

        JVMObject o3 = new JVMObject("k");
        o3.setField("next", l5);

        JVMObject o4 = new JVMObject("k");
        o4.setField("next", Value.NULL);

        JVMObject o5 = new JVMObject("k");
        o5.setField("next", Value.NULL);

        // Add objects to memory
        state.allocateObject(l1, o1);
        state.allocateObject(l2, o2);
        state.allocateObject(l3, o3);
        state.allocateObject(l4, o4);
        state.allocateObject(l5, o5);

        // Display the state
        logger.info(() -> "JVM State Example:");
        logger.info(() -> state.toString());
        logger.info(() -> "\nDetailed JVM State:");
        logger.info(() -> state.toDetailedString());
    }
}
