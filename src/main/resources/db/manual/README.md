# Gönderi durum ve tarih alanları

`001_shipment_lifecycle.sql` PostgreSQL içindir. Flyway kurulmuş değildir; bu dosya uygulama tarafından otomatik bir migration olarak çalıştırılmaz. Güncellenmiş backend'i ilk kez başlatmadan önce veritabanına uygulanır. Projedeki Hibernate update ayarı da yeni sütunları aynı varsayılanlarla oluşturabilir; SQL dosyası mevcut/yarım kalmış verinin geçişini açık ve tekrar çalıştırılabilir tutar.

```sh
psql -v ON_ERROR_STOP=1 --single-transaction -d cargo_automation_dev -f src/main/resources/db/manual/001_shipment_lifecycle.sql
```

Bağlantı kullanıcısını ve parolasını yerel PostgreSQL ayarlarından sağlayın; şifreleri bu dosyaya veya Git'e yazmayın.

- Yeni gönderiler CREATED başlar. Enum değerleri sayısal sıra yerine metin olarak saklanır.
- created_at UTC zaman anını tutar; API'de createdAt olarak döner. Yeni gönderilerde sunucu zamanı kullanılır.
- Eski kayıtların gerçek oluşturulma tarihi kaydedilmemişti. Boş created_at için GEÇİŞ ZAMANI kullanılır; bu tarih gerçek geçmiş oluşturulma zamanı değildir.
- delivered_at başlangıçta boş kalır. Teslim işlemi sonraki oturumda bu alanı dolduracaktır.
- SQL yalnız eksik durum/tarih değerlerini doldurur. Mevcut durumlar, tarihler, barkodlar ve silinmiş kayıtlar korunur.
- Genel ShipmentRequest'e status, createdAt veya deliveredAt eklenmez. Durum geçişleri ayrı yetkili API'lerle sonraki oturumlarda geliştirilecektir.

SQL testini yalnız geliştirme veritabanında şu şekilde çalıştırabilirsiniz. Şema adı benzersiz olmalıdır; test en sonda tüm örnek şemayı ROLLBACK ile geri alır:

```sh
psql -v ON_ERROR_STOP=1 -v test_schema=shipment_migration_test -d cargo_automation_dev -f src/test/resources/shipment-lifecycle-migration.sql
```
