package me.meadow.command;

import me.meadow.InfiniteCake;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InfiniteCakeCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "infinitecake.admin";

    private final InfiniteCake plugin;

    public InfiniteCakeCommand(InfiniteCake plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.message("players-only"));
                    return true;
                }

                plugin.cakeManager().startPlacement(player);
                player.sendMessage(plugin.message("placement-start"));
                return true;
            }

            case "cancel" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.message("players-only"));
                    return true;
                }

                plugin.cakeManager().cancelPlacement(player);
                player.sendMessage(plugin.message("placement-cancelled"));
                return true;
            }

            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(plugin.message("reload"));
                return true;
            }

            case "info" -> {
                sender.sendMessage(plugin.message("info", Map.of("%location%", plugin.cakeManager().locationText())));
                return true;
            }

            default -> {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION) || args.length != 1) {
            return Collections.emptyList();
        }

        String input = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>(4);

        for (String option : List.of("set", "cancel", "reload", "info")) {
            if (option.startsWith(input)) {
                completions.add(option);
            }
        }

        return completions;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§aInfinite Cake Commands");
        sender.sendMessage("§f/" + label + " set §7- Right-click a block to place the cake on top, or click an existing cake.");
        sender.sendMessage("§f/" + label + " cancel §7- Cancel placement mode.");
        sender.sendMessage("§f/" + label + " info §7- Show the current cake location.");
        sender.sendMessage("§f/" + label + " reload §7- Reload the configuration.");
    }
}
