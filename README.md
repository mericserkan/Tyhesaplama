
# Trendyol Hesaplama (Android — Kotlin + Compose)

Bu proje, Excel dosyanızdaki B45 (Net Kâr) hücresine kadar olan hesaplama zincirini temel alır ve
uygulama içinde birebir çalışma mantığına uyarlanmıştır.

## Girdi Alanları
- C20: Ürün Alış Fiyatı (USD)
- A21: Ürün Alış KDV Oranı (%)
- A16: Komisyon Oranı (%)
- A41: Satış Fiyatı (TL)

## Otomatik
- D20: USD Alış Kuru (TCMB `today.xml` üzerinden otomatik çekilir)
- A38, B24, B28: Varsayılanlar Excel'den okunup gömüldü.
  - A38 = 16.0
  - B24 = 66.5
  - B28 = 8.5

## Formül Zinciri (özet)
B45 = B41 - B43
B43 = B22 + B26 + B30 + B32 + B38 + B35
B22 = B20 + B21
B20 = C20 * D20
B21 = B20 / 100 * 10
B26 = B24 + B25
B25 = B24 / 100 * (A21)
B30 = B28 + B29
B29 = B28 / 100 * (A21)
B32 = B41 / 100 * 1
B38 = B41 / 100 * A38
B39 = B38 - (B38 / (1 + A21/100))
B33 = B41 - (B41 / (1 + A21/100))
B34 = B21 + B25 + B29 + B39
B35 = B33 - B34

## Derleme
Android Studio ile açın, `Run` deyin. `minSdk=23`, `targetSdk=34`.
