package com.nexilia.aiassistant.command;

import com.nexilia.aiassistant.NexiliaAIAssistant;
import com.nexilia.aiassistant.ai.PromptBuilder;
import com.nexilia.aiassistant.ai.ResponseFormatter;
import com.nexilia.aiassistant.index.FileEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Level;

public class NexAiCommand implements CommandExecutor {

    private final NexiliaAIAssistant plugin;
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ResponseFormatter formatter;

    public NexAiCommand(NexiliaAIAssistant plugin) {
        this.plugin = plugin;
        this.formatter = new ResponseFormatter(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Not: izin kontrolu Bukkit tarafindan plugin.yml'deki "permission: nexai.admin" ile otomatik yapilir.

        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reindex")) {
            sender.sendMessage(plugin.msg("reindex-start"));
            plugin.getFileIndexer().scanAsync(sender);
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.msg("reload-done"));
            return true;
        }

        String question = String.join(" ", args);
        sender.sendMessage(plugin.msg("thinking"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handleQuestion(sender, question));
        return true;
    }

    private void handleQuestion(CommandSender sender, String question) {
        int maxRelevant = plugin.cfg().getInt("prompt.max-relevant-files", 3);
        int maxChars = plugin.cfg().getInt("prompt.max-chars-per-file", 1500);

        List<FileEntry> matches = plugin.getIndexCache().search(question, maxRelevant);

        if (matches.isEmpty()) {
            reply(sender, plugin.msg("not-found-generic"));
            return;
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userContent = promptBuilder.buildUserContent(question, matches, maxChars);

        plugin.getOpenAIClient().ask(systemPrompt, userContent).whenComplete((aiResponse, error) -> {
            if (error != null) {
                handleError(sender, error, matches);
                return;
            }
            String formatted = formatter.format(aiResponse, matches);
            reply(sender, formatted);
        });
    }

    private void handleError(CommandSender sender, Throwable error, List<FileEntry> matches) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage() == null ? "" : cause.getMessage();

        plugin.getLogger().log(Level.WARNING, "[NexAI] Soru islenirken hata: " + message);

        if (message.contains("api-key-missing")) {
            reply(sender, plugin.msg("api-key-missing"));
        } else {
            // Genel hata: kullaniciyi bos birakma, en azindan hangi dosyalara bakabilecegini soyle.
            reply(sender, plugin.msg("api-error"));
            if (!matches.isEmpty()) {
                String files = matches.stream().map(FileEntry::getRelativePath)
                        .reduce((a, b) -> a + "\n" + b).orElse("");
                reply(sender, plugin.msg("not-found").replace("{file}", files));
            }
        }
    }

    private void reply(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }
}
