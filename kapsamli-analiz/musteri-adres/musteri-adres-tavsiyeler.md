# Müşteri Adresi Ekleme Endpoint'i İçin Best Practice Tavsiyeleri

## 1. Zorunlu alanları net tanımla
Bu yaklaşım kullan:
- `cityId`, `districtId`, `neighborhood` zorunlu olsun.
- boş string ve whitespace kabul edilmesin.

## 2. DTO ile giriş yapısını tanımla
Bu mantık daha temiz olur:
- request DTO kullan.
- `neighborhood` alanı `String` olsun.
- `cityId` ve `districtId` `Integer` olsun.

## 3. Validasyon ekle
Önerilen kurallar:
- `cityId`: `@NotNull`
- `districtId`: `@NotNull`
- `neighborhood`: `@NotBlank` ve `@Size(max = 100)`

## 4. Tip güvenliğini koru
Bu yaklaşım uygundur:
- `neighborhood` sadece string olmalı.
- sayı/boolean gibi farklı tipler reddedilmeli.

## 5. Referans doğrulamasını netleştir
Şu yaklaşım daha iyi olur:
- geçersiz `cityId` veya `districtId` için 404/400 dön.
- `userId` yoksa da net hata dön.

## 6. Exception handling ekle
Daha profesyonel yaklaşım:
- `MethodArgumentNotValidException`
- `EntityNotFoundException`
- `IllegalArgumentException`

gibi durumlar için handler eklenmeli.

## 7. Testleri sınır değerlerle yaz
Şu senaryoları mutlaka test et:
- geçerli adres ekleme
- eksik alan
- boş/whitespace neighborhood
- geçersiz city/district id
- yanlış tipte neighborhood
- bozuk JSON body

## 8. Veritabanı tarafında da koruma ekle
Bu yaklaşım iyi sonuç verir:
- `neighborhood` için uygun uzunluk kısıtı eklenebilir.
- ilişkisel veriler için foreign key kontrolü korunmalı.

## 9. Kısa öneri
Müşteri adresi ekleme endpoint'i için başlangıç olarak şu adımlar uygun olur:
1. zorunlu alanları net tanımla
2. boş/null/whitespace değerlerini reddet
3. tip güvenliğini koru
4. invalid referanslar için net hata dön
5. testleri ekleyerek doğrula
