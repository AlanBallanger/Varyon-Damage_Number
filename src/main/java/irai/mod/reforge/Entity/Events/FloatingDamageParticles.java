package irai.mod.reforge.Entity.Events;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

public final class FloatingDamageParticles {

    private static final double HEIGHT_ABOVE_ENTITY = 1.85;
    private static final double DIGIT_SPACING = 0.08;
    private static final double ICON_SLOT_WIDTH = 0.12;
    private static final double ICON_NUDGE_TOWARD_DIGITS = 0.09;

    private FloatingDamageParticles() {}

    public static boolean trySpawn(Store<EntityStore> store,
                                   @Nullable CommandBuffer<EntityStore> commandBuffer,
                                   Ref<EntityStore> targetRef,
                                   float amount,
                                   String kindId,
                                   @Nullable Vector3d viewerPosition) {
        String resolved = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        DamageNumbers.KindStyle style = DamageNumbers.getKindStyle(resolved);
        if (style == null || style.particleFontId() == null || style.particleFontId().isBlank()) {
            return false;
        }
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        TransformComponent transform = null;
        if (commandBuffer != null) {
            transform = commandBuffer.getComponent(targetRef, transformType);
        }
        if (transform == null) {
            transform = store.getComponent(targetRef, transformType);
        }
        if (transform == null) {
            return false;
        }
        int whole = (int) Math.floor(amount);
        if (whole <= 0) {
            return false;
        }
        String digits = Integer.toString(whole);
        String font = style.particleFontId().trim();
        String iconId = style.particleIconId();
        String iconSystem = (iconId != null && !iconId.isBlank()) ? iconId.trim() : null;

        Vector3d base = transform.getPosition();
        double y = base.y + HEIGHT_ABOVE_ENTITY;

        double rightX;
        double rightZ;
        {
            double[] r = new double[2];
            resolveHorizontalRight(base, viewerPosition, transform, r);
            rightX = r[0];
            rightZ = r[1];
        }

        int iconSlots = iconSystem != null ? 1 : 0;
        double totalWidth = iconSlots * ICON_SLOT_WIDTH + digits.length() * DIGIT_SPACING;
        double cursor = -totalWidth / 2.0;

        var accessor = commandBuffer != null ? commandBuffer : store;
        try {
            if (iconSystem != null) {
                double along = cursor + ICON_SLOT_WIDTH / 2.0 + ICON_NUDGE_TOWARD_DIGITS;
                double px = base.x + rightX * along;
                double pz = base.z + rightZ * along;
                ParticleUtil.spawnParticleEffect(iconSystem, new Vector3d(px, y, pz), accessor);
                cursor += ICON_SLOT_WIDTH;
            }
            for (int i = 0; i < digits.length(); i++) {
                char c = digits.charAt(i);
                if (c < '0' || c > '9') {
                    continue;
                }
                String systemName = font + "_Digit_" + c;
                double along = cursor + DIGIT_SPACING / 2.0;
                double px = base.x + rightX * along;
                double pz = base.z + rightZ * along;
                ParticleUtil.spawnParticleEffect(systemName, new Vector3d(px, y, pz), accessor);
                cursor += DIGIT_SPACING;
            }
        } catch (Throwable ignored) {
            return false;
        }
        return true;
    }

    private static void resolveHorizontalRight(Vector3d base,
                                               @Nullable Vector3d viewerPosition,
                                               TransformComponent targetTransform,
                                               double[] outRightXZ) {
        if (viewerPosition != null) {
            double fx = base.getX() - viewerPosition.getX();
            double fz = base.getZ() - viewerPosition.getZ();
            double len = Math.hypot(fx, fz);
            if (len > 1e-4) {
                fx /= len;
                fz /= len;
                outRightXZ[0] = -fz;
                outRightXZ[1] = fx;
                return;
            }
        }
        Vector3f rot = targetTransform.getRotation();
        double yawRad = Math.toRadians(rot.getYaw());
        outRightXZ[0] = -Math.cos(yawRad);
        outRightXZ[1] = -Math.sin(yawRad);
    }
}
