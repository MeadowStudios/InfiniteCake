package me.meadow.cake;

import me.meadow.InfiniteCake;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class InfiniteCakeManager implements Listener {
    private static final ItemStack CAKE_ITEM = new ItemStack(Material.CAKE);
    private static final String ADMIN_PERMISSION = "infinitecake.admin";

    private final InfiniteCake plugin;
    private final File dataFile;
    private final Set<UUID> placementMode = new HashSet<>();

    private FileConfiguration data;
    private CakeLocation location;

    public InfiniteCakeManager(InfiniteCake plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void reload() {
        placementMode.clear();
        data = YamlConfiguration.loadConfiguration(dataFile);

        migrateLegacyConfigLocation();
        location = loadLocation();

        cleanLegacyConfigKeys();
        ensureCakeExists();
    }

    public void clearPlacementModes() {
        placementMode.clear();
    }

    public void startPlacement(Player player) {
        if (player != null) {
            placementMode.add(player.getUniqueId());
        }
    }

    public void cancelPlacement(Player player) {
        if (player != null) {
            placementMode.remove(player.getUniqueId());
        }
    }

    public String locationText() {
        if (location == null) {
            return "not set";
        }

        return location.world() + " " + location.x() + ", " + location.y() + ", " + location.z();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (placementMode.contains(player.getUniqueId())) {
            handlePlacement(event);
            return;
        }

        handleCakeUse(event);
    }

    private void handlePlacement(PlayerInteractEvent event) {
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!player.hasPermission(ADMIN_PERMISSION)) {
            placementMode.remove(player.getUniqueId());
            player.sendMessage(plugin.message("no-permission"));
            return;
        }

        Block target = targetBlock(event);
        if (target == null || !canPlaceCakeAt(target)) {
            player.sendMessage(plugin.message("placement-invalid"));
            return;
        }

        target.setType(Material.CAKE, false);
        setCakeLocation(target);

        placementMode.remove(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.4F);
        player.sendMessage(plugin.message("placement-set", Map.of("%location%", format(target))));
    }

    private void handleCakeUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAKE || !isCakeLocation(block)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);

        Player player = event.getPlayer();
        GameMode gameMode = player.getGameMode();

        if (gameMode == GameMode.SPECTATOR || gameMode == GameMode.ADVENTURE) {
            return;
        }

        feed(player);
        playEffects(player, block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCakeBreak(BlockBreakEvent event) {
        if (isProtectedBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isProtectedBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (isProtectedBlock(event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (isProtectedBlock(event.getBlock())) {
            event.setCancelled(true);
            ensureCakeExistsNextTick();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (wouldAffectProtectedBlock(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (wouldAffectProtectedBlock(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        protectExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        protectExplosion(event.blockList());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (location != null && event.getWorld().getName().equals(location.world())) {
            ensureCakeExists();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        placementMode.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        placementMode.remove(event.getPlayer().getUniqueId());
    }

    private CakeLocation loadLocation() {
        if (data == null || !data.isConfigurationSection("cake")) {
            return null;
        }

        return readLocation(data);
    }

    private void migrateLegacyConfigLocation() {
        if (data != null && data.isConfigurationSection("cake")) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        if (!config.isConfigurationSection("cake")) {
            return;
        }

        CakeLocation legacy = readLocation(config);
        if (legacy == null) {
            return;
        }

        saveLocationToData(legacy);
    }

    private CakeLocation readLocation(FileConfiguration config) {
        String world = config.getString("cake.world");
        if (world == null || world.isBlank()) {
            return null;
        }

        if (!config.contains("cake.x") || !config.contains("cake.y") || !config.contains("cake.z")) {
            return null;
        }

        return new CakeLocation(
                world,
                config.getInt("cake.x"),
                config.getInt("cake.y"),
                config.getInt("cake.z")
        );
    }

    private void cleanLegacyConfigKeys() {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("cake")) {
            return;
        }

        config.set("cake", null);
        plugin.saveConfig();
    }

    private void feed(Player player) {
        int amount = Math.max(0, plugin.getConfig().getInt("food.amount", 2));
        float saturation = Math.max(0.0F, (float) plugin.getConfig().getDouble("food.saturation", 0.4D));

        int food = player.getFoodLevel();
        if (amount <= 0 || food >= 20) {
            return;
        }

        int newFood = Math.min(20, food + amount);
        player.setFoodLevel(newFood);
        player.setSaturation(Math.min(newFood, player.getSaturation() + saturation));
    }

    private void playEffects(Player player, Block block) {
        if (plugin.getConfig().getBoolean("effects.sound", true)) {
            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_GENERIC_EAT,
                    1.0F,
                    0.8F + ThreadLocalRandom.current().nextFloat() * 0.4F
            );
        }

        if (plugin.getConfig().getBoolean("effects.particles", true)) {
            player.spawnParticle(
                    Particle.ITEM,
                    block.getX() + 0.5D,
                    block.getY() + 0.65D,
                    block.getZ() + 0.5D,
                    6,
                    0.25D,
                    0.15D,
                    0.25D,
                    0.02D,
                    CAKE_ITEM
            );
        }

        if (plugin.getConfig().getBoolean("effects.actionbar", true)) {
            String message = plugin.message("cake-actionbar");
            if (!message.isBlank()) {
                player.sendActionBar(plugin.component(message));
            }
        }
    }

    private void setCakeLocation(Block block) {
        removeOldCakeIfMoved(block);

        CakeLocation newLocation = new CakeLocation(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        saveLocationToData(newLocation);
        location = newLocation;

        ensureCakeExists();
    }

    private void saveLocationToData(CakeLocation cakeLocation) {
        if (data == null) {
            data = YamlConfiguration.loadConfiguration(dataFile);
        }

        data.set("cake.world", cakeLocation.world());
        data.set("cake.x", cakeLocation.x());
        data.set("cake.y", cakeLocation.y());
        data.set("cake.z", cakeLocation.z());

        saveData();
    }

    private void saveData() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                return;
            }

            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save data.yml: " + exception.getMessage());
        }
    }

    private void removeOldCakeIfMoved(Block newBlock) {
        Block oldBlock = currentCakeBlock();
        if (oldBlock == null || sameBlock(oldBlock, newBlock)) {
            return;
        }

        if (oldBlock.getType() == Material.CAKE) {
            oldBlock.setType(Material.AIR, false);
        }
    }

    private Block currentCakeBlock() {
        if (location == null) {
            return null;
        }

        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            return null;
        }

        return world.getBlockAt(location.x(), location.y(), location.z());
    }

    private void ensureCakeExists() {
        if (location == null) {
            return;
        }

        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            plugin.getLogger().warning("Infinite cake world is not loaded: " + location.world());
            return;
        }

        Block block = world.getBlockAt(location.x(), location.y(), location.z());

        if (block.getType() == Material.CAKE) {
            return;
        }

        if (!canPlaceCakeAt(block)) {
            plugin.getLogger().warning("Could not place infinite cake at " + locationText() + ". The location is blocked or unsupported.");
            return;
        }

        block.setType(Material.CAKE, false);
    }

    private void ensureCakeExistsNextTick() {
        if (location == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, this::ensureCakeExists);
    }

    private Block targetBlock(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return null;
        }

        if (clicked.getType() == Material.CAKE) {
            return clicked;
        }

        return clicked.getRelative(event.getBlockFace());
    }

    private boolean canPlaceCakeAt(Block block) {
        if (block == null) {
            return false;
        }

        Material type = block.getType();
        if (type != Material.CAKE && !type.isAir()) {
            return false;
        }

        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType().isSolid();
    }

    private boolean isProtectedBlock(Block block) {
        return isCakeLocation(block) || isSupportBlock(block);
    }

    private boolean isSupportBlock(Block block) {
        return location != null
                && block != null
                && block.getWorld().getName().equals(location.world())
                && block.getX() == location.x()
                && block.getY() == location.y() - 1
                && block.getZ() == location.z();
    }

    private boolean isCakeLocation(Block block) {
        return location != null
                && block != null
                && block.getWorld().getName().equals(location.world())
                && block.getX() == location.x()
                && block.getY() == location.y()
                && block.getZ() == location.z();
    }

    private boolean wouldAffectProtectedBlock(List<Block> blocks, BlockFace direction) {
        for (Block block : blocks) {
            if (isProtectedBlock(block) || isProtectedBlock(block.getRelative(direction))) {
                return true;
            }
        }

        return false;
    }

    private void protectExplosion(List<Block> blocks) {
        blocks.removeIf(this::isProtectedBlock);
    }

    private static boolean sameBlock(Block first, Block second) {
        return first != null
                && second != null
                && first.getWorld().equals(second.getWorld())
                && first.getX() == second.getX()
                && first.getY() == second.getY()
                && first.getZ() == second.getZ();
    }

    private static String format(Block block) {
        return block.getWorld().getName() + " " + block.getX() + ", " + block.getY() + ", " + block.getZ();
    }

    private record CakeLocation(String world, int x, int y, int z) {
    }
}
