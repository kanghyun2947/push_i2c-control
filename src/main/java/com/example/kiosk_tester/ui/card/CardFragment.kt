package com.example.kiosk_tester.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.kiosk_tester.R

class CardFragment : Fragment() {

    private lateinit var cardViewModel: CardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        cardViewModel =
            ViewModelProvider(this).get(CardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_card, container, false)


        val cardTestResultC: TextView = root.findViewById(R.id.card_check_result)
        val payResultC: TextView = root.findViewById(R.id.card_pay_result)
        val payCancelResultC: TextView = root.findViewById(R.id.card_cancel_result)
        val payRefundResultC: TextView = root.findViewById(R.id.card_refund_result)
        val cardExchangeKeyResultC: TextView = root.findViewById(R.id.card_exchanging_key_result)

        cardViewModel.cardTestResult.observe(viewLifecycleOwner, Observer {
            cardTestResultC.text = it
        })
        cardViewModel.payResult.observe(viewLifecycleOwner, Observer {
          payResultC.text = it
        })
        cardViewModel.payCancelResult.observe(viewLifecycleOwner, Observer {
            payCancelResultC.text = it
        })
        cardViewModel.cardKeyResult.observe(viewLifecycleOwner, Observer {
            cardExchangeKeyResultC.text = it
        })

        val cardCheckBtn: Button = root.findViewById(R.id.card_check)
        cardCheckBtn.setOnClickListener { cardViewModel.cardTest(cardTestResultC)}
        val cardPayBtn: Button = root.findViewById(R.id.card_pay)
        cardPayBtn.setOnClickListener { cardViewModel.payApproval(payResultC) }
        val cardPayCancelBtn: Button = root.findViewById(R.id.card_cancel)
        cardPayCancelBtn.setOnClickListener { cardViewModel.payCancel(payCancelResultC) }
        val cardPayRefundBtn: Button = root.findViewById(R.id.card_refund)
        cardPayRefundBtn.setOnClickListener { cardViewModel.payRefund(payRefundResultC) }
        val cardExchangeKeyBtn: Button = root.findViewById(R.id.card_exchanging_key)
        cardExchangeKeyBtn.setOnClickListener { cardViewModel.cardkey(cardExchangeKeyResultC)}

        return root
    }
}