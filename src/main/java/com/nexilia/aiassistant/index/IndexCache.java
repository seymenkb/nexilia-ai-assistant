package com.nexilia.aiassistant.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basit bellek-ici cache. Surekli/otomatik yenilenmez; sadece FileIndexer
 * tarama yaptiginda doldurulur. Boylece arka planda RAM/CPU tuketen bir
 * dongu yoktur.
 */
public class IndexCache {

    private final ConcurrentHashMap<String, FileEntry> entries = new ConcurrentHashMap<>();

    public void put(FileEntry entry) {
        entries.put(entry.getRelativePath(), entry);
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public List<FileEntry> all() {
        return new ArrayList<>(entries.values());
    }

    /**
     * Soru metnine gore basit anahtar-kelime skorlamasiyla en alakali dosyalari dondurur.
     * Harici bir arama kutuphanesi kullanilmaz; amac token/CPU tasarrufu.
     */
    public List<FileEntry> search(String question, int maxResults) {
        String q = question.toLowerCase(Locale.ROOT);
        String[] words = q.split("[^a-z0-9çğıöşü]+");

        List<ScoredEntry> scored = new ArrayList<>();
        for (FileEntry entry : entries.values()) {
            int score = 0;
            String path = entry.getRelativePath().toLowerCase(Locale.ROOT);
            String plugin = entry.getPluginName() == null ? "" : entry.getPluginName().toLowerCase(Locale.ROOT);
            String content = entry.getContent().toLowerCase(Locale.ROOT);

            for (String w : words) {
                if (w.isBlank() || w.length() < 2) continue;
                if (plugin.contains(w)) score += 5;      // plugin adi eslesmesi en degerli
                if (path.contains(w)) score += 3;
                if (content.contains(w)) score += 1;
            }

            if (score > 0) {
                scored.add(new ScoredEntry(entry, score));
            }
        }

        scored.sort(Comparator.comparingInt((ScoredEntry se) -> se.score).reversed());

        List<FileEntry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, scored.size()); i++) {
            result.add(scored.get(i).entry);
        }
        return result;
    }

    private record ScoredEntry(FileEntry entry, int score) {
    }
}
