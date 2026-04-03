package com.varyon.damagenumber.combat;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.EntityViewer;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.modules.entityui.asset.CombatTextUIComponent;
import com.hypixel.hytale.server.core.modules.entityui.asset.EntityUIComponent;
import com.hypixel.hytale.protocol.UIComponentsUpdate;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.varyon.damagenumber.meta.DamageNumberStyle;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;

public final class CombatTextColorHelper {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    static final String DEFAULT_COMBAT_TEXT = "CombatText";
    private static volatile Field componentsField;
    private static volatile Field componentIdsField;
    private static volatile boolean componentsFieldResolved;
    private static volatile boolean componentIdsFieldResolved;

    private CombatTextColorHelper() {}

    private static Field resolveComponentsField() {
        if (componentsFieldResolved) {
            return componentsField;
        }
        synchronized (CombatTextColorHelper.class) {
            if (componentsFieldResolved) {
                return componentsField;
            }
            componentsFieldResolved = true;
            try {
                Field f = UIComponentList.class.getDeclaredField("components");
                f.setAccessible(true);
                componentsField = f;
            } catch (ReflectiveOperationException e) {
                LOGGER.at(Level.WARNING).log(
                        "CombatText UI swap unavailable (field components): {0}", e.getMessage());
                componentsField = null;
            }
            return componentsField;
        }
    }

    private static Field resolveComponentIdsField() {
        if (componentIdsFieldResolved) {
            return componentIdsField;
        }
        synchronized (CombatTextColorHelper.class) {
            if (componentIdsFieldResolved) {
                return componentIdsField;
            }
            componentIdsFieldResolved = true;
            try {
                Field f = UIComponentList.class.getDeclaredField("componentIds");
                f.setAccessible(true);
                componentIdsField = f;
            } catch (ReflectiveOperationException e) {
                LOGGER.at(Level.WARNING).log(
                        "CombatText UI swap: componentIds field unavailable: {0}", e.getMessage());
                componentIdsField = null;
            }
            return componentIdsField;
        }
    }

    public static boolean isColorSwapAvailable() {
        return resolveComponentsField() != null;
    }

    public static boolean canApplyColor(UIComponentList list) {
        Field f = resolveComponentsField();
        if (f == null || list == null) {
            return false;
        }
        try {
            return findCombatTextSlotIndex(list, f) >= 0;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static int findCombatTextSlotIndex(UIComponentList list, Field f) throws IllegalAccessException {
        String[] comps = (String[]) f.get(list);
        if (comps == null) {
            return -1;
        }
        var assetMap = EntityUIComponent.getAssetMap();
        for (int i = 0; i < comps.length; i++) {
            String id = comps[i];
            if (id == null || id.isEmpty()) {
                continue;
            }
            if (DEFAULT_COMBAT_TEXT.equals(id) || id.startsWith("Varyon_CombatText_")) {
                return i;
            }
            int ix = assetMap.getIndex(id);
            if (ix == Integer.MIN_VALUE) {
                continue;
            }
            EntityUIComponent asset = assetMap.getAsset(ix);
            if (asset instanceof CombatTextUIComponent) {
                return i;
            }
        }
        return -1;
    }

    private static void syncComponentIdsFromComponents(UIComponentList list, Field componentsF, Field idsField)
            throws IllegalAccessException {
        String[] comps = (String[]) componentsF.get(list);
        if (comps == null || idsField == null) {
            return;
        }
        int n = comps.length;
        int[] ids = new int[n];
        var assetMap = EntityUIComponent.getAssetMap();
        for (int i = 0; i < n; i++) {
            String key = comps[i];
            if (key == null || key.isEmpty()) {
                ids[i] = 0;
                continue;
            }
            int ix = assetMap.getIndex(key);
            ids[i] = (ix != Integer.MIN_VALUE) ? ix : 0;
        }
        idsField.set(list, ids);
        list.update();
    }

    private static void setSlotAssetId(UIComponentList list, String assetId, Field f) throws IllegalAccessException {
        int idx = findCombatTextSlotIndex(list, f);
        if (idx < 0) {
            return;
        }
        String[] comps = (String[]) f.get(list);
        if (comps == null) {
            return;
        }
        String[] next = comps.clone();
        next[idx] = assetId;
        f.set(list, next);
        Field idsF = resolveComponentIdsField();
        if (idsF != null) {
            syncComponentIdsFromComponents(list, f, idsF);
        } else {
            list.update();
        }
    }

    public static void broadcastUiComponentsForTarget(
            Ref<EntityStore> targetRef, UIComponentList list, Store<EntityStore> store) {
        var visibleType = EntityModule.get().getVisibleComponentType();
        EntityTrackerSystems.Visible visible = store.getComponent(targetRef, visibleType);
        if (visible == null || visible.visibleTo == null || visible.visibleTo.isEmpty()) {
            return;
        }
        broadcastUiComponentsToViewers(targetRef, list, visible.visibleTo.values());
    }

    public static void broadcastUiComponentsToViewers(
            Ref<EntityStore> targetRef,
            UIComponentList list,
            Iterable<EntityViewer> viewers) {
        int[] ids = list.getComponentIds();
        if (ids == null) {
            return;
        }
        UIComponentsUpdate update = new UIComponentsUpdate(ids);
        for (EntityViewer v : viewers) {
            if (v != null) {
                v.queueUpdate(targetRef, update);
            }
        }
    }

    public static void applyStyleSendAndScheduleRestore(
            Ref<EntityStore> targetRef,
            UIComponentList uiList,
            DamageNumberStyle style,
            World world,
            Store<EntityStore> store,
            Runnable sendCombatTextUpdates) {
        applyStyleSendAndScheduleRestore(targetRef, uiList, style, world, store, sendCombatTextUpdates, null);
    }

    public static void applyStyleSendAndScheduleRestore(
            Ref<EntityStore> targetRef,
            UIComponentList uiList,
            DamageNumberStyle style,
            World world,
            Store<EntityStore> store,
            Runnable sendCombatTextUpdates,
            @Nullable List<EntityViewer> uiViewersOverride) {
        Field f = resolveComponentsField();
        if (f == null) {
            sendCombatTextUpdates.run();
            return;
        }
        try {
            setSlotAssetId(uiList, style.getCombatTextAssetId(), f);
        } catch (IllegalAccessException e) {
            LOGGER.at(Level.WARNING).log("CombatText color swap failed: {0}", e.getMessage());
            sendCombatTextUpdates.run();
            return;
        }
        if (uiViewersOverride != null && !uiViewersOverride.isEmpty()) {
            broadcastUiComponentsToViewers(targetRef, uiList, uiViewersOverride);
        } else {
            broadcastUiComponentsForTarget(targetRef, uiList, store);
        }
        sendCombatTextUpdates.run();
        world.execute(
                () -> world.execute(
                        () -> {
                            try {
                                if (!targetRef.isValid()) {
                                    return;
                                }
                                Field fld = resolveComponentsField();
                                if (fld == null) {
                                    return;
                                }
                                UIComponentList fresh =
                                        store.getComponent(targetRef, UIComponentList.getComponentType());
                                if (fresh == null) {
                                    return;
                                }
                                setSlotAssetId(fresh, DEFAULT_COMBAT_TEXT, fld);
                                if (uiViewersOverride != null && !uiViewersOverride.isEmpty()) {
                                    broadcastUiComponentsToViewers(targetRef, fresh, uiViewersOverride);
                                } else {
                                    broadcastUiComponentsForTarget(targetRef, fresh, store);
                                }
                            } catch (IllegalAccessException e) {
                                LOGGER.at(Level.WARNING).log("CombatText restore failed: {0}", e.getMessage());
                            } catch (RuntimeException e) {
                                LOGGER.at(Level.WARNING).log("CombatText restore failed: {0}", e.getMessage());
                            }
                        }));
    }
}
