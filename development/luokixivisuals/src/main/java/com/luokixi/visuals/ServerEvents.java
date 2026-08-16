package com.luokixi.visuals;

import com.luokixi.visuals.network.VisualNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = LuokixiVisuals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {
    public static final String SHOWCASE_TAG = "divine_showcase_bot";
    public static final String SHOWCASE_NAME = "OracleShowcase";
    private static final String FAKE_CLASS = "com.advancedfakeplayers.entity.FakeServerPlayer";
    private static final String A_SET = "LuokixiShowcaseAnchorSet";
    private static final String A_X = "LuokixiShowcaseAnchorX";
    private static final String A_Y = "LuokixiShowcaseAnchorY";
    private static final String A_Z = "LuokixiShowcaseAnchorZ";
    private static final String A_DIM = "LuokixiShowcaseAnchorDimension";
    private static final double RANGE_SQR = 32.0D * 32.0D;

    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        System.out.println("[LuokixiVisuals] " + YsmRuntimeLock.enforce().message());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        YsmRuntimeLock.enforce();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        VisualNetwork.tick(server);
        if (server.getTickCount() % 200 == 0) YsmRuntimeLock.enforce();
        if ((server.getTickCount() & 1) != 0) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer showcase : players) {
            if (isShowcase(showcase)) stabilize(showcase, players);
        }
    }

    public static boolean isShowcase(ServerPlayer player) {
        return player != null && (SHOWCASE_NAME.equals(player.getGameProfile().getName())
                || player.getTags().contains(SHOWCASE_TAG));
    }

    public static void captureAnchor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(A_SET, true);
        data.putDouble(A_X, player.getX());
        data.putDouble(A_Y, player.getY());
        data.putDouble(A_Z, player.getZ());
        data.putString(A_DIM, player.level().dimension().location().toString());
    }

    public static void clearAnchor(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(A_SET); data.remove(A_X); data.remove(A_Y); data.remove(A_Z); data.remove(A_DIM);
    }

    private static void stabilize(ServerPlayer showcase, List<ServerPlayer> players) {
        showcase.setInvulnerable(true);
        showcase.setNoGravity(true);
        showcase.setDeltaMovement(Vec3.ZERO);
        showcase.fallDistance = 0.0F;
        showcase.setAirSupply(showcase.getMaxAirSupply());
        if (showcase.getHealth() < showcase.getMaxHealth()) showcase.setHealth(showcase.getMaxHealth());
        pin(showcase);

        ServerPlayer nearest = null;
        double best = RANGE_SQR;
        for (ServerPlayer candidate : players) {
            if (candidate == showcase || candidate.isSpectator() || !candidate.isAlive() || isShowcase(candidate)) continue;
            if (FAKE_CLASS.equals(candidate.getClass().getName())) continue;
            if (candidate.serverLevel() != showcase.serverLevel()) continue;
            double dist = showcase.distanceToSqr(candidate);
            if (dist < best) { best = dist; nearest = candidate; }
        }
        if (nearest == null) return;

        double dx = nearest.getX() - showcase.getX();
        double dz = nearest.getZ() - showcase.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) return;
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        double dy = nearest.getEyeY() - (showcase.getEyeY() + 0.28D);
        float targetPitch = Mth.clamp((float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI))), -22.0F, 18.0F);
        float yaw = approachAngle(showcase.getYRot(), targetYaw, 10.5F);
        float pitch = Mth.approach(showcase.getXRot(), targetPitch, 4.5F);
        showcase.setYRot(yaw);
        showcase.setYHeadRot(yaw);
        showcase.yBodyRot = yaw;
        showcase.yBodyRotO = yaw;
        showcase.setXRot(pitch);
    }

    private static void pin(ServerPlayer showcase) {
        CompoundTag data = showcase.getPersistentData();
        if (!data.getBoolean(A_SET)) return;
        String dim = data.getString(A_DIM);
        if (!dim.isEmpty() && !dim.equals(showcase.level().dimension().location().toString())) return;
        double x = data.getDouble(A_X), y = data.getDouble(A_Y), z = data.getDouble(A_Z);
        if (showcase.distanceToSqr(x, y, z) > 0.0004D) showcase.setPos(x, y, z);
    }

    private static float approachAngle(float current, float target, float step) {
        float delta = Mth.wrapDegrees(target - current);
        return current + Mth.clamp(delta, -step, step);
    }
}
