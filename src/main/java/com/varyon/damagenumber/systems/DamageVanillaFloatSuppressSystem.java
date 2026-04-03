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
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entityui.EntityUIModule;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.damagenumber.meta.DamageNumberMeta;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DamageVanillaFloatSuppressSystem extends DamageEventSystem {

    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies = Set.of(
            new SystemDependency<>(Order.AFTER, DamageSystems.ReticleEvents.class),
            new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class));

    public DamageVanillaFloatSuppressSystem() {
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
        if (damage.getAmount() <= 0.0f) {
            return;
        }
        DamageNumberStyle style = damage.getIfPresentMetaObject(DamageNumberMeta.STYLE_KEY);
        if (style == null) {
            Damage.Source src = damage.getSource();
            if (src instanceof Damage.EntitySource es) {
                Ref<EntityStore> ref = es.getRef();
                if (ref != null && ref.isValid()) {
                    PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
                    if (pr != null && pr.isValid()) {
                        style = DamageNumberStyle.NORMAL;
                        damage.getMetaStore().putMetaObject(DamageNumberMeta.STYLE_KEY, style);
                    }
                }
            }
        }
        if (style == null) {
            return;
        }
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource)) {
            return;
        }
        Damage.EntitySource entitySource = (Damage.EntitySource) source;
        if (!entitySource.getRef().isValid()) {
            return;
        }
        damage.getMetaStore().putMetaObject(DamageNumberMeta.SAVED_AMOUNT_KEY, damage.getAmount());
        damage.setAmount(0.0f);
    }
}
