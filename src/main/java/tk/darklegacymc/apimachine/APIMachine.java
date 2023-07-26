package tk.darklegacymc.apimachine;
import com.google.gson.Gson;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.clip.placeholderapi.PlaceholderAPI;
import tk.darklegacymc.apimachine.commands.ReloadCommand;

import static spark.Spark.*;

public final class APIMachine extends JavaPlugin {
    public FileConfiguration config;

    @Override
    public void onEnable() {
        // Plugin startup logic
        config = getConfig();
        saveDefaultConfig();
        port(config.getInt("port"));
        setupRoutes();
        this.getCommand("apimachine").setExecutor(new ReloadCommand(this));
        this.getLogger().info("APIMachine started. Wiki: https://mallusrgreat.gitbook.io/mallusrgreats-plugins/");
    }

    public void setupRoutes() {
        Gson gson = new Gson();
        get("/api/users/:name", (req, res) -> {
            String username = req.params("name");
            OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(username);
            if (!offlinePlayer.hasPlayedBefore()) {
                res.status(404); // Not Found
                return "Invalid";
            }
            List<String> userEnabled = config.getStringList("users.enabled");
            Map<String, Object> playerData = new HashMap<>();
            userEnabled.forEach(enabledKey -> {
                String value = config.getString("users." + enabledKey);
                if (value == null) {
                    getLogger().warning("A value returned null: " + enabledKey);
                    return;
                }
                // Replace {username} with the player name or an empty string if null
                value = value.replaceAll("\\{username\\}", Matcher.quoteReplacement(Optional.ofNullable(offlinePlayer.getName()).orElse("")));

                // Format the last seen timestamp using LocalDateTime and DateTimeFormatter
                LocalDateTime lastSeen = LocalDateTime.ofEpochSecond(offlinePlayer.getLastSeen() / 1000L, 0, java.time.ZoneOffset.UTC);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                value = value.replaceAll("\\{last_seen\\}", Matcher.quoteReplacement(formatter.format(lastSeen)));

                // Handle {papi:...} placeholders in the balance field
                if (value.contains("{papi:")) {
                    Pattern papiPattern = Pattern.compile("\\{papi:(.*?)\\}");
                    Matcher matcher = papiPattern.matcher(value);
                    StringBuilder sb = new StringBuilder();
                    while (matcher.find()) {
                        String placeholder = matcher.group(1);
                        String replacement = PlaceholderAPI.setPlaceholders(offlinePlayer, placeholder);
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(sb);
                    value = sb.toString();
                }

                playerData.put(enabledKey, value);
            });

            res.type("application/json");

            return gson.toJson(playerData);
        });
        get("/api/global", (req, res) -> {
            List<String> userEnabled = config.getStringList("global.enabled");
            Map<String, Object> playerData = new HashMap<>();
            userEnabled.forEach(enabledKey -> {
                String value = config.getString("global." + enabledKey);
                if (value == null) {
                    getLogger().warning("A value returned null: " + enabledKey);
                    return;
                }

                // Handle {papi:...} placeholders in the balance field
                if (value.contains("{papi:")) {
                    Pattern papiPattern = Pattern.compile("\\{papi:(.*?)\\}");
                    Matcher matcher = papiPattern.matcher(value);
                    StringBuilder sb = new StringBuilder();
                    while (matcher.find()) {
                        String placeholder = matcher.group(1);
                        String replacement = PlaceholderAPI.setPlaceholders(null, placeholder);
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(sb);
                    value = sb.toString();
                }

                playerData.put(enabledKey, value);
            });

            res.type("application/json");

            return gson.toJson(playerData);
        });
    }
    public void reloadConfigValues() {
        reloadConfig();
        config = getConfig();
    }

}
