package com.bibi.myapplication

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    @SerializedName("message") val message: String,
    @SerializedName("nutrisi") val nutrisi: NutritionData,
    @SerializedName("prediksi") val prediksi: Int, // Menggunakan tipe Int untuk indeks prediksi
    @SerializedName("tingkat_kepercayaan") val tingkatKepercayaan: Double,
    @SerializedName("class_names") val classNames: List<String> // Properti baru untuk nama-nama kelas
)


data class NutritionData(
    val Kalori: Int?,
    val Karbohidrat: String?,
    val Lemak: String?,
    val Protein: String?,
    val Tautan: String?
)

data class ErrorResponse(
    val error: String?
)

