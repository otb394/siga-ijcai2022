package base;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a norm
 */
public class Norm {
    private Predicate<Context> antecedent;
    public Action consequent;
    private Context conditions;

    public Norm(Context conditions, Action consequent) {
        this.conditions = conditions;
        this.consequent = consequent;
        this.antecedent = getAntecedent(this.conditions);
    }

    private static Predicate<Context> getAntecedent(Context conditions) {
        return context -> {
            if (conditions.time != null && conditions.time != context.time)  return false;
            if (conditions.calleeLocation != null && conditions.calleeLocation != context.calleeLocation)  return false;
            if (conditions.callerLocation != null && conditions.callerLocation != context.callerLocation)  return false;
            if (conditions.callerRelationship != null && conditions.callerRelationship != context.callerRelationship)
                return false;
            if (conditions.activity != null && conditions.activity != context.activity)  return false;
            if (conditions.callUrgency != null && !conditions.callUrgency.equals(context.callUrgency))  return false;
            return true;
        };
    }

    public boolean triggers(Context context) {
        return antecedent.test(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Norm norm = (Norm) o;
        return consequent == norm.consequent &&
                Objects.equals(conditions, norm.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consequent, conditions);
    }

    @Override
    public String toString() {
        return "Norm{" +
                "consequent=" + consequent +
                "; conditions=" + conditions +
                '}';
    }

    public Context getConditions() {
        return conditions;
    }

    public void setConditions(Context conditions) {
        this.conditions = conditions;
        this.antecedent = getAntecedent(this.conditions);
    }

    public boolean subsumes(Norm other) {
        return this.triggers(other.getConditions()) && this.consequent == other.consequent;
    }
}
