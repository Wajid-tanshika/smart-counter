package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ProductLookupService(
    private val context: Context,
    private val dao: SmartCounterDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun lookupProduct(barcode: String, format: String): ScannedProductInfo = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        val isNetworkAvailable = isOnline()

        // 1. Check if barcode is a Web URL
        if (cleanBarcode.startsWith("http://", ignoreCase = true) || cleanBarcode.startsWith("https://", ignoreCase = true)) {
            val host = try {
                Uri.parse(cleanBarcode).host ?: "Website URL"
            } catch (e: Exception) {
                "Website URL"
            }
            return@withContext ScannedProductInfo(
                barcode = cleanBarcode,
                name = host,
                brand = "Web Link",
                format = format.ifBlank { "QR_CODE" },
                isUrl = true,
                url = cleanBarcode,
                description = "Scanned website or URL resource",
                isFound = true,
                isOffline = !isNetworkAvailable
            )
        }

        // 2. Check local offline cache from previous scans
        val cached = dao.getScannedProduct(cleanBarcode)
        if (cached != null) {
            return@withContext ScannedProductInfo(
                barcode = cached.barcode,
                name = cached.name,
                brand = cached.brand,
                imageUrl = cached.imageUrl,
                category = cached.category,
                quantityInfo = cached.quantityInfo,
                price = cached.price,
                description = cached.description,
                manufacturer = cached.manufacturer,
                countryOfOrigin = cached.countryOfOrigin,
                ingredients = cached.ingredients,
                format = cached.format.ifBlank { format },
                isUrl = cached.isUrl,
                url = cached.url,
                isFound = true,
                isOffline = !isNetworkAvailable,
                scannedAt = cached.scannedAt
            )
        }

        // 3. Check local inventory items
        val inventoryItem = dao.findInventoryByBarcode(cleanBarcode)
        if (inventoryItem != null) {
            return@withContext ScannedProductInfo(
                barcode = cleanBarcode,
                name = inventoryItem.name,
                brand = "Local Inventory",
                category = inventoryItem.category,
                quantityInfo = "${inventoryItem.quantity} in stock",
                description = inventoryItem.notes.ifBlank { "Registered in local inventory" },
                format = format,
                isFound = true,
                isOffline = !isNetworkAvailable
            )
        }

        // 4. Online lookup via Open Food Facts / Open Products APIs
        if (isNetworkAvailable && cleanBarcode.matches(Regex("[0-9A-Za-z-]{4,30}"))) {
            val remoteProduct = fetchFromOpenDatabases(cleanBarcode, format)
            if (remoteProduct != null) {
                // Save in local cache for future offline access
                try {
                    dao.insertScannedProduct(
                        ScannedProductEntity(
                            barcode = remoteProduct.barcode,
                            name = remoteProduct.name,
                            brand = remoteProduct.brand,
                            imageUrl = remoteProduct.imageUrl,
                            category = remoteProduct.category,
                            quantityInfo = remoteProduct.quantityInfo,
                            price = remoteProduct.price,
                            description = remoteProduct.description,
                            manufacturer = remoteProduct.manufacturer,
                            countryOfOrigin = remoteProduct.countryOfOrigin,
                            ingredients = remoteProduct.ingredients,
                            format = remoteProduct.format,
                            isUrl = false,
                            scannedAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w("ProductLookup", "Failed to cache scanned product", e)
                }
                return@withContext remoteProduct
            }
        }

        // 5. Product Not Found fallback
        return@withContext ScannedProductInfo(
            barcode = cleanBarcode,
            name = "",
            format = format.ifBlank { "BARCODE" },
            isFound = false,
            isOffline = !isNetworkAvailable
        )
    }

    private fun fetchFromOpenDatabases(barcode: String, format: String): ScannedProductInfo? {
        val endpoints = listOf(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json",
            "https://world.openbeautyfacts.org/api/v0/product/$barcode.json",
            "https://world.openproductsfacts.org/api/v0/product/$barcode.json"
        )

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SmartCounterAndroidApp/2.0 (contact@smartcounter.app)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use

                    val bodyString = response.body?.string() ?: return@use
                    val json = JSONObject(bodyString)
                    val status = json.optInt("status", 0)

                    if (status == 1 && json.has("product")) {
                        val p = json.getJSONObject("product")

                        val name = p.optString("product_name").ifBlank {
                            p.optString("product_name_en").ifBlank {
                                p.optString("generic_name")
                            }
                        }.trim()

                        if (name.isNotBlank()) {
                            val brand = p.optString("brands").trim()
                            val imageUrl = p.optString("image_front_url").ifBlank {
                                p.optString("image_url").ifBlank {
                                    p.optString("image_small_url")
                                }
                            }.trim()

                            val rawCategories = p.optString("categories")
                            val cleanCategory = if (rawCategories.isNotBlank()) {
                                rawCategories.split(",").firstOrNull()?.trim() ?: rawCategories
                            } else {
                                "Retail Product"
                            }

                            val quantity = p.optString("quantity").trim()
                            val description = p.optString("generic_name").ifBlank {
                                p.optString("generic_name_en")
                            }.trim()

                            val manufacturer = p.optString("manufacturing_places").ifBlank {
                                p.optString("brand_owner").ifBlank {
                                    p.optString("creator")
                                }
                            }.trim()

                            val country = p.optString("countries").ifBlank {
                                p.optString("origins")
                            }.trim()

                            val ingredients = p.optString("ingredients_text").ifBlank {
                                p.optString("ingredients_text_en")
                            }.trim()

                            return ScannedProductInfo(
                                barcode = barcode,
                                name = name,
                                brand = brand,
                                imageUrl = imageUrl,
                                category = cleanCategory,
                                quantityInfo = quantity,
                                price = "",
                                description = description,
                                manufacturer = manufacturer,
                                countryOfOrigin = country,
                                ingredients = ingredients,
                                format = format,
                                isFound = true,
                                isOffline = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("ProductLookup", "Lookup error on $url: ${e.message}")
            }
        }
        return null
    }
}
