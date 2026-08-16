package com.luokixi.visuals.network;

import com.luokixi.visuals.LuokixiVisuals;
import com.luokixi.visuals.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class VisualNetwork {
    private static final String PROTOCOL = "1";
    private static final int RESEND_INTERVAL = 40;
    private static final int INFINITE_CLIENT_WINDOW = 1200;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LuokixiVisuals.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static final Map<UUID, FlameState> FLAMES = new HashMap<>();
    private static int packetId;

    private VisualNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(packetId++, BlackFlamePacket.class,
                BlackFlamePacket::encode, BlackFlamePacket::decode, BlackFlamePacket::handle);
    }

    public static void startBlackFlame(Entity entity, int ticks, int resonance) {
        if (entity == null || entity.isRemoved()) return;
        int safeRes = Math.max(0, Math.min(3, resonance));
        int safeTicks = Math.max(1, ticks);
        long now = entity.level().getGameTime();
        long expires = safeRes >= 3 ? Long.MAX_VALUE : now + safeTicks;
        FlameState old = FLAMES.get(entity.getUUID());
        if (old != null) {
            safeRes = Math.max(safeRes, old.resonance);
            expires = safeRes >= 3 ? Long.MAX_VALUE : Math.max(expires, old.expiresAt);
        }
        FlameState state = new FlameState(entity, expires, safeRes);
        FLAMES.put(entity.getUUID(), state);
        sendState(state, now);
    }

    public static void stopBlackFlame(Entity entity) {
        if (entity == null) return;
        FLAMES.remove(entity.getUUID());
        sendPacket(entity, 0, 0);
    }

    public static void tick(MinecraftServer server) {
        if (server == null || FLAMES.isEmpty() || server.getTickCount() % RESEND_INTERVAL != 0) return;
        Iterator<Map.Entry<UUID, FlameState>> it = FLAMES.entrySet().iterator();
        while (it.hasNext()) {
            FlameState state = it.next().getValue();
            Entity entity = state.entity;
            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                it.remove();
                continue;
            }
            long now = entity.level().getGameTime();
            if (state.expiresAt != Long.MAX_VALUE && state.expiresAt <= now) {
                sendPacket(entity, 0, 0);
                it.remove();
                continue;
            }
            sendState(state, now);
        }
    }

    private static void sendState(FlameState state, long now) {
        int ticks = state.expiresAt == Long.MAX_VALUE
                ? INFINITE_CLIENT_WINDOW
                : (int) Math.min(Integer.MAX_VALUE, Math.max(1L, state.expiresAt - now));
        sendPacket(state.entity, ticks, state.resonance);
    }

    private static void sendPacket(Entity entity, int ticks, int resonance) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new BlackFlamePacket(entity.getId(), ticks, resonance));
    }

    private record FlameState(Entity entity, long expiresAt, int resonance) {}

    public record BlackFlamePacket(int entityId, int ticks, int resonance) {
        public static void encode(BlackFlamePacket msg, FriendlyByteBuf buf) {
            buf.writeVarInt(msg.entityId);
            buf.writeVarInt(msg.ticks);
            buf.writeByte(msg.resonance);
        }

        public static BlackFlamePacket decode(FriendlyByteBuf buf) {
            return new BlackFlamePacket(buf.readVarInt(), buf.readVarInt(), buf.readUnsignedByte());
        }

        public static void handle(BlackFlamePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEvents.acceptBlackFlame(msg.entityId, msg.ticks, msg.resonance)));
            ctx.setPacketHandled(true);
        }
    }
}
