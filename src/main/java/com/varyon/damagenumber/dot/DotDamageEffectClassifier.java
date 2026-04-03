package com.varyon.damagenumber.dot;

import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import javax.annotation.Nullable;

public final class DotDamageEffectClassifier {

    private static final String METEORE_FIRE = "Varyon_Meteore_Fire";
    private static final String POINTE_POISON = "Varyon_Pointe_Poison";
    private static final String POISON_PARALYSANT = "Varyon_Poison_Paralysant";
    private static final String ENTAILLE_BLEED = "Varyon_Entaille_Bleed";

    private DotDamageEffectClassifier() {}

    @Nullable
    public static DamageNumberStyle classify(EffectControllerComponent effectController) {
        if (effectController == null) {
            return null;
        }
        int[] indexes = effectController.getActiveEffectIndexes();
        if (indexes == null || indexes.length == 0) {
            return null;
        }
        boolean poison = false;
        boolean bleed = false;
        boolean fire = false;
        for (int idx : indexes) {
            EntityEffect eff = EntityEffect.getAssetMap().getAsset(idx);
            if (eff == null) {
                continue;
            }
            String id = eff.getId();
            if (id == null) {
                continue;
            }
            if (METEORE_FIRE.equals(id)) {
                fire = true;
            } else if (POINTE_POISON.equals(id) || POISON_PARALYSANT.equals(id)) {
                poison = true;
            } else if (ENTAILLE_BLEED.equals(id)) {
                bleed = true;
            }
        }
        if (fire) {
            return DamageNumberStyle.FIRE_DOT;
        }
        if (poison) {
            return DamageNumberStyle.POISON_DOT;
        }
        if (bleed) {
            return DamageNumberStyle.BLEED_DOT;
        }
        return null;
    }
}
