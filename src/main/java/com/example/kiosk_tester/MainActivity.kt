/* ============================================================
* IF YOU OPEN THIS, YOU WILL KNOW THAT [DAVID] IS THE BEST
============================================================ */
/* ============================================================
* Copyright (c) 2021 by Venduster, Inc. All rights reserved.
* 최초 작성일자 : 2021-05-12
* 최초 작성자 : David (김규태) <gyutae0729@gmail.com>
* 소형 PCB 테스트 앱
* 기능 : 모터 6개 작동, 투출 감지, 도어 감지, LED 작동, 카드리더기 작동
============================================================ */
/* ============================================================
* vbc_tester_1.0.1
* 2021-05-12, David (김규태)
* 수정 내용
* 1. UI 변경 (모터 25 -> 6 개수 감소)
============================================================ */
/* ============================================================
* vbc_tester_1.0.2
* 2021-06-07, David (김규태)
* 수정 내용
* 1. 메뉴바에 종료 버튼 추가
* 2. Cat id 변경, 단말기 Driver 변경
============================================================ */
/* ============================================================
* vbc_tester_1.0.3
* 2021-06-10, David (김규태)
* 수정 내용
* 1. LED 타이밍 조절
* 2. 문 열고 모터 스위치 눌러지면 5초 후 모터 회전
* 3. 카드리더기 Key 초기화 작업 추가
============================================================ */
/* ============================================================
* vbc_tester_1.0.4
* 2021-06-22, David (김규태)
* 수정 내용
* 1. 튕김 오류 수정
* 2. 시작/중지 버튼 중복 클릭 방지
* 3. 버튼 UI 변경
* 4. 초기화 오류 해결
* 5. 사운드 추가
============================================================ */

package com.example.kiosk_tester

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import service.vcat.smartro.com.vcat.SmartroVCatCallback
import service.vcat.smartro.com.vcat.SmartroVCatInterface
import java.io.IOException
import java.util.*
import java.util.logging.Handler
import java.util.logging.LogRecord
import kotlin.concurrent.timerTask
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

const val version = "1.0.4"

private const val SERVER_ACTION = "smartro.vcat.action"
private const val SERVER_PACKAGE = "service.vcat.smartro.com.vcat"
var mSmartroVCatInterface: SmartroVCatInterface? = null
var welcomeSound:MediaPlayer ?= null
var oySound:MediaPlayer ?= null
var ocSound:MediaPlayer ?= null
var onSound:MediaPlayer ?= null
var meSound:MediaPlayer ?= null
var pfSound:MediaPlayer ?= null


class MainActivity : AppCompatActivity() {


    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_card, R.id.navigation_pcb, R.id.navigation_motor
            )
        )
        welcomeSound = MediaPlayer.create(this, R.raw.start)
        oySound = MediaPlayer.create(this, R.raw.oy)
        onSound = MediaPlayer.create(this, R.raw.on)
        ocSound = MediaPlayer.create(this, R.raw.oc)
        meSound = MediaPlayer.create(this, R.raw.me)
        pfSound = MediaPlayer.create(this, R.raw.pf)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        //PCB.pcbInit()
        //cardInit()
        welcomeSound?.start()
        //PCB.motorRun(1)
        //I2C.i2cInit()
        //I2C.initI2C()
        PUSH.pushInit()
        //PUSH.pushReset()
        I2CUltrasonic.ultrasonicInit()
        //I2CUltrasonic.choice()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.turnoff, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.turnoff -> {
                finishAffinity()
                System.runFinalization()
                exitProcess(0)
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onStop() {
        super.onStop()
        Log.d("stop", "stopped")
        //PCB.pcbDestroy()
        PUSH.pushDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        //PCB.pcbInit()
        //PCB.motorReset()
        cardInit()
    }


    fun cardInit() {
        val intentTemp: Intent = Intent(SERVER_ACTION)
        intentTemp.setPackage(SERVER_PACKAGE)

        // 카드리더기의 입력값이 들어갈 jsonInput 선언
        val jsonInput = JSONObject()

        val cardServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mSmartroVCatInterface = SmartroVCatInterface.Stub.asInterface(service)

                val jsonArray = JSONArray()
                jsonArray.put("com")
                jsonArray.put("ftdi1")
                jsonArray.put("115200")
                jsonInput.put("service", "setting")
                jsonInput.put("device-comm", jsonArray)

                try {
                    Log.d("Card", "서비스가 연결되었습니다.")
                } catch (e: IOException) {
                    Log.d("Card", "에러 발생: " + e)
                }

                try {
                    getVCatInterface()!!.executeService(
                        jsonInput.toString(),
                        object : SmartroVCatCallback.Stub() {
                            override fun onServiceEvent(strEventJSON: String?) {} // 서비스가 실행되었을 때떄발생할 서비스

                            override fun onServiceResult(strResultJSON: String?) {
                                try {
                                    val jsonResult = JSONObject(strResultJSON)

                                    val iResult: Int =
                                        Integer.valueOf(jsonResult.getString("service-result"))

                                    if (iResult == 0) {
                                        Log.d("Card", "권한부여성공  $jsonResult")
                                    } else {
                                        Log.d("Card", "오류" + jsonResult["service-result"])
                                    }

                                } catch (e: Exception) {
                                    Log.d("Card", "error at jsonResult :  " + e)
                                }
                            }
                        })
                } catch (e: Exception) {
                    Log.d("Card", "error at Service Interface : " + e)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d("Card", "서비스 연결이 해제 되었습니다.")
                mSmartroVCatInterface = null
            }
        }
        bindService(intentTemp, cardServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun getVCatInterface(): SmartroVCatInterface? {
        return mSmartroVCatInterface
    }


}

