package fr.univreunion.bcterm.analysis.aliasing;

import java.util.Objects;

public class AliasPair {
    private final String var1;
    private final String var2;

    /**
     * Constructs an AliasPair with two variables, ensuring a consistent ordering.
     * 
     * @param var1 the first variable to be compared
     * @param var2 the second variable to be compared
     * 
     *             Variables are ordered lexicographically to maintain consistent
     *             equality checks
     *             regardless of the input order.
     */
    public AliasPair(String var1, String var2) {
        // Ensure consistent ordering for equality checks
        if (var1.compareTo(var2) <= 0) {
            this.var1 = var1;
            this.var2 = var2;
        } else {
            this.var1 = var2;
            this.var2 = var1;
        }
    }

    public String getVar1() {
        return var1;
    }

    public String getVar2() {
        return var2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AliasPair aliasPair = (AliasPair) o;
        return Objects.equals(var1, aliasPair.var1) &&
                Objects.equals(var2, aliasPair.var2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var1, var2);
    }

    @Override
    public String toString() {
        return "(" + var1 + "," + var2 + ")";
    }
}
