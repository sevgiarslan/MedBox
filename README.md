# MedBox

Android için ev ilaç envanteri uygulaması. Kotlin + Jetpack Compose ile yazıldı.

## Özellikler

- **İlaç listesi**: Tüm ilaçlar son kullanma tarihine göre sıralı listelenir; süresi geçmiş/yaklaşan ilaçlar renkli rozetlerle vurgulanır.
- **Arama**: İsme göre anlık arama.
- **Etiketleme**: Ağrı kesici, antibiyotik, çocuk için, vitamin gibi hazır etiketler + kendi etiketini oluşturma. Etikete göre filtreleme.
- **Barkod ile tekrar-stok kısayolu**: CameraX + ML Kit ile kamerayı barkoda tutarak barkod numarasını okur ve barkod alanını doldurur. Bunun amacı isim/tarihi otomatik çekmek değil (genel, güvenilir bir ilaç barkod veritabanı yok) — asıl fayda, **aynı ürünü tekrar aldığında** barkodu tekrar okutunca uygulamanın "bu ürün zaten envanterde, mevcut kaydı güncellemek ister misin?" diye sorup seni doğrudan o kayda götürmesi; böylece isim/etiket gibi bilgileri yeniden yazmadan sadece miktarı ve yeni kutunun son kullanma tarihini güncelleyebilirsin. İlk ekleyişte ilaç adı ve son kullanma tarihi her zaman elle girilir.
- **Son kullanma tarihi bildirimleri**: Günde bir kez arka planda (WorkManager) kontrol edilir; süresi dolmuş veya 30 gün içinde dolacak ilaçlar için bildirim gönderilir.

## Proje yapısı

```
app/src/main/java/com/medbox/app/
  data/         Room entity/DAO/veritabanı, repository
  notification/ WorkManager worker + bildirim yardımcıları
  barcode/      ML Kit barkod analiz sınıfı
  ui/           Compose ekranları (liste, ekle/düzenle, barkod tarama), navigasyon, tema
  util/         Basit elle yazılmış DI konteyneri
```

Veriler tamamen cihazda, yerel bir SQLite (Room) veritabanında tutulur; herhangi bir sunucuya veri gönderilmez.

## Derleme

Bu proje standart bir Gradle/Android Gradle Plugin projesidir:

```
./gradlew assembleDebug
```

APK'yı fiziksel telefona kurmak için (USB hata ayıklama açıkken):

```
./gradlew installDebug
```

Gereksinimler: Android Studio (Koala veya üzeri) ya da JDK 17 + Android SDK (compileSdk 34) kurulu bir ortam. Kamera ve bildirim izinleri ilk açılışta istenir.

### Bilgisayarsız kurulum (GitHub Actions)

Bu depoda her push'ta debug APK'yı derleyen bir GitHub Actions iş akışı var (`.github/workflows/build-apk.yml`). Android Studio kurmadan, telefonda da kurulum yapılabilir:

- Güncel APK: **[Releases → latest-debug](https://github.com/sevgiarslan/MedBox/releases/latest)** sayfasından `MedBox-debug.apk` dosyasını doğrudan indirip telefonda kurabilirsin ("bilinmeyen kaynaklardan yükleme" izni gerekir).

## Kullanılan teknolojiler

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (yerel veritabanı)
- WorkManager (günlük son kullanma tarihi kontrolü)
- CameraX + ML Kit Barcode Scanning (barkod okuma)
