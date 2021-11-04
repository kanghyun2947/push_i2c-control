package com.example.kiosk_tester.ui.pcb

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
import com.example.kiosk_tester.R
import com.example.kiosk_tester.PCB
import com.example.kiosk_tester.ui.motor.MotorViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PCBFragment : Fragment() {

    private lateinit var pcbViewModel: PCBViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        pcbViewModel = ViewModelProvider(this).get(PCBViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_pcb, container, false)
        val textView: TextView = root.findViewById(R.id.version)
        pcbViewModel.testerVersion.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        val pcbTestResult:TextView = root.findViewById(R.id.pcb_test_result)
        val passTestResult:TextView = root.findViewById(R.id.pcb_pass_test_result)
        val doorTestResult:TextView = root.findViewById(R.id.pcb_door_test_result)
        val ledTestResult:TextView = root.findViewById(R.id.pcb_led_test_result)
        val motorResetResult:TextView = root.findViewById(R.id.pcb_motor_reset_result)
        pcbViewModel.door.observe(viewLifecycleOwner, Observer {
            doorTestResult.text = it
        })
        val pcbTestBtn:Button = root.findViewById(R.id.pcb_test)
        pcbTestBtn.setOnClickListener { pcbViewModel.pcbCheck(pcbTestResult) }

        val passTestBtn:Button = root.findViewById(R.id.pcb_pass_test)
        passTestBtn.setOnClickListener { pcbViewModel.passCheck(passTestResult) }

        val doorTestBtn:Button = root.findViewById(R.id.pcb_door_test)
        doorTestBtn.setOnClickListener { pcbViewModel.doorCheck(doorTestResult) }

        val ledTestBtn:Button = root.findViewById(R.id.pcb_led_test)
        ledTestBtn.setOnClickListener { pcbViewModel.ledCheck(ledTestResult) }

        val motorResetBtn:Button = root.findViewById(R.id.pcb_motor_reset)
        motorResetBtn.setOnClickListener { pcbViewModel.motorReset(motorResetResult) }
        activity?.runOnUiThread{
            pcbViewModel.firstUI(pcbTestResult,passTestResult, doorTestResult)
        }
        return root
    }
}