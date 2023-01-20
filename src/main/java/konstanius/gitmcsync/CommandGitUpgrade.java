package konstanius.gitmcsync;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static konstanius.gitmcsync.ActionMerge.mergeFiles;
import static konstanius.gitmcsync.CommandGitMerge.fetchFiles;
import static konstanius.gitmcsync.GitMcSync.*;

public class CommandGitUpgrade implements CommandExecutor {
    private final Plugin plugin;

    public CommandGitUpgrade(GitMcSync gitMcSync) {
        plugin = gitMcSync;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("gitsync.merge")) {
            sender.sendMessage(getString("permission-denied"));
            return true;
        }
        if (busy) {
            sender.sendMessage(getString("busy"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(getString("missing-argument"));
            return true;
        }
        busy = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fetchFiles(plugin);
            Path src = Path.of(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/plugins/" + args[0]);
            mergeFiles(src);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (args.length > 1) {
                    if (args[1].matches("-r")) {
                        plugin.getServer().dispatchCommand(sender, "cmi schedule restart");
                    }
                }
                sender.sendMessage(getString("successful-plugin"));
                busy = false;
                if (sender instanceof Player) {
                    try {
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                    } catch (Exception ignored) {
                    }
                }
            });
        });
        return true;
    }
}