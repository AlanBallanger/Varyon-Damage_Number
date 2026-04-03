package com.varyon.damagenumber.meta;

public enum DamageNumberStyle {
    NORMAL("Varyon_CombatText_Normal", "#FFFFFF"),
    CRIT("Varyon_CombatText_Crit", "#E99204"),
    MAGIC("Varyon_CombatText_Magic", "#63FFEC"),
    POISON_DOT("Varyon_CombatText_PoisonDot", "#47B701"),
    FIRE_DOT("Varyon_CombatText_FireDot", "#FCCF8F"),
    BLEED_DOT("Varyon_CombatText_BleedDot", "#B90F15");

    private final String combatTextAssetId;
    private final String hex;

    DamageNumberStyle(String combatTextAssetId, String hex) {
        this.combatTextAssetId = combatTextAssetId;
        this.hex = hex;
    }

    public String getCombatTextAssetId() {
        return combatTextAssetId;
    }

    public String getHex() {
        return hex;
    }
}
