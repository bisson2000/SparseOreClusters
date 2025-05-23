package com.bisson2000.sparseoreclusters.command;

import com.bisson2000.sparseoreclusters.SparseOreClusters;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public class CommandHandler {

    private static ArgumentBuilder<CommandSourceStack, ?> listFeaturesCommand;
    public static CommandHandler commandHandler = new CommandHandler();

    private void registerCommands(CommandBuildContext ctx) {
        listFeaturesCommand = ListFeaturesCommand.register();
    }

    public void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        registerCommands(event.getBuildContext());
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal(SparseOreClusters.MOD_ID)
                        .then(listFeaturesCommand)
        );
    }

}

