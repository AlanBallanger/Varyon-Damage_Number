package com.varyon.damagenumber.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entityui.EntityUIModule;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.damagenumber.combat.CombatTextColorHelper;
import com.varyon.damagenumber.meta.DamageNumberMeta;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DamageColoredFloatSystem extends DamageEventSystem {

    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies =
            Set.of(new SystemDependency<>(Order.AFTER, DamageSystems.EntityUIEvents.class));

    public DamageColoredFloatSystem() {
        this.query = Query.and(
                EntityModule.get().getVisibleComponentType(),
                EntityUIModule.get().getUIComponentListType());
    }

    @Override
    @Nullable
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        Float saved = damage.getIfPresentMetaObject(DamageNumberMeta.SAVED_AMOUNT_KEY);
        if (saved == null) {
            return;
        }
        DamageNumberStyle style = damage.getIfPresentMetaObject(DamageNumberMeta.STYLE_KEY);
        damage.getMetaStore().removeMetaObject(DamageNumberMeta.SAVED_AMOUNT_KEY);
        damage.getMetaStore().removeMetaObject(DamageNumberMeta.STYLE_KEY);
        damage.setAmount(saved);

        if (style == null) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }
        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (!sourceRef.isValid()) {
            return;
        }
        PlayerRef sourcePlayerRef = commandBuffer.getComponent(sourceRef, PlayerRef.getComponentType());
        if (sourcePlayerRef == null || !sourcePlayerRef.isValid()) {
            return;
        }
        EntityTrackerSystems.EntityViewer viewer =
                commandBuffer.getComponent(sourceRef, EntityTrackerSystems.EntityViewer.getComponentType());
        if (viewer == null) {
            return;
        }

        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);
        Float hitAngleDeg = damage.getIfPresentMetaObject(Damage.HIT_ANGLE);
        int shown = (int) Math.floor(saved);
        String plain = Integer.toString(shown);

        UIComponentList uiList = commandBuffer.getComponent(targetRef, UIComponentList.getComponentType());
        if (uiList == null || !CombatTextColorHelper.canApplyColor(uiList)) {
            CombatTextUpdate update =
                    new CombatTextUpdate(hitAngleDeg == null ? 0.0f : hitAngleDeg.floatValue(), plain);
            viewer.queueUpdate(targetRef, update);
            return;
        }

        CombatTextColorHelper.applyStyleSendAndScheduleRestore(
                targetRef,
                uiList,
                style,
                store.getExternalData().getWorld(),
                store,
                () -> {
                    CombatTextUpdate update =
                            new CombatTextUpdate(
                                    hitAngleDeg == null ? 0.0f : hitAngleDeg.floatValue(), plain);
                    viewer.queueUpdate(targetRef, update);
                },
                List.of(viewer));
    }
}
