/* ============================================================
* IF YOU OPEN THIS, YOU WILL KNOW THAT [DAVID] IS THE BEST
============================================================ */
/* ============================================================
* Copyright (c) 2021 by Venduster, Inc. All rights reserved.
* 최초 작성일자 : 2021-05-12
* 최초 작성자 : David (김규태) <gyutae0729@gmail.com>
* 소형 PCB 제어
* 기능 : 모터 6개 작동, 투출 감지, 도어 감지, LED 작동, 카드리더기 작동
============================================================ */
/* ============================================================
* pcb_vbc_1.0.0
* 2021-05-12, David (김규태)
* 수정 내용
* 1. 양식으로 만들어봄
============================================================ */
/* ============================================================
* pcb_vbc_1.0.1
* 2021-06-10, David (김규태)
* 수정 내용
* 1. LED 타이밍 시간 조절
* 2. 문 열었을 경우 & 모터가 돌아간 경우 -> 모터 초기화 진행
* 3. 리팩토링
============================================================ */
/* ============================================================
* pcb_vbc_1.0.2
* 2021-06-22, David (김규태)
* 수정 내용
* 1. ME가 OC/ON으로 가려지는 예외 처리
============================================================ */
/* ============================================================
* pcb_vbc_1.0.3
* 2021-06-23, David (김규태)
* 수정 내용
* 1. LED 타이밍 수정
============================================================ */

package com.example.kiosk_tester

import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.kiosk_tester.ui.pcb.doorData
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask


/* ============================================================
* Motor SW Port
============================================================ */
private const val MS1_PORT: String = "7"
private const val MS2_PORT: String = "11"
private const val MS3_PORT: String = "13"
private const val MS4_PORT: String = "15"
private const val MS5_PORT: String = "19"
private const val MS6_PORT: String = "18"


/* ============================================================
* Motor GND Port
============================================================ */
private const val M1_PORT: String = "21"
private const val M2_PORT: String = "23"
private const val M3_PORT: String = "29"
private const val M4_PORT: String = "31"
private const val M5_PORT: String = "33"
private const val M6_PORT: String = "16"


/* ============================================================
* Pass Sensor Port
============================================================ */
private const val PS_PORT: String = "10"
private const val PR_PORT: String = "12"


/* ============================================================
* Door Port
============================================================ */
private const val DOOR_PORT: String = "8"


/* ============================================================
* LED Port
============================================================ */
private const val LED_PORT: String = "35"


/* ============================================================
* Motor SW
============================================================ */
private var MS1: Gpio? = null
private var MS2: Gpio? = null
private var MS3: Gpio? = null
private var MS4: Gpio? = null
private var MS5: Gpio? = null
private var MS6: Gpio? = null


/* ============================================================
* Motor GND
============================================================ */
private var M1: Gpio? = null
private var M2: Gpio? = null
private var M3: Gpio? = null
private var M4: Gpio? = null
private var M5: Gpio? = null
private var M6: Gpio? = null


/* ============================================================
* Pass Sensor
============================================================ */
private var PS: Gpio? = null
private var PR: Gpio? = null


/* ============================================================
* Door
============================================================ */
private var Door: Gpio? = null


/* ============================================================
* LED
============================================================ */
private var LED: Gpio? = null


/* ============================================================
* GPIO Port List
============================================================ */
private val outPortList = arrayOf(
    M1_PORT,
    M2_PORT,
    M3_PORT,
    M4_PORT,
    M5_PORT,
    M6_PORT,
    PS_PORT,
    LED_PORT
)

private val inPortList = arrayOf(
    MS1_PORT,
    MS2_PORT,
    MS3_PORT,
    MS4_PORT,
    MS5_PORT,
    MS6_PORT,
    PR_PORT,
    DOOR_PORT
)

private var outList = mutableListOf(M1, M2, M3, M4, M5, M6, PS, LED)
private var inList = mutableListOf(MS1, MS2, MS3, MS4, MS5, MS6, PR, Door)


/* ============================================================
* Variable
============================================================ */
private var doorDetect: Boolean = true // Door Interrupt Flag
private var passDetect: Boolean = false // Pass Interrupt Flag
private var passState: Boolean = false // Pass State
private var LEDState: Boolean = false // LED State
private var runState: String = "" // Run State
private var LEDTimer: Timer? = null // LED State
var ocState: Boolean = false // OC State

var passResult: String = "PO" // Pass Check Result
var doorResult: String = "" // Door Check Result
var doorReset: Boolean = false // When Door is Opened, True to Reset Motors
var doorResetFlag: Boolean = false // Ready to Reset Motor Flag

var PCBResult = "PO" // 삭제


object PCB : AppCompatActivity() {

    /* ============================================================
    * GPIO Output by Low Setting
    ============================================================ */
    private fun setOutputLow(gpio: Gpio?) {
        gpio?.apply { setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW) }
    }

    /* ============================================================
    * GPIO Input Setting
    ============================================================ */
    private fun setInput(gpio: Gpio?) {
        gpio?.apply { setDirection(Gpio.DIRECTION_IN) }
    }


    /* ============================================================
    * PCB 시작하기
    * 사용할 GPIO를 설정해줍니다.
    * Output or Input GPIO Init
    * Input Interrupt Init
    ============================================================ */
    fun pcbInit() {
        // Output Pin Init
        for (i in outList.indices) {
            outList[i] = try {
                PeripheralManager.getInstance().openGpio(outPortList[i])
            } catch (e: IOException) {
                resultSend(3)
                // Log.d("PCB Error", outPortList[i] + " can not init")
                null
            }
            outList[i].apply { setOutputLow(this) }
        }
        // Input Pin Init
        for (i in inList.indices) {
            inList[i] = try {
                PeripheralManager.getInstance().openGpio(inPortList[i])
            } catch (e: IOException) {
                resultSend(3)
                // Log.d("PCB Error", inPortList[i] + " can not init")
                null
            }
            inList[i].apply { setInput(this) }
        }

        PS = outList[6]
        LED = outList[7]

        PR = inList[6]
        Door = inList[7]

        // Pass Interrupt Init
        Door?.apply {
            setEdgeTriggerType(Gpio.EDGE_BOTH)
            registerGpioCallback(DoorCallback)
        }

        // Pass Interrupt Init
        PR?.apply {
            setEdgeTriggerType(Gpio.EDGE_RISING)
            registerGpioCallback(PassCallback)
        }

        // pcbCheck() // PCB Check
        doorCheck() // Door Check
        passCheck() // Pass Check
        ledCheck() // LED Check
        motorReset() // Motor Reset
    }


    /* ============================================================
    * PCB 연결 해제
    * 어플을 종료시에 반드시 진행해줘야합니다.
    * pcbDestroy 하지 않고 pcbInit 할 경우 오류 발생!
    ============================================================ */
    fun pcbDestroy() {
        try {
            passRun(false)
            ledRun(false)
            for (i in outList.indices) {
                outList[i]?.value = false
                outList[i]?.close()
                outList[i] = null
            }
            for (i in inList.indices) {
                inList[i]?.close()
                inList[i] = null
            }
        } catch (e: IOException) {
            resultSend(3) // PD Handler
            // Log.w("PCB ERROR", "Unable to close GPIO", e)
        }
    }


    /* ============================================================
    * 문 감지 인터럽트
    ============================================================ */
    private val DoorCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            if (doorDetect) {
                doorDetect = false // Door Interrupt Stop
                Timer().schedule(timerTask {
                    if (gpio.value) {
                        doorResult = "DC"
                        // doorReset = false
                        resultSend(1) // DC Handler
                        // motorReset() // Motor Reset
                    } else {
                        doorResult = "DO"
                        // doorReset = true
                        // motorCheck()
                        resultSend(2) // DO Handler
                    }
                    doorDetect = true // Door Interrupt Run
                }, 1000)
            }
            return true
        }

        override fun onGpioError(gpio: Gpio, error: Int) {
            // Log.w("PCB Door error", "$gpio: Error event $error")
        }
    }


    /* ============================================================
    * 투출 감지 인터럽트
    ============================================================ */
    private val PassCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            if (passDetect) {
                passDetect = false // Pass Interrupt Stop
                passState = true // Pass OK!

                ledRun(true) // LED ON

                LEDState = true
                LEDTimer = Timer()
                LEDTimer?.schedule(timerTask {
                    ledRun(false) // LED OFF
                    LEDState = false
                }, 5000)

                // Log.d("PCB Pass Detect", "OK")

                GlobalScope.launch(Dispatchers.Default) {
                    delay(1000L) // For Product Falling Time
                    passCheck()
                    delay(300L) // Wait Light On
                    if (passResult != "PF") {
                        runState = "OY"
                        resultSend(7) // OY Handler
                    }
                }
            }
            return true
        }

        override fun onGpioError(gpio: Gpio, error: Int) {
            //Log.w("PCB Pass error", "$gpio: Error event $error")
        }
    }

    /* ============================================================
    * 모터 작동 & 모터 스위치 인식
    ============================================================ */
    private fun motorOperation(motorNum: Int, state: Boolean) {
        outList[motorNum - 1]?.value = state // Motor Run or Stop
    }

    private fun motorDetection(motorNum: Int): Boolean? {
        return inList[motorNum - 1]?.value
    }

    /* ============================================================
    * 모터 작동
    ============================================================ */
    fun motorRun(motor: Int) {
        GlobalScope.launch(Dispatchers.Default) {
            passCheck() // Pass Check
            delay(300L) // Wait Light On
            if (motor in 1..6 && passResult != "PF") {
                passRun(true) // Pass Out Sensor ON
                ledRun(false) // LED OFF

                if (LEDState) {
                    LEDTimer?.cancel()
                }

                delay(500L) // Wait for Pass Sensor ON

                passDetect = true // Pass Interrupt ON
                motorOperation(motor, true) // Motor Run

                delay(2000L) // Wait for Motor SW ON

                // Motor SW Error - Motor does Not Run
                if (motorDetection(motor) == true) {
                    runState = "ME"
                    resultSend(5) // ME Handler
                } else {
                    var motorTimeFlag = false
                    val timer = Timer()
                    timer.schedule(timerTask {
                        motorTimeFlag = true
                    }, 2000)

                    while (motorDetection(motor) == false) {
                        if (motorTimeFlag) break
                    } // Wait For Motor SW OFF
                    timer.cancel()

                    motorOperation(motor, false) // Motor Stop

                    delay(1500L)

                    if (motorDetection(motor) == false) {
                        runState = "ME"
                        resultSend(5) // ME Handler
                    } else if (!passState) {
                        if (ocState) {
                            runState = "ON"
                            resultSend(8) // ON Handler
                        } else {
                            runState = "OC"
                            resultSend(6) // OC Handler
                        }
                    }
                }

                motorOperation(motor, false) // Motor Stop
                passRun(false) // Pass Out Sensor OFF

                passState = false // Pass Data Reset
                passDetect = false // Pass Interrupt OFF
            }
            else if (passResult != "PF") {
                runState = "ME"
                resultSend(5) // ME Handler
            }
            ocState = runState == "OC"
        }
    }

    /* ============================================================
    * 모터 초기화
    * 1~6번까지 모터 순차적으로 스위치 눌러짐 확인 후 작동
    * motorDetection(i) == false : Motor SW ON
    * motorDetection(i) == true : Motor SW OFF
    ============================================================ */
    fun motorReset() {
        GlobalScope.launch(Dispatchers.Default) {
            for (i in 0..5) {
                var motorTimeFlag = false
                val timer = Timer()
                timer.schedule(timerTask {
                    motorTimeFlag = true
                }, 4000)

                while (inList[i]?.value == false) {
                    motorOperation(i + 1, true) // Motor Run
                    if (motorTimeFlag) break
                } // Wait For Motor SW OFF
                motorOperation(i + 1, false) // Motor Stop
                timer.cancel()
            }
        }
    }

    private fun motorCheck() {
        Timer().schedule(timerTask {
            if (doorReset) {
                motorCheck()
                if (!doorResetFlag) motorDetect()
            }
        }, 3000)
    }

    private fun motorDetect() {
        GlobalScope.launch(Dispatchers.Default) {
            for (i in 0..5) {
                if (motorDetection(i + 1) == true) {
                    doorResetFlag = true
                    Timer().schedule(timerTask {
                        motorReset()
                        doorResetFlag = false
                    }, 5000)
                }
            }
        }
    }


    /* ============================================================
    * 투출 센서 발광 작동
    ============================================================ */
    fun passRun(mode: Boolean) {
        PS?.value = mode // Pass Out Sensor ON or OFF
    }


    /* ============================================================
    * LED 작동
    ============================================================ */
    fun ledRun(requestLight: Boolean) {
        LED?.value = requestLight // LED ON or OFF
    }


    /* ============================================================
    * 초기 PCB (GPIO) 작동 확인
    ============================================================ */
    fun pcbCheck() {
        GlobalScope.launch(Dispatchers.Default) {
            PCBResult = "PO" // 삭제
            var sensorError = 0
            passRun(true) // Pass Out Sensor ON
            delay(200L) // Wait Light On
            for (i in 1..10) {
                if (PR?.value == true) sensorError++
            }
            passRun(false) // Pass Out Sensor OFF
            if (sensorError > 8) {
                resultSend(3) // PD Handler
            }
        }
    }


    /* ============================================================
    * 투출 센서 상태와 물건 걸림 확인
    * 10회 확인 중에 9회 이상일 경우
    * PF 오류로 판단
    * 센서 고장 or 물품 걸림
    ============================================================ */
    fun passCheck() {
        GlobalScope.launch(Dispatchers.Default) {
            passResult = "PO"
            var sensorError = 0
            passRun(true) // Pass Out Sensor ON
            delay(200L) // Wait Light On
            for (i in 1..10) {
                if (PR?.value == true) sensorError++
            }
            passRun(false) // Pass Out Sensor OFF
            if (sensorError > 8) {
                passResult = "PF"
                runState = "PF"
                resultSend(4) // PF Handler
            }
        }
    }


    /* ============================================================
    * 현재 문 상태 확인
    * 열림 or 닫힘
    ============================================================ */
    private fun doorCheck() {
        doorResult = if (Door?.value == true)
            "DC"
        else
            "DO"
    }


    /* ============================================================
    * LED 초기 테스트
    * 3회 깜빡이도록 진행
    ============================================================ */
    private fun ledCheck() {
        GlobalScope.launch(Dispatchers.Default) {
            ledRun(true)
            delay(500L)
            ledRun(false)
            delay(500L)
            ledRun(true)
            delay(500L)
            ledRun(false)
        }
    }


    /* ============================================================
    * Handler
    * 아래의 Handler에 꿈을 펼치면 됩니다.
    ============================================================ */
    fun resultSend(value: Int) {
        handler.obtainMessage(value).sendToTarget()
    }

    val handler = Handler {
        when (it.what) {
            // 핸들러 부분으로 여기서 데이터 변경 이루어지시고 사용하시면 될 것으로 예상됩니다.
            1 -> { // DC - 문 닫힘
                Log.d("PCB Handler", "DC")
                doorData.value = "닫힘" // 삭제
            }
            2 -> { // DO - 물 열림
                Log.d("PCB Handler", "DO")
                doorData.value = "열림" // 삭제
            }
            3 -> { // PD - PCB 이상 발생
                Log.d("PCB Handler", "PD")
                PCBResult = "PD" // 삭제
            }
            4 -> { // PF - 투출 센서 이상 or 상품 걸림 발생
                Log.d("PCB Handler", "PF")
            }
            5 -> { // ME - 모터 없음 or 모터 고장
                Log.d("PCB Handler", "ME")
            }
            6 -> { // OC - 한번 더
                Log.d("PCB Handler", "OC")
            }
            7 -> { // OY - 투출 성공
                Log.d("PCB Handler", "OY")
            }
            8 -> { // ON - 투출 실패
                Log.d("PCB Handler", "ON")
            }

        }
        true
    }

    /* ============================================================
    * 삭제 예정 - 테스트 어플에서만 필요
    ============================================================ */
    fun motorResult(): String {
        val returnString = runState
        runState = ""
        return returnString
    }

}


