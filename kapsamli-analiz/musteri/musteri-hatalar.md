# Müşteri Ekleme Endpoint'i İçin Hata/Problem Notları

## Test edilen endpoint
- POST /users

## Test yöntemi
Aşağıdaki giriş türleri doğrudan endpoint'e gönderildi:
- geçerli değer
- boş obje
- eksik alanlar
- invalid email
- yaş küçük
- null değer
- boş string
- whitespace
- kısa TC
- kısa telefon
- telefonun 0 ile başlamaması
- sayı olarak `name`
- boolean olarak `name`
- bozuk JSON
- düz string body
- array body
- null body

## Test sonuçları

### 1) Geçerli veri
- Girdi: geçerli müşteri bilgileri
- Sonuç: 500 Internal Server Error
- Problem: geçerli veri ile bile işlem bazı durumlarda sunucu hatası verdi.

### 2) Boş obje gönderimi
- Girdi: {}
- Sonuç: 400 Bad Request
- Problem: validation hataları döndü.

### 3) Eksik alanlar
- Girdi: bazı alanlar eksik
- Sonuç: 400 Bad Request
- Problem: alan bazlı validation mesajları döndü.

### 4) Invalid email
- Girdi: {"email":"not-an-email"}
- Sonuç: 400 Bad Request
- Problem: geçersiz email için doğrulama çalıştı.

### 5) Yaş küçük
- Girdi: age = 17
- Sonuç: 400 Bad Request
- Problem: yaş doğrulama çalıştı.

### 6) `name: null`
- Girdi: {"name":null}
- Sonuç: 400 Bad Request
- Problem: boş değer doğrulandı.

### 7) Boş string
- Girdi: {"name":""}
- Sonuç: 400 Bad Request

### 8) Whitespace
- Girdi: {"name":"   "}
- Sonuç: 400 Bad Request

### 9) Kısa TC
- Girdi: tc = "123"
- Sonuç: 400 Bad Request

### 10) Kısa telefon
- Girdi: telNo = "123"
- Sonuç: 400 Bad Request

### 11) Telefon 0 ile başlamıyor
- Girdi: telNo = "12345678901"
- Sonuç: 400 Bad Request

### 12) Sayı olarak `name`
- Girdi: {"name":123}
- Sonuç: 500 Internal Server Error
- Problem: yanlış tipte veri sunucu hatasına yol açtı.

### 13) Boolean olarak `name`
- Girdi: {"name":true}
- Sonuç: 200 OK
- Problem: boolean değer kabul edildi.

### 14) Bozuk JSON body
- Girdi: {"name":"Ali"
- Sonuç: 400 Bad Request

### 15) Düz string body
- Girdi: "Ali"
- Sonuç: 400 Bad Request

### 16) Array body
- Girdi: ["Ali","Veli"]
- Sonuç: 400 Bad Request

### 17) Null body
- Girdi: null
- Sonuç: 400 Bad Request

## Ana bulgular
1. Müşteri endpoint'inde validation kuralları kısmen çalışıyor.
2. Bazı geçerli ve yanlış tipte girişler 500 hatası veriyor.
3. Boolean gibi farklı tipler kabul ediliyor.
4. Hata mesajları çoğunlukla var ama standart değil.
5. Tip güvenliği ve null kontrolü daha iyi hale getirilmeli.
