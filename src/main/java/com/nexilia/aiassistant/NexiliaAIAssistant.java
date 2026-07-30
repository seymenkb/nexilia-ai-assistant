package com.nexilia.aiassistant;

import com.nexilia.aiassistant.ai.OpenAIClient;
import com.nexilia.aiassistant.command.NexAiCommand;
import com.nexilia.aiassistant.command.NexAiTabCompleter;
import com.nexilia.aiassistant.index.FileIndexer;
import com.nexilia.aiassistant.index.IndexCache;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Nexilia AI Assistant
 * Sunucu plugin config/messages/permission dosyalarini indeksleyip
 * OpenAI uzerinden dogal dil sorularina cevap veren yardimci plugin.
 *
 * Onemli: World dosyalarina KESINLIKLE dokunmaz, surekli/arka plan tarama YAPMAZ.
 * Tarama sadece sunucu acilisinda (config'e bagli) ya da /nexai reindex ile calisir.
 */
public final class NexiliaAIAssistant extends JavaPlugin {

    private static NexiliaAIAssistant instance;

    private YamlConfiguration messages;
    private IndexCache indexCache;
    private FileIndexer fileIndexer;
    private OpenAIClient openAIClient;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadMessages();

        this.indexCache = new IndexCache();
        this.fileIndexer = new FileIndexer(this, indexCache);
        this.openAIClient = new OpenAIClient(this);

        NexAiCommand executor = new NexAiCommand(this);
        var command = getCommand("nexai");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(new NexAiTabCompleter());
        }

        if (getConfig().getBoolean("scanner.scan-on-startup", true)) {
            // Async calisir; sunucu acilisini bloklamaz, RAM/CPU kontrolu FileIndexer icinde.
            fileIndexer.scanAsync(null);
        } else {
            getLogger().info("[NexAI] Otomatik tarama kapali. '/nexai reindex' ile manuel taratabilirsiniz.");
        }
    }

    @Override
    public void onDisable() {
        if (indexCache != null) {
            indexCache.clear();
        }
        getLogger().info("[NexAI] Kapatiliyor, index bellekten temizlendi.");
    }

    /** config.yml + messages.yml yeniden yuklenir. Dosyalar TEKRAR TARANMAZ (ayri islem: reindex). */
    public void reloadAll() {
        reloadConfig();
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        // Jar icindeki varsayilanlari fallback olarak ekle (eksik key durumunda hata vermesin).
        try (var stream = getResource("messages.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                this.messages.setDefaults(defaults);
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "messages.yml varsayilanlari yuklenemedi.", e);
        }
    }

    public String msg(String path) {
        String raw = messages.getString(path, "&c[Eksik mesaj: " + path + "]");
        String prefix = messages.getString("prefix", "");
        return prefix + raw;
    }

    public String rawMsg(String path) {
        return messages.getString(path, "");
    }

    public FileConfiguration cfg() {
        return getConfig();
    }

    public IndexCache getIndexCache() {
        return indexCache;
    }

    public FileIndexer getFileIndexer() {
        return fileIndexer;
    }

    public OpenAIClient getOpenAIClient() {
        return openAIClient;
    }

    public static NexiliaAIAssistant get() {
        return instance;
    }
}
