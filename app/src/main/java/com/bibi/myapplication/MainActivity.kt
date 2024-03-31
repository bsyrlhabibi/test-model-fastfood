package com.bibi.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var btnProcessPrediction: Button
    private lateinit var imageView: ImageView
    private lateinit var tvResult: TextView
    private lateinit var loadingView: View

    private val CAMERA_PERMISSION_CODE = 101
    private val PICK_IMAGE_CAMERA_REQUEST = 1
    private val PICK_IMAGE_GALLERY_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnProcessPrediction = findViewById(R.id.btnProcessPrediction)
        imageView = findViewById(R.id.imageView)
        tvResult = findViewById(R.id.tvResult)
        loadingView = findViewById(R.id.loadingView)

        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestCameraPermission()
            } else {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, PICK_IMAGE_CAMERA_REQUEST)
            }
        }

        btnGallery.setOnClickListener {
            val pickImageIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickImageIntent, PICK_IMAGE_GALLERY_REQUEST)
        }

        btnProcessPrediction.setOnClickListener {
            if (imageView.drawable != null) {
                val imageDrawable = imageView.drawable
                val imageBitmap = (imageDrawable as BitmapDrawable).bitmap
                showLoading()
                UploadImageTask().execute(imageBitmap)
            } else {
                tvResult.text = "No image selected"
            }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePictureIntent, PICK_IMAGE_CAMERA_REQUEST)
            } else {
                Toast.makeText(
                    this,
                    "Permission denied, camera cannot be accessed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_CAMERA_REQUEST -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                }
                PICK_IMAGE_GALLERY_REQUEST -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        val imageBitmap =
                            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        imageView.setImageBitmap(imageBitmap)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingView.visibility = View.GONE
    }

    private inner class UploadImageTask : AsyncTask<Bitmap, Void, String>() {
        override fun doInBackground(vararg params: Bitmap?): String {
            val imageBitmap = params[0] ?: return ""

            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                val client = OkHttpClient()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        "image.jpg",
                        okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://flask-fast-food-5ca29f0fb46e.herokuapp.com/predict")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                return response.body?.string() ?: ""
            } catch (e: Exception) {
                Log.e("UploadImageTask", "Error: ${e.message}")
            }

            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideLoading()
            try {
                val jsonResponse = JSONObject(result)
                val prediction = jsonResponse.getString("prediksi")
                val confidence = jsonResponse.getDouble("tingkat_kepercayaan")
                val nutrition = jsonResponse.getJSONObject("nutrisi")

                val message =
                    "Prediksi: $prediction\nTingkat Kepercayaan: $confidence\n\nNutrisi:\n" +
                            "Kalori: ${nutrition.getInt("Kalori")}\n" +
                            "Karbohidrat: ${nutrition.getString("Karbohidrat")}\n" +
                            "Lemak: ${nutrition.getString("Lemak")}\n" +
                            "Protein: ${nutrition.getString("Protein")}\n" +
                            "Tautan: ${nutrition.getString("Tautan")}"

                tvResult.text = message
            } catch (e: JSONException) {
                Log.e("UploadImageTask", "Error parsing JSON: ${e.message}")
                tvResult.text = "Error parsing JSON response"
            }
        }
    }
}