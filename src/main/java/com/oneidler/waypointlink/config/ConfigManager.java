package com.oneidler.waypointlink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneidler.waypointlink.WaypointLinkMod;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve(WaypointLinkMod.MOD_ID + ".json");

    public static ModConfig CONFIG = new ModConfig();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                CONFIG = GSON.fromJson(json, ModConfig.class);
            } catch (Exception e) {
                log.error("Failed to load config: {}" , e.getMessage());
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.writeString(
                    CONFIG_PATH,
                    GSON.toJson(CONFIG)
            );
        } catch (IOException e) {
            log.error("Failed to create config: {}" , e.getMessage());
        }
    }
}
