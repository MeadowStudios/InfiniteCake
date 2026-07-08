package me.meadow;

import me.meadow.cake.InfiniteCakeManager;
import me.meadow.command.InfiniteCakeCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfiniteCake extends JavaPlugin {
    private static final Pattern HEX_COLOR = Pattern.compile("(?i)&#([a-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private InfiniteCakeManager cakeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.cakeManager = new InfiniteCakeManager(this);
        getServer().getPluginManager().registerEvents(cakeManager, this);

        registerCommand();
        cakeManager.reload();
    }

    @Override
    public void onDisable() {
        if (cakeManager != null) {
            cakeManager.clearPlacementModes();
        }
    }

    public void reloadPlugin() {
        reloadConfig();

        if (cakeManager != null) {
            cakeManager.reload();
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("infinitecake");
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: infinitecake");
            return;
        }

        InfiniteCakeCommand executor = new InfiniteCakeCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public InfiniteCakeManager cakeManager() {
        return cakeManager;
    }

    public String message(String key) {
        return message(key, Map.of());
    }

    public String message(String key, Map<String, String> placeholders) {
        String message = getConfig().getString("messages." + key, fallbackMessage(key));

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
        }

        return color(message);
    }

    public Component component(String message) {
        return LEGACY.deserialize(message == null ? "" : message);
    }

    private static String fallbackMessage(String key) {
        return switch (key) {
            case "no-permission" -> "§a→ §fYou do not have permission to use this.";
            case "players-only" -> "§a→ §fOnly players can use this command.";
            case "reload" -> "§a→ §fInfinite Cake configuration reloaded.";
            case "placement-start" -> "§a→ §fRight-click the block the cake should sit on, or right-click an existing cake.";
            case "placement-cancelled" -> "§a→ §fCake placement cancelled.";
            case "placement-invalid" -> "§a→ §fThe cake must be placed on top of a solid block, or you can right-click an existing cake.";
            case "placement-set" -> "§a→ §fInfinite cake placed at §a%location%§f.";
            case "info" -> "§a→ §fInfinite cake location: §a%location%§f.";
            case "cake-actionbar" -> "ᴍᴍ ʏᴜᴍ ᴄᴀᴋᴇ";
            default -> "";
        };
    }

    private static String color(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', translateHex(message));
    }

    private static String translateHex(String message) {
        Matcher matcher = HEX_COLOR.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x"
                    + "§" + hex.charAt(0)
                    + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4)
                    + "§" + hex.charAt(5);

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
