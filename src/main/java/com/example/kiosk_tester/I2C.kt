package com.example.kiosk_tester

import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.google.android.things.pio.PeripheralManager
import java.io.IOException
import com.google.android.things.pio.I2cDevice
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.RuntimeException
import com.qualcomm.robotcore.hardware.DistanceSensor

import com.qualcomm.robotcore.hardware.I2cAddr

import com.qualcomm.robotcore.hardware.I2cDeviceSynch

import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice

import com.qualcomm.robotcore.hardware.I2cWaitControl

import com.qualcomm.hardware.stmicroelectronics.VL53L0X
import com.qualcomm.robotcore.util.TypeConversion
import com.qualcomm.robotcore.hardware.*
import com.qualcomm.robotcore.util.ElapsedTime


import java.nio.charset.Charset
import java.sql.Time
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.time.ExperimentalTime
import kotlin.time.microseconds
import kotlin.time.toDuration

//
// I2C Device Name
private const val I2C_DEVICE_NAME: String = "I2C-1" //odroid i2c name


// I2C Worker Address
private const val IODIRA_ADDRESS: Int = 0x00 // IODIRA의 Control Register
private const val I2C_ADDRESS: Int = 0x20 // MCP23017 opcode(깨우는 용도)
private const val I2C_ADDRESS2: Int = 0x29// 거리센서 device address
private const val GPIOA_ADDRESS: Int = 0x12 // GPIOA의 Control Register
private const val GPIOB_ADDRESS: Int = 0x13 // GPIOB의 Control Register

// I2C Variables
private var configControl: Int = 0
private var stopVariable: Byte? = null
private var signalRateLimit = 0f
private var timeout: Int = 0
private var ioTimeout: Int = 0
private var measurement_timing_budget_us: Long = 0
private var spadCount: Int = 0
private var spad_type_is_aperture = false

private var m1Device: I2cDevice? = null
private var m2Device: I2cDevice? = null
private var m3Device: I2cDevice? = null

//data input
private var inputData: Byte? = null

object I2C : AppCompatActivity() {

    enum class Register(var bVal: Int) {
        SYSRANGE_START(0x00),
        SYSTEM_THRESH_HIGH(0x0C),
        SYSTEM_THRESH_LOW(0x0E),
        SYSTEM_SEQUENCE_CONFIG(0x01),
        SYSTEM_RANGE_CONFIG(0x09),
        SYSTEM_INTERMEASUREMENT_PERIOD(0x04),
        SYSTEM_INTERRUPT_CONFIG_GPIO(0x0A),
        GPIO_HV_MUX_ACTIVE_HIGH(0x84),
        SYSTEM_INTERRUPT_CLEAR(0x0B),
        RESULT_INTERRUPT_STATUS(0x13),

        RESULT_RANGE_STATUS(0x14),
        RESULT_CORE_AMBIENT_WINDOW_EVENTS_RTN(0xBC),
        RESULT_CORE_RANGING_TOTAL_EVENTS_RTN(0xC0),
        RESULT_CORE_AMBIENT_WINDOW_EVENTS_REF(0xD0),
        RESULT_CORE_RANGING_TOTAL_EVENTS_REF(0xD4),
        RESULT_PEAK_SIGNAL_RATE_REF(0xB6),
        ALGO_PART_TO_PART_RANGE_OFFSET_MM(0x28),
        I2C_SLAVE_DEVICE_ADDRESS(0x8A),
        MSRC_CONFIG_CONTROL(0x60),
        PRE_RANGE_CONFIG_MIN_SNR(0x27),
        PRE_RANGE_CONFIG_VALID_PHASE_LOW(0x56),
        PRE_RANGE_CONFIG_VALID_PHASE_HIGH(0x57),
        PRE_RANGE_MIN_COUNT_RATE_RTN_LIMIT(0x64),
        FINAL_RANGE_CONFIG_MIN_SNR(0x67),
        FINAL_RANGE_CONFIG_VALID_PHASE_LOW(0x47),
        FINAL_RANGE_CONFIG_VALID_PHASE_HIGH(0x48),
        FINAL_RANGE_CONFIG_MIN_COUNT_RATE_RTN_LIMIT(0x44),
        PRE_RANGE_CONFIG_SIGMA_THRESH_HI(0x61),
        PRE_RANGE_CONFIG_SIGMA_THRESH_LO(0x62),
        PRE_RANGE_CONFIG_VCSEL_PERIOD(0x50),
        PRE_RANGE_CONFIG_TIMEOUT_MACROP_HI(0x51),
        PRE_RANGE_CONFIG_TIMEOUT_MACROP_LO(0x52),
        SYSTEM_HISTOGRAM_BIN(0x81),
        HISTOGRAM_CONFIG_INITIAL_PHASE_SELECT(0x33),

        HISTOGRAM_CONFIG_READOUT_CTRL(0x55),
        FINAL_RANGE_CONFIG_VCSEL_PERIOD(0x70),
        FINAL_RANGE_CONFIG_TIMEOUT_MACROP_HI(0x71),
        FINAL_RANGE_CONFIG_TIMEOUT_MACROP_LO(0x72),
        CROSSTALK_COMPENSATION_PEAK_RATE_MCPS(0x20),
        MSRC_CONFIG_TIMEOUT_MACROP(0x46),
        SOFT_RESET_GO2_SOFT_RESET_N(0xBF),
        IDENTIFICATION_MODEL_ID(0xC0),
        IDENTIFICATION_REVISION_ID(0xC2),
        OSC_CALIBRATE_VAL(0xF8),
        GLOBAL_CONFIG_VCSEL_WIDTH(0x32),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_0(0xB0),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_1(0xB1),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_2(0xB2),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_3(0xB3),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_4(0xB4),
        GLOBAL_CONFIG_SPAD_ENABLES_REF_5(0xB5),
        GLOBAL_CONFIG_REF_EN_START_SELECT(0xB6),
        DYNAMIC_SPAD_NUM_REQUESTED_REF_SPAD(0x4E),
        DYNAMIC_SPAD_REF_EN_START_OFFSET(0x4F),
        POWER_MANAGEMENT_GO1_POWER_FORCE(0x80),
        VHV_CONFIG_PAD_SCL_SDA__EXTSUP_HV(0x89),
        ALGO_PHASECAL_LIM(0x30),
        ALGO_PHASECAL_CONFIG_TIMEOUT(0x30)
    }

    internal enum class vcselPeriodType {
        VcselPeriodPreRange, VcselPeriodFinalRange
    }


    @ExperimentalTime
    fun i2cInit() {
        ////////////////////////////////////
        /*val manager = PeripheralManager.getInstance()
        val deviceList: List<String> = manager.i2cBusList
        if (deviceList.isEmpty()) {
            Log.i("I2C", "No I2C bus available on this device.")
        } else {
            Log.i("I2C", "List of available devices: $deviceList")
        }*/
        ///////////////////// /
        GlobalScope.launch(Dispatchers.Default) {
            // Attempt to access the I2C device
            m1Device = try {
                PeripheralManager.getInstance()
                    .openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS)
            } catch (e: IOException) {
                Log.w("I2C", "Unable to access I2C device", e)
                null
            }
            m2Device = try {
                PeripheralManager.getInstance()
                    .openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS2)
            } catch (e: IOException) {
                Log.w("I2C", "Unable to access I2C device", e)
                null
            }
            //doInitialize()
//            initDistanceSensor()

///////////// MCP 간단 동작 코드
            /*
            for (i in 1..10) {
                println("HI")
                writeRegisterFlag(m1Device, gpioRegisterAddress, 0xFF)//bit 1~ bit5 까지 on
                delay(500L)
                writeRegisterFlag(m1Device, gpioRegisterAddress, 0x00)
                delay(500L)
//                readRegisterFlag(m1Device, gpioRegisterAddress2)
//                println(inputData)
            }*/
        }
    }

    private fun doInitialize(): Boolean {
        var bVal: Byte?
        val A: Long
        Log.d("i2c", "checking it is really useful address of i2c")
        bVal = m2Device?.readRegByte(0xC0)
        Log.d("i2c", "Register 0xC0 = ${bVal} (should be 0xEE)")
        println(0xEE.toByte())
        bVal = m2Device?.readRegByte(0xC1)
        Log.d("i2c", "Register 0xC1 = ${bVal} (should be 0xAA)")
        println(0xAA.toByte())
        bVal = m2Device?.readRegByte(0xC2)
        Log.d("i2c", "Register 0xC2 = ${bVal} (should be 0x10)")
        println(0x10.toByte())
        bVal = m2Device?.readRegByte(0x51)
        Log.d("i2c", "Register 0x51 = ${bVal} (should be 0x0099)")
        println(0x0099.toByte())
        bVal = m2Device?.readRegByte(0x61)
        Log.d("i2c", "Register 0x61 = ${bVal} (should be 0x0000)")
        println(0x0000.toByte())

        initDistanceSensor()

        return true
    }

    private fun initDistanceSensor(): Boolean {

        if (m2Device?.readRegByte(0xC0)!! != 0xEE.toByte()) {//EE
            println("error : this is not identification model ID")
            //onDestroy()
        }
        //Set I2C standard mode
        writeRegisterFlag(m2Device, 0x88, 0x00)
        writeRegisterFlag(m2Device, 0x80, 0x01)
        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x00, 0x00)
        stopVariable = m2Device?.readRegByte(0x91)
        writeRegisterFlag(m2Device, 0x00, 0x01)
        writeRegisterFlag(m2Device, 0xff, 0x00)
        writeRegisterFlag(m2Device, 0x80, 0x00)
        ///////////////////////////////
        // disable SIGNAL_RATE_MSRC (bit 1) and SIGNAL_RATE_PRE_RANGE (bit 4) limit checks
        configControl = m2Device?.readRegByte(Register.MSRC_CONFIG_CONTROL.bVal)!!.toInt() or 0x12 //MSRC_CONFIG_CONTROL = 0x60
        writeRegisterFlag(m2Device, Register.MSRC_CONFIG_CONTROL.bVal, configControl)
        Log.d("i2c", "initial sig rate lim (MCPS) ${getSignalRateLimit()}")

        // set final range signal rate limit to 0.25 MCPS (million counts per second)
        setSignalRateLimit(0.25f)
        Log.d("i2c", "initial sig rate lim (MCPS) ${getSignalRateLimit()}")
        signalRateLimit = 0.25f
        writeRegisterFlag(m2Device, Register.SYSTEM_SEQUENCE_CONFIG.bVal, 0xFF)

        // VL53L0X_DataInit() end

        // VL53L0X_StaticInit() begin
        // set spad_count and spad_type_is_aperature
        //if (!getSpadInfo(&spad_count, &spad_type_is_aperture)) { return false; }
        if (!spadInfo) {
            return false
        }

        // The SPAD map (RefGoodSpadMap) is read by VL53L0X_get_info_from_device() in
        // the API, but the same data seems to be more easily readable from
        // GLOBAL_CONFIG_SPAD_ENABLES_REF_0 through _6, so read it from there
        val refSpadMap: ByteArray
        //refSpadMap = TypeConversion.shortToByteArray(m2Device?.readRegWord(0xB0)!!) + TypeConversion.shortToByteArray(m2Device?.readRegWord(0xB0)!!) + TypeConversion.shortToByteArray(m2Device?.readRegWord(0xB0)!!)
        refSpadMap = ByteArray(6).also { data -> m2Device?.readRegBuffer(Register.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.bVal, data, 6)!! }
        ///////////////////////////////이게 맞는 표현인가????////////////////////////////////////////////
        //레지스터에서 6바이트를 읽어오는건데 readRegWord를 세번 하면 6바이트가 순차적으로 들어오는건지 모르겠다 아니면 그냥[0,1]에 덧셈만 되는건가?

        //ByteArray를 어떻게 만들어 주는지 모르겠다ㅠㅠㅠㅠㅠㅠㅠㅠ
        //m2Device?.read(0xB0.toByteArray(), 6)
        //m2Device?.readRegByte(0xB0)!!.toInt()//GLOBAL_CONFIG_SPAD_ENABLES_REF_0 == 0xB0
        //this.deviceClient!!.read(com.example.kiosk_tester.VL53L0X.Register.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.bVal, 6)

        // -- VL53L0X_set_reference_spads() begin (assume NVM values are valid)
        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, Register.DYNAMIC_SPAD_REF_EN_START_OFFSET.bVal, 0x00)
        writeRegisterFlag(m2Device, Register.DYNAMIC_SPAD_NUM_REQUESTED_REF_SPAD.bVal, 0x2C)
        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, Register.GLOBAL_CONFIG_REF_EN_START_SELECT.bVal, 0xB4)
        val firstSpadToEnable = (if (spad_type_is_aperture) 12 else 0).toByte() // 12 is the first aperture spad
        var spadsEnabled: Byte = 0
        for (i in 0..47) {
            if (i < firstSpadToEnable || spadsEnabled == spadCount.toByte()) {
                // This bit is lower than the first one that should be enabled, or
                // (reference_spad_count) bits have already been enabled, so zero this bit
                refSpadMap[i / 8] = refSpadMap[i / 8] and (1 shl i % 8).inv().toByte()
            } else if (refSpadMap[i / 8].toInt() shr i % 8 and 0x1 != 0) {
                spadsEnabled++
            }
        }

        // write the byte array to register GLOBAL_CONFIG_SPAD_ENABLES_REF_0.
        //writeMulti(GLOBAL_CONFIG_SPAD_ENABLES_REF_0, ref_spad_map, 6);
        //writeRegisterFlag(m2Device, Register.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.bVal, refSpadMap)
        m2Device?.writeRegBuffer(Register.GLOBAL_CONFIG_SPAD_ENABLES_REF_0.bVal, refSpadMap, 6)
        // -- VL53L0X_set_reference_spads() end


        // DefaultTuningSettings from vl53l0x_tuning.h

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x00, 0x00)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x09, 0x00)
        writeRegisterFlag(m2Device, 0x10, 0x00)
        writeRegisterFlag(m2Device, 0x11, 0x00)

        writeRegisterFlag(m2Device, 0x24, 0x01)
        writeRegisterFlag(m2Device, 0x25, 0xFF)
        writeRegisterFlag(m2Device, 0x75, 0x00)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x4E, 0x2C)
        writeRegisterFlag(m2Device, 0x48, 0x00)
        writeRegisterFlag(m2Device, 0x30, 0x20)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x30, 0x09)
        writeRegisterFlag(m2Device, 0x54, 0x00)
        writeRegisterFlag(m2Device, 0x31, 0x04)
        writeRegisterFlag(m2Device, 0x32, 0x03)
        writeRegisterFlag(m2Device, 0x40, 0x83)
        writeRegisterFlag(m2Device, 0x46, 0x25)
        writeRegisterFlag(m2Device, 0x60, 0x00)
        writeRegisterFlag(m2Device, 0x27, 0x00)
        writeRegisterFlag(m2Device, 0x50, 0x06)
        writeRegisterFlag(m2Device, 0x51, 0x00)
        writeRegisterFlag(m2Device, 0x52, 0x96)
        writeRegisterFlag(m2Device, 0x56, 0x08)
        writeRegisterFlag(m2Device, 0x57, 0x30)
        writeRegisterFlag(m2Device, 0x61, 0x00)
        writeRegisterFlag(m2Device, 0x62, 0x00)
        writeRegisterFlag(m2Device, 0x64, 0x00)
        writeRegisterFlag(m2Device, 0x65, 0x00)
        writeRegisterFlag(m2Device, 0x66, 0xA0)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x22, 0x32)
        writeRegisterFlag(m2Device, 0x47, 0x14)
        writeRegisterFlag(m2Device, 0x49, 0xFF)
        writeRegisterFlag(m2Device, 0x4A, 0x00)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x7A, 0x0A)
        writeRegisterFlag(m2Device, 0x7B, 0x00)
        writeRegisterFlag(m2Device, 0x78, 0x21)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x23, 0x34)
        writeRegisterFlag(m2Device, 0x42, 0x00)
        writeRegisterFlag(m2Device, 0x44, 0xFF)
        writeRegisterFlag(m2Device, 0x45, 0x26)
        writeRegisterFlag(m2Device, 0x46, 0x05)
        writeRegisterFlag(m2Device, 0x40, 0x40)
        writeRegisterFlag(m2Device, 0x0E, 0x06)
        writeRegisterFlag(m2Device, 0x20, 0x1A)
        writeRegisterFlag(m2Device, 0x43, 0x40)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x34, 0x03)
        writeRegisterFlag(m2Device, 0x35, 0x44)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x31, 0x04)
        writeRegisterFlag(m2Device, 0x4B, 0x09)
        writeRegisterFlag(m2Device, 0x4C, 0x05)
        writeRegisterFlag(m2Device, 0x4D, 0x04)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x44, 0x00)
        writeRegisterFlag(m2Device, 0x45, 0x20)
        writeRegisterFlag(m2Device, 0x47, 0x08)
        writeRegisterFlag(m2Device, 0x48, 0x28)
        writeRegisterFlag(m2Device, 0x67, 0x00)
        writeRegisterFlag(m2Device, 0x70, 0x04)
        writeRegisterFlag(m2Device, 0x71, 0x01)
        writeRegisterFlag(m2Device, 0x72, 0xFE)
        writeRegisterFlag(m2Device, 0x76, 0x00)
        writeRegisterFlag(m2Device, 0x77, 0x00)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x0D, 0x01)

        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x80, 0x01)
        writeRegisterFlag(m2Device, 0x01, 0xF8)

        writeRegisterFlag(m2Device, 0xFF, 0x01)
        writeRegisterFlag(m2Device, 0x8E, 0x01)
        writeRegisterFlag(m2Device, 0x00, 0x01)
        writeRegisterFlag(m2Device, 0xFF, 0x00)
        writeRegisterFlag(m2Device, 0x80, 0x00)

        // -- VL53L0X_load_tuning_settings() end

        // "Set interrupt config to new sample ready"
        // -- VL53L0X_SetGpioConfig() begin
        writeRegisterFlag(m2Device, Register.SYSTEM_INTERRUPT_CONFIG_GPIO.bVal, 0x04)
        writeRegisterFlag(
            m2Device, Register.GPIO_HV_MUX_ACTIVE_HIGH.bVal,
            m2Device?.readRegByte(Register.GPIO_HV_MUX_ACTIVE_HIGH.bVal)!!.toInt() and 0x10.inv()
        ) // active low
        writeRegisterFlag(m2Device, Register.SYSTEM_INTERRUPT_CLEAR.bVal, 0x01)

        // -- VL53L0X_SetGpioConfig() end
        measurement_timing_budget_us = measurementTimingBudget

        // "Disable MSRC and TCC by default"
        // MSRC = Minimum Signal Rate Check
        // TCC = Target CentreCheck
        // -- VL53L0X_SetSequenceStepEnable() begin
        writeRegisterFlag(m2Device, Register.SYSTEM_SEQUENCE_CONFIG.bVal, 0xE8)

        // -- VL53L0X_SetSequenceStepEnable() end

        // "Recalculate timing budget"
        setMeasurementTimingBudget(measurement_timing_budget_us)

        // VL53L0X_StaticInit() end

        // VL53L0X_PerformRefCalibration() begin (VL53L0X_perform_ref_calibration())

        // -- VL53L0X_perform_vhv_calibration() begin
        writeRegisterFlag(m2Device, Register.SYSTEM_SEQUENCE_CONFIG.bVal, 0x01)
        if (!performSingleRefCalibration(0x40)) {
            return false
        }

        // -- VL53L0X_perform_vhv_calibration() end

        // -- VL53L0X_perform_phase_calibration() begin
        writeRegisterFlag(m2Device, Register.SYSTEM_SEQUENCE_CONFIG.bVal, 0x02)
        if (!performSingleRefCalibration(0x00)) {
            return false
        }

        // -- VL53L0X_perform_phase_calibration() end

        // "restore the previous Sequence Config"
        writeRegisterFlag(m2Device, Register.SYSTEM_SEQUENCE_CONFIG.bVal, 0xE8)

        // VL53L0X_PerformRefCalibration() end


        // set timeout period (milliseconds)
        timeout = 200
        setTimeout(timeout)
        println("ioTimeout is.. $ioTimeout")


        return true
    }

    private fun getSignalRateLimit(): Float {
        return (m2Device?.readRegByte(Register.FINAL_RANGE_CONFIG_MIN_COUNT_RATE_RTN_LIMIT.bVal)!! / (1 shl 7)).toFloat()
    }

    private fun setSignalRateLimit(limit_Mcps: Float): Boolean {
        //check range
        if (limit_Mcps < 0 || limit_Mcps > 511.99) return false
        //Q9.7 fixed point format (9 integer bits, 7 fractional bits)
        writeRegisterFlag(m2Device, Register.FINAL_RANGE_CONFIG_MIN_COUNT_RATE_RTN_LIMIT.bVal, (limit_Mcps * (1 shl 7)).toInt())
        return true
    }

    // Get reference SPAD (single photon avalanche diode) count and type
    // based on VL53L0X_get_info_from_device(),
    // but only gets reference SPAD count and type


    private val spadInfo: Boolean
        private get() {
            val tmp: Byte
            writeRegisterFlag(m2Device, 0x80, 0x01)
            writeRegisterFlag(m2Device, 0xFF, 0x01)
            writeRegisterFlag(m2Device, 0x00, 0x00)
            writeRegisterFlag(m2Device, 0xFF, 0x06)
            writeRegisterFlag(m2Device, 0x83, (m2Device?.readRegByte(0x83)!!.toByte() or 0x04.toByte()).toInt())
            writeRegisterFlag(m2Device, 0xFF, 0x07)
            writeRegisterFlag(m2Device, 0x81, 0x01)
            writeRegisterFlag(m2Device, 0x80, 0x01)
            writeRegisterFlag(m2Device, 0x94, 0x6b)
            writeRegisterFlag(m2Device, 0x83, 0x00)

            // example had a timeout mechanism, but
            // this was disabled and not active.
            // checkTimeoutExpired() in example returned false since timer
            // was never initialized.
            // comment it out in our translation of the sample driver.
            //        startTimeout();
            //        while (this.deviceClient.read8(0x83) == 0x00) {
            //            if (checkTimeoutExpired()) {
            //                return false;
            //            }
            //        }
            writeRegisterFlag(m2Device, 0x83, 0x01)
            tmp = m2Device?.readRegByte(0x92)!!

            //  *count = tmp & 0x7f;
            //  *type_is_aperture = (tmp >> 7) & 0x01;
            spadCount = (tmp and 0x7f.toByte()).toInt()
            spad_type_is_aperture = (tmp.toInt() shr 7) and 0x01 != 0
            writeRegisterFlag(m2Device, 0x81, 0x00)
            writeRegisterFlag(m2Device, 0xFF, 0x06)
            writeRegisterFlag(m2Device, 0x83, (m2Device?.readRegByte(0x83)!! and 0x04.inv()).toInt())
            writeRegisterFlag(m2Device, 0xFF, 0x01)
            writeRegisterFlag(m2Device, 0x00, 0x01)

            writeRegisterFlag(m2Device, 0xFF, 0x00)
            writeRegisterFlag(m2Device, 0x80, 0x00)

            return true
        }// note that this is different than the value in set_

    private val measurementTimingBudget: Long
        get() {
            // getMeasurementTimingBudget method uses local structures and passes them by
            // reference... we have to define them as classes.  Then when they are passed as an argument
            // their fields will get updated within the method.
            val enables: SequenceStepEnables = SequenceStepEnables()
            val timeouts: SequenceStepTimeouts = SequenceStepTimeouts()
            val StartOverhead = 1910 // note that this is different than the value in set_
            val EndOverhead = 960
            val MsrcOverhead = 660
            val TccOverhead = 590
            val DssOverhead = 690
            val PreRangeOverhead = 660
            val FinalRangeOverhead = 550

            // "Start and end overhead times always present"
            var budget_us: Long = (StartOverhead + EndOverhead).toLong()
            getSequenceStepEnables(enables)
            getSequenceStepTimeouts(enables, timeouts)
            if (enables.tcc) {
                budget_us += timeouts.msrc_dss_tcc_us + TccOverhead
            }
            if (enables.dss) {
                budget_us += 2 * (timeouts.msrc_dss_tcc_us + DssOverhead)
            } else if (enables.msrc) {
                budget_us += timeouts.msrc_dss_tcc_us + MsrcOverhead
            }
            if (enables.pre_range) {
                budget_us += timeouts.pre_range_us + PreRangeOverhead
            }
            if (enables.final_range) {
                budget_us += timeouts.final_range_us + FinalRangeOverhead
            }
            measurement_timing_budget_us = budget_us // store for internal reuse
            return budget_us
        }

    class SequenceStepEnables {
        var tcc = false
        var msrc = false
        var dss = false
        var pre_range = false
        var final_range = false
    }

    class SequenceStepTimeouts {
        var pre_range_vcsel_period_pclks = 0
        var final_range_vcsel_period_pclks = 0
        var msrc_dss_tcc_mclks = 0
        var pre_range_mclks = 0
        var final_range_mclks = 0
        var msrc_dss_tcc_us: Long = 0
        var pre_range_us: Long = 0
        var final_range_us: Long = 0
    }

    // Get sequence step enables
    // based on VL53L0X_GetSequenceStepEnables()
    fun getSequenceStepEnables(enables: SequenceStepEnables) {
        val sequence_config: Int = m2Device?.readRegByte(Register.SYSTEM_SEQUENCE_CONFIG.bVal)!!.toInt()//
        enables.tcc = if (sequence_config shr 4 and 0x1 != 0) true else false
        enables.dss = if (sequence_config shr 3 and 0x1 != 0) true else false
        enables.msrc = if (sequence_config shr 2 and 0x1 != 0) true else false
        enables.pre_range = if (sequence_config shr 6 and 0x1 != 0) true else false
        enables.final_range = if (sequence_config shr 7 and 0x1 != 0) true else false
    }

    // Get sequence step timeouts
    // based on get_sequence_step_timeout(),
    // but gets all timeouts instead of just the requested one, and also stores
    // intermediate values
    fun getSequenceStepTimeouts(enables: SequenceStepEnables, timeouts: SequenceStepTimeouts) {
        timeouts.pre_range_vcsel_period_pclks = getVcselPulsePeriod(vcselPeriodType.VcselPeriodPreRange)
        timeouts.msrc_dss_tcc_mclks = m2Device?.readRegByte(Register.MSRC_CONFIG_TIMEOUT_MACROP.bVal)!! + 1 //MSRC_CONFIG_TIMEOUT_MACROP = 0x46
        timeouts.msrc_dss_tcc_us = timeoutMclksToMicroseconds(
            timeouts.msrc_dss_tcc_mclks,
            timeouts.pre_range_vcsel_period_pclks
        )
        timeouts.pre_range_mclks = decodeTimeout(readShort(Register.PRE_RANGE_CONFIG_TIMEOUT_MACROP_HI).toInt())
        timeouts.pre_range_us = timeoutMclksToMicroseconds(
            timeouts.pre_range_mclks,
            timeouts.pre_range_vcsel_period_pclks
        )
        timeouts.final_range_vcsel_period_pclks = getVcselPulsePeriod(vcselPeriodType.VcselPeriodFinalRange)
        timeouts.final_range_mclks = decodeTimeout(readShort(Register.FINAL_RANGE_CONFIG_TIMEOUT_MACROP_HI).toInt())
        if (enables.pre_range) {
            timeouts.final_range_mclks -= timeouts.pre_range_mclks
        }
        timeouts.final_range_us = timeoutMclksToMicroseconds(
            timeouts.final_range_mclks,
            timeouts.final_range_vcsel_period_pclks
        )
    }

    // Decode sequence step timeout in MCLKs from register value
    // based on VL53L0X_decode_timeout()
    // Note: the original function returned a uint32_t, but the return value is
    // always stored in a uint16_t.
    fun decodeTimeout(reg_val: Int): Int {
        // format: "(LSByte * 2^MSByte) + 1"
        return (reg_val and 0x00FF shl
                (reg_val and 0xFF00.toInt() shr 8)) + 1
    }

    // Get the VCSEL pulse period in PCLKs for the given period type.
    // based on VL53L0X_get_vcsel_pulse_period()
    private fun getVcselPulsePeriod(type: vcselPeriodType): Int {
        return if (type == vcselPeriodType.VcselPeriodPreRange) {
            decodeVcselPeriod(m2Device?.readRegByte(Register.PRE_RANGE_CONFIG_VCSEL_PERIOD.bVal)!!.toInt())
        } else if (type == vcselPeriodType.VcselPeriodFinalRange) {
            decodeVcselPeriod(m2Device?.readRegByte(Register.FINAL_RANGE_CONFIG_VCSEL_PERIOD.bVal)!!.toInt())
        } else {
            255
        }
    }

    // Decode VCSEL (vertical cavity surface emitting laser) pulse period in PCLKs
    // from register value
    // based on VL53L0X_decode_vcsel_period()
    fun decodeVcselPeriod(reg_val: Int): Int {
        return reg_val + 1 shl 1
    }

    fun setMeasurementTimingBudget(budget_us: Long): Boolean {
        val enables: SequenceStepEnables = SequenceStepEnables()
        val timeouts: SequenceStepTimeouts = SequenceStepTimeouts()
        val StartOverhead = 1320 // note that this is different than the value in get_
        val EndOverhead = 960
        val MsrcOverhead = 660
        val TccOverhead = 590
        val DssOverhead = 690
        val PreRangeOverhead = 660
        val FinalRangeOverhead = 550
        val MinTimingBudget: Long = 20000
        if (budget_us < MinTimingBudget) {
            return false
        }
        var used_budget_us = (StartOverhead + EndOverhead).toLong()
        getSequenceStepEnables(enables)
        getSequenceStepTimeouts(enables, timeouts)
        if (enables.tcc) {
            used_budget_us += timeouts.msrc_dss_tcc_us + TccOverhead
        }
        if (enables.dss) {
            used_budget_us += 2 * (timeouts.msrc_dss_tcc_us + DssOverhead)
        } else if (enables.msrc) {
            used_budget_us += timeouts.msrc_dss_tcc_us + MsrcOverhead
        }
        if (enables.pre_range) {
            used_budget_us += timeouts.pre_range_us + PreRangeOverhead
        }
        if (enables.final_range) {
            used_budget_us += FinalRangeOverhead.toLong()

            // "Note that the final range timeout is determined by the timing
            // budget and the sum of all other timeouts within the sequence.
            // If there is no room for the final range timeout, then an error
            // will be set. Otherwise the remaining time will be applied to
            // the final range."
            if (used_budget_us > budget_us) {
                // "Requested timeout too big."
                return false
            }
            val final_range_timeout_us = budget_us - used_budget_us

            // set_sequence_step_timeout() begin
            // (SequenceStepId == VL53L0X_SEQUENCESTEP_FINAL_RANGE)

            // "For the final range timeout, the pre-range timeout
            //  must be added. To do this both final and pre-range
            //  timeouts must be expressed in macro periods MClks
            //  because they have different vcsel periods."
            var final_range_timeout_mclks = timeoutMicrosecondsToMclks(
                final_range_timeout_us,
                timeouts.final_range_vcsel_period_pclks
            )
            if (enables.pre_range) {
                final_range_timeout_mclks += timeouts.pre_range_mclks.toLong()
            }
            writeShort(
                Register.FINAL_RANGE_CONFIG_TIMEOUT_MACROP_HI,
                encodeTimeout(final_range_timeout_mclks.toInt()).toShort()
            )

            // set_sequence_step_timeout() end
            measurement_timing_budget_us = budget_us // store for internal reuse
        }
        return true
    }

    // Convert sequence step timeout from microseconds to MCLKs with given VCSEL period in PCLKs
    // based on VL53L0X_calc_timeout_mclks()
    fun timeoutMicrosecondsToMclks(timeout_period_us: Long, vcsel_period_pclks: Int): Long {
        val macro_period_ns = calcMacroPeriod(vcsel_period_pclks)
        return (timeout_period_us * 1000 + macro_period_ns / 2) / macro_period_ns
    }

    // based on VL53L0X_perform_single_ref_calibration()
    private fun performSingleRefCalibration(vhv_init_byte: Int): Boolean {
        writeRegisterFlag(m2Device, Register.SYSRANGE_START.bVal, 0x01 or vhv_init_byte) // VL53L0X_REG_SYSRANGE_MODE_START_STOP

        // in example, during initialization the timeout was disabled, so i'm commenting out here.
        // if we call this method after init is complete, we might have
        // to implement this timeout-related code.
//        startTimeout();
//        while ((readReg(RESULT_INTERRUPT_STATUS) & 0x07) == 0) {
//            if (checkTimeoutExpired()) {
//                return false;
//            }
//        }
        writeRegisterFlag(m2Device, Register.SYSTEM_INTERRUPT_CLEAR.bVal, 0x01)
        writeRegisterFlag(m2Device, Register.SYSRANGE_START.bVal, 0x00)
        return true
    }

    // Convert sequence step timeout from MCLKs to microseconds with given VCSEL period in PCLKs
    // based on VL53L0X_calc_timeout_us()
    fun timeoutMclksToMicroseconds(timeout_period_mclks: Int, vcsel_period_pclks: Int): Long {
        val macro_period_ns = calcMacroPeriod(vcsel_period_pclks)
        return (timeout_period_mclks * macro_period_ns + macro_period_ns / 2) / 1000
    }

    // Encode sequence step timeout register value from timeout in MCLKs
    // based on VL53L0X_encode_timeout()
    // Note: the original function took a uint16_t, but the argument passed to it
    // is always a uint16_t.
    fun encodeTimeout(timeout_mclks: Int): Long {
        // format: "(LSByte * 2^MSByte) + 1"
        var ls_byte: Long = 0
        var ms_byte = 0
        return if (timeout_mclks > 0) {
            ls_byte = (timeout_mclks - 1).toLong()
            while (ls_byte and -0x100 > 0) {
                ls_byte = ls_byte shr 1
                ms_byte++
            }
            (ms_byte shl 8 or (ls_byte and 0xFF).toInt()).toLong()
        } else {
            0
        }
    }

    // Calculate macro period in *nanoseconds* from VCSEL period in PCLKs
    // based on VL53L0X_calc_macro_period_ps()
    // PLL_period_ps = 1655; macro_period_vclks = 2304
    fun calcMacroPeriod(vcsel_period_pclks: Int): Long {
        return (2304.toLong() * vcsel_period_pclks * 1655 + 500) / 1000
    }

    fun writeShort(reg: Register, value: Short) {
        m2Device?.writeRegBuffer(reg.bVal, TypeConversion.shortToByteArray(value), TypeConversion.shortToByteArray(value).size)
    }
    //여기서는 2바이트 쓰고 2바이트 읽는다 그니까 read는 word로 사용해 2바이트만 읽어준다

    fun readShort(reg: Register): Short {
        return m2Device?.readRegWord(reg.bVal)!!
    }

    fun setTimeout(timeout: Int) {
        ioTimeout = timeout
    }

    private var getTimeout: Int = ioTimeout
    private var ioElapsedTime = ElapsedTime()

    private fun readRangeContinuousMillimeters(): Int {
        if (ioTimeout > 0) {
            ioElapsedTime.reset()
        }
        while ((m2Device?.readRegByte(Register.RESULT_INTERRUPT_STATUS.bVal)!!.toInt() and 0x07) == 0) {//Register.RESULT_INTERRUPT_STATUS = 0x13
            if (ioElapsedTime.milliseconds() > ioTimeout)
                return 65535
        }
        val range: Int = m2Device?.readRegWord(Register.RESULT_RANGE_STATUS.bVal + 10)!!.toInt() //Register.RESULT_RANGE_STATUS = 0x14
        writeRegisterFlag(m2Device, Register.SYSTEM_INTERRUPT_CLEAR.bVal, 0x01) //Register.SYSTEM_INTERRUPT_CLEAR = 0x0B
        return range
    }

    private fun getDistance(): Double {
        var range: Double = readRangeContinuousMillimeters().toDouble()
        return range
    }


    /* ============================================================
    * IODIRA [GPIO 용도로 사용] 초기 설정
    ============================================================ */
    private fun initGPIOA(mode: String) {
        if (mode == "output")
            writeRegisterFlag(m1Device, IODIRA_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 0 -> output, 1 -> input // 골라서 사용 가능
        else if (mode == "input")
            writeRegisterFlag(m1Device, IODIRA_ADDRESS, 0xFF) // 0x00 = 0b1111 1111 // 0 -> output, 1 -> input // 골라서 사용 가능
    }


    /* ============================================================
    * I2C 쓰기
    * device : 초기에 i2c open 진행한 것
    * address : Control Register
    * data : 입력할 데이터
    ============================================================ */
    private fun writeRegisterFlag(device: I2cDevice?, address: Int, data: Int) {
        device?.writeRegByte(address, data.toByte())
    }

    /* ============================================================
    * I2C 읽기
    * device : 초기에 i2c open 진행한 것
    * address : Control Register
    ============================================================ */
    private fun readRegisterFlag(device: I2cDevice?, address: Int) {
        inputData = device?.readRegByte(address)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            m1Device?.close()
            m2Device?.close()
            //m2Device?.close()
            m1Device = null
            m2Device = null
            //m2Device = null
        } catch (e: IOException) {
            Log.w("I2C", "Unable to close I2C device", e)
        }
    }

/*
    /* ============================================================
    * I2C 시작
    ============================================================ */
    fun initI2C() {
        mDevice = try {
            PeripheralManager.getInstance().openI2cDevice(I2C_DEVICE_NAME, I2C_ADDRESS)
        } catch (e: IOException) {
            Log.w("I2C Error", "Unable to access I2C device", e)
            null
        }

        initIODIRA("output") // GPIOA 전체를 output 설정
        initIODIRB("input") // GPIOB 전체를 input 설정

        testing() // GPIOA : LED 껐다 켰다 // GPIOB : 데이터 읽기
    }


    /* ============================================================
    * I2C 쓰기
    * device : 초기에 i2c open 진행한 것
    * address : Control Register
    * data : 입력할 데이터
    ============================================================ */
    private fun writeRegisterFlag(device: I2cDevice?, address: Int, data: Int) {
        device?.writeRegByte(address, data.toByte())
    }


    /* ============================================================
    * I2C 읽기
    * device : 초기에 i2c open 진행한 것
    * address : Control Register
    ============================================================ */
    private fun readRegisterFlag(device: I2cDevice?, address: Int) {
        inputData = device?.readRegByte(address)
    }


    /* ============================================================
    * IODIRA [GPIO 용도로 사용] 초기 설정
    ============================================================ */
    private fun initIODIRA(mode:String){
        if (mode == "output")
            writeRegisterFlag(mDevice, IODIRA_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 0 -> output, 1 -> input // 골라서 사용 가능
        else if (mode == "input")
            writeRegisterFlag(mDevice, IODIRA_ADDRESS, 0xFF) // 0x00 = 0b1111 1111 // 0 -> output, 1 -> input // 골라서 사용 가능
    }


    /* ============================================================
    * IODIRB [GPIO 용도로 사용] 초기 설정
    ============================================================ */
    private fun initIODIRB(mode:String){
        if (mode == "output")
            writeRegisterFlag(mDevice, IODIRB_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 0 -> output, 1 -> input // 골라서 사용 가능
        else if (mode == "input")
            writeRegisterFlag(mDevice, IODIRB_ADDRESS, 0xFF) // 0x00 = 0b1111 1111 // 0 -> output, 1 -> input // 골라서 사용 가능
    }

    /* ============================================================
    * 그냥 테스트
    ============================================================ */
    private fun testing() {
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                writeRegisterFlag(mDevice, GPIOA_ADDRESS, 0xFF) // 0xFF = 0b1111 1111 // 전체 High output 설정 // 골라서 사용 가능
                delay(1000L)
                writeRegisterFlag(mDevice, GPIOA_ADDRESS, 0x00) // 0x00 = 0b0000 0000 // 전체 LOW output 설정 // 골라서 사용 가능
                delay(1000L)
                readRegisterFlag(mDevice, GPIOB_ADDRESS) // GPIOB 읽기
                // 0b0000 0000 == 0(int) <- 모두가 GND 연결 // 0b1111 1111 == =128(int) <- 모두가 3.3V 연결 // 0b0000 0001 == 1(int) <- GPIOB.1만 3.3V 연결
                Log.d("data", inputData.toString())
            }
        }
    }

*/
}
