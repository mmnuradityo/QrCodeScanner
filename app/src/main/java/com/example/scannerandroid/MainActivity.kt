package com.example.scannerandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scannerandroid.qrcode.QrCodeAnalyzer
import com.example.scannerandroid.qrcode.QrCodeBoxView
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity(), QrCodeAnalyzer.Action {

    private lateinit var cameraPreview: PreviewView
    private lateinit var barcodebox: QrCodeBoxView

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraPreview = findViewById(R.id.cameraPreview)
        barcodebox = findViewById(R.id.barcodebox)
        barcodebox.setStokeSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10F, resources.displayMetrics
            )
        )
        barcodebox.setStrokeColor(
            ContextCompat.getColor(this, R.color.purple_500)
        )
        barcodebox.setAction(this)

    }

    /*private fun cofigureBarcode() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }*/

    override fun onResume() {
        super.onResume()
        if (!barcodebox.isRunning()) {
            checkCameraPermission()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        barcodebox.shutdown()
    }


    /**
     * 1. This function is responsible to request the required CAMERA permission
     */
    private fun checkCameraPermission() {
        try {
            val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(this, requiredPermissions, 10)
        } catch (e: IllegalArgumentException) {
            checkIfCameraPermissionIsGranted()
        }
    }

    /**
     * 2. This function will check if the CAMERA permission has been granted.
     * If so, it will call the function responsible to initialize the camera preview.
     * Otherwise, it will raise an alert.
     */
    private fun checkIfCameraPermissionIsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted: start the preview
            barcodebox.startCamera(cameraPreview)
        } else {
            // Permission denied
            MaterialAlertDialogBuilder(this)
                .setTitle("Permission required")
                .setMessage("This application needs to access the camera to process barcodes")
                .setPositiveButton("Ok") { _, _ ->
                    // Keep asking for permission until granted
                    checkCameraPermission()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                    show()
                }
        }
    }

    /**
     * 3. This function is executed once the user has granted or denied the missing permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkIfCameraPermissionIsGranted()
    }

    override fun onResult(result: String) {
        showMessage(result)
        barcodebox.shutdown()
    }

    override fun onFail(params: Exception) {
        params.message?.let { showMessage(it) }
        barcodebox.startCamera(cameraPreview)
    }

    private fun showMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}