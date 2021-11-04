package com.example.kiosk_tester.ui.PUSH

import androidx.lifecycle.ViewModel
import com.example.kiosk_tester.PUSH
import android.graphics.Color
import android.media.MediaPlayer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.kiosk_tester.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.*

/*======================================================
======================== Variable ======================
=======================================================*/
private var countState: Boolean = false
private var startState: Boolean = false
//private var buttonNum: Int = 0
//private var pushRunState: Boolean = false

var productState = mutableListOf<Int>(0, 0, 0, 0, 0, 0) // P1~ P6 상태 State 0: 회색(상품X, 터치반응X), 1: 보라(상품O, 버튼활성화) 2: 노랑(모터동작 대기), 3: 파랑(모터동작중)


class PUSHViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun pushReset(P1: Button, P2: Button, P3: Button, P4: Button, P5: Button, P6: Button, count_btn: Button, pushStart_btn: Button) {
        //초기화
        //모든 버튼 초기화
        //모든 변수 초기화
        //UI 초기화
        //PUSH.pushReset()
        for (i in productState.indices) {
            productState[i] = 0
        }
        buttonState(P1, 0)
        buttonState(P2, 0)
        buttonState(P3, 0)
        buttonState(P4, 0)
        buttonState(P5, 0)
        buttonState(P6, 0)
        countState = false
        startState = false
        count_btn.text = "1회"
        count_btn.setBackgroundColor(Color.parseColor("#424242"))
        pushStart_btn.setBackgroundColor(Color.parseColor("#424242"))
        println("Reset")
    }

    fun pushStart(P1: Button, P2: Button, P3: Button, P4: Button, P5: Button, P6: Button, count_btn: Button, pushStart_btn: Button) {
        GlobalScope.launch(Dispatchers.Default) {
            //모터 동작
            //노랑색으로(동작대기상태) 표시된 상품을 순차적으로 동작
            //횟수(순차적 동작(1개 모터 동작))
            //'시작' 버튼 누르면 '정지' 글자로 바뀜
            //pushReset(P1, P2, P3, P4, P5, P6, count_btn)
            val productButtonList = mutableListOf<Button>(P1, P2, P3, P4, P5, P6)

            startState = !startState

            if (startState && (productState[0] == 2 || productState[1] == 2 || productState[2] == 2 || productState[3] == 2 || productState[4] == 2 || productState[5] == 2)) {
                pushStart_btn.setBackgroundColor(Color.parseColor("#00BFFF"))

                if (count_btn.text == "1회") {
                    for (i in productButtonList.indices) {

                        if (productState[i] == 2) {
                            productState[i] = 1
                            buttonState(productButtonList[i], 3)
                            PUSH.pushRun(i + 1)

                            delay(10000L)

                            buttonState(productButtonList[i], 1)
                            pushStart_btn.setBackgroundColor(Color.parseColor("#424242"))
                        }
                    }
                    startState = !startState

                } else if (count_btn.text == "무한") {
                    for (i in productButtonList.indices) {

                        if (productState[i] == 2) {
                            productState[i] = 2
                            buttonState(productButtonList[i], 3)
                            PUSH.pushRun(i + 1)

                            delay(10000L)

                            buttonState(productButtonList[i], 2)

                        }
                    }
                    startState = !startState
                    pushStart(P1, P2, P3, P4, P5, P6, count_btn, pushStart_btn)

                }

            } else {
                pushStart_btn.setBackgroundColor(Color.parseColor("#424242"))
            }
        }
    }

    fun productDetection(P1: Button, P2: Button, P3: Button, P4: Button, P5: Button, P6: Button) {
        //상품확인
        //누르면 push코드 안에 blockDetection 함수 실행 & returnPush 변수 값을 P1~P6까지에 반영
        //(ex. returnPush값이 0,1,2 이면 P1,P2,P3버튼이 보라색으로 변함)
        val block: Int? = pushList.maxOrNull() // 최대상품구분갯수 반환
        val productButtonList = mutableListOf<Button>(P1, P2, P3, P4, P5, P6)
        PUSH.blockDetect()
        for (i in 1..block!!) {
            productState[i - 1] = 1
            buttonState(productButtonList[i - 1], 1)
            //productButtonList[i-1].setBackgroundColor(Color.parseColor("#EE82EE"))
        }
    }

    fun countNum(count_btn: Button) {
        //1회 or 무한
        //기본은 "1회" 한번 클릭하면 "무한"
        //state = false -> 1회, state = true ->무한, 순환구조
        if (!startState) {
            countState = !countState
            if (countState) {
                count_btn.text = "무한"
                count_btn.setBackgroundColor(Color.parseColor("#00BFFF"))
            } else {
                count_btn.text = "1회"
                count_btn.setBackgroundColor(Color.parseColor("#424242"))
            }
        }
    }

    fun clickButton(buttonNum: Button, num: Int) {
        //버튼 클릭하면 발생하는 함수
        //보라색 버튼 누르면 노랑으로 변함(buttonState함수 실행)
        //노란색 버튼 누르면 보라색으로 변함(")
        //회색 버튼 누르면 변화X
        if (!startState) {
            if (productState[num - 1] == 1) {
                productState[num - 1]++
                buttonState(buttonNum, 2)
            } else if (productState[num - 1] == 2) {
                productState[num - 1]--
                buttonState(buttonNum, 1)
            }
        }

    }

    private fun buttonState(buttonNum: Button, state: Int) {
        //버튼 누름 상태
        //기본 활성화된 버튼 -> 보라         (상태 1)
        //동작시킬 상품 버튼 누름 -> 노랑    (상태 2)
        //동작 중 -> 파랑                   (상태 3)
        //동작 종료 후 -> 노랑              (상태 2)

        when (state) {
            0 -> buttonNum.setBackgroundColor(Color.parseColor("#424242"))//회색
            1 -> buttonNum.setBackgroundColor(Color.parseColor("#EE82EE"))//보라
            2 -> buttonNum.setBackgroundColor(Color.parseColor("#F6FF00"))//노랑
            3 -> buttonNum.setBackgroundColor(Color.parseColor("#00BFFF"))//파랑
        }
    }
}