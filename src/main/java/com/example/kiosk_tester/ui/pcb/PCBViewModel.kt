package com.example.kiosk_tester.ui.pcb

import android.graphics.Color
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kiosk_tester.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask

val doorData = MutableLiveData<String>()


class PCBViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = version
    }
    val testerVersion: LiveData<String> = _text

    val door : LiveData<String>
        get() = doorData

    fun resultReturn(text: TextView, meg: String) {
        text.visibility = TextView.VISIBLE
        text.text = meg
    }

    fun pcbCheck(text: TextView) {
        PCB.pcbCheck()
        if (PCBResult == "PO") {
            resultReturn(text, "정상")
        } else {
            resultReturn(text, "오류")
        }
    }

    fun passCheck(text: TextView) {
        GlobalScope.launch(Dispatchers.Main) {
            PCB.passCheck()
            delay(200L)
            if (passResult == "PO") {
                text.setTextColor(Color.parseColor("#FFFFFF"))
                resultReturn(text, "정상")
            } else if (passResult == "PF") {
                text.setTextColor(Color.parseColor("#F42700"))
                resultReturn(text, "오류")
            }
        }
    }

    fun doorCheck(text: TextView) {
        if (doorResult == "DO") {
            resultReturn(text, "열림")
        } else if (doorResult == "DC") {
            resultReturn(text, "닫힘")
        }
    }
    private var ledONOFF:Boolean = false
    fun ledCheck(text: TextView) {
        if (!ledONOFF){
            PCB.ledRun(true)
            resultReturn(text, "켜짐")
            ledONOFF = true
        }
        else{
            PCB.ledRun(false)
            resultReturn(text, "꺼짐")
            ledONOFF = false
        }
    }
    fun motorReset(text: TextView) {
        PCB.motorReset()
        text.setTextColor(Color.parseColor("#FFFFFF"))
        resultReturn(text, "완료")
    }

    fun firstUI(text1: TextView,text2: TextView,text3: TextView){
        if (PCBResult == "PO") {
            resultReturn(text1, "정상")
        } else {
            resultReturn(text1, "오류")
        }
        if (passResult == "PO") {
            text2.setTextColor(Color.parseColor("#FFFFFF"))
            resultReturn(text2, "정상")
        } else if (passResult == "PF") {
            text2.setTextColor(Color.parseColor("#F42700"))
            resultReturn(text2, "오류")
        }

        doorCheck(text3)
    }
}



