package com.nexilia.aiassistant.ai;

import com.nexilia.aiassistant.index.FileEntry;

import java.util.List;

/**
 * Token tasarrufu icin: tum index degil, sadece en alakali (IndexCache.search
 * tarafindan secilen) az sayida dosya AI'a gonderilir ve her dosya
 * max-chars-per-file ile kirpilir.
 */
public class PromptBuilder {

    public static final String NOT_FOUND_MARKER = "NOT_FOUND";

    public String buildSystemPrompt() {
        return """
                Sen bir Minecraft sunucusu (Nexilia Network) icin teknik config asistanisin.
                Sana bazi plugin config/ayar dosyalarinin icerigi ve bir soru verilecek.

                KURALLAR:
                1. SADECE sana verilen dosya icerigine dayanarak cevap ver, tahmin/uydurma yapma.
                2. Cevap TAM OLARAK su formatta olmali (baska hicbir sey ekleme):
                DOSYA: <dosyanin tam yolu>
                AYAR: <ilgili config anahtari/bolumu>
                ACIKLAMA: <sorunun cevabi, 1-3 cumle, Turkce, teknik ve net>
                3. Eger verilen dosya icerigi soruyu cevaplamak icin yetersizse, SADECE
                   şu tek kelimeyi yaz: NOT_FOUND
                4. Asla dosyayi otomatik degistirmeyi teklif etme veya degistirdigini soyleme,
                   sadece mevcut ayari acikla.
                5. Yanitinda markdown, kod bloğu veya fazladan yorum kullanma.
                """;
    }

    public String buildUserContent(String question, List<FileEntry> matches, int maxCharsPerFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("SORU: ").append(question).append("\n\n");

        if (matches.isEmpty()) {
            sb.append("(Ilgili dosya bulunamadi.)\n");
            return sb.toString();
        }

        sb.append("ILGILI DOSYALAR:\n");
        for (FileEntry entry : matches) {
            String content = entry.getContent();
            if (content.length() > maxCharsPerFile) {
                content = content.substring(0, maxCharsPerFile) + "\n...(kirpildi)...";
            }
            sb.append("---\n");
            sb.append("YOL: ").append(entry.getRelativePath()).append("\n");
            sb.append("ICERIK:\n").append(content).append("\n");
        }
        return sb.toString();
    }
}
