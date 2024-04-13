package com.example.taller2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_CAMERA
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_GALLERY
import com.example.taller2.Datos.Companion.REQUEST_IMAGE_CAPTURE
import com.example.taller2.Datos.Companion.REQUEST_IMAGE_PICK


class CamaraActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        imageView = findViewById(R.id.imageView)
        val btnOpenCamera: Button = findViewById(R.id.buttonCamara)
        val btnOpenGallery: Button = findViewById(R.id.buttonGaleria)

        btnOpenCamera.setOnClickListener {
            askCameraPermission()
        }

        btnOpenGallery.setOnClickListener {
            askGalleryPermission()
        }
    }

    private fun askCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
                //Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(
                    this,
                    "Se necesita permiso para acceder a la cámara",
                    Toast.LENGTH_LONG
                ).show()
                requestCameraPermission()
            }

            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun askGalleryPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
               // Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                Toast.makeText(
                    this,
                    "Se necesita permiso para acceder a la galería",
                    Toast.LENGTH_LONG
                ).show()
                requestGalleryPermission()
            }

            else -> {
                requestGalleryPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            MY_PERMISSION_REQUEST_CAMERA
        )
    }

    private fun requestGalleryPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MY_PERMISSION_REQUEST_GALLERY
        )
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                }

                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri = data?.data
                    imageView.setImageURI(selectedImageUri)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openCamera()
                    //Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Funcionalidades limitadas debido a permisos denegados",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            MY_PERMISSION_REQUEST_GALLERY -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openGallery()
                    //Toast.makeText(this, "Gracias", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "Funcionalidades limitadas debido a permisos denegados",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}