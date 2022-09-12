package com.example.kidsdrawningapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get


class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null
    private var mImage: ImageButton? = null


    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted){
                    Toast.makeText(
                        this,
                        "Permission Granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this,
                            "Permission was denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.llPaintColor)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ibBrush : ImageButton = findViewById(R.id.ibBrush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        mImage = findViewById(R.id.ibImage)
        mImage?.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Kids Drawing App",
            "Kids Drawing App needs to access your external storage.")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showRationaleDialog(title: String, message : String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")

        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ibLargeBrush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view : View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }
}