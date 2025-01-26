package com.bisson2000.biggeroreclusters.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public enum Mods {
    MEKANISM;

    private final String id;

    Mods() {
        id = name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the mod id
     */
    public String id() {
        return id;
    }

    public Block getBlock(String path) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id, path));
    }

    public Item getItem(String path) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id, path));
    }

    /**
     * @return a boolean of whether the mod is loaded or not based on mod id
     */
    public boolean isLoaded() {
        return ModList.get().isLoaded(id);
    }

    /**
     * Simple hook to run code if a mod is installed
     * @param toRun will be run only if the mod is loaded
     * @return Optional.empty() if the mod is not loaded, otherwise an Optional of the return value of the given supplier
     */
    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.of(toRun.get().get());
        return Optional.empty();
    }

    /**
     * Simple hook to execute code if a mod is installed
     * @param toExecute will be executed only if the mod is loaded
     */
    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (isLoaded()) {
            toExecute.get().run();
        }
    }
}
