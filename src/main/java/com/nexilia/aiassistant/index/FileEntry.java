package com.nexilia.aiassistant.index;

/**
 * Indekslenen tek bir dosyayi temsil eder.
 * Sadece hafif veriler tutulur (tam dosya her zaman bellekte agir durmaz,
 * icerik zaten tarama sirasinda maxCharsi asilarak kirpilir).
 */
public class FileEntry {

    private final String relativePath; // ornek: plugins/Towny/config.yml
    private final String pluginName;   // ornek: Towny
    private final String fileName;     // ornek: config.yml
    private final String content;      // kirpilmis dosya icerigi

    public FileEntry(String relativePath, String pluginName, String fileName, String content) {
        this.relativePath = relativePath;
        this.pluginName = pluginName;
        this.fileName = fileName;
        this.content = content;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }
}
