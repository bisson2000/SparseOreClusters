package com.bisson2000.biggeroreclusters.command;

import com.bisson2000.biggeroreclusters.BiggerOreClusters;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.function.Supplier;

public class CommandHandler {

    private static ArgumentBuilder<CommandSourceStack, ?> listOresCommand;
    public static CommandHandler commandHandler = new CommandHandler();

    private void registerCommands(CommandBuildContext ctx) {
        listOresCommand = ListOresCommand.register();
    }

    public void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        registerCommands(event.getBuildContext());
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal(BiggerOreClusters.MOD_ID)
                        .then(listOresCommand)
        );
    }

}

