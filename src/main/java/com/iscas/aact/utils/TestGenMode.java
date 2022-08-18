package com.iscas.aact.utils;

public enum TestGenMode {
    /**
     * Generate testcases automatically if not exits
     */
    AUTO,

    /**
     * Only generate testcases (without running)
     */
    ONLY,

    /**
     * Do not generate testcases if not exists
     */
    NONE
}
