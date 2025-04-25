package fr.univreunion.bcterm.analysis.sharing;

/**
 * Represents an immutable pair of variables with a consistent ordering.
 * 
 * This class ensures that the two variables are always stored in a sorted
 * order,
 * providing consistent comparison and hash code generation for use in
 * collections
 * and comparisons.
 */
public class SharingPair {
    private final String var1;
    private final String var2;

    /**
     * Constructs a SharingPair with two variables.
     * 
     * @param var1 the first variable to be stored
     * @param var2 the second variable to be stored
     */
    public SharingPair(String var1, String var2) {
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SharingPair other = (SharingPair) obj;
        return var1.equals(other.var1) && var2.equals(other.var2);
    }

    @Override
    public int hashCode() {
        return var1.hashCode() * 42 + var2.hashCode();
    }

    @Override
    public String toString() {
        return "(" + var1 + "," + var2 + ")";
    }

}
