package tk.darklegacymc.apimachine;
import com.google.gson.Gson;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spark.Request;
import spark.Response;
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
        Objects.requireNonNull(this.getCommand("apimachine")).setExecutor(new ReloadCommand(this));
        this.getLogger().info("APIMachine started. Wiki: https://mallusrgreat.gitbook.io/mallusrgreats-plugins/");
    }

    public void setupRoutes() {
        get("/favicon.ico", (req, res) -> "");
        get("/api/:endpoint", this::playerNotRequiredRoute);
        get("/api/:endpoint/:username", this::playerRequiredRoute);
    }
    public void reloadConfigValues() {
        reloadConfig();
        config = getConfig();
    }
    private String playerNotRequiredRoute(Request req, Response res) {
        Gson gson = new Gson();
        String requestedEndpoint = req.params("endpoint");
        if (config.getConfigurationSection("endpoints") == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "endpoints not found.");
            return gson.toJson(errorMap);
        }

        if (!config.isConfigurationSection("endpoints." + requestedEndpoint)) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Endpoint '" + requestedEndpoint + "' not found.");
            return gson.toJson(errorMap);
        }

        if (config.getConfigurationSection("endpoints." + requestedEndpoint + ".object") == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "endpoints." + requestedEndpoint + ".object not found.");
            return gson.toJson(errorMap);
        }

        Set<String> objectKeys = Objects.requireNonNull(config.getConfigurationSection("endpoints." + requestedEndpoint + ".object")).getKeys(false);
        Map<String, String> data = new HashMap<>();
        for (String key : objectKeys) {
            String value = config.getString("endpoints." + requestedEndpoint + ".object." + key);
            assert value != null;
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
            data.put(key, value);
        }
        return gson.toJson(data);
    }

    private String playerRequiredRoute(Request req, Response res) {
        Gson gson = new Gson();
        String requestedEndpoint = req.params("endpoint");
        String requestedUsername = req.params("username");
        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(requestedUsername);

        if (config.getConfigurationSection("endpoints") == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "endpoints not found.");
            return gson.toJson(errorMap);
        }

        if (!config.isConfigurationSection("endpoints." + requestedEndpoint)) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "Endpoint '" + requestedEndpoint + "' not found.");
            return gson.toJson(errorMap);
        }

        if (config.getConfigurationSection("endpoints." + requestedEndpoint + ".object") == null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", "endpoints." + requestedEndpoint + ".object not found.");
            return gson.toJson(errorMap);
        }

        Set<String> objectKeys = Objects.requireNonNull(config.getConfigurationSection("endpoints." + requestedEndpoint + ".object")).getKeys(false);
        Map<String, String> data = new HashMap<>();
        for (String key : objectKeys) {
            String value = config.getString("endpoints." + requestedEndpoint + ".object." + key);
            assert value != null;
            value = value.replaceAll("\\{username\\}", Matcher.quoteReplacement(Optional.ofNullable(offlinePlayer.getName()).orElse("")));

            LocalDateTime lastSeen = LocalDateTime.ofEpochSecond(offlinePlayer.getLastSeen() / 1000L, 0, java.time.ZoneOffset.UTC);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            value = value.replaceAll("\\{last_seen\\}", Matcher.quoteReplacement(formatter.format(lastSeen)));

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
            data.put(key, value);
        }
        return gson.toJson(data);
    }
}
