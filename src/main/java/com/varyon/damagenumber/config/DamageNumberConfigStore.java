package com.varyon.damagenumber.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;

public final class DamageNumberConfigStore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Path configPath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public DamageNumberConfigStore(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.json");
    }

    public DamageNumberConfig loadOrCreate() {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to create config directory: {0}", e.getMessage());
            return new DamageNumberConfig();
        }
        if (!Files.exists(configPath)) {
            DamageNumberConfig defaults = new DamageNumberConfig();
            save(defaults);
            LOGGER.at(Level.INFO).log("Created default config at {0}", configPath);
            return defaults;
        }
        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);
            return parseMerged(raw);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to read config: {0}", e.getMessage());
            return new DamageNumberConfig();
        }
    }

    public void save(DamageNumberConfig config) {
        try {
            Files.writeString(configPath, gson.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save config: {0}", e.getMessage());
        }
    }

    private DamageNumberConfig parseMerged(String json) {
        JsonObject defaults = JsonParser.parseString(gson.toJson(new DamageNumberConfig())).getAsJsonObject();
        JsonObject fromFile = JsonParser.parseString(json).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> e : defaults.entrySet()) {
            if (!fromFile.has(e.getKey())) {
                fromFile.add(e.getKey(), e.getValue());
            }
        }
        return gson.fromJson(fromFile, DamageNumberConfig.class);
    }
}
