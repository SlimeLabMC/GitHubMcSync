package konstanius.gitmcsync;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static konstanius.gitmcsync.GitMcSync.*;

public class ActionMerge {
    public static void mergeAll(Plugin plugin, CommandSender sender) {
        Path src = Path.of(plugin.getDataFolder().getAbsolutePath() + "/RepoClone");
        mergeFiles(src);
        ready = false;
        sender.sendMessage(getString("successful-commit"));


        Plugin[] plugins = plugin.getServer().getPluginManager().getPlugins();
        Arrays.sort(plugins, Comparator.comparing(Plugin::getName));
        for(int i = 0; i < plugins.length; i += 3) {
            Plugin p1 = plugins[i];
            TextComponent tc1 = new TextComponent(getString("reload-format").replace("%plugin%", p1.getName()));
            tc1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/BileTools:bile reload " + p1.getName()));
            tc1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(getString("reload-hover").replace("%plugin%", p1.getName()))));
            TextComponent tc2;
            if(plugins.length > i + 1) {
                Plugin p2 = plugins[i + 1];
                tc2 = new TextComponent(getString("reload-format").replace("%plugin%", p2.getName()));
                tc2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/BileTools:bile reload " + p2.getName()));
                tc2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(getString("reload-hover").replace("%plugin%", p2.getName()))));
                tc1.addExtra(tc2);
            }
            TextComponent tc3;
            if(plugins.length > i + 2) {
                Plugin p3 = plugins[i + 2];
                tc3 = new TextComponent(getString("reload-format").replace("%plugin%", p3.getName()));
                tc3.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/BileTools:bile reload " + p3.getName()));
                tc3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(getString("reload-hover").replace("%plugin%", p3.getName()))));
                tc1.addExtra(tc3);
            }
            sender.spigot().sendMessage(tc1);
        }
        busy = false;
    }

    public static void mergeFiles(Path src) {
        List<Path> pathsNew = new ArrayList<>();
        try {
            Files.walkFileTree(src, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    pathsNew.add(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Path newPath = Path.of(String.valueOf(dir).replace("plugins/GitMcSync/RepoClone/", ""));
                    try {
                        Files.copy(Path.of(dir.toFile().getAbsolutePath()), newPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch(DirectoryNotEmptyException ignored) {}
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(Path p: pathsNew) {
            Path newPath = Path.of(String.valueOf(p).replace("plugins/GitMcSync/RepoClone/", ""));
            try {
                Files.copy(Path.of(p.toFile().getAbsolutePath()), newPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }
    }
}
