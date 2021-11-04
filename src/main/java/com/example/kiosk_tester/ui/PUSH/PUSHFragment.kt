package com.example.kiosk_tester.ui.PUSH

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.kiosk_tester.PUSH.findViewById
import com.example.kiosk_tester.PUSH.pushReset
import com.example.kiosk_tester.R
import com.example.kiosk_tester.ui.motor.MotorViewModel

class PUSHFragment : Fragment() {

    private lateinit var pushViewModel: PUSHViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        pushViewModel = ViewModelProvider(this).get(PUSHViewModel::class.java)

        val root = inflater.inflate(R.layout.push, container, false)

        val P1: Button = root.findViewById(R.id.product1_btn)
        val P2: Button = root.findViewById(R.id.product2_btn)
        val P3: Button = root.findViewById(R.id.product3_btn)
        val P4: Button = root.findViewById(R.id.product4_btn)
        val P5: Button = root.findViewById(R.id.product5_btn)
        val P6: Button = root.findViewById(R.id.product6_btn)

        val pushReset_btn: Button = root.findViewById(R.id.pushReset_btn)
        val pushStart_btn: Button = root.findViewById(R.id.pushStart_btn)
        val pushCount_btn: Button = root.findViewById(R.id.count_btn)
        val productCheck_btn: Button = root.findViewById(R.id.productCheck_btn)

        pushReset_btn.setOnClickListener { pushViewModel.pushReset(P1,P2,P3,P4,P5,P6,pushCount_btn,pushStart_btn) }
        pushStart_btn.setOnClickListener { pushViewModel.pushStart(P1,P2,P3,P4,P5,P6,pushCount_btn,pushStart_btn) }
        pushCount_btn.setOnClickListener { pushViewModel.countNum(pushCount_btn) }
        productCheck_btn.setOnClickListener { pushViewModel.productDetection(P1,P2,P3,P4,P5,P6) }

        P1.setOnClickListener { pushViewModel.clickButton(P1, 1) }
        P2.setOnClickListener { pushViewModel.clickButton(P2, 2) }
        P3.setOnClickListener { pushViewModel.clickButton(P3, 3) }
        P4.setOnClickListener { pushViewModel.clickButton(P4, 4) }
        P5.setOnClickListener { pushViewModel.clickButton(P5, 5) }
        P6.setOnClickListener { pushViewModel.clickButton(P6, 6) }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        pushViewModel = ViewModelProvider(this).get(PUSHViewModel::class.java)
        // TODO: Use the ViewModel
    }

}