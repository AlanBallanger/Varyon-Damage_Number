package irai.mod.reforge.Entity.Events;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberMeta;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

public final class FloatingDamageParticles {

    private static final double HEIGHT_ABOVE_ENTITY = 1.85;
    private static final double DIGIT_SPACING = 0.1;
    private static final double ICON_SLOT_WIDTH = 0.138;
    private static final double ICON_NUDGE_TOWARD_DIGITS = 0.055;
    /** Extra world-space gap between icon and first digit when the number is short (narrow group). */
    private static final double ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT = 0.023;
    /** Scale nudge down for 1–2 digit amounts so the burst sits less into the first digit. */
    private static final double ICON_NUDGE_SHORT_NUMBER_SCALE = 0.28;
    private static final double HORIZONTAL_GROUP_JITTER = 0.26;

    private FloatingDamageParticles() {}

    public static boolean trySpawn(Store<EntityStore> store,
                                   @Nullable CommandBuffer<EntityStore> commandBuffer,
                                   Ref<EntityStore> targetRef,
                                   float amount,
                                   @Nullable String kindId,
                                   @Nullable Vector3d viewerPosition,
                                   @Nullable Damage damage) {
        String resolved;
        if (damage != null) {
            resolved = DamageNumbers.resolveKindId(damage);
        } else {
            resolved = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        }
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
        font = resolveCriticalDigitFont(resolved, font, iconSystem, damage);

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

        int digitCount = digits.length();
        int iconSlots = iconSystem != null ? 1 : 0;
        double iconToDigitGap = iconSlots > 0 ? iconToDigitGapAfterIcon(digitCount) : 0.0;
        double iconNudge = iconSlots > 0 ? iconNudgeTowardDigits() : 0.0;
        double totalWidth = iconSlots * ICON_SLOT_WIDTH + iconToDigitGap + digitCount * DIGIT_SPACING;
        double cursor = -totalWidth / 2.0;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double groupAlong = rng.nextDouble(-HORIZONTAL_GROUP_JITTER, HORIZONTAL_GROUP_JITTER);

        var accessor = commandBuffer != null ? commandBuffer : store;
        try {
            if (iconSystem != null) {
                double along = cursor + ICON_SLOT_WIDTH / 2.0 + iconNudge + groupAlong;
                double px = base.x + rightX * along;
                double pz = base.z + rightZ * along;
                ParticleUtil.spawnParticleEffect(iconSystem, new Vector3d(px, y, pz), accessor);
                cursor += ICON_SLOT_WIDTH + iconToDigitGap;
            }
            for (int i = 0; i < digits.length(); i++) {
                char c = digits.charAt(i);
                if (c < '0' || c > '9') {
                    continue;
                }
                String systemName = font + "_Digit_" + c;
                double along = cursor + DIGIT_SPACING / 2.0 + groupAlong;
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

    private static final String FONT_FLAT = "FloatingDamage_FLAT";
    private static final String FONT_CRITICAL = "FloatingDamage_CRITICAL";

    private static String resolveCriticalDigitFont(String resolvedKind,
                                                 String font,
                                                 @Nullable String iconSystem,
                                                 @Nullable Damage damage) {
        String upper = resolvedKind == null ? "" : resolvedKind.toUpperCase(Locale.ROOT);
        if ("CRITICAL".equals(upper)) {
            return FONT_CRITICAL;
        }
        if (font == null || font.isBlank()) {
            return font;
        }
        if (!font.contains(FONT_FLAT)) {
            return font;
        }
        if (damage != null) {
            if (DamageNumberMeta.isCritical(damage)
                    || DamageNumberMeta.inferCriticalFromImpactVfx(damage)
                    || DamageNumberMeta.inferCriticalFromMetaStringScan(damage)) {
                return FONT_CRITICAL;
            }
        }
        if (iconSystem != null && iconSystem.contains("Icon_Critical")) {
            return FONT_CRITICAL;
        }
        return font;
    }

    private static double iconToDigitGapAfterIcon(int digitCount) {
        double gap = Math.max(0, 3 - digitCount) * ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT;
        if (digitCount >= 3) {
            gap = Math.max(gap, ICON_TO_DIGIT_GAP_PER_MISSING_DIGIT);
        }
        return gap;
    }

    private static double iconNudgeTowardDigits() {
        return ICON_NUDGE_TOWARD_DIGITS * ICON_NUDGE_SHORT_NUMBER_SCALE;
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
