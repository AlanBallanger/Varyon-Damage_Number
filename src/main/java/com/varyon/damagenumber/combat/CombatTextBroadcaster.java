package com.varyon.damagenumber.combat;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import java.util.ArrayList;
import java.util.List;

public final class CombatTextBroadcaster {

    private static final double NEARBY_RANGE = 64.0;

    private CombatTextBroadcaster() {}

    public static void sendCombatText(
            Ref<EntityStore> entityRef,
            String text,
            float hitAngle,
            Vector3d position,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer) {
        sendCombatTextPlain(entityRef, text, hitAngle, position, store, commandBuffer);
    }

    public static void sendCombatText(
            Ref<EntityStore> entityRef,
            String text,
            float hitAngle,
            Vector3d position,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            DamageNumberStyle style) {
        if (style == null) {
            sendCombatTextPlain(entityRef, text, hitAngle, position, store, commandBuffer);
            return;
        }
        UIComponentList uiList = commandBuffer.getComponent(entityRef, UIComponentList.getComponentType());
        if (uiList == null || !CombatTextColorHelper.canApplyColor(uiList)) {
            sendCombatTextPlain(entityRef, text, hitAngle, position, store, commandBuffer);
            return;
        }
        List<EntityTrackerSystems.EntityViewer> uiViewers =
                collectCombatTextViewers(entityRef, position, store, commandBuffer);
        CombatTextColorHelper.applyStyleSendAndScheduleRestore(
                entityRef,
                uiList,
                style,
                store.getExternalData().getWorld(),
                store,
                () -> sendCombatTextPlain(entityRef, text, hitAngle, position, store, commandBuffer),
                uiViewers.isEmpty() ? null : uiViewers);
    }

    private static List<EntityTrackerSystems.EntityViewer> collectCombatTextViewers(
            Ref<EntityStore> entityRef,
            Vector3d position,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer) {
        SpatialResource playerSpatial = (SpatialResource) commandBuffer.getResource(
                EntityModule.get().getPlayerSpatialResourceType());
        SpatialStructure spatialStructure = playerSpatial.getSpatialStructure();
        List<Ref<EntityStore>> nearbyPlayers = SpatialResource.getThreadLocalReferenceList();
        spatialStructure.collect(position, NEARBY_RANGE, nearbyPlayers);
        ComponentType viewerType = EntityTrackerSystems.EntityViewer.getComponentType();
        ArrayList<EntityTrackerSystems.EntityViewer> out = new ArrayList<>();
        for (int i = 0; i < nearbyPlayers.size(); ++i) {
            Ref<EntityStore> playerRef = nearbyPlayers.get(i);
            if (!playerRef.isValid()) {
                continue;
            }
            EntityTrackerSystems.EntityViewer viewer = (EntityTrackerSystems.EntityViewer) commandBuffer
                    .getComponent(playerRef, viewerType);
            if (viewer == null || !viewer.visible.contains(entityRef)) {
                continue;
            }
            out.add(viewer);
        }
        return out;
    }

    private static void sendCombatTextPlain(
            Ref<EntityStore> entityRef,
            String text,
            float hitAngle,
            Vector3d position,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer) {
        List<EntityTrackerSystems.EntityViewer> viewers =
                collectCombatTextViewers(entityRef, position, store, commandBuffer);
        for (int i = 0; i < viewers.size(); ++i) {
            EntityTrackerSystems.EntityViewer viewer = viewers.get(i);
            viewer.queueUpdate(entityRef, (ComponentUpdate) new CombatTextUpdate(hitAngle, text));
        }
    }
}
