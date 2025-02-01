package com.bisson2000.sparseoreclusters.command;

import com.bisson2000.sparseoreclusters.config.SparseOreClustersConfig;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;
import java.util.List;

public class ListFeaturesCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("listfeatures").executes(ListFeaturesCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        context.getSource().sendSuccess(() -> Component.literal("List of targeted features:"), true);
        HashSet<Holder<PlacedFeature>> uniqueFeatures = new HashSet<>();
        for (HashSet<Holder<PlacedFeature>> placedFeatures: SparseOreClustersConfig.TARGETED_FEATURES_IN_BIOME.values()) {
            uniqueFeatures.addAll(placedFeatures);
        }

        List<Holder<PlacedFeature>> sortedFeatures = uniqueFeatures.stream().sorted((o1, o2) -> {
            String str1 = o1.unwrapKey().isPresent() ? o1.unwrapKey().get().location().toString() : "";
            String str2 = o2.unwrapKey().isPresent() ? o2.unwrapKey().get().location().toString() : "";
            return str1.compareTo(str2);
        }).toList();


        for (Holder<PlacedFeature> placedFeature : sortedFeatures) {
            if (placedFeature.unwrapKey().isPresent()) {
                context.getSource().sendSuccess(() -> Component.literal(placedFeature.unwrapKey().get().location().toString()), true);
            }
        }

        return 0;
    }
}
