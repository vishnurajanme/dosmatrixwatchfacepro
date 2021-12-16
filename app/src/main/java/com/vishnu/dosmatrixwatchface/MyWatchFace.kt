package com.vishnu.dosmatrixwatchface

import android.content.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.*
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.text.TextPaint
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {

        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {

            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private var bgcolor = Color.BLACK
        private var numTap = 0

        lateinit var sharedPreferences: SharedPreferences
        private lateinit var mCalendar: Calendar
        val stf = SimpleDateFormat("hh:mm:ssa")
        val amb = SimpleDateFormat("hh:mm a")
        val sdf = SimpleDateFormat("dd\\MM\\yy")
        val sdayf = SimpleDateFormat("EEE")
        var matrixFlag:Int = 1
        var cursorFlag:Int = 1
        var randomFlag:Int = 1
        var mainFontSize:Float = 50f
        var matrixFontSize:Float = 22f
        var xData = 6f
        var yData = 6f
        var sliderDensity = 100f
        var saveBtnTimeValue = "time"
        var terminaFlag = 0
        var myfont = R.font.windows_command_prompt


        private lateinit var timenow: String
        private lateinit var datenow: String
        private lateinit var daynow: String
        private lateinit var timeamb: String

        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode: Boolean = false
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F
        private var mwidthX: Float = 0F
        private var mheightX: Float = 0F


        private lateinit var mDigitalPaint: TextPaint
        private lateinit var mMatrix: TextPaint
        private lateinit var mBackgroundPaint: Paint
        private lateinit var mCursor: Paint

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private val mUpdateTimeHandler = EngineHandler(this)

        private fun getBatteryPercentage(context: Context): Int {
            return if (Build.VERSION.SDK_INT >= 21) {
                val bm = context.getSystemService(BATTERY_SERVICE) as BatteryManager
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } else {
                val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, iFilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = level / scale.toDouble()
                (batteryPct * 100).toInt()
            }
        }

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                    WatchFaceStyle.Builder(this@MyWatchFace)
                            .setAcceptsTapEvents(true)
                            .build()
            )
            mCalendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = bgcolor
            }
        }

        private fun initializeWatchFace() {
            mDigitalPaint = TextPaint().apply {
                textSize = mainFontSize
                color = Color.GREEN
                isAntiAlias = false
                style = Paint.Style.FILL
                textAlign = Paint.Align.LEFT
                typeface =
                    (ResourcesCompat.getFont(this@MyWatchFace, myfont))
            }

            mMatrix = TextPaint().apply {
                textSize = matrixFontSize
                color = Color.GREEN
                isAntiAlias = false
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = (ResourcesCompat.getFont(this@MyWatchFace, R.font.matrix))
                alpha = 100
            }

        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE
            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                mDigitalPaint.alpha = if (inMuteMode) 80 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            mCenterX = width / 2f
            mCenterY = height / 2f
            mwidthX = width.toFloat()
            mheightX = height.toFloat()

        }


        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TAP -> {
                    when (numTap) {
                        0 -> {
                            Toast.makeText(this@MyWatchFace, "Tap again to remix", Toast.LENGTH_SHORT).show()
                        }
                        1 -> {
                            //command prompt green
                            bgcolor = Color.parseColor("#000000")
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.parseColor("#00ff00")
                        }
                        2 -> {
                            //Powershell
                            bgcolor = Color.rgb(0, 51, 153)
                            mMatrix.color = Color.WHITE
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.WHITE
                        }
                        3 -> {
                            //matrix half color
                            bgcolor = Color.rgb(0, 102, 0)
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        4 -> {
                            //matrix full color
                            bgcolor = Color.GREEN
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK

                        }
                        5 -> {
                            //command prompt green matrix
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.WHITE
                        }
                        6 -> {
                            //Linux
                            bgcolor = Color.parseColor("#3C0315")
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.parseColor("#FFFFFD")
                        }
                        7 -> {
                            //command prompt white matrix
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.WHITE
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.WHITE
                        }
                        8 -> {
                            //command prompt Yellow
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.YELLOW
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.YELLOW
                        }
                        9 -> {
                            //command prompt yellow green matrix
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.YELLOW
                        }
                        10 -> {
                            //command prompt red bg
                            bgcolor = Color.RED
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        11 -> {
                            //command prompt reddish
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.RED
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.RED
                        }
                        12 -> {
                            //command prompt lt grey inverted
                            bgcolor = Color.LTGRAY
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        13 -> {
                            //command prompt magenta
                            bgcolor = Color.MAGENTA
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        14 -> {
                            //linux yellow
                            bgcolor = Color.parseColor("#3C0315")
                            mMatrix.color = Color.GREEN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.parseColor("#FFFD76")
                        }
                        15 -> {
                            //command prompt cyan white
                            bgcolor = Color.parseColor("#264d00")
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.YELLOW
                        }
                        16 -> {
                            //command prompt cyan black
                            bgcolor = Color.BLACK
                            mMatrix.color = Color.CYAN
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.CYAN
                        }
                        17 -> {
                            //command prompt
                            bgcolor = Color.YELLOW
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        18 -> {
                            //command prompt
                            bgcolor = Color.RED
                            mMatrix.color = Color.BLACK
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                        }
                        19 -> {
                            //command prompt
                            bgcolor = Color.GRAY
                            mMatrix.color = Color.WHITE
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.WHITE
                        }
                        20 -> {
                            //command prompt
                            bgcolor = Color.GRAY
                            mMatrix.color = Color.WHITE
                            mMatrix.alpha = 100
                            mDigitalPaint.color = Color.BLACK
                            numTap = 0
                        }

                    }
                    numTap += 1
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()

            mCalendar.timeInMillis = now
            sharedPreferences = getSharedPreferences(getString(R.string.preference_file_name), Context.MODE_PRIVATE)
            matrixFlag = sharedPreferences.getInt("matrixFlag", 1)
            cursorFlag = sharedPreferences.getInt("cursorFlag", 1)
            randomFlag = sharedPreferences.getInt("randomFlag", 1)
            mDigitalPaint.textSize = sharedPreferences.getFloat("mainFontSize", 60f)
            mMatrix.textSize = sharedPreferences.getFloat("matrixFontSize", 25f)
            sliderDensity = sharedPreferences.getFloat("sliderDensity", 100f)
            xData = sharedPreferences.getFloat("xData", mwidthX/8)
            yData = sharedPreferences.getFloat("yData", (mwidthX/3.5).toFloat())
            saveBtnTimeValue = sharedPreferences.getString("saveBtnTimeValue", "time")
            terminaFlag = sharedPreferences.getInt("terminaFlag", 0)
            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        fun getRandomString(length: Int): String {
            val charset =
                "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Aspernatur consequuntur quibusdam repellat sint sunt velit vero. Excepturi magni natus possimus veritatis voluptate. Accusantium adipisci at commodi cupiditate debitis, dolor fugiat laboriosam molestiae quibusdam recusandae repudiandae soluta tempore veniam. Ab libero nobis pariatur qui quidem quo recusandae. Autem expedita numquam officia."
            return (1..length)
                .map { charset.random() }
                .joinToString("")
        }

        private fun drawBackground(canvas: Canvas) {

            if (terminaFlag == 0) {
                myfont = R.font.windows_command_prompt
            }

            else {
                myfont = R.font.monospacebold
                mDigitalPaint.textSize = (mDigitalPaint.textSize/1.4).toFloat()
            }

            mDigitalPaint.typeface = (ResourcesCompat.getFont(this@MyWatchFace, myfont))

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else if (mAmbient) {
                canvas.drawColor(Color.BLACK)
                //canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawColor(bgcolor)
                if (matrixFlag == 1) {

                    for (i in 1..sliderDensity.toInt()) {
                        canvas.drawText(
                                getRandomString(1), (0..mwidthX.toInt()).random().toFloat(),
                                (0..mheightX.toInt()).random().toFloat(), mMatrix
                        )
                    }

                }

                /*if (randomFlag == 1) {
                    val red = ((0..100).random())
                    val green = ((0..100).random())
                    val blue = ((0..100).random())
                    bgcolor = Color.rgb(red,green,blue)
                }*/

            }
        }

        private fun drawWatchFace(canvas: Canvas) {
            //mDigitalPaint.typeface = (ResourcesCompat.getFont(this@MyWatchFace, R.font.monospacebold))
            timenow = stf.format(Date())
            datenow = sdf.format(Date())
            timeamb = amb.format(Date())
            daynow = sdayf.format(Date())
            //mDigitalPaint.alpha = if (mAmbient) 50 else 255
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                if (randomFlag == 1) {
                    displayAll3(canvas, timeamb, datenow)
                }
                else {
                    displayAll(canvas, timeamb, datenow)
                }
            } else if (mAmbient) {
                if (randomFlag == 1) {
                    displayAll3(canvas, timeamb, datenow)
                }
                else {
                    displayAll(canvas, timeamb, datenow)
                }
            } else {
                if (randomFlag == 1) {
                    displayAll3(canvas, timenow, datenow)
                }
                else {
                    displayAll(canvas, timenow, datenow)
                }
            }
        }

        private fun displayAll(canvas: Canvas, time: String, date: String) {
            canvas.drawText("C:\\>" + saveBtnTimeValue, (xData), (yData), mDigitalPaint)
            canvas.drawText(time, (xData), (yData + mheightX * 1f / 6), mDigitalPaint)
            canvas.drawText("D:\\>" + daynow, (xData), (yData + mheightX * 2f / 6), mDigitalPaint)
            if (cursorFlag == 1) {
                val s = mCalendar.get(Calendar.SECOND)
                if (s%2 == 0) {
                    canvas.drawText(date + "▊", (xData), (yData + mheightX * 3f / 6), mDigitalPaint)
                }
                else {
                    canvas.drawText(date, (xData), (yData + mheightX * 3f / 6), mDigitalPaint)
                }
            }
            else {
                canvas.drawText(date, (xData), (yData + mheightX * 3f / 6), mDigitalPaint)
            }
        }

        private fun displayAll3(canvas: Canvas, time: String, date: String) {
            canvas.drawText("C:\\>" + saveBtnTimeValue, (xData), (yData), mDigitalPaint)
            canvas.drawText(time, (xData), (yData + mheightX * 0.8f / 6), mDigitalPaint)
            canvas.drawText("D:\\>" + daynow, (xData), (yData + mheightX * 2.4f / 6), mDigitalPaint)
            canvas.drawText("batt@${getBatteryPercentage(context = this@MyWatchFace)}%", (xData), (yData + mheightX * 1.6f / 6), mDigitalPaint)
            if (cursorFlag == 1) {
                val s = mCalendar.get(Calendar.SECOND)
                if (s%2 == 0) {
                    canvas.drawText(date + "▊", (xData), (yData + mheightX * 3.2f / 6), mDigitalPaint)
                }
                else {
                    canvas.drawText(date, (xData), (yData + mheightX * 3.2f / 6), mDigitalPaint)
                }
            }
            else {
                canvas.drawText(date, (xData), (yData + mheightX * 3.2f / 6), mDigitalPaint)
            }


        }


        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}