package konstanius.gitmcsync;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static konstanius.gitmcsync.ActionMerge.mergeFiles;
import static konstanius.gitmcsync.ActionMerge.mergeReloads;
import static konstanius.gitmcsync.GitMcSync.*;

public class CommandGitMerge implements CommandExecutor {
    private final Plugin plugin;

    public CommandGitMerge(GitMcSync gitMcSync) {
        plugin = gitMcSync;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("gitsync.merge")) {
            sender.sendMessage(getString("permission-denied"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(getString("missing-argument"));
            return true;
        }
        if (current.equals(old)) {
            sender.sendMessage(getString("outdated-commit"));
            return true;
        }
        if (!args[0].equals(current)) {
            sender.sendMessage(getString("invalid-commit"));
            return true;
        }
        if (!ready) {
            sender.sendMessage(getString("outdated-commit"));
            return true;
        }
        if (busy) {
            sender.sendMessage(getString("busy"));
            return true;
        }
        busy = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fetchFiles(plugin);
            if (!current.equals(getString("repository-url"))) {
                old = current;
            }
            if (args.length > 1 && ((Arrays.asList(args).contains("-r")) || Arrays.asList(args).contains("-s"))) {
                mergeFiles("");
                ready = false;
                sender.sendMessage(getString("successful-commit"));
                busy = false;
            } else {
                mergeReloads(sender);
            }
            if (args.length > 1 && Arrays.asList(args).contains("-r")) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().dispatchCommand(sender, "cmi schedule restart"));
            }
            if (sender instanceof Player) {
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    public static void cloneRepo(Plugin plugin) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(getString("repository-url").replace("https://", "https://oauth2:" + getString("token") + "@"));
        if (Boolean.parseBoolean(getString("authenticate"))) {
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(getString("token"), ""));
        }
        cloneCommand.setDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone"));
        cloneCommand.call();
    }

    public static void fetchFiles(Plugin plugin) {
        try {
            FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoOld"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            FileUtils.copyDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone"), new File(plugin.getDataFolder().getAbsolutePath() + "/RepoOld"), true);
        } catch (Exception ignored) {
        }

        if(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/.git").exists()){
            try {
                InitCommand initCommand = Git.init();
                initCommand.setDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone"));
                Git git = initCommand.call();
                git.checkout().setCreateBranch(false).setName("master").call();
                ObjectId objectId = git.getRepository().resolve("refs/remotes/origin/master");
                MergeCommand mergeCommand = git.merge().setMessage("Merge to master.").include(objectId).setCommit(true);
                MergeResult result = mergeCommand.call();
                MergeResult.MergeStatus status = result.getMergeStatus();

                if (!status.equals(MergeResult.MergeStatus.MERGED) && !status.equals(MergeResult.MergeStatus.ALREADY_UP_TO_DATE)){
                    try {
                        FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cloneRepo(plugin);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Files.createDirectory(Path.of(plugin.getDataFolder() + "/RepoClone"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                cloneRepo(plugin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/plugins/GitMcSync"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*try {
            FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/.git"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            (new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/.git")).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            (new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/.gitignore")).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            (new File(plugin.getDataFolder().getAbsolutePath() + "/RepoClone/README.md")).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
