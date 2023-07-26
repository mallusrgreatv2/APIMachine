package tk.darklegacymc.apimachine.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import spark.Spark;
import tk.darklegacymc.apimachine.APIMachine;

public class ReloadCommand implements CommandExecutor {
    private APIMachine plugin;
    public ReloadCommand(APIMachine apiMachine) {
        plugin = apiMachine;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings[0].equals("reload")) {
            if(!commandSender.hasPermission("apimachine.reload")) {
                commandSender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this plugin.");
                return true;
            }
            plugin.reloadConfigValues();
            commandSender.sendMessage("Reloaded APIMachine.");
            return true;
        }
        return false;
    }
}
