package rulebasedrl;

import java.util.List;

public interface NormProvider {
    List<NormEntry> provide();
}
