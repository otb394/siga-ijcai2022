package lcs;

import base.Context;

public interface MutationStrategy {
    Rule mutate(Rule rule, Context situation);
}
