package emergentnorms;

import sim.util.Bag;
import simulation.Call;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

public class CallsBasedInstanceGenerator implements InstanceGenerator {
    private List<Call> calls;

    public CallsBasedInstanceGenerator(Bag calls) {
        this.calls = new ArrayList<>();
        int size = calls.size();
        for (int i = 0; i < size; i++) {
            this.calls.add((Call)calls.get(i));
        }
    }

    @Override
    public Instances getInstances() {
        return null;
    }
}
