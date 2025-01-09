package com.bisson2000.largepatchgenerator.item;

import com.bisson2000.largepatchgenerator.LargePatchGenerator;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LargePatchGenerator.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
