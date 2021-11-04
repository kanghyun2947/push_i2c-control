package com.example.kiosk_tester

import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.kiosk_tester.ui.pcb.doorData
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.round


//////////////////////// I2C Device Name ////////////////////////
private const val I2C_DEVICE_NAME: String = "I2C-1" //odroid i2c name
/////////////////////////////////////////////////////////////////


//////////////////////// I2C Worker Address ////////////////////////
private const val MCP_ADDRESS: Int = 0x20 // MCP23017 opcode(깨우는 용도)
private const val IODIRA_ADDRESS: Int = 0x00 // IODIRA의 Control Register
private const val IODIRB_ADDRESS: Int = 0x01 // IODIRB의 Control Register
private const val GPIOA_ADDRESS: Int = 0x12 // GPIOA의 Control Register
private const val GPIOB_ADDRESS: Int = 0x13 // GPIOB의 Control Register
////////////////////////////////////////////////////////////////////


//////////////////////// Device GND ////////////////////////
private var m1Device: I2cDevice? = null
private var MCPMotorDevice: I2cDevice? = null
private var MCPPassDevice: I2cDevice? = null
private var MCPVolumeDevice: I2cDevice? = null
////////////////////////////////////////////////////////////


/*============================================================
 *  Push motor - sensor - product sensor module address(I2C MCP23017)
 *  I2C Device 1 (0x20) -> push motor & sensor
 *  I2C Device 2 (0x21) -> Ultrasonic Pass sensor
 *  I2C Device 3 (0x22) -> Ultrasonic volume sensor
============================================================*/
private const val Module1: Int = 0x01
private const val Module2: Int = 0x02
private const val Module3: Int = 0x04
private const val Module4: Int = 0x08
private const val Module5: Int = 0x10
private const val Module6: Int = 0x20


/*=============================================================
* I2C Device Address
=============================================================*/
private const val MCPMotor: Int = 0x20
private const val MCPPass: Int = 0x21
private const val MCPVolume: Int = 0x22


/*============================================================
 *  Push motor port
============================================================*/
private const val P1_PORT: String = "21"
private const val P2_PORT: String = "23"
private const val P3_PORT: String = "29"
private const val P4_PORT: String = "31"
private const val P5_PORT: String = "33"
private const val P6_PORT: String = "16"


/*============================================================
 *  Push sensor port
=============================================================*/
private const val PS1_PORT: String = "7"
private const val PS2_PORT: String = "11"
private const val PS3_PORT: String = "13"
private const val PS4_PORT: String = "15"
private const val PS5_PORT: String = "19"
private const val PS6_PORT: String = "18"


/* ============================================================
* Block Sensor Port
============================================================ */
//private const val BS1_PORT: String = "8"
private const val BS1_PORT: String = "22"
private const val BS2_PORT: String = "24"
private const val BS3_PORT: String = "26"
private const val BS4_PORT: String = "32"
private const val BS5_PORT: String = "36"

/* ============================================================
* Pass Sensor Port
============================================================ */
private const val PS_PORT: String = "10"
private const val PR_PORT: String = "12"


/* ============================================================
* Push SW
============================================================ */
private var PS1: Gpio? = null
private var PS2: Gpio? = null
private var PS3: Gpio? = null
private var PS4: Gpio? = null
private var PS5: Gpio? = null
private var PS6: Gpio? = null


/* ============================================================
* Push GND
============================================================ */
private var P1: Gpio? = null
private var P2: Gpio? = null
private var P3: Gpio? = null
private var P4: Gpio? = null
private var P5: Gpio? = null
private var P6: Gpio? = null

/* ============================================================
* Block Sensor
============================================================ */
private var BS1: Gpio? = null
private var BS2: Gpio? = null
private var BS3: Gpio? = null
private var BS4: Gpio? = null
private var BS5: Gpio? = null

/* ============================================================
* Pass Sensor
============================================================ */
private var PS: Gpio? = null
private var PR: Gpio? = null

/* ============================================================
* GPIO Port List
============================================================ */
private val outPortList = arrayOf(
    P1_PORT,
    P2_PORT,
    P3_PORT,
    P4_PORT,
    P5_PORT,
    P6_PORT,
    PS_PORT
)

private val inPortList = arrayOf(
    PS1_PORT,
    PS2_PORT,
    PS3_PORT,
    PS4_PORT,
    PS5_PORT,
    PS6_PORT,
    PR_PORT,
    BS1_PORT,
    BS2_PORT,
    BS3_PORT,
    BS4_PORT,
    BS5_PORT
)

private var outList = mutableListOf(P1, P2, P3, P4, P5, P6, PS)
private var inList = mutableListOf(PS1, PS2, PS3, PS4, PS5, PS6, PR, BS1, BS2, BS3, BS4, BS5)

private var module = mutableListOf(Module1, Module2, Module3, Module4, Module5, Module6)
private var I2CDevice = mutableListOf(MCPMotorDevice, MCPPassDevice, MCPVolumeDevice)
private var I2CDevice_AddressPort = mutableListOf(MCPMotor, MCPPass, MCPVolume)

/*======================================================
* variable
======================================================*/
private var passDetect: Boolean = false
private var passState: Boolean = false
private var runState: String = ""
private var returnPush = mutableListOf<Int>()
var pushList = mutableListOf(0, 0, 0, 0, 0, 0)
var nanoTimeStart: Long = 0
var nanoTimeEnd: Long = 0
var ultrasonicMean: Double = 0.0

object PUSH : AppCompatActivity() {


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

    fun pushInit() {
        // I2C Device init
        for (i in I2CDevice.indices) {
            I2CDevice[i] = try {
                PeripheralManager.getInstance()
                    .openI2cDevice(I2C_DEVICE_NAME, I2CDevice_AddressPort[i])
            } catch (e: IOException) {
                Log.w("I2C", "Unable to access I2C device", e)
                null
            }
            /*
            * 모든 MCP
            * */
            I2CDevice[i]?.writeRegByte(IODIRA_ADDRESS, 0x00.toByte()) // Output
            I2CDevice[i]?.writeRegByte(IODIRB_ADDRESS, 0xFF.toByte()) // Input
        }
        MCPMotorDevice = I2CDevice[0]
        MCPPassDevice = I2CDevice[1]
        MCPVolumeDevice = I2CDevice[2]

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

        PR = inList[6]

        // Pass Interrupt Init
        PR?.apply {
            setEdgeTriggerType(Gpio.EDGE_RISING)
            registerGpioCallback(PassCallback)
        }

        //PUSH.pushReset()
        blockDetect()
        pushDetection(2)
        println(0x08 - 0x02)
    }

    /* ============================================================
    * 투출 감지 인터럽트
    ============================================================ */
    private val PassCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            if (passDetect) {
                passDetect = false // Pass Interrupt Stop
                passState = true // Pass OK!

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
    * 투출 센서 발광 작동
    ============================================================ */
    private fun passRun(mode: Boolean) {
        PS?.value = mode // Pass Out Sensor ON or OFF
    }

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
                passResult = "OY"//PF
                runState = "PF"
                resultSend(4) // PF Handler
            }
        }
    }

    /* ============================================================
    * 초음파 투출센서 PF 체크
    ============================================================ */
    fun I2CPassCheck() {
        GlobalScope.launch(Dispatchers.Default) {
            passResult = "PO"

            for (i in returnPush.indices) {
                val passCheck = I2CUltrasonic.runUltrasonic(returnPush[i])
                if (passCheck < 18f) {
                    passResult = "PF"//원래 PF
                    runState = "PF"
                    resultSend(4) // PF Handler
                }
            }
        }
    }

    /* ============================================================
    * 초음파 투출센서 Pass 체크
    ============================================================ */
    private fun i2cPassRun(motor: Int) {

        var oneMeasuredDistance: Float = 0f

        writeRegisterFlag(MCPPassDevice, GPIOA_ADDRESS, 0x00)
        Thread.sleep(0, 2000)
        writeRegisterFlag(MCPPassDevice, GPIOA_ADDRESS, deviceChoice(motor))//sensorChoice(motor) 0xff대신 넣을것
        // 0xFF = 0b1111 1111 // 전체 High output 설정 // 골라서 사용 가능
        Thread.sleep(0, 10000)
        writeRegisterFlag(MCPPassDevice, GPIOA_ADDRESS, 0x00)

        var v1: Int = 0
        //Log.d("ultrasonic", "Let's Start!")

        while (MCPPassDevice?.readRegByte(GPIOB_ADDRESS)!!.toInt() and deviceChoice(motor) == 0x00) { //echo?.value == false
            v1 = 0
        }
        nanoTimeStart = System.nanoTime()
        //Log.d("ultrasonic", "echo ARRIVED!")

        while (MCPPassDevice?.readRegByte(GPIOB_ADDRESS)!!.toInt() and deviceChoice(motor) != 0x00) {
            v1 = 1
        }
        nanoTimeEnd = System.nanoTime()
        //Log.d("ultrasonic", "echo ENDED!")

        //println("The result is..${nanoTimeEnd - nanoTimeStart}")
        oneMeasuredDistance = round(((nanoTimeEnd - nanoTimeStart) / 1000.0) / 58.23).toFloat() // Cm
        if (oneMeasuredDistance < 10) passState = true
    }

    /* ============================================================
    * 모터 작동 & 모터 스위치 인식
    ============================================================ */
    private fun pushOperation(motorNum: Int, state: Boolean) {
        outList[motorNum]?.value = state // Motor Run or Stop
    }

    private fun pushSensorDetection(motorNum: Int): Boolean? {
        return inList[motorNum]?.value
    }

    /* ============================================================
    * 모터 동작 (메인)
    ============================================================ */

    fun pushRun(motor: Int) {

        GlobalScope.launch(Dispatchers.Default) {
            blockDetect()
            passCheck() // Pass Check
            pushDetection(motor) //Select Push Motor


            delay(300L) // Wait Light On

            if (motor in 1..6 && passResult != "PF") {
                passRun(true) // Pass Out Sensor ON

                delay(300L)
                passDetect = true                     //투출센서 감지 실시
                for (i in returnPush.indices) {       //모터 동작
                    pushOperation(returnPush[i], true)
                    delay(10L)
                }

                delay(3000L)

                var motorTimeFlag = false
                val timer = Timer()
                timer.schedule(timerTask {
                    motorTimeFlag = true
                }, 5000)

                var aDummyPushList2 = mutableListOf<Boolean?>()
                val aDummyPushList3 = mutableListOf<Boolean?>()

                for (i in returnPush.indices) {
                    aDummyPushList3.add(outList[returnPush[i]]?.value)
                }

                while (aDummyPushList3.all { it == true }) {
                    //println("yes")
                    for (i in returnPush.indices) {
                        aDummyPushList2.add(inList[returnPush[i]]?.value)
                    }
                    for (i in returnPush.indices) {
                        if (aDummyPushList2[i] == true) {
                            pushOperation(returnPush[i], false)
                            delay(10L)
                            //println("no")
                        }
                    }
                    //println(aDummyPushList2)
                    if (aDummyPushList2.all { it == true }) {
                        //println("This situation is correct")
                        break
                    }
                    if (motorTimeFlag) {
                        //println("shit")
                        break
                    }
                    aDummyPushList2 = mutableListOf()
                } // Wait For Motor SW OFF
                timer.cancel()

                for (i in returnPush.indices) {       // Motor Stop
                    pushOperation(returnPush[i], false)
                    delay(10L)
                }

                delay(3000L)
                //println(aDummyPushList2)

                if (pushSensorDetection(returnPush[0]) == false) {
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
            val ultrasonicResultList = mutableListOf<Float>()
            for (i in returnPush.indices) {
                ultrasonicResultList[i] = I2CUltrasonic.runUltrasonic(returnPush[i]) // 이게 끝나면 다음 명령 실행하고 그런건 아닐텐데..
                Thread.sleep(1000)
            }
            ultrasonicMean = round(ultrasonicResultList.average())
            Log.d("push", "mean distance is..$ultrasonicMean")
            returnPush.clear()
        }
    }

    /* ===========================================================================
    * MCP23017칩 기반 푸쉬모터, 투출센서, 재고파악센서 통합 동작코드
    * 1~6번 모터 및 센서 주소 module[0~5], 파트별 디바이스 주소 I2CDevice[0~2] 사용
    * module = mutableListOf(Module1, Module2, Module3, Module4, Module5, Module6)
    * I2CDevice = mutableListOf(MCPMotorAddress, MCPPassAddress, MCPVolumeAddress)
    * I2CDevice[i]?.(readRegByte or writeRegByte)로 데이터 읽고 쓰기
    =========================================================================== */
    fun pushUlSensorRun(motor: Int) {

        GlobalScope.launch(Dispatchers.Default) {
            blockDetect()
            I2CPassCheck() // Ultrasonic Sensor Pass Check
            pushDetection(motor) //Select Push Motor

            Thread.sleep(300)// Wait Light On
            if (motor in 1..6 && passResult != "PF") {
                passRun(true) // Pass Out Sensor ON

                Thread.sleep(300)
                var passFlag: Boolean = false // 투출센서 동작 플래그
                var mergedMotor = 0x00 // 동작시킬 모터(들) 핵스값
                for (i in returnPush.indices) { //동작 모터 핵스값 계산
                    mergedMotor = mergedMotor or deviceChoice(returnPush[i])
                }
                writeRegisterFlag(MCPMotorDevice, GPIOA_ADDRESS, mergedMotor) //모터 동작
                GlobalScope.launch(Dispatchers.Default) { // 투출센서 동작 시작
                    while (!passFlag) {
                        i2cPassRun(returnPush[0])
                        if(passFlag) break
                    }
                }

                Thread.sleep(3000)// 모터 스위치 떨어지는데 기다리는 시간
                var motorTimeFlag = false // 모터 동작 타이머 플래그
                val timer = Timer()
                timer.schedule(timerTask {
                    motorTimeFlag = true
                }, 5000)

                while (mergedMotor != 0x00) {
                    //println("yes")

                    for (i in returnPush.indices) {
                        if (readRegisterFlag(MCPMotorDevice, GPIOB_ADDRESS)!!.toInt() and deviceChoice(returnPush[i]) == deviceChoice(returnPush[i])) {
                            mergedMotor -= deviceChoice(returnPush[i])
                            writeRegisterFlag(MCPMotorDevice, GPIOA_ADDRESS, mergedMotor)
                            Thread.sleep(5)
                            //println("no")
                        }
                    }
                    //println(aDummyPushList2)
                    if (mergedMotor == 0x00) {
                        //println("This situation is correct")
                        break
                    }
                    if (motorTimeFlag) {
                        //println("shit")
                        break
                    }

                } // Wait For Motor SW OFF
                timer.cancel()

                writeRegisterFlag(MCPMotorDevice, GPIOA_ADDRESS, 0x00) // All Motor Stop Again

                Thread.sleep(3000)
                var mergedMotorSwitch = 0x00
                for (i in returnPush.indices) mergedMotorSwitch = mergedMotorSwitch or deviceChoice(returnPush[i])

                if (readRegisterFlag(MCPMotorDevice, GPIOB_ADDRESS)!!.toInt() != mergedMotorSwitch) {
                    runState = "ME"
                    resultSend(5) // ME Handler
                } else if (!passState) { ////////////////////// pass interrupt표현을 어떻게 해주지?
                    if (ocState) {
                        runState = "ON"
                        resultSend(8) // ON Handler
                    } else {
                        runState = "OC"
                        resultSend(6) // OC Handler
                    }
                }
                passState = false
                passFlag = true

            }

            val ultrasonicResultList = mutableListOf<Float>()
            for (i in returnPush.indices) {
                ultrasonicResultList[i] = I2CUltrasonic.runUltrasonic(returnPush[i]) // 이게 끝나면 다음 명령 실행하고 그런건 아닐텐데..
                Thread.sleep(1000)
            }
            ultrasonicMean = round(ultrasonicResultList.average())
            Log.d("push", "mean distance is..$ultrasonicMean")
            returnPush.clear()
        }
    }

    private fun deviceChoice(motor: Int): Int {
        val result: Int = when (motor) {
            1 -> module[0]
            2 -> module[1]
            3 -> module[2]
            4 -> module[3]
            5 -> module[4]
            6 -> module[5]
            else -> 0x00
        }
        return result
    }

    private fun writeRegisterFlag(device: I2cDevice?, address: Int, data: Int) {
        device?.writeRegByte(address, data.toByte())
    }

    private fun readRegisterFlag(device: I2cDevice?, address: Int): Byte? {
        return device?.readRegByte(address)
    }

    /* ============================================================
    * PCB 연결 해제
    * 어플을 종료시에 반드시 진행해줘야합니다.
    * pcbDestroy 하지 않고 pcbInit 할 경우 오류 발생!
    ============================================================ */
    fun pushDestroy() {
        try {
            PCB.passRun(false)
            PCB.ledRun(false)
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
    * Push Reset
    ============================================================ */

    fun pushReset() {
        GlobalScope.launch(Dispatchers.Default) {
            var aDummyPushList2 = mutableListOf<Boolean?>()
            val aDummyPushList3 = mutableListOf<Boolean?>()
            for (i in pushList.indices) {
                pushOperation(i, true)
                delay(10L)
            }

            delay(3000L)

            var motorTimeFlag = false
            val timer = Timer()
            timer.schedule(timerTask {
                motorTimeFlag = true
            }, 5000)


            for (i in pushList.indices) {
                aDummyPushList3.add(outList[i]?.value)
            }

            while (aDummyPushList3.all { it == true }) {
                //println("yes")
                for (i in pushList.indices) {
                    aDummyPushList2.add(inList[i]?.value)
                }
                for (i in pushList.indices) {
                    if (aDummyPushList2[i] == true) {
                        pushOperation(i, false)
                        delay(10L)
                        //println("${i}no")
                    }
                }
                //println(aDummyPushList2)
                if (aDummyPushList2.all { it == true }) {
                    println("This situation is correct")
                    break
                }
                if (motorTimeFlag) {
                    //println("shit")
                    break
                }
                aDummyPushList2 = mutableListOf()
            } // Wait For Motor SW OFF
            timer.cancel()

            for (i in pushList.indices) {       // Motor Stop
                pushOperation(i, false)
                delay(10L)
            }

            //println("Push reset is finished")
        }
    }

/* ============================================================
* 동작모터 선정 (returnPush 배열값 사용)
============================================================ */

    private fun pushDetection(motor: Int) {
        val dummyPushList = pushList

        for (i in dummyPushList.indices) {
            if (dummyPushList.indexOf(motor) != -1) {
                returnPush.add(dummyPushList.indexOf(motor))
                dummyPushList[dummyPushList.indexOf(motor)] = 0
            }
        }
        println("returnPush is..${returnPush}")
    }

/* ============================================================
* 칸막이 감지
============================================================ */

    private fun blockDetection(blockNum: Int): Boolean? {
        return inList[blockNum + 7]?.value
    }

    fun blockDetect() {

        for (i in 0..4) {
            if (i == 0) {
                pushList[i] = 1
                if (blockDetection(i) == false) {
                    pushList[i + 1] = pushList[i]
                }
            } else {
                if (pushList[i] == 0) {
                    pushList[i] = pushList[i - 1] + 1
                }
                if (blockDetection(i) == false) {
                    pushList[i + 1] = pushList[i]
                } else {
                    if (i == 4) {
                        Log.d("push", "yes")
                        if (blockDetection(i) == true) {
                            pushList[i + 1] = pushList[i] + 1
                            Log.d("push", "no")
                        }
                    }
                }
            }
        }
        Log.d("push block1", blockDetection(0).toString())
        Log.d("push block2", blockDetection(1).toString())
        Log.d("push block3", blockDetection(2).toString())
        Log.d("push block4", blockDetection(3).toString())
        Log.d("push block5", blockDetection(4).toString())
        Log.d("push block", pushList.toString())
    }

    /* ============================================================
    * Handler
    * 아래의 Handler에 꿈을 펼치면 됩니다.
    ============================================================ */
    fun resultSend(value: Int) {
        handler.obtainMessage(value).sendToTarget()
    }

    private val handler = Handler {
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

}
