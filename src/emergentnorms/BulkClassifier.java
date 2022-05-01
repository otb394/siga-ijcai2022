package emergentnorms;

import weka.core.Instances;

public interface BulkClassifier {
    Instances classifyAll(Instances instances);
}
