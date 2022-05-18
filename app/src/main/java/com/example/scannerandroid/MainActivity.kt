package com.example.scannerandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.civilians.project.qrcode.QrCodeAnalyzer
import com.civilians.project.qrcode.QrCodeFrameView
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity(), QrCodeAnalyzer.Action {

    private lateinit var qrCodeView: QrCodeFrameView
    private lateinit var tvResult: TextView
    private lateinit var btnRestart: Button

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        qrCodeView = findViewById(R.id.barcodeView)
        tvResult = findViewById(R.id.tvResult)
        btnRestart = findViewById(R.id.btnRestart)

        qrCodeView.setStokeSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5F, resources.displayMetrics
            )
        )
        qrCodeView.setStrokeColor(
            ContextCompat.getColor(this, R.color.purple_500)
        )
        qrCodeView.setAction(this)

        btnRestart.setOnClickListener {
            startCamera()
        }
    }

    private fun startCamera() {
        qrCodeView.startCamera()
        tvResult.text = ""
    }

    override fun onResume() {
        super.onResume()
        if (!qrCodeView.isRunning()) {
            checkCameraPermission()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        qrCodeView.shutdown()
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
            startCamera()
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
        tvResult.text = "Result : $result"
        /*barcodebox.postDelayed(
            { startCamera() }, 1000
        )*/
    }

    override fun onFail(params: Exception) {
        params.message?.let { showMessage(it) }
        startCamera()
    }

    private fun showMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}