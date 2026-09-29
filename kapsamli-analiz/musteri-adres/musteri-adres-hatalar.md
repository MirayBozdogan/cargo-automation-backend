# Müşteri Adresi Ekleme Endpoint'i İçin Hata/Problem Notları

## Test edilen endpoint
- POST /users/{userId}/address

## Test yöntemi
Aşağıdaki giriş türleri doğrudan endpoint'e gönderildi:
- geçerli değer
- boş obje
- eksik `cityId`
- eksik `districtId`
- eksik `neighborhood`
- `cityId: null`
- boş string `neighborhood`
- whitespace `neighborhood`
- geçersiz `cityId`
- geçersiz `districtId`
- sayı olarak `neighborhood`
- boolean olarak `neighborhood`
- dizi olarak `neighborhood`
- obje olarak `neighborhood`
- bozuk JSON
- düz string body
- array body
- null body

## Test sonuçları

### 1) Geçerli veri
- Girdi: geçerli `cityId`, `districtId`, `neighborhood`, `buildingNo`, `apartmentNo`
- Sonuç: 200 OK
- Beklenen davranış: kabul edilmelidir.

### 2) Boş obje gönderimi
- Girdi: {}
- Sonuç: 500 Internal Server Error
- Problem: eksik alanlar nedeniyle sunucu hatası oluştu.

### 3) `cityId` eksik
- Girdi: sadece `districtId` ve diğer alanlar var
- Sonuç: 500 Internal Server Error

### 4) `districtId` eksik
- Girdi: sadece `cityId` ve diğer alanlar var
- Sonuç: 500 Internal Server Error

### 5) `neighborhood` eksik
- Girdi: `neighborhood` alanı yok
- Sonuç: 200 OK
- Problem: alan eksik olsa bile işlem başarılı kabul edildi.

### 6) `cityId: null`
- Girdi: `cityId` = null
- Sonuç: 500 Internal Server Error

### 7) Boş string `neighborhood`
- Girdi: `neighborhood`: ""
- Sonuç: 200 OK
- Problem: boş değer kabul edildi.

### 8) Whitespace `neighborhood`
- Girdi: `neighborhood`: "   "
- Sonuç: 200 OK
- Problem: sadece boşluklardan oluşan değer kabul edildi.

### 9) Geçersiz `cityId`
- Girdi: `cityId`: 999999
- Sonuç: 500 Internal Server Error
- Problem: geçersiz referans için sunucu hatası verdi.

### 10) Geçersiz `districtId`
- Girdi: `districtId`: 999999
- Sonuç: 500 Internal Server Error

### 11) Sayı olarak `neighborhood`
- Girdi: `neighborhood`: 123
- Sonuç: 200 OK
- Problem: yanlış tipte veri kabul edildi.

### 12) Boolean olarak `neighborhood`
- Girdi: `neighborhood`: true
- Sonuç: 200 OK
- Problem: boolean tipi kabul edildi.

### 13) Dizi olarak `neighborhood`
- Girdi: `neighborhood`: ["Moda"]
- Sonuç: 400 Bad Request

### 14) Obje olarak `neighborhood`
- Girdi: `neighborhood`: {"x":1}
- Sonuç: 400 Bad Request

### 15) Bozuk JSON body
- Girdi: {"cityId":1,"districtId":1
- Sonuç: 400 Bad Request

### 16) Düz string body
- Girdi: "Moda"
- Sonuç: 400 Bad Request

### 17) Array body
- Girdi: ["Moda","Kısıklı"]
- Sonuç: 400 Bad Request

### 18) Null body
- Girdi: null
- Sonuç: 400 Bad Request

## Ana bulgular
1. Adres ekleme endpoint'i de zayıf doğrulama yapıyor.
2. `neighborhood` alanı boş/whitespace olarak kabul ediliyor.
3. Eksik alanlar çoğu durumda 500 hatası oluşturuyor.
4. `cityId` ve `districtId` için referans doğrulama zayıf.
5. Tip güvenliği ve hata mesajları iyileştirilmeli.
