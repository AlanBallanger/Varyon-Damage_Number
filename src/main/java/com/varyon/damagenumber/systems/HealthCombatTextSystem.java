package com.varyon.damagenumber.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.damagenumber.DamageNumberPlugin;
import com.varyon.damagenumber.combat.CombatTextBroadcaster;
import com.varyon.damagenumber.config.DamageNumberConfig;
import com.varyon.damagenumber.dot.DotDamageEffectClassifier;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HealthCombatTextSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Query<EntityStore> query;
    private final DamageNumberPlugin plugin;
    private final Map<Ref<EntityStore>, Float> previousHealth = new HashMap<>();

    public HealthCombatTextSystem(DamageNumberPlugin plugin) {
        this.plugin = plugin;
        this.query = Query.and(EntityStatMap.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return null;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {
        DamageNumberConfig config = plugin.config();
        EntityStatMap statMap = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        if (!entityRef.isValid()) {
            previousHealth.remove(entityRef);
            return;
        }
        int healthStatIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthStatIndex);
        if (healthStat == null) {
            return;
        }
        float currentHealth = healthStat.get();
        Float prevHealth = previousHealth.get(entityRef);
        if (prevHealth != null && prevHealth > 0.0f) {
            float healAmount = currentHealth - prevHealth;
            if (config.isHealingEnabled() && healAmount > config.getHealingMinimum()) {
                String text = config.getHealingPrefix() + Math.round(healAmount);
                CombatTextBroadcaster.sendCombatText(
                        entityRef, text, 0.0f, transform.getPosition(), store, commandBuffer);
            } else if (config.isDotNumbersEnabled() && healAmount < -0.1f) {
                float damage = prevHealth - currentHealth;
                if (damage > 0.1f && damage <= config.getDotDamageThreshold()) {
                    EffectControllerComponent fx = archetypeChunk.getComponent(
                            index, EffectControllerComponent.getComponentType());
                    DamageNumberStyle dotStyle = DotDamageEffectClassifier.classify(fx);
                    if (dotStyle == null) {
                        dotStyle = DamageNumberStyle.BLEED_DOT;
                    }
                    int shown = Math.min(Math.round(damage), 100);
                    String text = Integer.toString(shown);
                    LOGGER.at(Level.INFO).log(String.format(
                            "[Varyon-Damage_Number][DOT] show dmg=%s style=%s maxThresh=%s",
                            shown,
                            dotStyle.name(),
                            config.getDotDamageThreshold()));
                    CombatTextBroadcaster.sendCombatText(
                            entityRef,
                            text,
                            0.0f,
                            transform.getPosition(),
                            store,
                            commandBuffer,
                            dotStyle);
                } else if (damage > 0.1f) {
                    LOGGER.at(Level.INFO).log(String.format(
                            "[Varyon-Damage_Number][DOT] skip dmg=%s >= DotDamageThreshold %s",
                            damage,
                            config.getDotDamageThreshold()));
                }
            }
        }
        previousHealth.put(entityRef, currentHealth);
    }

    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }
}
