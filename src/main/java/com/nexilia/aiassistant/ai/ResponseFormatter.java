package com.nexilia.aiassistant.ai;

import com.nexilia.aiassistant.NexiliaAIAssistant;
import com.nexilia.aiassistant.index.FileEntry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI'nin "DOSYA / AYAR / ACIKLAMA" formatindaki ham metnini,
 * messages.yml'deki oyuncuya-gorunur formata cevirir.
 */
public class ResponseFormatter {

    private static final Pattern PATTERN = Pattern.compile(
            "DOSYA:\\s*(.+?)\\s*\\n+AYAR:\\s*(.+?)\\s*\\n+ACIKLAMA:\\s*(.+)",
            Pattern.DOTALL
    );

    private final NexiliaAIAssistant plugin;

    public ResponseFormatter(NexiliaAIAssistant plugin) {
        this.plugin = plugin;
    }

    public String format(String aiRawResponse, List<FileEntry> candidates) {
        if (aiRawResponse == null || aiRawResponse.isBlank()
                || aiRawResponse.trim().equalsIgnoreCase(PromptBuilder.NOT_FOUND_MARKER)) {
            return notFound(candidates);
        }

        Matcher m = PATTERN.matcher(aiRawResponse.trim());
        if (!m.find()) {
            // Beklenmeyen format geldiyse (nadir), yine de bulunamadi olarak ele al.
            return notFound(candidates);
        }

        String file = m.group(1).trim();
        String setting = m.group(2).trim();
        String description = m.group(3).trim();

        return plugin.msg("answer-format")
                .replace("{file}", file)
                .replace("{setting}", setting)
                .replace("{description}", description)
                + "\n" + plugin.msg("suggest-change");
    }

    private String notFound(List<FileEntry> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return plugin.msg("not-found-generic");
        }
        String files = candidates.stream()
                .map(FileEntry::getRelativePath)
                .collect(Collectors.joining("\n"));
        return plugin.msg("not-found").replace("{file}", files);
    }
}
