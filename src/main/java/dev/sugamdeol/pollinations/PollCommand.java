package dev.sugamdeol.pollinations;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * /poll command: text generation and AI images on maps.
 */
public class PollCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("text", "image", "help");

    private final JavaPlugin plugin;
    private final ApiClient api;

    public PollCommand(JavaPlugin plugin, ApiClient api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);
            case "text" -> {
                if (!api.hasKey()) {
                    sender.sendMessage(ChatColor.RED + "No API key configured. Set api-key in plugins/PollinationsMC/config.yml");
                    return true;
                }
                String prompt = join(args, 1);
                if (prompt.isBlank()) {
                    sender.sendMessage(ChatColor.RED + "Usage: /poll text <prompt>");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Generating...");
                api.generateText(prompt).whenComplete((text, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null) {
                        sender.sendMessage(ChatColor.RED + "Error: " + err.getMessage());
                    } else {
                        for (String chunk : split(text, 256)) {
                            sender.sendMessage(ChatColor.AQUA + "[AI] " + ChatColor.WHITE + chunk);
                        }
                    }
                }));
            }
            case "image" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use /poll image.");
                    return true;
                }
                if (!api.hasKey()) {
                    sender.sendMessage(ChatColor.RED + "No API key configured. Set api-key in plugins/PollinationsMC/config.yml");
                    return true;
                }
                String prompt = join(args, 1);
                if (prompt.isBlank()) {
                    sender.sendMessage(ChatColor.RED + "Usage: /poll image <prompt>");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Painting your image... (a few seconds)");
                api.generateImage(prompt).whenComplete((img, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null) {
                        sender.sendMessage(ChatColor.RED + "Error: " + err.getMessage());
                        return;
                    }
                    giveMapItem(player, img, prompt);
                }));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void giveMapItem(Player player, BufferedImage img, String prompt) {
        MapView view = Bukkit.createMap(player.getWorld());
        for (MapRenderer r : view.getRenderers()) view.removeRenderer(r);
        view.addRenderer(new ImageMapRenderer(ApiClient.toMapColors(img), prompt));

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        meta.setMapView(view);
        meta.setDisplayName(org.bukkit.ChatColor.AQUA + "AI: " + truncate(prompt, 40));
        map.setItemMeta(meta);

        player.getInventory().addItem(map).forEach((slot, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(org.bukkit.ChatColor.GREEN + "Here is your AI image: \"" + org.bukkit.ChatColor.WHITE + prompt + org.bukkit.ChatColor.GREEN + "\"");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "PollinationsMC " + ChatColor.GRAY + "- AI for your server");
        sender.sendMessage(ChatColor.YELLOW + "/poll text <prompt>" + ChatColor.GRAY + " - ask an AI model anything");
        sender.sendMessage(ChatColor.YELLOW + "/poll image <prompt>" + ChatColor.GRAY + " - AI image on a map item");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

    // ---------- helpers ----------

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static String[] split(String s, int max) {
        if (s == null || s.isEmpty()) return new String[]{"(empty response)"};
        int n = (s.length() + max - 1) / max;
        String[] out = new String[n];
        for (int i = 0; i < n; i++) {
            out[i] = s.substring(i * max, Math.min(s.length(), (i + 1) * max));
        }
        return out;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}