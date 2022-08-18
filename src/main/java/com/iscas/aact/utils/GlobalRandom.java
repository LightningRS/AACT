package com.iscas.aact.utils;

import java.util.Random;

public class GlobalRandom {
    public static Random getInstance() {
        return new Random(Config.getInstance().getSeed());
    }
}
