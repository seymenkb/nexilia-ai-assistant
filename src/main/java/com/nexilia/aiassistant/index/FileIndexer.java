package com.nexilia.aiassistant.index;

import com.nexilia.aiassistant.NexiliaAIAssistant;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Sunucu dosyalarini GUVENLI ve SINIRLI sekilde tarar.
 *
 * Garantiler:
 *  - World klasorleri asla taranmaz (level.dat kontrolu + isim kara listesi).
 *  - Sadece config.yml'de belirtilen dosya adlari okunur.
 *  - Dosya basina ve toplamda boyut/adet limiti vardir (RAM korumasi).
 *  - Otomatik/periyodik tarama YOKTUR; sadece acilis (opsiyonel) ve /nexai reindex ile calisir.
 */
public class FileIndexer {

    private final NexiliaAIAssistant plugin;
    private final IndexCache cache;
    private volatile boolean scanning = false;

    public FileIndexer(NexiliaAIAssistant plugin, IndexCache cache) {
        this.plugin = plugin;
        this.cache = cache;
    }

    public boolean isScanning() {
        return scanning;
    }

    /** Taramayi async thread'de calistirir. sender null olabilir (konsol/acilis durumu). */
    public void scanAsync(CommandSender sender) {
        if (scanning) {
            if (sender != null) sender.sendMessage(plugin.msg("reindex-start"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            scanning = true;
            long start = System.currentTimeMillis();
            try {
                doScan();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[NexAI] Tarama sirasinda hata olustu.", e);
            } finally {
                scanning = false;
            }
            long took = System.currentTimeMillis() - start;
            int count = cache.size();
            plugin.getLogger().info("[NexAI] Index tamamlandi: " + count + " dosya, " + took + "ms");
            if (sender != null) {
                String out = plugin.msg("reindex-done")
                        .replace("{count}", String.valueOf(count))
                        .replace("{time}", String.valueOf(took));
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(out));
            }
        });
    }

    private void doScan() {
        cache.clear();

        FileConfiguration cfg = plugin.cfg();
        Set<String> allowedNames = new HashSet<>(cfg.getStringList("scanner.allowed-filenames"));
        Set<String> excludedFolders = new HashSet<>(cfg.getStringList("scanner.excluded-folders"));
        List<String> includePaths = cfg.getStringList("scanner.include-paths");
        boolean skipWorldFolders = cfg.getBoolean("scanner.skip-world-folders", true);
        long maxFileBytes = cfg.getLong("scanner.max-file-size-kb", 200) * 1024L;
        int maxIndexedFiles = cfg.getInt("scanner.max-indexed-files", 300);

        // plugins/ klasoru = bu pluginin data folder'inin bir ustu.
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        // sunucu kok dizini = plugins klasorunun bir ustu.
        File serverRoot = pluginsFolder.getParentFile();

        for (String rel : includePaths) {
            if (cache.size() >= maxIndexedFiles) break;
            File target = new File(serverRoot, rel);
            if (!target.exists()) continue;

            if (target.isFile()) {
                tryIndexFile(target, rel, serverRoot, allowedNames, maxFileBytes, maxIndexedFiles);
            } else if (target.isDirectory()) {
                walk(target, serverRoot, allowedNames, excludedFolders, skipWorldFolders, maxFileBytes, maxIndexedFiles);
            }
        }
    }

    private void walk(File dir, File serverRoot, Set<String> allowedNames, Set<String> excludedFolders,
                       boolean skipWorldFolders, long maxFileBytes, int maxIndexedFiles) {
        if (cache.size() >= maxIndexedFiles) return;

        File[] children = dir.listFiles();
        if (children == null) return;

        // World klasoru guvenlik agi: level.dat varsa bu klasoru ve alt klasorlerini tamamen atla.
        if (skipWorldFolders && new File(dir, "level.dat").exists()) {
            return;
        }

        for (File child : children) {
            if (cache.size() >= maxIndexedFiles) return;

            if (child.isDirectory()) {
                String name = child.getName().toLowerCase();
                if (excludedFolders.contains(name)) continue;
                walk(child, serverRoot, allowedNames, excludedFolders, skipWorldFolders, maxFileBytes, maxIndexedFiles);
            } else if (allowedNames.contains(child.getName())) {
                String rel = serverRoot.toPath().relativize(child.toPath()).toString().replace('\\', '/');
                indexFile(child, rel, maxFileBytes);
            }
        }
    }

    private void tryIndexFile(File file, String rel, File serverRoot, Set<String> allowedNames,
                               long maxFileBytes, int maxIndexedFiles) {
        if (cache.size() >= maxIndexedFiles) return;
        if (!allowedNames.contains(file.getName())) return;
        indexFile(file, rel, maxFileBytes);
    }

    private void indexFile(File file, String relativePath, long maxFileBytes) {
        try {
            long size = file.length();
            byte[] bytes;
            if (size > maxFileBytes) {
                bytes = new byte[(int) maxFileBytes];
                try (var in = Files.newInputStream(file.toPath())) {
                    int read = in.read(bytes);
                    if (read < bytes.length) {
                        bytes = java.util.Arrays.copyOf(bytes, Math.max(read, 0));
                    }
                }
            } else {
                bytes = Files.readAllBytes(file.toPath());
            }
            String content = new String(bytes, StandardCharsets.UTF_8);

            String pluginName = extractPluginName(relativePath);
            cache.put(new FileEntry(relativePath, pluginName, file.getName(), content));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[NexAI] Dosya okunamadi: " + relativePath, e);
        }
    }

    private String extractPluginName(String relativePath) {
        // plugins/Towny/config.yml -> Towny
        if (relativePath.startsWith("plugins/")) {
            String rest = relativePath.substring("plugins/".length());
            int idx = rest.indexOf('/');
            if (idx > 0) return rest.substring(0, idx);
        }
        return "server";
    }
}
