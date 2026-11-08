# Türkiye il ve ilçe verileri

Bu klasördeki SQL dosyası 81 il ve merkez ilçeler dahil 973 ilçenin adlarını içerir. 8 Kasım 2026 tarihinde ubeydeozdmr/turkiye-api projesinin aşağıdaki sabit commitinden alınmıştır:

- Kaynak: https://github.com/ubeydeozdmr/turkiye-api/tree/a0ad8351cb52153e7a92aa7ba9a5bc1e3294da8d/datasets
- provinces.json Git blob: 815e91e843c089b58799996fde73e59a0c6a4a92
- districts.json Git blob: 35bc98e30b1454c288d4126fc6cc2eb3d28c16fb
- Kullanılan alanlar: provinces.name, districts.name ve provinceId ilişkisi.

Kaynak açık kaynak bir derlemedir; resmî canlı nüfus/adres servisi değildir. Resmî İçişleri portalı bu çalışmada yanıt vermediği için tüm listenin resmî kaynaktan güncel doğrulaması yapılamadı. Kaynak sürümü sabitlenmiştir; ileride idari değişiklikler olduğunda veri gözden geçirilmelidir.

Kaynak/plaka IDleri uygulamanın tablo IDleri yerine kullanılmaz. Eksik iller isimle, ilçeler ise il+ilçe adıyla eklenir. Mevcut satırlar silinmez veya güncellenmez. Aynı script tekrar çalıştırıldığında kayıtlar çoğalmaz. Kullanıcılar tarafından sonradan oluşturulmuş farklı yazımlı/aynı adlı mükerrer kayıtlar otomatik birleştirilmez.

Dev profilinde önce dev-data.sql (DB guard ve örnek fiyatlar), sonra bu dosya çalışır. Mevcut geliştirme veritabanına elle uygulamak için doğru bağlantı ayarlarıyla tek bir transaction kullanın:

```sh
psql -X -v ON_ERROR_STOP=1 --single-transaction -d cargo_automation_dev -f src/main/resources/db/reference/turkey-locations.sql
```

Bu aktarım kullanıcı talebiyle ayrıca transaction/tablo kilidi ve önce/sonra veri kontrolüyle uygulanmıştır. Uygulamanın çalışma anında bu kaynağa ağ isteği yapılmaz.

## Kaynak lisansı

MIT License

Copyright (c) 2022-2026 Ubeyde Emir Özdemir

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
