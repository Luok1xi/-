package com.luokixi.visuals;

import com.luokixi.visuals.network.VisualNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = LuokixiVisuals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerCommands {
    private ServerCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("luokixi_visuals")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("blackflame")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 72000))
                                                .then(Commands.argument("resonance", IntegerArgumentType.integer(0, 3))
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                                            int resonance = IntegerArgumentType.getInteger(ctx, "resonance");
                                                            targets.forEach(e -> VisualNetwork.startBlackFlame(e, ticks, resonance));
                                                            return targets.size();
                                                        })))))
                        .then(Commands.literal("blackflame_stop")
                                .then(Commands.argument("targets", EntityArgument.entities()).executes(ctx -> {
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                    targets.forEach(VisualNetwork::stopBlackFlame);
                                    return targets.size();
                                })))
                        .then(Commands.literal("totem")
                                .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                    for (ServerPlayer player : players) player.level().broadcastEntityEvent(player, (byte) 35);
                                    return players.size();
                                })))
                        .then(Commands.literal("showcase_anchor")
                                .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                    int count = 0;
                                    for (ServerPlayer player : players) {
                                        if (!ServerEvents.isShowcase(player)) continue;
                                        ServerEvents.captureAnchor(player); count++;
                                    }
                                    return count;
                                })))
                        .then(Commands.literal("showcase_unanchor")
                                .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                    for (ServerPlayer player : players) ServerEvents.clearAnchor(player);
                                    return players.size();
                                })))
                        .then(Commands.literal("ysm_lock").executes(ctx -> {
                            YsmRuntimeLock.LockResult result = YsmRuntimeLock.enforce();
                            ctx.getSource().sendSuccess(() -> Component.literal(result.message()), false);
                            return result.locked() ? 1 : 0;
                        }))
                        .then(Commands.literal("status").executes(ctx -> {
                            YsmRuntimeLock.LockResult result = YsmRuntimeLock.enforce();
                            ctx.getSource().sendSuccess(() -> Component.literal("LuokixiVisuals 1.0.0: Amaterasu / Projection / WardrobeLock / Totem35; " + result.message()), false);
                            return 1;
                        }))
        );
    }
}
