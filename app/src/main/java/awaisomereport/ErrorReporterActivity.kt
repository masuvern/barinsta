package awaisomereport

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import awais.instagrabber.R
import awais.instagrabber.databinding.ActivityCrashErrorBinding
import awaisomereport.CrashReporterHelper.startCrashEmailIntent
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

class ErrorReporterActivity : Activity(), View.OnClickListener {

    private lateinit var binding: ActivityCrashErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrashErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(false)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val crashTitle = SpannableString("   " + getString(R.string.crash_title))
        crashTitle.setSpan(
            CenteredImageSpan(this, android.R.drawable.stat_notify_error),
            0,
            1,
            Spannable.SPAN_INCLUSIVE_EXCLUSIVE
        )
        title = crashTitle
        binding.btnReport.setOnClickListener(this)
        binding.btnCancel.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v === binding.btnReport) {
            startCrashEmailIntent(this)
        }
        finish()
        exitProcess(10)
    }

    private class CenteredImageSpan(context: Context, @DrawableRes drawableRes: Int) : ImageSpan(context, drawableRes) {

        private var drawable: WeakReference<Drawable>? = null

        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: FontMetricsInt?
        ): Int {
            fm?.apply {
                val pfm = paint.fontMetricsInt
                ascent = pfm.ascent
                descent = pfm.descent
                top = pfm.top
                bottom = pfm.bottom
            }
            return cachedDrawable.bounds.right
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            canvas.save()
            val drawableHeight = cachedDrawable.intrinsicHeight
            val fontMetricsInt = paint.fontMetricsInt
            val transY = bottom - cachedDrawable.bounds.bottom + (drawableHeight - fontMetricsInt.descent + fontMetricsInt.ascent) / 2
            canvas.translate(x, transY.toFloat())
            cachedDrawable.draw(canvas)
            canvas.restore()
        }

        private val cachedDrawable: Drawable
            get() = drawable?.get() ?: getDrawable().also { drawable = WeakReference(it) }
    }
}