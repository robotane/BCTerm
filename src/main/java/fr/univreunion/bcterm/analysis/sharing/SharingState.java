package fr.univreunion.bcterm.analysis.sharing;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Represents the sharing state of variables during program analysis.
 * 
 * This class tracks sharing relationships between variables and maintains an
 * abstract stack.
 * It provides methods to add, query, and manipulate sharing pairs, as well as
 * perform
 * transitive closure computation on sharing relationships.
 */
public class SharingState {

    private final Set<SharingPair> sharingPairs;

    private final Stack<String> abstractStack;

    /**
     * Constructs a new SharingState with empty sharing pairs and an empty abstract
     * stack.
     * 
     * Initializes the internal data structures for tracking variable sharing
     * relationships
     * and maintaining an abstract representation of a stack during program
     * analysis.
     */
    public SharingState() {
        this.sharingPairs = new HashSet<>();
        this.abstractStack = new Stack<>();
    }

    /**
     * Constructs a new SharingState by creating a deep copy of another
     * SharingState.
     * 
     * Creates a new instance with a copy of the sharing pairs and a copy of the
     * abstract stack
     * from the provided SharingState instance.
     * 
     * @param other The SharingState to copy from
     */
    public SharingState(SharingState other) {
        this.sharingPairs = new HashSet<>(other.sharingPairs);
        this.abstractStack = new Stack<>();
        this.abstractStack.addAll(other.abstractStack);
    }

    public SharingState copy() {
        return new SharingState(this);
    }

    public void reset() {
        sharingPairs.clear();
        abstractStack.clear();
    }

    public void addSharingPair(String var1, String var2) {
        if (var1.equals(var2)) {
            return;
        }

        sharingPairs.add(new SharingPair(var1, var2));
    }

    public Set<SharingPair> getSharingPairs() {
        return new HashSet<>(sharingPairs);
    }

    public boolean mayShare(String var1, String var2) {
        if (var1.equals(var2)) {
            return true;
        }

        return sharingPairs.contains(new SharingPair(var1, var2)) ||
                sharingPairs.contains(new SharingPair(var2, var1));
    }

    public void pushToStack(String var) {
        abstractStack.push(var);
    }

    public String popFromStack() {
        if (abstractStack.isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty abstract stack");
        }
        return abstractStack.pop();
    }

    public String peekStack() {
        if (abstractStack.isEmpty()) {
            throw new IllegalStateException("Cannot peek empty abstract stack");
        }
        return abstractStack.peek();
    }

    public int getStackSize() {
        return abstractStack.size();
    }

    public Set<String> getSharingVarsWith(String var) {
        Set<String> sharingVars = new HashSet<>();

        for (SharingPair pair : sharingPairs) {
            if (pair.getVar1().equals(var)) {
                sharingVars.add(pair.getVar2());
            } else if (pair.getVar2().equals(var)) {
                sharingVars.add(pair.getVar1());
            }
        }

        return sharingVars;
    }

    public String formatForLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (SharingPair pair : sharingPairs) {
            if (!first) {
                sb.append(",");
            }
            sb.append(pair.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SharingState{" +
                "sharingPairs=" + formatForLabel() +
                ", stack=" + abstractStack +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        SharingState other = (SharingState) obj;

        return sharingPairs.equals(other.sharingPairs);
    }

    @Override
    public int hashCode() {
        return sharingPairs.hashCode();
    }

    public void computeTransitiveClosure() {
        Set<SharingPair> newPairs = new HashSet<>();
        boolean changed;

        do {
            changed = false;

            for (SharingPair pair1 : sharingPairs) {
                for (SharingPair pair2 : sharingPairs) {
                    SharingPair newPair = null;

                    // Case 1: (a,b) and (b,c) -> (a,c)
                    if (pair1.getVar2().equals(pair2.getVar1())) {
                        newPair = new SharingPair(pair1.getVar1(), pair2.getVar2());
                    }

                    // Case 2: (a,b) and (c,b) -> (a,c)
                    else if (pair1.getVar2().equals(pair2.getVar2())) {
                        newPair = new SharingPair(pair1.getVar1(), pair2.getVar1());
                    }

                    // Case 3: (b,a) and (b,c) -> (a,c)
                    else if (pair1.getVar1().equals(pair2.getVar1())) {
                        newPair = new SharingPair(pair1.getVar2(), pair2.getVar2());
                    }

                    // Case 4: (b,a) and (c,b) -> (c,a)
                    else if (pair1.getVar1().equals(pair2.getVar2())) {
                        newPair = new SharingPair(pair1.getVar2(), pair2.getVar1());
                    }

                    if (newPair != null && !sharingPairs.contains(newPair) &&
                            !newPair.getVar1().equals(newPair.getVar2())) {
                        newPairs.add(newPair);
                        changed = true;
                    }
                }
            }

            sharingPairs.addAll(newPairs);
            newPairs.clear();
        } while (changed);
    }

    public void removeSharingPairsFor(String var) {
        sharingPairs.removeIf(pair -> pair.getVar1().equals(var) || pair.getVar2().equals(var));
    }
}