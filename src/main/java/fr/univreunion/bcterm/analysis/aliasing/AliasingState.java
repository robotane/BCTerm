package fr.univreunion.bcterm.analysis.aliasing;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Represents the state of aliasing information during program analysis.
 * 
 * This class tracks alias relationships between variables and maintains an
 * abstract stack
 * for analysis purposes. It supports operations like adding alias pairs,
 * computing transitive
 * closure of aliases, and managing a stack of variables.
 * 
 * The class provides methods to:
 * - Add and remove alias relationships
 * - Check if two variables are aliases
 * - Manipulate an abstract stack (push, pop, peek)
 * - Create copies and reset the aliasing state
 */
public class AliasingState {

    private final Set<AliasPair> aliasPairs;

    private final Stack<String> abstractStack;

    /**
     * Creates a new AliasingState with empty alias pairs and an empty abstract
     * stack.
     * 
     * This constructor initializes an AliasingState object with no predefined alias
     * relationships
     * and a clean abstract stack, ready for tracking variable aliases during
     * program analysis.
     */
    public AliasingState() {
        this.aliasPairs = new HashSet<>();
        this.abstractStack = new Stack<>();
    }

    /**
     * Creates a copy of an existing AliasingState.
     * 
     * This constructor creates a new AliasingState by deep copying the alias pairs
     * and abstract stack from another AliasingState instance.
     * 
     * @param other The AliasingState to copy from
     */
    public AliasingState(AliasingState other) {
        this.aliasPairs = new HashSet<>(other.aliasPairs);
        this.abstractStack = new Stack<>();
        this.abstractStack.addAll(other.abstractStack);
    }

    public AliasingState copy() {
        return new AliasingState(this);
    }

    public void reset() {
        aliasPairs.clear();
        abstractStack.clear();
    }

    public void addAliasPair(String var1, String var2) {
        if (var1.equals(var2)) {
            return;
        }

        aliasPairs.add(new AliasPair(var1, var2));
        computeTransitiveClosure();
    }

    public void removeAliasesFor(String var) {
        aliasPairs.removeIf(pair -> pair.getVar1().equals(var) || pair.getVar2().equals(var));
    }

    public void removeAliasPair(String var1, String var2) {
        aliasPairs.removeIf(pair -> (pair.getVar1().equals(var1) && pair.getVar2().equals(var2)) ||
                (pair.getVar1().equals(var2) && pair.getVar2().equals(var1)));
    }

    void computeTransitiveClosure() {
        Set<AliasPair> newAliases = new HashSet<>();
        boolean changed;

        do {
            changed = false;
            for (AliasPair pair1 : aliasPairs) {
                for (AliasPair pair2 : aliasPairs) {
                    AliasPair newPair = null;

                    // Case 1: (a,b) and (b,c) -> (a,c)
                    if (pair1.getVar2().equals(pair2.getVar1())) {
                        newPair = new AliasPair(pair1.getVar1(), pair2.getVar2());
                    }

                    // Case 2: (a,b) and (c,b) -> (a,c)
                    else if (pair1.getVar2().equals(pair2.getVar2())) {
                        newPair = new AliasPair(pair1.getVar1(), pair2.getVar1());
                    }

                    // Case 3: (b,a) and (b,c) -> (a,c)
                    else if (pair1.getVar1().equals(pair2.getVar1())) {
                        newPair = new AliasPair(pair1.getVar2(), pair2.getVar2());
                    }

                    // Case 4: (b,a) and (c,b) -> (c,a)
                    else if (pair1.getVar1().equals(pair2.getVar2())) {
                        newPair = new AliasPair(pair1.getVar2(), pair2.getVar1());
                    }

                    if (newPair != null && !aliasPairs.contains(newPair)
                            && !newPair.getVar1().equals(newPair.getVar2())) {
                        newAliases.add(newPair);
                        changed = true;
                    }
                }
            }

            aliasPairs.addAll(newAliases);
            newAliases.clear();
        } while (changed);
    }

    public Set<AliasPair> getAliasPairs() {
        return new HashSet<>(aliasPairs);
    }

    public boolean areAliases(String var1, String var2) {
        if (var1.equals(var2)) {
            return true;
        }

        return aliasPairs.contains(new AliasPair(var1, var2)) ||
                aliasPairs.contains(new AliasPair(var2, var1));
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

    public boolean isStackEmpty() {
        return abstractStack.isEmpty();
    }

    private String getStackElement(int index) {
        if (index < 0 || index >= abstractStack.size()) {
            throw new IndexOutOfBoundsException("Invalid stack index: " + index);
        }

        return abstractStack.get(0);
    }

    public String formatForLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (AliasPair pair : aliasPairs) {
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
        return "AliasingState{" +
                "aliasPairs=" + formatForLabel() +
                ", stack=" + abstractStack +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AliasingState other = (AliasingState) obj;

        if (abstractStack.size() != other.abstractStack.size())
            return false;
        for (int i = 0; i < abstractStack.size(); i++) {
            if (!getStackElement(i).equals(other.getStackElement(i))) {
                return false;
            }
        }

        return aliasPairs.equals(other.aliasPairs);
    }

    @Override
    public int hashCode() {
        int result = aliasPairs.hashCode();
        result = 31 * result + abstractStack.hashCode();
        return result;
    }

    /**
     * Creates a deep copy of this AliasingState.
     *
     * This method creates a new AliasingState instance with deep copies of all
     * alias pairs
     * and abstract stack elements from the current instance. Modifications to the
     * returned instance or its contents will not affect the original.
     *
     * @return A new AliasingState instance that is a deep copy of this one
     */
    public AliasingState deepCopy() {
        AliasingState copy = new AliasingState();

        // Deep copy of alias pairs
        for (AliasPair pair : this.aliasPairs) {
            copy.aliasPairs.add(new AliasPair(pair.getVar1(), pair.getVar2()));
        }

        // Deep copy of stack (String is immutable, so no need to clone strings)
        copy.abstractStack.addAll(this.abstractStack);

        return copy;
    }

    public AliasingState union(AliasingState other) {
        AliasingState result = new AliasingState();

        result.aliasPairs.addAll(this.aliasPairs);
        result.aliasPairs.addAll(other.aliasPairs);

        result.abstractStack.addAll(this.abstractStack);
        for (String var : other.abstractStack) {
            if (!result.abstractStack.contains(var)) {
                result.abstractStack.add(var);
            }
        }

        result.computeTransitiveClosure();
        return result;
    }

    public AliasingState intersection(AliasingState other) {
        AliasingState result = new AliasingState();

        for (AliasPair pair : this.aliasPairs) {
            if (other.aliasPairs.contains(pair)) {
                result.aliasPairs.add(pair);
            }
        }

        for (String var : this.abstractStack) {
            if (other.abstractStack.contains(var)) {
                result.abstractStack.add(var);
            }
        }

        return result;
    }

    public Set<String> getAliasesOf(String var) {
        Set<String> aliases = new HashSet<>();
        Set<AliasPair> pairs = getAliasPairs();

        for (AliasPair pair : pairs) {
            if (pair.getVar1().equals(var)) {
                aliases.add(pair.getVar2());
            } else if (pair.getVar2().equals(var)) {
                aliases.add(pair.getVar1());
            }
        }

        return aliases;
    }
}