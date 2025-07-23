package com.example.demo.application;

public enum Env {
    LOCAL("local"),
    DEVELOPMENT("dev"),
    QUALITY("qual"),
    PRODUCTION("prod");

    private String value;

    Env(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Env getEnv(String value) {
        for (Env env : Env.values()) {
            if (env.getValue().equalsIgnoreCase(value) || env.name().equalsIgnoreCase(value)) {
                return env;
            }
        }
        return null;
    }
}
