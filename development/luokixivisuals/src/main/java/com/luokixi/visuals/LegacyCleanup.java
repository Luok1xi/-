package com.luokixi.visuals;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Removes only Luokixi's obsolete V10.3 vanilla-fire resource overrides. */
public final class LegacyCleanup {
    private static final String[] LEGACY = {
            "kubejs/assets/minecraft/textures/block/fire_0.png",
            "kubejs/assets/minecraft/textures/block/fire_0.png.mcmeta",
            "kubejs/assets/minecraft/textures/block/fire_1.png",
            "kubejs/assets/minecraft/textures/block/fire_1.png.mcmeta"
    };

    private LegacyCleanup() {}

    public static void removeLegacyFireOverrides() {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        for (String relative : LEGACY) {
            Path target = gameDir.resolve(relative).normalize();
            if (!target.startsWith(gameDir)) continue;
            try {
                if (Files.deleteIfExists(target)) {
                    System.out.println("[LuokixiVisuals] Removed obsolete vanilla-fire override: " + target);
                }
            } catch (IOException error) {
                System.err.println("[LuokixiVisuals] Could not remove obsolete override " + target + ": " + error);
            }
        }
    }
}
