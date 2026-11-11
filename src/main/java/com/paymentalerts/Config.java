package com.paymentalerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    public boolean enabled = true;
    public boolean sounds = true;
    /**
     * Player chat can be typed by anyone (so someone could fake "Bob paid you $100B").
     * Leave this false to only react to server/system messages.
     */
    public boolean listenToChat = false;
    /** Group 1 = player name, group 2 = number, group 3 = optional K/M/B/T suffix. */
    public String pattern = "^[^A-Za-z0-9_]{0,6}([A-Za-z0-9_]{3,16})\\s+paid\\s+you\\s+\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kKmMbBtT])?(?![A-Za-z0-9])";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("paymentalerts.json");
    }

    public static Config load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    Config c = GSON.fromJson(r, Config.class);
                    if (c != null) return c;
                }
            }
        } catch (Exception e) {
            PaymentAlertsClient.LOG.warn("Could not read config, using defaults", e);
        }
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            PaymentAlertsClient.LOG.warn("Could not save config", e);
        }
    }
}
