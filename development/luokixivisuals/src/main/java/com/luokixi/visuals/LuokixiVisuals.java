package com.luokixi.visuals;

import com.luokixi.visuals.network.VisualNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(LuokixiVisuals.MOD_ID)
public final class LuokixiVisuals {
    public static final String MOD_ID = "luokixivisuals";

    public LuokixiVisuals() {
        LegacyCleanup.removeLegacyFireOverrides();
        VisualNetwork.register();
    }
}
