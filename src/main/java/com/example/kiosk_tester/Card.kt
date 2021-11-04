package com.example.kiosk_tester

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import service.vcat.smartro.com.vcat.SmartroVCatCallback
import service.vcat.smartro.com.vcat.SmartroVCatInterface


object Card : AppCompatActivity() {
    var mAppDate: String? = null    // Payment Data
    var mApprovalNo: String? = null     // Payment Number

    private var testResult = ""
    private var payResult = ""
    private var cancelResult = ""
    private var refundResult = ""
    private var cardKeyResult = ""


    fun cardStatusCheck() {
        val jsonInput = JSONObject()
        jsonInput.put("service", "indicate")
        jsonInput.put("available", "com")

        try {
            getVCatInterface()!!.executeService(jsonInput.toString(),
                object : SmartroVCatCallback.Stub() {
                    override fun onServiceEvent(strEventJSON: String?) {} // 서비스가 실행되었을 때떄발생할 서비스
                    override fun onServiceResult(strResultJSON: String) {
                        try {
                            val iResult: Int
                            val jsonResult = JSONObject(strResultJSON)
                            iResult = Integer.valueOf(jsonResult.getString("service-result"))
                            testResult = if (iResult == 0) "Success"
                            else "Fail"
                        } catch (e: Exception) {
                            Log.d("Card Check", "오류 발생 :  $e")
                        }
                    }
                })
        } catch (e: Exception) {
            Log.d("Card Check", "error at Service Interface : $e")
        }
    }

    fun cardExchangingKey() {
        val jsonInput = JSONObject()
        jsonInput.put("service", "function")
        jsonInput.put("cat-id", 1112887042) //단말기 번호
        jsonInput.put("business-no", 2178114493) //사업자 번호
        jsonInput.put("device-manage", "exchange-key")

        try {
            getVCatInterface()!!.executeService(jsonInput.toString(),
                object : SmartroVCatCallback.Stub() {
                    override fun onServiceEvent(strEventJSON: String?) {} // 서비스가 실행되었을 때떄발생할 서비스
                    override fun onServiceResult(strResultJSON: String) {
                        try {
                            val iResult: Int
                            val jsonResult = JSONObject(strResultJSON)
                            iResult = Integer.valueOf(jsonResult.getString("service-result"))
                            cardKeyResult = if (iResult == 0) "Success"
                            else "Fail"
                        } catch (e: Exception) {
                        }
                    }
                })
        } catch (e: Exception) {
            Log.d("Card Key", "error at Service Interface : $e")
        }
    }


    fun payApproval() {
        val jsonInput = JSONObject()
        jsonInput.put("total-amount", 10)      //임의로 10원 설정
        jsonInput.put("type", "credit")
        jsonInput.put("deal", "Approval")
        jsonInput.put("surtax", "0")
        jsonInput.put("tip", "0")
        jsonInput.put("cat-id", "1112887042")
        jsonInput.put("business-no", "2178114493")

        try {
            getVCatInterface()!!.executeService(jsonInput.toString(),
                object : SmartroVCatCallback.Stub() {
                    override fun onServiceEvent(strEventJSON: String?) {
                        //Log.d("Card Pay", "Pay")
                    }

                    override fun onServiceResult(strResultJSON: String) {
                        try {
                            val iResult: Int
                            val jsonResult = JSONObject(strResultJSON)
                            iResult = Integer.valueOf(jsonResult.getString("service-result"))
                            if (iResult == 0) {
                                if (jsonResult.getString("response-code") == "00") {
                                    payResult = "Success"
                                    // Log.d("Card Pay", "결제성공")
                                    setApprovalInformation(
                                        jsonResult.getString("approval-date"),
                                        jsonResult.getString("approval-no")
                                    )
                                } else {
                                    payResult = "Fail"
                                    // Log.d("Card Pay", "거래거절")
                                }
                            } else {
                                payResult = "Fail"
                                /*
                                if (jsonResult.has("service-description"))
                                    Log.d("Card Pay", "오류사유 " + jsonResult["service-description"] + "(" + iResult + ")")
                                else
                                    Log.d("Card Pay", "오류 " + jsonResult["service-result"])
                                */

                            }
                        } catch (e: Exception) {
                            payResult = "Fail"
                            //Log.d("Card Pay", "error  :  " + e)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun payCancel() {
        cancelResult = try {
            getVCatInterface()!!.cancelService()
            "Success"
        } catch (e: Exception) {
            e.printStackTrace()
            "Fail"
        }
    }


    fun payRefund() {
        val jsonInput = JSONObject()
        jsonInput.put("type", "credit")
        jsonInput.put("deal", "Cancellation")
        jsonInput.put("surtax", "0")
        jsonInput.put("tip", "0")
        jsonInput.put("total-amount", 10)
        jsonInput.put("cat-id", 1112887042)
        jsonInput.put("business-no", 2178114493)
        jsonInput.put("approval-date", mAppDate)
        jsonInput.put("approval-no", mApprovalNo)

        try {
            getVCatInterface()!!.executeService(jsonInput.toString(),
                object : SmartroVCatCallback.Stub() {
                    override fun onServiceEvent(strEventJSON: String?) {
                        // Log.d("Pay Refund", "결제 취소 이벤트")
                    }

                    override fun onServiceResult(strResultJSON: String) {
                        try {
                            val iResult: Int
                            val jsonResult = JSONObject(strResultJSON)
                            iResult = Integer.valueOf(jsonResult.getString("service-result"))
                            refundResult = if (iResult == 0) {
                                if (jsonResult.getString("response-code") == "00") {
                                    "Success"
                                    // Log.d("Pay Refund", "결제 취소 성공")
                                } else {
                                    "Fail"
                                    // Log.d("Pay Refund", "거래 거절")
                                    // Log.d("Pay Refund", jsonResult.getString("display-msg"))
                                }
                            } else {
                                "Fail"
                                /*if (jsonResult.has("service-description"))
                                                            Log.d("Pay Refund", "오류 사유 : ${jsonResult["service-description"]}"
                                                        */
                            }
                        } catch (e: Exception) {
                            refundResult = "Fail"
                            e.printStackTrace()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            refundResult = "Fail"
            e.printStackTrace()
        }
    }

    private fun setApprovalInformation(strAppDate: String, strAppNo: String) {
        mAppDate = strAppDate
        mApprovalNo = strAppNo
    }

    fun getResult(num: Int): String {
        return when (num) {
            1 -> {
                payResult
            }
            2 -> {
                cancelResult
            }
            3 -> {
                testResult
            }
            4 -> {
                cardKeyResult
            }
            else -> refundResult
        }
    }

    fun resetResult(num: Int) {
        when (num) {
            1 -> {
                payResult = ""
            }
            2 -> {
                cancelResult = ""
            }
            3 -> {
                testResult = ""
            }
            4 -> {
                cardKeyResult = ""
            }
            else -> {
                refundResult = ""
            }
        }
    }


    private fun getVCatInterface(): SmartroVCatInterface? {
        return mSmartroVCatInterface
    }
}