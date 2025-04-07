package com.example.currency

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.example.currency.RateCheckService

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: MainViewModel
    lateinit var textRate: TextView
    lateinit var rootView: View
    lateinit var isLaunchTextView: TextView
    lateinit var btnSubscribeToRate: Button

    var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViewModel()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    fun initViewModel() {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.usdRate.observe(this, {
            textRate.text = "$it"
        })

        viewModel.onCreate()
    }

    fun initView() {
        textRate = findViewById(R.id.textUsdRubRate)
        rootView = findViewById(R.id.rootView)
        isLaunchTextView = findViewById(R.id.is_launch)
        btnSubscribeToRate = findViewById(R.id.btnSubscribeToRate)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            viewModel.onRefreshClicked()
        }

        btnSubscribeToRate.setOnClickListener {
            if (isServiceRunning) {
                RateCheckService.stopService(this)
                isLaunchTextView.text = getString(R.string.service_off)
                isServiceRunning = false
            } else {
                RateCheckService.startService(this)
                isLaunchTextView.text = getString(R.string.service_on)
                isServiceRunning = true
            }
        }
    }
}