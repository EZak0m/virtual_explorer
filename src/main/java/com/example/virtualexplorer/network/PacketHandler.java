package com.example.virtualexplorer.network;

import com.example.virtualexplorer.VirtualExplorer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = VirtualExplorer.MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VirtualExplorer.MODID).versioned("1.0.0");

        registrar.playToServer(
                UninstallUpgradePayload.TYPE,
                UninstallUpgradePayload.STREAM_CODEC,
                UninstallUpgradePayload::handle
        );
    }
}
