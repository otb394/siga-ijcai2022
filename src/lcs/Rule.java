package lcs;

import base.Norm;

import java.util.List;
import java.util.Objects;

public class Rule {
    public Norm norm;
    public double errorOfPrediction;
    public double rewardPrediction;
    public int numerosity;
    public int experienceSinceRD;
    public int actionSetSize;

    /**
     * The number of times the rule appears in the action set
     */
    public int experience;

    /**
     * This is a measure of accuracy of prediction of reward
     */
    public double fitness;

    public Rule(Norm norm) {
        this.norm = norm;
        this.errorOfPrediction = 0.00001;
        this.rewardPrediction = 0.00001;
        this.fitness = 0.00001;
        this.numerosity = 1;
        this.experience = 0;
        this.experienceSinceRD = 0;
        this.actionSetSize = 1;
    }

    public Rule(Rule from) {
        this.norm = from.norm;
        this.errorOfPrediction = from.errorOfPrediction;
        this.rewardPrediction = from.rewardPrediction;
        this.fitness = from.fitness;
        this.numerosity = from.numerosity;
        this.experience = from.experience;
        this.experienceSinceRD = from.experienceSinceRD;
        this.actionSetSize = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(norm, rule.norm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(norm);
    }

    public int getNumberOfKnownBits() {
        int knownBits = 0;
        List<Object> vector = this.norm.getConditions().getVector();
        for (Object element : vector) {
            if (element != null) knownBits++;
        }
        return knownBits;
    }

    public void incrementExperience() {
        this.experience++;
        this.experienceSinceRD++;
    }

    public void resetExperienceSinceRD() {
        this.experienceSinceRD = 0;
    }

    public void setNorm(Norm norm) {
        this.norm = norm;
    }

    public Norm getNorm() {
        return norm;
    }

    public String toString(boolean withStats) {
        return String.format("[Norm: %s, prediction: %f, numerosity: %d]", norm, rewardPrediction, numerosity);
    }

    public double getRewardPrediction() {
        return rewardPrediction;
    }

    public boolean subsumes(Rule other) {
        return this.norm.subsumes(other.norm);
//        return this.norm.triggers(other.norm.getConditions()) && this.norm.consequent == other.norm.consequent;
    }
}
