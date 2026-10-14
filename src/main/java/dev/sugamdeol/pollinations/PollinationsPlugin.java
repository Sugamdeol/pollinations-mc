package dev.sugamdeol.pollinations;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * PollinationsMC - Bring Pollinations AI to Minecraft servers.
 * Players generate AI text and images (rendered on in-game maps)
 * using the Pollinations API with the server owner's Pollen.
 */
public final class PollinationsPlugin extends JavaPlugin {

    private ApiClient api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String apiKey = getConfig().getString("api-key", "");
        String textModel = getConfig().getString("text-model", "openai/gpt-5.4-nano");
        String imageModel = getConfig().getString("image-model", "black-forest-labs/flux.1-schnell");
        String baseUrl = getConfig().getString("base-url", "https://gen.pollinations.ai");

        this.api = new ApiClient(baseUrl, apiKey, textModel, imageModel, getLogger());

        PollCommand cmd = new PollCommand(this, api);
        getCommand("poll").setExecutor(cmd);
        getCommand("poll").setTabCompleter(cmd);

        getLogger().info("PollinationsMC enabled. API key " + (apiKey == null || apiKey.isBlank() ? "NOT SET" : "configured") + ".");
    }

    @Override
    public void onDisable() {
        getLogger().info("PollinationsMC disabled.");
    }

    public ApiClient getApi() {
        return api;
    }
}