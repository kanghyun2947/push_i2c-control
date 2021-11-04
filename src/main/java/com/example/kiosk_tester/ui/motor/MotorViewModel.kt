package com.example.kiosk_tester.ui.motor

import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kiosk_tester.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MotorViewModel : ViewModel() {
    private var executeCount = 0
    private var successCount = 0
    private var failCount = 0

    private val executeD = MutableLiveData<Int>().apply {
        value = executeCount
    }
    val execute: LiveData<Int> = executeD

    private val successD = MutableLiveData<Int>().apply {
        value = successCount
    }
    val success: LiveData<Int> = successD

    private val failD = MutableLiveData<Int>().apply {
        value = failCount
    }
    val fail: LiveData<Int> = failD

    private var runState = false
    private var runningTest = false
    private var sBtn: Button? = null


    fun startStop(start: Button) {
        sBtn = start

        if (motors.any()) {
            if (!runState && !runningTest) {
                start.setBackgroundColor(Color.parseColor("#E53206"))
                start.text = "정지"
                nowMotor = 0
                runState = true
                runningTest = false
                motorTestRun()
            } else {
                start.setBackgroundColor(Color.parseColor("#01A9DB"))
                start.text = "시작"
                runState = false
            }
        }
    }

    fun stopMotorReset() {
        for (i in motorsBtn) {
            i.setBackgroundColor(Color.parseColor("#E58306"))
        }
    }

    fun reset(text: TextView) {
        if (!runState && !runningTest) {
            var count = 0
            for (i in motorsBtn) {
                i.setBackgroundColor(Color.parseColor("#333333"))
                i.text = "M" + motors[count].toString()
                count++
                //i.text = "모터 일체"
            }

            executeCount = 0
            successCount = 0
            failCount = 0
            executeD.value = executeCount
            successD.value = successCount
            failD.value = failCount
            motorsBtn.clear()
            motors.clear()
            errorReset(text)

        }
    }

    fun errorReset(text: TextView) {
        GlobalScope.launch(Dispatchers.Main) {
            PCB.passCheck()
            delay(200L)
            when (passResult) {
                "PF" -> {
                    text.text = "투출 센서 이상"
                    text.visibility = TextView.VISIBLE
                }
                else -> {
                    text.visibility = TextView.INVISIBLE
                }
            }
        }

    }

    private var motorMode = false
    fun mode(mode: Button) {
        if ((!runState && !runningTest)) {
            if (motorMode) {
                mode.text = "1회" // false
                motorMode = false

            } else {
                mode.text = "무한" // true
                motorMode = true
            }
        }
    }

    private var motorsBtn = arrayListOf<Button>()
    private var motors = arrayListOf<Int>()
    fun motorSelect(motor: Button, num: Int) {
        if (!runState && !runningTest) {
            if (motor in motorsBtn) {
                motor.setBackgroundColor(Color.parseColor("#333333"))
                motorsBtn.remove(motor)
                motors.remove(num)
                motor.text = "M$num"

            } else {
                motor.setBackgroundColor(Color.parseColor("#E58306"))
                motorsBtn.add(motor)
                motors.add(num)
            }
        }
        swap()
    }

    fun swap() {
        for (i in motors.indices) {
            var temp = i
            for (j in (i + 1 until motors.size)) {
                if (motors[temp] > motors[j]) temp = j
            }
            var mtemp = motors[temp]
            var btemp = motorsBtn[temp]
            motors[temp] = motors[i]
            motorsBtn[temp] = motorsBtn[i]
            motors[i] = mtemp
            motorsBtn[i] = btemp
        }
    }

    fun motorRemove(motor: Button, num: Int) {
        if (!motorMode) {
            motor.setBackgroundColor(Color.parseColor("#333333"))
            motorsBtn.remove(motor)
            motors.remove(num)
            nowMotor = -1
        }
    }

    private var nowMotor: Int = 0
    fun motorTestRun() {
        GlobalScope.launch(Dispatchers.Main) {
            stopMotorReset()
            if (motors.isEmpty() && runState) {
                sBtn?.setBackgroundColor(Color.parseColor("#01A9DB"))
                sBtn?.text = "시작"
                runState = false
                ocState = false
            } else if (!runState) {
                ocState = false
            } else if (runState && motors.any()) {
                runningTest = true
                if (!ocState) {
                    motorsBtn[nowMotor].text = "M" + motors[nowMotor].toString()
                }
                executeCount++
                executeD.postValue(executeCount)

                PCB.motorRun(motors[nowMotor])
                motorsBtn[nowMotor].setBackgroundColor(Color.parseColor("#A901DB"))

                delay(6500L)

                motorsBtn[nowMotor].setBackgroundColor(Color.parseColor("#E58306"))

                val motorResult = PCB.motorResult()
                var motorButtonResult = "M" + motors[nowMotor]

                when (motorResult) {
                    "OY" -> {
                        motorsBtn[nowMotor].text = "$motorButtonResult\n[OY]"
                        motorRemove(motorsBtn[nowMotor], motors[nowMotor])
                        successCount++
                        oySound?.start()
                    }
                    "OC" -> {
                        motorsBtn[nowMotor].text = "$motorButtonResult\n[OC]"
                        nowMotor--
                        failCount++
                        ocSound?.start()
                    }
                    "ON" -> {
                        motorsBtn[nowMotor].text = "$motorButtonResult\n[ON]"
                        motorRemove(motorsBtn[nowMotor], motors[nowMotor])
                        failCount++
                        onSound?.start()
                    }
                    "ME" -> {
                        failCount++
                        motorsBtn[nowMotor].text = "$motorButtonResult\n[ME]"
                        motorRemove(motorsBtn[nowMotor], motors[nowMotor])
                        meSound?.start()
                    }
                    "PF" -> {
                        failCount++
                        motorsBtn[nowMotor].text = "$motorButtonResult\n[PF]"
                        motorRemove(motorsBtn[nowMotor], motors[nowMotor])
                        pfSound?.start()
                    }
                }
                Log.d("Motor Test Result", motorResult)
                successD.postValue(successCount)
                failD.postValue(failCount)
                nowMotor++
                if (nowMotor == motors.size)
                    nowMotor = 0
                runningTest = false
                motorTestRun()
            }
        }
    }

}