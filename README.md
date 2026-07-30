# Nexilia AI Assistant

Nexilia Network (Paper 1.20.4 / Java 17) icin, sunucu plugin config/messages/permission
dosyalarini indeksleyip OpenAI ile dogal dil sorularina cevap veren admin araci.

## Ozellikler
- `/nexai <soru>` — plugin config dosyalarindan cevap arar, **DOSYA / AYAR / ACIKLAMA**
  formatinda yanit verir.
- `/nexai reindex` — dosyalari manuel yeniden tarar.
- `/nexai reload` — config.yml ve messages.yml'i yeniden yukler (tarama yapmaz).
- Tek izin: **`nexai.admin`** (varsayilan: op).
- World dosyalarina KESINLIKLE dokunmaz (level.dat kontrolu + klasor kara listesi).
- Otomatik/arka plan surekli tarama yoktur; RAM ve CPU dostu.
- Sadece config.yml'de `allowed-filenames` altinda listelenen dosyalar okunur.
- Her sorguda tum index degil, en alakali 1-3 dosya AI'a gonderilir (token tasarrufu).
- Otomatik dosya degistirme YAPMAZ, sadece "bu ayari degistirmemi ister misin?" diye sorar.
- AI cevap veremezse "Bu bilgi bulunamadi, su dosyalari kontrol etmelisin" seklinde
  kontrol edilecek dosyalari listeler.

## Kurulum
1. `mvn package` ile derleyin (Paper 1.20.4 API internetten cekilir).
2. Olusan `target/NexiliaAIAssistant-1.0.0.jar` dosyasini `plugins/` klasorune atin.
3. Sunucuyu bir kez baslatip kapatin (config.yml ve messages.yml otomatik olusur).
4. `plugins/NexiliaAIAssistant/config.yml` icine OpenAI API anahtarinizi girin
   (`openai-api-key`).
5. Sunucuyu tekrar baslatin veya `/nexai reindex` calistirin.

## Ornek Kullanim
```
/nexai TAB scoreboard nereden ayarlanıyor?
```
Cevap:
```
📁 Dosya:
plugins/TAB/config.yml
⚙ Ayar:
scoreboard.enabled
💡 Aciklama:
Bu ayar scoreboard sistemini kontrol eder.
➤ Bu ayari degistirmemi ister misin? (Manuel onay gerekir, otomatik degisiklik yapilmaz)
```

## Config Ozeti (config.yml)
- `openai-api-key`, `model`, `max-tokens`, `temperature`
- `scanner.*` — taranacak dosya adlari, klasorler, world/log haric tutma, boyut/adet limitleri
- `prompt.*` — soru basina AI'a gonderilecek dosya sayisi ve karakter siniri

## Mesajlar (messages.yml)
Tum oyuncuya gorunen metinler (📁⚙💡 formati dahil) buradan degistirilebilir.

## Mimarinin Kisaca Ozeti
```
NexiliaAIAssistant (main)
 ├─ index/FileIndexer   -> guvenli, limitli dosya tarama (sadece acilis/reindex)
 ├─ index/IndexCache    -> bellek-ici cache + basit anahtar-kelime aramasi
 ├─ ai/PromptBuilder    -> soru + en alakali dosyalardan kompakt prompt
 ├─ ai/OpenAIClient     -> java.net.http ile dogrudan OpenAI cagrisi
 ├─ ai/ResponseFormatter-> AI cevabini 📁⚙💡 formatina cevirir
 └─ command/NexAiCommand-> /nexai, /nexai reindex, /nexai reload
```

## Guvenlik / Optimizasyon Notlari
- World, playerdata, region, stats, cache gibi agir klasorler `excluded-folders` ile
  tamamen disaridadir; ayrica her klasorde `level.dat` kontrolu ek guvenlik agidir.
- Tarama sadece acilista (config ile kapatilabilir) veya `/nexai reindex` ile,
  yani surekli CPU/RAM tuketen bir dongu yoktur.
- `max-indexed-files` ve `max-file-size-kb` ile toplam bellek kullanimi sinirlanir.
- OpenAI istegi her zaman async thread'de yapilir, ana sunucu thread'i bloklanmaz.
