package com.varyon.damagenumber.meta;

import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;

public final class DamageNumberMeta {

    public static final MetaKey<DamageNumberStyle> STYLE_KEY = Damage.META_REGISTRY.registerMetaObject();
    public static final MetaKey<Float> SAVED_AMOUNT_KEY = Damage.META_REGISTRY.registerMetaObject();

    private DamageNumberMeta() {}

    public static void applyOutgoingAttackStyle(Damage damage, boolean wasCrit, boolean magic) {
        if (damage == null) {
            return;
        }
        if (wasCrit) {
            damage.getMetaStore().putMetaObject(STYLE_KEY, DamageNumberStyle.CRIT);
        } else if (magic) {
            damage.getMetaStore().putMetaObject(STYLE_KEY, DamageNumberStyle.MAGIC);
        } else {
            damage.getMetaStore().putMetaObject(STYLE_KEY, DamageNumberStyle.NORMAL);
        }
    }
}
