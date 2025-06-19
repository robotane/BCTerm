package fr.univreunion.bcterm.analysis.cyclicity;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicityState {
    private final Set<CyclicVariable> possiblyCyclicVariables;
    private final Stack<String> abstractStack;

    public CyclicityState() {
        this.possiblyCyclicVariables = new HashSet<>();
        this.abstractStack = new Stack<>();
    }

    public CyclicityState(CyclicityState other) {
        this.possiblyCyclicVariables = new HashSet<>(other.possiblyCyclicVariables);
        this.abstractStack = new Stack<>();
        this.abstractStack.addAll(other.abstractStack);
    }

    public void addPossiblyCyclic(String variable) {
        possiblyCyclicVariables.add(new CyclicVariable(variable));
    }

    public void removePossiblyCyclic(String variable) {
        possiblyCyclicVariables.remove(new CyclicVariable(variable));
    }

    public boolean isPossiblyCyclic(String variable) {
        return possiblyCyclicVariables.contains(new CyclicVariable(variable));
    }

    public Set<CyclicVariable> getCyclicVariables() {
        return new HashSet<>(possiblyCyclicVariables);
    }

    public Set<CyclicVariable> getPossiblyCyclicVariables() {
        return possiblyCyclicVariables;
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

    public void reset() {
        possiblyCyclicVariables.clear();
        abstractStack.clear();
    }

    public CyclicityState deepCopy() {
        return new CyclicityState(this);
    }

    public CyclicityState union(CyclicityState other) {
        CyclicityState result = new CyclicityState();
        result.possiblyCyclicVariables.addAll(this.possiblyCyclicVariables);
        result.possiblyCyclicVariables.addAll(other.possiblyCyclicVariables);

        result.abstractStack.addAll(this.abstractStack);
        for (String var : other.abstractStack) {
            if (!result.abstractStack.contains(var)) {
                result.abstractStack.add(var);
            }
        }

        return result;
    }

    public String formatForLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (CyclicVariable cyclicVar : possiblyCyclicVariables) {
            if (!first) {
                sb.append(",");
            }
            sb.append(cyclicVar.getVariableName());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CyclicityState{" +
                "possiblyCyclic=" + formatForLabel() +
                ", stack=" + abstractStack +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        CyclicityState other = (CyclicityState) obj;
        return possiblyCyclicVariables.equals(other.possiblyCyclicVariables) &&
                abstractStack.equals(other.abstractStack);
    }

    @Override
    public int hashCode() {
        return possiblyCyclicVariables.hashCode() * 31 + abstractStack.hashCode();
    }
}