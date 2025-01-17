package konstanius.gitmcsync;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import static konstanius.gitmcsync.GitMcSync.*;

public class CommandGitExport implements CommandExecutor {
    private final Plugin plugin;

    public CommandGitExport(GitMcSync gitMcSync) {
        plugin = gitMcSync;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("gitsync.export")) {
            sender.sendMessage(getString("no-permission"));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(getString("missing-argument"));
            return true;
        }
        String path = plugin.getServer().getWorldContainer().getAbsolutePath() + args[0];
        File original = new File(path);
        if (!original.exists()) {
            sender.sendMessage(getString("invalid-directory"));
            return true;
        }
        if (!Boolean.parseBoolean(getString("authenticate"))) {
            sender.sendMessage(getString("not-authenticated"));
            return true;
        }
        if (busy) {
            sender.sendMessage(getString("busy"));
            return true;
        }
        busy = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                try {
                    FileUtils.deleteDirectory(new File(plugin.getDataFolder().getAbsolutePath().replace("/.", "") + "/RepoTemp"));
                    Files.createDirectory(Path.of(plugin.getDataFolder().getAbsolutePath().replace("/.", "") + "/RepoTemp"));
                } catch (Exception ignored) {
                }
                File file = new File(plugin.getDataFolder().getAbsolutePath().replace("/.", "") + "/RepoTemp");
                Git git = Git.cloneRepository()
                        .setURI(getString("repository-url").replace("https://", "https://oauth2:" + getString("token") + "@"))
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(getString("token"), ""))
                        .setDirectory(file)
                        .call();
                Repository repository = git.getRepository();
                Path newPath = Path.of(plugin.getServer().getWorldContainer().getAbsolutePath().replace("/.", "/plugins/GitMcSync/RepoTemp" + args[0]));

                if(original.isDirectory()){
                    try {
                        Files.createDirectories(newPath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    FileUtils.copyDirectory(new File(path), new File(newPath.toString()));
                    if (Boolean.parseBoolean(getString("whitelist-filetypes"))) {
                        try {
                            Files.walkFileTree(newPath, new FileVisitor<>() {
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                    return FileVisitResult.CONTINUE;
                                }

                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                    for (String type : Objects.requireNonNull(config.getStringList("filetypes"))) {
                                        if (file.toAbsolutePath().toString().contains(type)) {
                                            return FileVisitResult.CONTINUE;
                                        }
                                    }
                                    (new File(file.toAbsolutePath().toString())).delete();
                                    return FileVisitResult.CONTINUE;
                                }

                                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                    return FileVisitResult.CONTINUE;
                                }

                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                    return FileVisitResult.CONTINUE;
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }else if(original.isFile()){
                    Files.createDirectories(newPath.getParent());
                    Files.copy(Path.of(path), newPath, StandardCopyOption.REPLACE_EXISTING);
                    if (Boolean.parseBoolean(getString("whitelist-filetypes"))) {
                        boolean delete = true;
                        for (String type : Objects.requireNonNull(config.getStringList("filetypes"))) {
                            if (newPath.toString().contains(type)) {
                                delete = false;
                                break;
                            }
                        }
                        if(delete) (new File(newPath.toString())).delete();
                    }
                }

                try {
                    FileUtils.deleteDirectory(new File(newPath + "/plugins/GitMcSync"));
                } catch (Exception ignored) {
                }
                git.add().addFilepattern(".").call();
                RevCommit result = git.commit().setMessage(getString("export-message").replace("%path%", args[0]).replace("%player%", sender.getName())).call();
                git.push()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(getString("token"), ""))
                        .call();
                sender.sendMessage(getString("export-successful"));
                busy = false;
                if (sender instanceof Player) {
                    try {
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
