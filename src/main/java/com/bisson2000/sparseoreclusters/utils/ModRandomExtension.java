package com.bisson2000.sparseoreclusters.utils;

import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.stream.IntStream;

public class ModRandomExtension {

    /**
     * Puts the heavy weighted values at the front
     * */
    public static <T> List<T> weightedShuffle(List<T> list, List<Double> weights) {
        if (list.size() <= 1) {
            return list;
        }

        List<Integer> rangeList = new ArrayList<>(IntStream.rangeClosed(0, list.size() - 1).boxed().toList());
        rangeList.sort(Comparator.comparingDouble(weights::get));

        List<T> res = new ArrayList<>();
        for (int i : rangeList) {
            res.add(list.get(i));
        }
        return res;
    }

}
