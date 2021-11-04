package com.example.kiosk_tester.ui.motor

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.kiosk_tester.*

class MotorFragment : Fragment() {

    private lateinit var motorViewModel: MotorViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        motorViewModel = ViewModelProvider(this).get(MotorViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_motor, container, false)
        val execute_c: TextView = root.findViewById(R.id.execute_cnt)
        val success_c: TextView = root.findViewById(R.id.success_cnt)
        val fail_c: TextView = root.findViewById(R.id.fail_cnt)
        val error_text: TextView = root.findViewById(R.id.error_text)

        motorViewModel.execute.observe(viewLifecycleOwner, Observer {
            execute_c.text = it.toString()
        })
        motorViewModel.success.observe(viewLifecycleOwner, Observer {
            success_c.text = it.toString()
        })
        motorViewModel.fail.observe(viewLifecycleOwner, Observer {
            fail_c.text = it.toString()
        })
        val reset_btn: Button = root.findViewById(R.id.reset_btn)
        reset_btn.setOnClickListener { motorViewModel.reset(error_text) }
        val mode_btn: Button = root.findViewById(R.id.one_infinit_btn)
        mode_btn.setOnClickListener { motorViewModel.mode(mode_btn) }
        val start_btn: Button = root.findViewById(R.id.start_btn)
        start_btn.setOnClickListener { motorViewModel.startStop(start_btn) }

        activity?.runOnUiThread{
            when (passResult) {
                "PF" -> {
                    error_text.text = "투출 센서 이상"
                    error_text.visibility = TextView.VISIBLE
                }
                else -> {
                    error_text.visibility = TextView.INVISIBLE
                }
            }
        }
        val M1: Button = root.findViewById(R.id.M1)
        val M2: Button = root.findViewById(R.id.M2)
        val M3: Button = root.findViewById(R.id.M3)
        val M4: Button = root.findViewById(R.id.M4)
        val M5: Button = root.findViewById(R.id.M5)
        val M6: Button = root.findViewById(R.id.M6)

        M1.setOnClickListener { motorViewModel.motorSelect(M1,1) }
        M2.setOnClickListener { motorViewModel.motorSelect(M2,2) }
        M3.setOnClickListener { motorViewModel.motorSelect(M3,3) }
        M4.setOnClickListener { motorViewModel.motorSelect(M4,4) }
        M5.setOnClickListener { motorViewModel.motorSelect(M5,5) }
        M6.setOnClickListener { motorViewModel.motorSelect(M6,6) }
        return root
    }
}