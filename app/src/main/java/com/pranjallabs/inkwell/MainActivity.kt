package com.pranjallabs.inkwell

import android.R.drawable.ic_dialog_alert
import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.android.material.snackbar.Snackbar
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var imgBtnCurrPaint:ImageButton? = null
    private var isShareOn: Boolean = false

    var customProgressDialog: Dialog? = null

    val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if (result.resultCode == RESULT_OK && result.data != null){
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(10.toFloat())

        var colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.dialog_color)
        val linearLayoutPaintColors:LinearLayout = colorDialog.findViewById(R.id.ll_paint_colors)
        imgBtnCurrPaint = linearLayoutPaintColors[4] as ImageButton

        // updating layout of Pressed button
        imgBtnCurrPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        val ibBrush:ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibColor:ImageButton = findViewById(R.id.ib_color)
        ibColor.setOnClickListener{
            showColorChooserDialog()
        }

        val ibEraser:ImageButton = findViewById(R.id.ib_eraser)
        ibEraser.setOnClickListener{
            selectEraser()
        }

        val ibReset:ImageButton = findViewById(R.id.ib_reset)
        ibReset.setOnClickListener{
            resetAlertDialog()
        }

        val ibUndo:ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo:ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibImage:ImageButton = findViewById(R.id.ib_image)
        ibImage.setOnClickListener{
            requestPermissions()
            checkReadPermission()
            requestPermissions()
        }

        val ibSave:ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener{
            showProgressDialog()
            requestPermissions()
            checkWritePermission()
            cancelProgressDialog()
        }

        val ibHideImage: ImageButton = findViewById(R.id.ib_hideImage)
        ibHideImage.setOnClickListener{
            hideImageFromLayout()
        }

        val ibShare:ImageButton = findViewById(R.id.ib_share)
        ibShare.setOnClickListener{
            isShareOn = true
            requestPermissions()
            checkWritePermission()
        }

    }


//    Stack Overflow
    private fun checkReadPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
        // picking image from local storage
        if(result == PackageManager.PERMISSION_GRANTED){
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(pickIntent)
        }
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun checkWritePermission(): Boolean{
        val result =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        // saving image to local storage
        try{
            lifecycleScope.launch {
                val flDrawingView:FrameLayout = findViewById(R.id.fl_drawingViewContainer)
                saveBitmapFile( getBitmapFromView(flDrawingView) )
            }
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)){
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                111
            )
        }
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        // set the screen content from resource file
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        // start the dialog & display it on the screen
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun hideImageFromLayout(){
        val imageBackground: ImageView = findViewById(R.id.iv_background)
        imageBackground.setImageURI(null)
    }


    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }


    private fun resetAlertDialog(){
        // builder class for convenient dialog construction
        var builder = AlertDialog.Builder(this)
        // set title for alert dialog
        builder.setTitle("Alert!")
        builder.setMessage("Are you sure you want to clear all?")
        builder.setIcon(ic_dialog_alert)

        // performing positive action
        builder.setPositiveButton("Yes"){ dialogInterface, which ->
            Snackbar.make(drawingView as View, "Clearing Ink from sheet", Snackbar.LENGTH_LONG).show()
            drawingView!!.clearDoodle()
            dialogInterface.dismiss()
        }

        // performing negative action
        builder.setNegativeButton("No"){ dialogInterface, which ->
            // we don't want to do anything over here
            dialogInterface.dismiss()
        }

//        // we don't need neutral in this case
//        // performing neutral action
//        builder.setNeutralButton("Yes"){ dialogInterface, which ->
//            Toast.makeText(this, "Clearing Ink from sheet", Toast.LENGTH_LONG).show()
//            drawingView!!.clearDoodle()
//            dialogInterface.dismiss()
//        }

        // creating the alert dialog
        val alertDialog:AlertDialog = builder.create()
        // set other dialog properties
        alertDialog.show()

    }

    private fun showBrushSizeChooserDialog(){
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }


    fun selectEraser(){
//        val colorTag = "@color/white"
        val colorTag = "#ffffff"
        drawingView?.setColor(colorTag)
        Snackbar.make(drawingView as View, "Selected eraser", Snackbar.LENGTH_LONG).show()
    }


    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        } else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String {
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "InkWell_" + System.currentTimeMillis()/1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath

                    runOnUiThread{
                        if(result.isNotEmpty()){
                            if(isShareOn == true){
                                isShareOn = false
                                shareImage(result)
                            }
                            else{
                                Toast.makeText(
                                    this@MainActivity,
                                    "File saved successfully: $result",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else{
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }


    fun paintClicked(view: View) {
//        Toast.makeText(this, "Clicked Paint", Toast.LENGTH_LONG).show()
        if(view != imgBtnCurrPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            //updating layout of current pressed color Button to pallet_pressed
            imageButton!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            //updating layout of previous selected color Button to pallet_normal
            imgBtnCurrPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            imgBtnCurrPaint = view
        }
    }


    private fun showColorChooserDialog(){
        var colorDialog = Dialog(this)
        colorDialog.setContentView(R.layout.dialog_color)
        colorDialog.show()
    }

}

