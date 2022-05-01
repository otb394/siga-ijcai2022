package util;

import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.List;

public class Randomizer {
    public MersenneTwisterFast random;

    public Randomizer(MersenneTwisterFast random) {
        this.random = random;
    }

    public <T> List<T> sample(List<T> list, int size, int k) {
        if (k > size) throw new IllegalArgumentException();
        List<T> sample = new ArrayList<>(list);
        for (int i = 0; i < k; i++) {
            int randInd = getRandom(i, size);
            swap(sample, i, randInd);
        }
        return sample.subList(0, k);
    }

    private <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * Random number in the given range.
     *
     * @param start Start of the range, inclusive.
     * @param end End of the range, exclusive
     * @return Random number in [start, end)
     */
    public int getRandom(int start, int end) {
        return start + random.nextInt(end - start);
    }

    /**
     * Random element chosen from the list, as per the given weights
     * Note: Investigate problems with floating point precision error.
     *
     * @param list List of elements
     * @param weights Probability of each element of the list to be chosen
     * @param <T> Type of the list element
     * @return Chosen element
     */
    public <T> T choice(List<T> list, double[] weights) {
        List<Double> cumulativeWeights = new ArrayList<>();
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
            cumulativeWeights.add(sum);
        }
        double randomIndex = random.nextDouble() * sum;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            double cumW = cumulativeWeights.get(i);
            if (randomIndex < cumW) {
                return list.get(i);
            }
        }
        Debugger.debug(weights, "weights", sum, "sum", cumulativeWeights, "cumWeights", randomIndex, "randomIndex");
        throw new RuntimeException();
    }

    /**
     * Random element chosen from the list, as per the given weights
     *
     * @param list List of elements
     * @param weights Probability of each element of the list to be chosen
     * @param <T> Type of the list element
     * @return Chosen element
     */
    public <T> T choice(List<T> list, int[] weights) {
        List<Integer> cumulativeWeights = new ArrayList<>();
        int sum = 0;
        for (int w : weights) {
            sum += w;
            cumulativeWeights.add(sum);
        }
        int randomIndex = random.nextInt(sum);
        int size = list.size();
        for (int i = 0; i < size; i++) {
            int cumW = cumulativeWeights.get(i);
            if (randomIndex < cumW) {
                return list.get(i);
            }
        }
        throw new RuntimeException();
    }
}
