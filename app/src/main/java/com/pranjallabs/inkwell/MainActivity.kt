package com.pranjallabs.inkwell

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var imgBtnCurrPaint:ImageButton? = null

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
            Toast.makeText(this, "Clearing Ink from sheet", Toast.LENGTH_LONG).show()
            drawingView!!.clearDoodle()
        }

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

