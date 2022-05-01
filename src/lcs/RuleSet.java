package lcs;

import base.Action;
import base.Context;
import base.Norm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleSet {
    private Map<Norm, Rule> ruleMap;
    private Runnable addCallback;

    public RuleSet(Runnable addCallback) {
        this.ruleMap = new LinkedHashMap<>();
        this.addCallback = addCallback;
    }

    public RuleSet() {
        this.ruleMap = new LinkedHashMap<>();
    }

    public RuleSet(List<Rule> rules, Runnable addCallback) {
        this.addCallback = addCallback;
        this.ruleMap = new LinkedHashMap<>();
        for (Rule rule : rules) {
            this.ruleMap.put(rule.norm, rule);
        }
    }

    public RuleSet(List<Rule> rules) {
        this.ruleMap = new LinkedHashMap<>();
        for (Rule rule : rules) {
            this.ruleMap.put(rule.norm, rule);
        }
    }

    public RuleSet getMatchSet(Context context) {
        List<Rule> matchingRules = new ArrayList<>();
        for (Map.Entry<Norm, Rule> entry : ruleMap.entrySet()) {
            if (entry.getKey().triggers(context)) {
                matchingRules.add(entry.getValue());
            }
        }
        return new RuleSet(matchingRules);
    }

    public RuleSet getActionSet(Action action) {
        List<Rule> actionRules = new ArrayList<>();
        for (Map.Entry<Norm, Rule> entry : ruleMap.entrySet()) {
            if (entry.getKey().consequent == action) {
                actionRules.add(entry.getValue());
            }
        }
        return new RuleSet(actionRules);
    }

    public void addRule(Rule rule) {
        Rule existingRule = ruleMap.get(rule.norm);
        if (existingRule == null) {
            ruleMap.put(rule.norm, rule);
        } else {
            existingRule.numerosity += rule.numerosity;
        }
        if (addCallback != null) {
            addCallback.run();
        }
    }

    public void addAll(List<Rule> rules) {
        for (Rule rule : rules) {
            addRule(rule);
        }
    }

    public boolean removeRule(Rule rule) {
        return removeRule(rule, 1);
    }

    public boolean removeRule(Rule rule, int count) {
        Rule existingRule = ruleMap.get(rule.norm);
        if (existingRule == null) {
            return false;
        }
        existingRule.numerosity -= count;
        if (existingRule.numerosity <= 0) {
            existingRule.numerosity = 0;
            ruleMap.remove(existingRule.norm);
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return ruleMap.isEmpty();
    }

    public List<Rule> getRules() {
        return new ArrayList<>(this.ruleMap.values());
    }

    public double getAvgExperienceSinceRD() {
        List<Rule> rules = getRules();
        int totalExperience = 0;
        int totalNumerosity = 0;
        for (Rule rule : rules) {
            totalExperience += rule.experienceSinceRD * rule.numerosity;
            totalNumerosity += rule.numerosity;
        }
        return totalNumerosity == 0 ? 0.0 : (((double)totalExperience) / totalNumerosity);
    }

    public boolean contains(Rule rule) {
        return ruleMap.containsKey(rule.norm);
    }

    public void resetExperienceSinceRD() {
        for (Map.Entry<Norm, Rule> entry : ruleMap.entrySet()) {
            entry.getValue().resetExperienceSinceRD();
        }
    }

    public void print() {
        System.out.printf("Population with %d unique rules\n", ruleMap.size());
        List<Rule> rules = getRules();
        rules.sort(Comparator.comparing(Rule::getRewardPrediction).reversed());
        for (Rule rule : rules) {
            System.out.println(rule.toString(false));
        }
    }

    //Union Find is the best solution for this. But using nested loops for now.
    public void subsumeAll() {
        List<Rule> rules = getRules();
        int ruleSize = rules.size();
        for (int i = 0; i < ruleSize; i++) {
            Rule iRule = rules.get(i);
            if (!this.ruleMap.containsKey(iRule.getNorm())) continue;
            for (int j = 0; j < ruleSize; j++) {
                if (i == j) continue;
                Rule jRule = rules.get(j);
                if (!this.ruleMap.containsKey(jRule.getNorm())) continue;
                if (iRule.subsumes(jRule)) this.ruleMap.remove(jRule.getNorm());
            }
        }
    }
}

