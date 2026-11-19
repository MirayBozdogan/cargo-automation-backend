# Veritabanı geçişleri

Şema artık Flyway ile yönetilir. Uygulama açılırken `src/main/resources/db/migration` içindeki `V<sürüm>__<açıklama>.sql` dosyaları sırayla ve bir kez çalıştırılır; uygulanan sürümler `flyway_schema_history` tablosunda tutulur. Hibernate `ddl-auto=validate` ile yalnız entity'lerin şemayla uyumunu kontrol eder, tablo oluşturmaz veya değiştirmez.

## Yeni bir şema değişikliği

Entity'yi değiştirdikten sonra aynı değişikliği yeni bir dosyada yazın, örneğin `V2__add_shipment_note.sql`. Uygulanmış bir `V` dosyası sonradan değiştirilmez; Flyway değişikliği fark edip açılışı durdurur.

## Flyway'den önce oluşturulmuş veritabanları

`ddl-auto=update` ile oluşmuş, içinde tablo olan ve `flyway_schema_history` tablosu bulunmayan bir veritabanı ilk açılışta `V1` olarak işaretlenir (`spring.flyway.baseline-on-migrate=true`). `V1__initial_schema.sql` bu veritabanında çalıştırılmaz; mevcut kayıtlar değişmez. Ardından Hibernate şemayı doğrular.

Çok eski bir veritabanında gönderi durum/tarih veya kurye sütunları yoksa doğrulama açılışı durdurur. Bu durumda önce aşağıdaki iki dosyayı sırayla uygulayın, sonra uygulamayı başlatın. Bu dosyalar Flyway tarafından otomatik çalıştırılmaz.

## Gönderi durum ve tarih alanları — 001_shipment_lifecycle.sql

`001_shipment_lifecycle.sql` PostgreSQL içindir ve tekrar çalıştırılabilir; mevcut/yarım kalmış verinin geçişini açık tutar.

```sh
psql -v ON_ERROR_STOP=1 --single-transaction -d cargo_automation_dev -f src/main/resources/db/manual/001_shipment_lifecycle.sql
```

Bağlantı kullanıcısını ve parolasını yerel PostgreSQL ayarlarından sağlayın; şifreleri bu dosyaya veya Git'e yazmayın.

- Yeni gönderiler CREATED başlar. Enum değerleri sayısal sıra yerine metin olarak saklanır.
- created_at UTC zaman anını tutar; API'de createdAt olarak döner. Yeni gönderilerde sunucu zamanı kullanılır.
- Eski kayıtların gerçek oluşturulma tarihi kaydedilmemişti. Boş created_at için GEÇİŞ ZAMANI kullanılır; bu tarih gerçek geçmiş oluşturulma zamanı değildir.
- delivered_at başlangıçta boş kalır. Teslim API’si bu alanı sunucunun UTC zamanı ile doldurur.
- SQL yalnız eksik durum/tarih değerlerini doldurur. Mevcut durumlar, tarihler, barkodlar ve silinmiş kayıtlar korunur.
- Genel ShipmentRequest'e status, createdAt veya deliveredAt eklenmez. Durum geçişleri ayrı yetkili API'lerle yapılır.

SQL testini yalnız geliştirme veritabanında şu şekilde çalıştırabilirsiniz. Şema adı benzersiz olmalıdır; test en sonda tüm örnek şemayı ROLLBACK ile geri alır:

```sh
psql -v ON_ERROR_STOP=1 -v test_schema=shipment_migration_test -d cargo_automation_dev -f src/test/resources/shipment-lifecycle-migration.sql
```

## Kurye ilişkisi — 002_shipment_courier.sql

İlk migration'dan sonra, hedef veritabanı/şema kontrol edilerek uygulanır. Nullable `courier_id`, `users(id)` dış anahtarı ve arama indeksi ekler. Mevcut gönderilere otomatik kurye atanmaz; mevcut durum, tarih ve silinme bilgileri değişmez. Tekrar çalıştırıldığında atamaları korur. SQL otomatik çalıştırılmaz.

Kurye ayrı rol değildir: yalnız aktif STAFF atanır. READY_FOR_DISTRIBUTION gönderiye atama, durumu OUT_FOR_DELIVERY yapar. Teslim API'si atanan personel veya ADMIN içindir; atanan personel aktif olmalıdır. Teslim sunucunun UTC zamanı ile bir kez kaydedilir. Eski kayıt OUT_FOR_DELIVERY olup kuryesi yoksa otomatik bir kurye varsayılmaz; teslim isteği reddedilir.

`ShipmentRequest` içine courier/status/tarih alanı eklenmez. `PATCH /shipment/{id}/assign-courier` isteği yalnız `{ "courierId": 7 }` alır; `PATCH /shipment/{id}/deliver` gövde gerektirmez. Detay ve response DTO'ları yalnız kurye id/ad/soyad bilgisini döndürür; User hesabı serileştirilmez.

SQL regresyon denemesi geçici şema içinde çalışır ve rollback ile temizlenir:

```sh
psql -v ON_ERROR_STOP=1 -v test_schema=shipment_courier_test -d cargo_automation_dev -f src/test/resources/shipment-courier-migration.sql
```
