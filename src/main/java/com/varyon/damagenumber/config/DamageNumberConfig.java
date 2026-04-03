package com.varyon.damagenumber.config;

import com.google.gson.annotations.SerializedName;

public final class DamageNumberConfig {

    @SerializedName("HealingEnabled")
    public boolean healingEnabled = true;

    @SerializedName("HealingFontSize")
    public double healingFontSize = 48.0;

    @SerializedName("HealingMinimum")
    public double healingMinimum = 0.5;

    @SerializedName("HealingPrefix")
    public String healingPrefix = "+";

    @SerializedName("DotNumbersEnabled")
    public boolean dotNumbersEnabled = true;

    @SerializedName("DotTickIntervalSeconds")
    public double dotTickIntervalSeconds = 0.5;

    @SerializedName("DotFontSize")
    public double dotFontSize = 36.0;

    @SerializedName("DotDamageThreshold")
    public double dotDamageThreshold = 100.0;

    public boolean isHealingEnabled() {
        return healingEnabled;
    }

    public int getHealingFontSize() {
        return (int) healingFontSize;
    }

    public float getHealingMinimum() {
        return (float) healingMinimum;
    }

    public String getHealingPrefix() {
        return healingPrefix;
    }

    public boolean isDotNumbersEnabled() {
        return dotNumbersEnabled;
    }

    public float getDotTickIntervalSeconds() {
        return (float) dotTickIntervalSeconds;
    }

    public int getDotFontSize() {
        return (int) dotFontSize;
    }

    public float getDotDamageThreshold() {
        return (float) dotDamageThreshold;
    }
}
