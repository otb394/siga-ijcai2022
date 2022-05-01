package rulebasedrl;

import base.Norm;

public class NormEntry {
    public Norm norm;
    public double weight;
    private boolean fixed;

    public NormEntry(Norm norm) {
        this.norm = norm;
        this.weight = 0.0;
        this.fixed = false;
    }

    public NormEntry(Norm norm, double weight) {
        this.norm = norm;
        this.weight = weight;
        this.fixed = false;
    }

    public NormEntry(Norm norm, double weight, boolean fixed) {
        this.norm = norm;
        this.weight = weight;
        this.fixed = fixed;
    }

    public void updateWeight(double newWeight) {
        if (!fixed) {
            this.weight = newWeight;
        }
    }

    @Override
    public String toString() {
        return "NormEntry{" +
                "norm=" + norm +
                ", weight=" + weight +
                ", fixed=" + fixed +
                '}';
    }
}
