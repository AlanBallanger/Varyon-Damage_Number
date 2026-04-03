package com.varyon.damagenumber;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.varyon.damagenumber.config.DamageNumberConfig;
import com.varyon.damagenumber.config.DamageNumberConfigStore;
import com.varyon.damagenumber.meta.DamageNumberMeta;
import com.varyon.damagenumber.systems.DamageColoredFloatSystem;
import com.varyon.damagenumber.systems.DamageVanillaFloatSuppressSystem;
import com.varyon.damagenumber.systems.HealthCombatTextSystem;

import java.util.Objects;
import java.util.logging.Level;

public class DamageNumberPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static DamageNumberPlugin instance;
    private DamageNumberConfig config;

    public DamageNumberPlugin(JavaPluginInit init) {
        super(init);
    }

    public static DamageNumberPlugin get() {
        return instance;
    }

    public DamageNumberConfig config() {
        return config;
    }

    @Override
    protected void setup() {
        instance = this;
        LOGGER.at(Level.INFO).log(String.format(
                "plugin file=%s | includesAssetPack=%s | embedded CombatText asset=%s",
                getFile(),
                getManifest().includesAssetPack(),
                getClass().getClassLoader().getResource("Server/Entity/UI/CombatText.json") != null));
        DamageNumberConfigStore store = new DamageNumberConfigStore(getDataDirectory());
        this.config = store.loadOrCreate();

        Objects.requireNonNull(DamageNumberMeta.STYLE_KEY, "DamageNumberMeta");

        ComponentRegistryProxy registry = getEntityStoreRegistry();
        registry.registerSystem((ISystem) new DamageVanillaFloatSuppressSystem());
        registry.registerSystem((ISystem) new DamageColoredFloatSystem());
        registry.registerSystem((ISystem) new HealthCombatTextSystem(this));

        LOGGER.at(Level.INFO).log(String.format(
                "Varyon-Damage_Number ready | data=%s | healing=%s | dot=%s",
                getDataDirectory(),
                config.isHealingEnabled(),
                config.isDotNumbersEnabled()));
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log(String.format(
                "started | healing=%s | dot=%s",
                config.isHealingEnabled(),
                config.isDotNumbersEnabled()));
    }
}
