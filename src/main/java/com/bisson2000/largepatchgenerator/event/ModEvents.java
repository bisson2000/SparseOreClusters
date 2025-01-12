package com.bisson2000.largepatchgenerator.event;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;


@EventBusSubscriber(modid = LargePatchGenerator.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void onGenericBlockBreakEvent(BlockEvent.BreakEvent breakEvent) {
        Player player  = breakEvent.getPlayer();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = breakEvent.getPos();
        BlockState blockState = serverLevel.getBlockState(pos);

        // Reduce charges for the broken block
        int charges = BlockChargeSavedData.get(serverLevel).reduceCharges(serverLevel, player, pos);

        if (charges > 0) {
            breakEvent.setCanceled(true);
        }
    }

}
