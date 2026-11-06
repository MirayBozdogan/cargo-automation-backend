# Backend kontrolleri

Java 21, PostgreSQL ve PATH içinde psql gereklidir. Maven Wrapper bağımlılıkları ilk kullanımda internetten indirir.

DB_URL, DB_USERNAME ve DB_PASSWORD değerlerini ortam değişkenleriyle veya repo kökündeki Git dışında tutulan `.env.local.properties` dosyasıyla sağlayın. Şifresiz şablon `.env.local.properties.example` dosyasındadır. Dosyada düz `KEY=value` satırları kullanılır; ortam değişkenleri önceliklidir. Bu değerleri komut geçmişine yazmayın.

```sh
bash scripts/test-isolated.sh
```

Komut yalnız adı `_dev` veya `_test` ile biten veritabanlarında çalışır. Desteklenen URL biçimi `jdbc:postgresql://localhost:5432/cargo_automation_dev` şeklindedir; URL sorgu parametreleri bu komutta kabul edilmez. Bağlantı kullanıcısının bu veritabanında şema oluşturma yetkisi olmalıdır.

Her çalıştırmada benzersiz, boş bir PostgreSQL şeması oluşturulur. Hibernate ve bağlantının currentSchema ayarları bu şemaya yöneltilir. Dev seed ve yönetici oluşturma kapalıdır; ayrı bir test JWT anahtarı kullanılır. Maven `clean verify` bütün testleri çalıştırır ve uygulamayı paketler. Normal tamamlanma veya hata halinde geçici şema silinir. Zorla kapatılan süreçlerde kalan `cargo_tests_...` şeması elle temizlenebilir.

Testler fiyat, gönderi ve kullanıcı verileri oluşturur; doğrudan `./mvnw test` komutunu kendi geliştirme kayıtlarınızın bulunduğu bağlantıyla çalıştırmayın. İzole komut ortak public şemasına test verisi yazmaz.

Sıfırdan kontrol için temiz checkout üzerinde kendi bağlantı ayarlarınızı sağlayıp yukarıdaki komutu çalıştırın. Başarı sonucunda `target/staj1-0.0.1-SNAPSHOT.jar` oluşur.
