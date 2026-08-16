package com.luokixi.visuals.client;

import com.luokixi.visuals.LuokixiVisuals;
import com.luokixi.visuals.ServerEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = LuokixiVisuals.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    private static final ResourceLocation AMATERASU = new ResourceLocation(LuokixiVisuals.MOD_ID, "textures/effect/amaterasu.png");
    private static final String YSM_SCREEN = "com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen";
    private static final int FRAMES = 12;
    private static final float SHOWCASE_SCALE = 1.58F;
    private static final float SHOWCASE_LIFT = 0.43F;
    private static final Map<Integer, FlameState> FLAMES = new HashMap<>();
    private static final ThreadLocal<Integer> SHOWCASE_POSE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Random RANDOM = new Random();
    private static long lastNotice = -1000L;

    private ClientEvents() {}

    public static void acceptBlackFlame(int entityId, int ticks, int resonance) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks <= 0) { FLAMES.remove(entityId); return; }
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        FLAMES.put(entityId, new FlameState(now + Math.max(1, ticks), Math.max(0, Math.min(3, resonance))));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        if (next == null || !YSM_SCREEN.equals(next.getClass().getName())) return;
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = mc.level == null ? 0L : mc.level.getGameTime();
        if (now - lastNotice > 40L) {
            lastNotice = now;
            mc.player.displayClientMessage(Component.literal("§d✦ YSM 原生换装已锁定，请使用 §f/wardrobe §d切换传奇着装。"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerPre(RenderPlayerEvent.Pre event) {
        if (!isShowcase(event.getEntity())) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        SHOWCASE_POSE_DEPTH.set(SHOWCASE_POSE_DEPTH.get() + 1);
        float bob = (float) Math.sin((event.getEntity().tickCount + event.getPartialTick()) * 0.085F) * 0.065F;
        pose.translate(0.0D, SHOWCASE_LIFT + bob, 0.0D);
        pose.scale(SHOWCASE_SCALE, SHOWCASE_SCALE, SHOWCASE_SCALE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlayerPreCanceled(RenderPlayerEvent.Pre event) {
        if (!event.isCanceled()) return;
        try { renderAmaterasu(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick()); }
        finally { if (isShowcase(event.getEntity())) popShowcasePose(event.getPoseStack()); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerPost(RenderPlayerEvent.Post event) {
        try { renderAmaterasu(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick()); }
        finally { if (isShowcase(event.getEntity())) popShowcasePose(event.getPoseStack()); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player) return;
        renderAmaterasu(event.getEntity(), event.getPoseStack(), event.getMultiBufferSource(), event.getPartialTick());
    }

    private static void popShowcasePose(PoseStack pose) {
        int depth = SHOWCASE_POSE_DEPTH.get();
        if (depth <= 0) return;
        pose.popPose();
        if (depth == 1) SHOWCASE_POSE_DEPTH.remove(); else SHOWCASE_POSE_DEPTH.set(depth - 1);
    }

    private static boolean isShowcase(Player player) {
        return ServerEvents.SHOWCASE_NAME.equals(player.getGameProfile().getName())
                || player.getTags().contains(ServerEvents.SHOWCASE_TAG);
    }

    private static void renderAmaterasu(LivingEntity entity, PoseStack pose, MultiBufferSource buffers, float partialTick) {
        FlameState state = FLAMES.get(entity.getId());
        ClientLevel level = Minecraft.getInstance().level;
        if (state == null || level == null || state.expiresAt <= level.getGameTime() || !entity.isAlive()) return;

        int resonance = state.resonance;
        float ew = Math.max(0.60F, entity.getBbWidth());
        float eh = Math.max(1.0F, entity.getBbHeight());
        float width = Math.max(1.55F, ew * (2.55F + resonance * 0.13F));
        float height = Math.max(2.45F, eh * (1.34F + resonance * 0.055F));
        float radius = Math.max(0.24F, ew * 0.58F);
        int frame = Math.floorMod((int) ((entity.tickCount + partialTick) / 2.0F), FRAMES);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(AMATERASU));

        pose.pushPose();
        pose.translate(0.0D, -0.08D, 0.0D);
        for (int i = 0; i < 8; i++) {
            drawSheet(pose, consumer, width * (0.90F + (i % 3) * 0.055F),
                    height * (0.92F + (i % 4) * 0.035F), radius * (0.88F + (i % 2) * 0.16F),
                    i * 45.0F + (entity.tickCount + partialTick) * (i % 2 == 0 ? 0.16F : -0.13F),
                    (frame + i * 2) % FRAMES, 245);
        }
        for (int i = 0; i < 6; i++) {
            drawSheet(pose, consumer, width * (1.30F + resonance * 0.035F), height * 1.10F,
                    radius * 1.42F, 15.0F + i * 60.0F - (entity.tickCount + partialTick) * 0.10F,
                    (frame + 3 + i) % FRAMES, 195);
        }
        pose.translate(0.0D, eh * 0.40D, 0.0D);
        for (int i = 0; i < 4; i++) {
            drawSheet(pose, consumer, width * 0.88F, height * 0.72F, radius * 0.82F,
                    22.5F + i * 45.0F + (entity.tickCount + partialTick) * 0.19F,
                    (frame + 7 + i) % FRAMES, 225);
        }
        pose.popPose();
    }

    private static void drawSheet(PoseStack stack, VertexConsumer consumer, float width, float height,
                                  float radius, float angle, int frame, int alpha) {
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(angle));
        stack.translate(0.0D, 0.0D, radius);
        PoseStack.Pose pose = stack.last();
        float half = width * 0.5F;
        float v0 = frame / (float) FRAMES;
        float v1 = (frame + 1) / (float) FRAMES;
        int light = 0x00F000F0;
        vertex(consumer, pose, -half, 0, 0, 0, v1, alpha, light, 1);
        vertex(consumer, pose, half, 0, 0, 1, v1, alpha, light, 1);
        vertex(consumer, pose, half, height, 0, 1, v0, alpha, light, 1);
        vertex(consumer, pose, -half, height, 0, 0, v0, alpha, light, 1);
        vertex(consumer, pose, half, 0, 0, 1, v1, alpha, light, -1);
        vertex(consumer, pose, -half, 0, 0, 0, v1, alpha, light, -1);
        vertex(consumer, pose, -half, height, 0, 0, v0, alpha, light, -1);
        vertex(consumer, pose, half, height, 0, 1, v0, alpha, light, -1);
        stack.popPose();
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose p, float x, float y, float z,
                               float u, float v, int alpha, int light, float nz) {
        c.vertex(p.pose(), x, y, z).color(255,255,255,alpha).uv(u,v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),0,0,nz).endVertex();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) { FLAMES.clear(); return; }
        long time = level.getGameTime();
        Iterator<Map.Entry<Integer, FlameState>> it = FLAMES.entrySet().iterator();
        while (it.hasNext()) if (it.next().getValue().expiresAt <= time) it.remove();
        if ((time & 1L) == 0L) spawnFlameParticles(level);
        if (time % 3L == 0L) spawnShowcaseParticles(level);
    }

    private static void spawnFlameParticles(ClientLevel level) {
        for (Map.Entry<Integer, FlameState> entry : FLAMES.entrySet()) {
            Entity raw = level.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity entity) || !entity.isAlive()) continue;
            Player local = Minecraft.getInstance().player;
            if (local != null && local.distanceToSqr(entity) > 9216.0D) continue;
            int count = 3 + entry.getValue().resonance;
            for (int i = 0; i < count; i++) {
                double spread = Math.max(1.05D, entity.getBbWidth() * 2.25D);
                level.addParticle(i % 2 == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL,
                        entity.getX() + (RANDOM.nextDouble() - .5) * spread,
                        entity.getY() + .1 + RANDOM.nextDouble() * Math.max(1.35D, entity.getBbHeight() * 1.2D),
                        entity.getZ() + (RANDOM.nextDouble() - .5) * spread,
                        (RANDOM.nextDouble() - .5) * .012, .014 + RANDOM.nextDouble() * .024,
                        (RANDOM.nextDouble() - .5) * .012);
            }
            if (RANDOM.nextFloat() < .55F) level.addParticle(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() * .7, entity.getZ(), 0, .02, 0);
            if (RANDOM.nextFloat() < .30F) level.addParticle(ParticleTypes.SQUID_INK, entity.getX(), entity.getY() + entity.getBbHeight() * .5, entity.getZ(), 0, .025, 0);
        }
    }

    private static void spawnShowcaseParticles(ClientLevel level) {
        for (Player player : level.players()) {
            if (!isShowcase(player)) continue;
            Player local = Minecraft.getInstance().player;
            if (local != null && local.distanceToSqr(player) > 4096.0D) continue;
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double ring = .68D + RANDOM.nextDouble() * .30D;
            level.addParticle(RANDOM.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
                    player.getX() + Math.cos(angle) * ring, player.getY() + .12D + RANDOM.nextDouble() * .26D,
                    player.getZ() + Math.sin(angle) * ring, 0, .018, 0);
            if (RANDOM.nextFloat() < .34F) level.addParticle(ParticleTypes.ENCHANT,
                    player.getX() + (RANDOM.nextDouble() - .5D) * 1.4D,
                    player.getY() + .2D + RANDOM.nextDouble() * 2.35D,
                    player.getZ() + (RANDOM.nextDouble() - .5D) * 1.4D, 0, 0, 0);
        }
    }

    private record FlameState(long expiresAt, int resonance) {}
}
