package com.example.mediaartfinalproject

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10

class MainActivity : AppCompatActivity() {

    //private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvSpeechInput: TextView
    private lateinit var colorView: View
    private val REQUEST_RECORD_AUDIO_PERMISSION = 1

    private var currentColor = R.color.sky1
    private var nowColor = R.color.sky1

    private var mediaRecorder: MediaRecorder? = null

    var colorSum = 0.0
    var colorCount = 0


    private var recorder: MediaRecorder? = null
    private var isRecording = false
    val handler = Handler(Looper.getMainLooper())
    private var filePath = ""

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        tvSpeechInput = findViewById(R.id.inputTextView)
        colorView = findViewById<View>(R.id.colorView)
        ;

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }

        colorView.setBackgroundColor(getResources().getColor(currentColor))

        startRecord()
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            onStop()
        }
    }

    private fun startRecord() {
        recorder = MediaRecorder()
        //외부 저장소 내 개별앱 공간에 저장하기
        val basePath = baseContext.getExternalFilesDir(null)?.absolutePath

        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) //외부에서 들어오는 소리를 녹음
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP) // 출력 파일 포맷을 설정
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // 오디오 인코더를 설정
            filePath = "${basePath}/myRecord.3gp"
            setOutputFile(filePath) //출력 파일 이름을 설정
        }
        try {
            recorder?.prepare() //초기화를 완료
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed")
            return
        }

        recorder?.start() //녹음기를 시작
        getDb() //데시벨 측정
    }

    override fun onStop() {
        super.onStop()
        isRecording = false
        handler.removeCallbacksAndMessages(null);
        if (recorder != null) {
            recorder?.stop() // 녹음기 중지
            recorder?.release() //리소스 확보
            recorder = null
        }
        Log.d(TAG, "onStop: 평균값 : ${colorSum/colorCount}")

        tvSpeechInput.text = DecimalFormat("#.########").format((colorSum/colorCount)).toString()
        checkColor(colorSum, colorCount)
        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            startRecord()
            delay(5000)
            onStop()
        }
    }


    private fun getDb() {
        recorder?.let {
            isRecording = true
            Log.d(TAG, "getDb: 호출")
            // Runnable 정의
            val runnable = object : Runnable {
                override fun run() {
                    if (isRecording) {
                        val amplitude = it.maxAmplitude
                        val db = 20 * log10(amplitude.toDouble())


                        if (amplitude > 0) {
                            colorSum += db
                            colorCount+=1
                        }

                        Log.d(TAG, "run: ${db} ${colorSum} ${colorCount}")

                        handler.postDelayed(this, 500L)
                    }
                }
            }

            handler.post(runnable)

        }
    }

    private fun checkColor(colorSum: Double, colorCount: Int) {
        val result = colorSum / colorCount
        Log.d(TAG, "checkColor: ${result}")
        nowColor = when {
            result < 55.0 -> R.color.sky1
            result in 55.0..57.0 -> R.color.sky2
            result in 57.0..61.0 -> R.color.sky3
            result in 61.0..65.0 -> R.color.sky4
            result in 65.0..69.0 -> R.color.sky5
            result in 69.0..73.0 -> R.color.sky6
            result in 73.0..77.0 -> R.color.sky7
            result in 77.0..81.0 -> R.color.sky8
            result >= 81.0 -> R.color.sky9
            else -> R.color.sky9
        }

        startAnimation(currentColor, nowColor)
        currentColor = nowColor

        this.colorSum = 0.0
        this.colorCount = 0
    }

    private fun startAnimation(fromColor: Int, toColor: Int) {
        val viewAnimator: ValueAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            getResources().getColor(fromColor),
            getResources().getColor(toColor)
        )
        viewAnimator.duration = 1800

        viewAnimator.addUpdateListener {
            colorView.setBackgroundColor(it.animatedValue as Int)
        }

        Log.d(ContentValues.TAG, "startAnimation")

        viewAnimator.start()
    }

//    private fun startListening() {
//        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//            putExtra(
//                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//            )
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 설정
//            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//        }
//        Log.d(ContentValues.TAG, "startListening")
//
//        speechRecognizer.startListening(intent)
//    }


    override fun onDestroy() {
        super.onDestroy()
        //speechRecognizer.destroy()
    }
}