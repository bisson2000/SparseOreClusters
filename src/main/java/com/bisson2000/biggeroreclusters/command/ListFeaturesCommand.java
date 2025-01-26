package com.bisson2000.biggeroreclusters.command;

import com.bisson2000.biggeroreclusters.config.BiggerOreClustersConfig;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;

public class ListFeaturesCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("listfeatures").executes(ListFeaturesCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        context.getSource().sendSuccess(() -> Component.literal("List of targeted features:"), true);
        HashSet<Holder<PlacedFeature>> uniqueFeatures = new HashSet<>();
        for (HashSet<Holder<PlacedFeature>> placedFeatures: BiggerOreClustersConfig.TARGETED_FEATURES_IN_BIOME.values()) {
            uniqueFeatures.addAll(placedFeatures);
        }

        for (Holder<PlacedFeature> placedFeature : uniqueFeatures) {
            if (placedFeature.unwrapKey().isPresent()) {
                context.getSource().sendSuccess(() -> Component.literal(placedFeature.unwrapKey().get().location().toString()), true);
            }
        }

        return 0;
    }
}
