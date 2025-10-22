package com.serkan.trendyol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.DecimalFormat
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppScreen()
        }
    }
}

@Composable
fun AppScreen() {
    val df = remember { DecimalFormat("#,##0.00") }

    var usdPrice by remember { mutableStateOf("") }      // C20 (USD)
    var vatPercent by remember { mutableStateOf("") }    // A21 (%)
    var commPercent by remember { mutableStateOf("") }   // A16 (%)
    var salePrice by remember { mutableStateOf("") }     // A41 (TL)

    var usdBuyRate by remember { mutableStateOf<Double?>(null) }
    var calcResult by remember { mutableStateOf<Double?>(null) }
    var b20Tl by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun parseDoubleSafe(s: String): Double {
        return s.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    }

    fun fetchUsdRate() {
        scope.launch {
            isLoading = true
            error = null
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://www.tcmb.gov.tr/kurlar/today.xml")
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (!response.isSuccessful) throw Exception("HTTP " + response.code)
                val body = response.body?.string() ?: throw Exception("Empty response")

                // Parse USD ForexBuying
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(body.byteInputStream())
                doc.documentElement.normalize()
                val list = doc.getElementsByTagName("Currency")
                var rate: Double? = null
                for (i in 0 until list.length) {
                    val node = list.item(i)
                    val code = node.attributes?.getNamedItem("CurrencyCode")?.nodeValue
                    if (code == "USD") {
                        val children = node.childNodes
                        for (j in 0 until children.length) {
                            val n = children.item(j)
                            if (n.nodeName == "ForexBuying") {
                                val txt = n.textContent.trim().replace(",", ".")
                                rate = txt.toDoubleOrNull()
                                break
                            }
                        }
                    }
                }
                if (rate == null) throw Exception("Kur bulunamadı")
                usdBuyRate = rate
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun calculate() {
        val C20 = parseDoubleSafe(usdPrice)
        val A21 = parseDoubleSafe(vatPercent) // VAT percent
        val A16 = parseDoubleSafe(commPercent) // commission percent
        val A41 = parseDoubleSafe(salePrice)   // sale price TL
        val D20 = usdBuyRate ?: 0.0            // USD buy rate from TCMB

        // Constants read from Excel as defaults (can be adjusted in future settings if needed)
        val A38 = 16.0   // percentage applied on B41 (from Excel A38)
        val B24 = 66.5   // base value, Excel B24
        val B28 = 8.5   // base value, Excel B28

        // Start computing with Excel-equivalent chain (adapted to use A21 where 20% was hardcoded, and 1.0 where needed)
        val B20 = C20 * D20
        val B21 = B20 / 100.0 * 10.0           // still 10% as in Excel
        val B22 = B20 + B21

        val B25 = B24 / 100.0 * A21
        val B26 = B24 + B25

        val B29 = B28 / 100.0 * A21
        val B30 = B28 + B29

        val B32 = A41 / 100.0 * 1.0

        val B38 = A41 / 100.0 * A38
        val B39 = B38 - (B38 / (1.0 + A21/100.0))

        val B33 = A41 - (A41 / (1.0 + A21/100.0))
        val B34 = B21 + B25 + B29 + B39
        val B35 = B33 - B34
        val B43 = B22 + B26 + B30 + B32 + B38 + B35
        val B45 = A41 - B43

        b20Tl = B20
        calcResult = B45
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Trendyol Hesaplama", style = MaterialTheme.typography.headlineSmall)
                Text("Girdi alanlarını doldurun ve 'Kur Çek' + 'Hesapla' yapın.")

                OutlinedTextField(
                    value = usdPrice, onValueChange = { usdPrice = it },
                    label = { Text("Ürün Alış Fiyatı (USD) — C20") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vatPercent, onValueChange = { vatPercent = it },
                    label = { Text("Ürün Alış KDV Oranı (%) — A21") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commPercent, onValueChange = { commPercent = it },
                    label = { Text("Komisyon Oranı (%) — A16") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePrice, onValueChange = { salePrice = it },
                    label = { Text("Satış Fiyatı (TL) — A41") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { fetchUsdRate() }, enabled = !isLoading) {
                        Text(if (isLoading) "Kur Çekiliyor..." else "Kur Çek")
                    }
                    Button(onClick = { calculate() }, enabled = usdBuyRate != null) {
                        Text("Hesapla")
                    }
                }

                if (usdBuyRate != null) {
                    Text("USD Alış Kuru: " + df.format(usdBuyRate))
                }
                if (b20Tl != null) {
                    Text("B20 (TL Alış Fiyatı): " + df.format(b20Tl))
                }
                if (calcResult != null) {
                    Text("B45 (Net Kâr): " + df.format(calcResult))
                }
                if (error != null) {
                    Text("Hata: " + error, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Text("Not: A38, B24, B28 varsayılanları Excel'den alındı. A21 değeri satış ve ara kalemlerde KDV oranı olarak uygulanır.")
            }
        }
    }
}