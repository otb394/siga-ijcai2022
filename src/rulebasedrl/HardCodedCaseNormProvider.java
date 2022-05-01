package rulebasedrl;

import base.Action;
import base.Context;
import base.Location;
import base.Norm;
import base.Relationship;

import java.util.ArrayList;
import java.util.List;

public class HardCodedCaseNormProvider implements NormProvider {
    @Override
    public List<NormEntry> provide() {
        List<NormEntry> initialNorms = new ArrayList<>();
        initialNorms.add(new NormEntry(new Norm(Context.builder().calleeLocation(Location.LIBRARY).build(),
                Action.IGNORE)));
        initialNorms.add(new NormEntry(new Norm(Context.builder().calleeLocation(Location.MEETING).build(),
                Action.IGNORE)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.STRANGER)
                        .callUrgency(false).build(),
                Action.IGNORE), 10000, true));
        initialNorms.add(new NormEntry(new Norm(Context.builder().callUrgency(true).build(), Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.SIGNIFICANT_OTHER).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.RELATIVE).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.FAMILY).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.FRIEND).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.COLLEAGUE).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().callerRelationship(Relationship.CLASSMATE).build(),
                Action.RING)));
        initialNorms.add(new NormEntry(new Norm(
                Context.builder().build(), Action.RING)));
        return initialNorms;
    }
}
