package fr.univreunion.bcterm;

import fr.univreunion.bcterm.jvm.state.JVMObject;
import fr.univreunion.bcterm.jvm.state.JVMState;
import fr.univreunion.bcterm.jvm.state.LocationValue;
import fr.univreunion.bcterm.jvm.state.Value;

/**
 * Example demonstrating how to create and display a specific JVM state
 */
public class JVMStateExample {

    public static void main(String[] args) {
        // Create a JVM state with 3 local variables and 2 stack elements max
        JVMState state = new JVMState(3, 2);

        // Create locations for our example
        LocationValue loc1 = new LocationValue(1);
        LocationValue loc2 = new LocationValue(2);
        LocationValue loc3 = new LocationValue(3);
        LocationValue loc4 = new LocationValue(4);
        LocationValue loc5 = new LocationValue(5);

        // Set local variables [l1, l2, l4]
        state.setLocalVariable(0, loc1);
        state.setLocalVariable(1, loc2);
        state.setLocalVariable(2, loc4);

        // Set operand stack l3::l2
        state.pushStack(loc2);
        state.pushStack(loc3);

        // Create objects
        JVMObject o1 = new JVMObject("k");
        o1.setField("next", loc4);

        JVMObject o2 = new JVMObject("k");
        o2.setField("next", Value.NULL);

        JVMObject o3 = new JVMObject("k");
        o3.setField("next", loc5);

        JVMObject o4 = new JVMObject("k");
        o4.setField("next", Value.NULL);

        JVMObject o5 = new JVMObject("k");
        o5.setField("next", Value.NULL);

        // Add objects to memory
        state.allocateObject(loc1, o1);
        state.allocateObject(loc2, o2);
        state.allocateObject(loc3, o3);
        state.allocateObject(loc4, o4);
        state.allocateObject(loc5, o5);

        // Display the state
        System.out.println("JVM State Example:");
        System.out.println(state);
        System.out.println("\nDetailed JVM State:");
        System.out.println(state.toDetailedString());
    }
}
