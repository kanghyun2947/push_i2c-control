package com.example.kiosk_tester

import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManager
import java.io.IOException
import com.google.android.things.pio.I2cDevice
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlin.system.measureTimeMillis
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
////////////////////////////////////////////////////////////


/////////////////////////// Variables ///////////////////////////
private var inputData: Byte? = null

private const val productHeight = 1f // 상품 높이 cm
private const val minDistance = 6f // 최대적재상태 거리 cm
private const val maxDistance = 106f // 품절상태 거리 cm
private val maxProductNum: Int = round((maxDistance - minDistance) / productHeight).toInt()
// 최대 상품 적재량
private var oneMeasuredDistance: Float = 0f // 1회 측정거리
private var MeanMeasuredDistance: Float = 0f // 평균측정거리
private var num: Int = 0 // 측정 횟수
private var volumeRatio: Int = 0 // 상품적재비율
private var estimatedProductNumNow: Int =0 //현재 상품 적재량

/////////////////////////////////////////////////////////////////


object I2CUltrasonic : AppCompatActivity() {

    fun ultrasonicInit() {
        // Attempt to access the I2C device
        m1Device = try {
            PeripheralManager.getInstance()
                .openI2cDevice(I2C_DEVICE_NAME, MCP_ADDRESS)
        } catch (e: IOException) {
            Log.w("I2C", "Unable to access I2C device", e)
            null
        }

        initIODIRA("output")
        initIODIRB("input")
        /*
        GlobalScope.launch(Dispatchers.Default) {
            for (i in 1..20) {
                initIODIRA("output")
                writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0xFF) // 0xFF = 0b1111 1111 // 전체 High output 설정 // 골라서 사용 가능
                delay(1000L)
                writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 전체 LOW output 설정 // 골라서 사용 가능
                delay(1000L)
                initIODIRB("input")
                readRegisterFlag(m1Device, GPIOB_ADDRESS) // GPIOB 읽기
                delay(100L)
                // 0b0000 0000 == 0(int) <- 모두가 GND 연결 // 0b1111 1111 == -128(int) <- 모두가 3.3V 연결 // 0b0000 0001 == 1(int) <- GPIOB.1만 3.3V 연결
                Log.d("data", inputData.toString())
            }
            //runUltrasonic(1)

        }*/
    }

    fun runUltrasonic(motor: Int): Float {
        ultrasonicReset()
        for (i in 1..200) {
            val elapseTime: Long = measureTimeMillis {
                writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0x00)
                Thread.sleep(0, 2000)
                writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0xff)//sensorChoice(motor) 0xff대신 넣을것
                // 0xFF = 0b1111 1111 // 전체 High output 설정 // 골라서 사용 가능
                Thread.sleep(0, 10000)
                writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0x00)
                var v1: Int = 0
                Log.d("ultrasonic", "Let's Start!")


                while (m1Device?.readRegByte(GPIOB_ADDRESS)!!.toInt() and sensorChoice(motor) == 0x00) { //echo?.value == false
                    v1 = 0
                }
                nanoTimeStart = System.nanoTime()
                //Log.d("ultrasonic", "echo ARRIVED!")

                while (m1Device?.readRegByte(GPIOB_ADDRESS)!!.toInt() and sensorChoice(motor) != 0x00) {
                    v1 = 1
                }
                nanoTimeEnd = System.nanoTime()
                //Log.d("ultrasonic", "echo ENDED!")

                //println("The result is..${nanoTimeEnd - nanoTimeStart}")
                oneMeasuredDistance = round(((nanoTimeEnd - nanoTimeStart) / 1000.0) / 58.23).toFloat() // Cm
                num ++
                mean()
                volume()
                Log.d("ultrasonic", "The number of measurement.. $num")
                Log.d("ultrasonic", "one measuring.. $oneMeasuredDistance cm")
                Log.d("ultrasonic", "mean measuring.. $MeanMeasuredDistance cm")
                //Log.d("ultrasonic", "Volume Ratio ${volumeRatio}%")
                println("")
                Thread.sleep(10)
            }
            println("elapsed time is.. $elapseTime ms")
        }
        return MeanMeasuredDistance
    }

    fun passCheckUltrasonic(motor: Int): Float{
        ultrasonicReset()
        for(i in 1..50){
            writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0x00)
            Thread.sleep(0, 2000)
            writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0xff)//sensorChoice(motor) 0xff대신 넣을것
            // 0xFF = 0b1111 1111 // 전체 High output 설정 // 골라서 사용 가능
            Thread.sleep(0, 10000)
            writeRegisterFlag(m1Device, GPIOA_ADDRESS, 0x00)
            var v1: Int = 0
            Log.d("ultrasonic", "Let's Start!")


            while (m1Device?.readRegByte(GPIOB_ADDRESS)!!.toInt() and sensorChoice(motor) == 0x00) { //echo?.value == false
                v1 = 0
            }
            nanoTimeStart = System.nanoTime()
            //Log.d("ultrasonic", "echo ARRIVED!")

            while (m1Device?.readRegByte(GPIOB_ADDRESS)!!.toInt() and sensorChoice(motor) != 0x00) {
                v1 = 1
            }
            nanoTimeEnd = System.nanoTime()
            //Log.d("ultrasonic", "echo ENDED!")

            //println("The result is..${nanoTimeEnd - nanoTimeStart}")
            oneMeasuredDistance = round(((nanoTimeEnd - nanoTimeStart) / 1000.0) / 58.23).toFloat() // Cm
            num ++
            mean()
            volume()
            Log.d("ultrasonic", "The number of measurement.. $num")
            Log.d("ultrasonic", "one measuring.. $oneMeasuredDistance cm")
            Log.d("ultrasonic", "mean measuring.. $MeanMeasuredDistance cm")
            //Log.d("ultrasonic", "Volume Ratio ${volumeRatio}%")
            println("")
            Thread.sleep(10)
        }
        return MeanMeasuredDistance
    }

    private fun sensorChoice(motor: Int): Int{ // 아예 모터 - 모터센서 - 초음파 센서(재고) 하나로 통합해버릴까? 디바이스, 레지스터 주소만 다르고  핀 주소값은 동일할테니깐
        val result: Int = when(motor){
            1 -> 0x01
            2 -> 0x02
            3 -> 0x04
            4 -> 0x08
            5 -> 0x10
            6 -> 0x20
            else -> 0x00
        }
        return result
    }


    /*============================================================
    * 재귀평균 함수
    ============================================================*/
    private fun mean(): Float {
        if (num < 50) {
            MeanMeasuredDistance = (MeanMeasuredDistance * (num - 1) / num) + (oneMeasuredDistance / num)
        } else if (num >= 50) {
            MeanMeasuredDistance = (MeanMeasuredDistance * 0.98f) + (oneMeasuredDistance / 50)
        }
        return MeanMeasuredDistance
    }

    /*============================================================
    * 적재량 표현 함수
    ============================================================*/
    private fun volume(): Int {
        estimatedProductNumNow = round((maxDistance - MeanMeasuredDistance) / productHeight).toInt()
        volumeRatio = round(((estimatedProductNumNow / maxProductNum) * 100).toDouble()).toInt()
        Log.d("ultrasonic", "max product num.. $maxProductNum EA")
        Log.d("ultrasonic", "now product num.. $estimatedProductNumNow EA")
        Log.d("ultrasonic", "Volume ratio.. $estimatedProductNumNow %")
        return volumeRatio
    }

    /*============================================================
    * 변수 초기화
    ============================================================*/
    private fun ultrasonicReset(){
        oneMeasuredDistance = 0f // 1회 측정거리
        MeanMeasuredDistance = 0f // 평균측정거리
        num = 0 // 측정 횟수
        volumeRatio = 0 // 상품적재비율
        estimatedProductNumNow = 0 //현재 상품 적재량
    }



    /* ============================================================
    * IODIRA [GPIO 용도로 사용] 초기 설정
    ============================================================ */
    fun initIODIRA(mode: String) {
        if (mode == "output")
            writeRegisterFlag(m1Device, IODIRA_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 0 -> output, 1 -> input // 골라서 사용 가능
        else if (mode == "input")
            writeRegisterFlag(m1Device, IODIRA_ADDRESS, 0xFF) // 0x00 = 0b1111 1111 // 0 -> output, 1 -> input // 골라서 사용 가능
    }

    /* ============================================================
    * IODIRB [GPIO 용도로 사용] 초기 설정
    ============================================================ */
    fun initIODIRB(mode: String) {
        if (mode == "output")
            writeRegisterFlag(m1Device, IODIRB_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 0 -> output, 1 -> input // 골라서 사용 가능
        else if (mode == "input")
            writeRegisterFlag(m1Device, IODIRB_ADDRESS, 0xFF) // 0x00 = 0b1111 1111 // 0 -> output, 1 -> input // 골라서 사용 가능
    }

    private fun writeRegisterFlag(device: I2cDevice?, address: Int, data: Int) {
        device?.writeRegByte(address, data.toByte())
    }

    private fun readRegisterFlag(device: I2cDevice?, address: Int) {
        inputData = device?.readRegByte(address)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            m1Device?.close()
            m1Device = null
        } catch (e: IOException) {
            Log.w("I2C", "Unable to close I2C device", e)
        }
    }
}