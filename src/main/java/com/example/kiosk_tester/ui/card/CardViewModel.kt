package com.example.kiosk_tester.ui.card

import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kiosk_tester.Card
import java.util.*
import kotlin.concurrent.timerTask

class CardViewModel : ViewModel() {

    private val cardTestResultD = MutableLiveData<String>().apply {
        value = "진행"
    }
    val cardTestResult: LiveData<String> = cardTestResultD

    private val payResultD = MutableLiveData<String>().apply {
        value = "진행"
    }
    val payResult: LiveData<String> = payResultD

    private val payCancelResultD = MutableLiveData<String>().apply {
        value = "진행"
    }
    val payCancelResult: LiveData<String> = payCancelResultD

    private val payRefundResultD = MutableLiveData<String>().apply {
        value = "진행"
    }
    val payRefundResult: LiveData<String> = payRefundResultD

    private val cardKeyResultD = MutableLiveData<String>().apply {
        value = "진행"
    }
    val cardKeyResult: LiveData<String> = cardKeyResultD


    fun resultReturn(text: TextView, meg: String) {
        text.visibility = TextView.VISIBLE
        text.text = meg
    }

    fun cardTest(text: TextView){
        Card.cardStatusCheck()
        resultReturn(text, "진행")
        checking(3, text)
    }

    fun payApproval(text: TextView) {
        Card.payApproval()
        resultReturn(text, "진행")
        checking(1, text)
    }

    fun payCancel(text: TextView) {
        Card.payCancel()
        var result = Card.getResult(2)
        if (result == "Success")
            resultReturn(text, "성공")
        else
            resultReturn(text, "실패")
        Card.resetResult(2)
    }

    fun payRefund(text: TextView) {
        Card.payRefund()
        resultReturn(text, "진행")
        checking(5, text)
    }

    fun cardkey(text: TextView){
        Card.cardExchangingKey()
        resultReturn(text, "진행")
        checking(4, text)
    }

    private fun checking(num: Int, text: TextView) {
        Timer().schedule(timerTask {
            Log.d("Motor Test", "$num")
            when (Card.getResult(num)) {
                "Success" -> {
                    if (num == 1) payResultD.postValue("성공")
                    else if (num == 3) cardTestResultD.postValue("정상")
                    else if (num == 4) cardKeyResultD.postValue("성공")
                    else payRefundResultD.postValue("성공")
                    Card.resetResult(num)
                }
                "Fail" -> {
                    if (num == 1) payResultD.postValue("실패")
                    else if (num == 3) cardTestResultD.postValue("오류")
                    else if (num == 4) cardKeyResultD.postValue("실패")
                    else payRefundResultD.postValue("실패")
                    Card.resetResult(num)
                }
                else -> {
                    checking(num, text)
                }
            }

        }, 1000)
    }



}