package com.bisson2000.biggeroreclusters.command;

import com.bisson2000.biggeroreclusters.config.BiggerOreClustersConfig;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class ListOresCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("listores").executes(ListOresCommand::run);
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        context.getSource().sendSuccess(() -> Component.literal("List of targeted blocks:"), true);
        for (Block block: BiggerOreClustersConfig.TARGETED_BLOCKS.keySet()) {
            context.getSource().sendSuccess(() -> Component.literal(block.toString()), true);
        }

        return 0;
    }
}
