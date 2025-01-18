package com.bisson2000.largepatchgenerator.utils;

import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.stream.IntStream;

public class ModRandomExtension {

    /**
     * Puts the heavy weighted values at the front
     * */
    public static <T> List<T> weightedShuffle(RandomSource randomSource, List<T> list, List<Double> weights) {
        if (list.size() <= 1) {
            return list;
        }

        List<Integer> rangeList = new ArrayList<>(IntStream.rangeClosed(0, list.size() - 1).boxed().toList());
        rangeList.sort((o1, o2) -> {
            double lhs = Math.pow(randomSource.nextDouble(), 1.0 / weights.get(o1));
            double rhs = Math.pow(randomSource.nextDouble(), 1.0 / weights.get(o2));
            if (lhs > rhs) {
                return -1;
            } else if (lhs == rhs) {
                return 0;
            }
            return 1;
        });

        List<T> res = new ArrayList<>();
        for (int i : rangeList) {
            res.add(list.get(i));
        }
        return res;
    }

}
