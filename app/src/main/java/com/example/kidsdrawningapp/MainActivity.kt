package com.example.kidsdrawningapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null
    private var mImage: ImageButton? = null
    private var customProgressDialog : Dialog? = null

    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val imageBackground : ImageView = findViewById(R.id.ivBackground)

                imageBackground.setImageURI(result.data?.data)
            }
        }

    private val requestPermission : ActivityResultLauncher<Array<String>> =
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

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch((pickIntent))
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

        val ibUndo : ImageButton = findViewById(R.id.ibUndo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.ibRedo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val iBSave : ImageButton = findViewById(R.id.ibSave)
        iBSave.setOnClickListener {

            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView : FrameLayout = findViewById(R.id.flDrawingViewContainer)

                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }

        mImage = findViewById(R.id.ibImage)
        mImage?.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun isReadStorageAllowed() : Boolean {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
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
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    private fun getBitmapFromView(view : View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap : Bitmap?) : String {
        var imagePath = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString()
                    + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 +
                    ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    imagePath = f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(imagePath.isNotEmpty()){
                            Snackbar.make(findViewById(R.id.myView),
                                "File saved successfully :$imagePath",
                            Snackbar.LENGTH_SHORT).show()

                            shareImage(imagePath)
                        }else{
                            Snackbar.make(findViewById(R.id.myView),
                                "Something went wrong while saving the file.",
                                Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                catch(e: Exception){
                    imagePath = ""
                    e.printStackTrace()
                }
            }
        }

        return imagePath
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.progress_dialog)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            _, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}