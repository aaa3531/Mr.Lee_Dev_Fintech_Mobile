package com.example.moneymachinemobile.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.view.Window
import android.widget.Button
import android.widget.CalendarView
import com.example.moneymachinemobile.R
import java.util.*

class calendarDialog(context: Context)  {

    private val dlg = Dialog(context)
    private lateinit var listener : MyDialogOKClickedListener2

    @SuppressLint("SimpleDateFormat")
    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_calendar)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.setCancelable(false)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        val calendar =dlg.findViewById<CalendarView>(R.id.calendarView)
        var m_year : String = ""
        var m_month : String = ""
        var m_day : String = ""

        calendar?.setOnDateChangeListener { view, year, month, dayOfMonth ->
            m_year = year.toString()

            m_month = (month+1).toString()
            if(m_month.length == 1)
            {
                m_month = "0$m_month"
            }

            m_day = dayOfMonth.toString()
            if(m_day.length == 1)
            {
                m_day = "0$m_day"
            }
            }

        val btnOK : Button = dlg.findViewById(R.id.buttonDateOK)
        btnOK.setOnClickListener {
            if(m_year.isEmpty())
            {
                val now = System.currentTimeMillis()
                val date = Date(now)
                val simpleYear = SimpleDateFormat("yyyy")
                val simpleMonth = SimpleDateFormat("MM")
                val simpleDay = SimpleDateFormat("dd")
                m_year = simpleYear.format(date)
                m_month = simpleMonth.format(date)
                m_day = simpleDay.format(date)

//                val cal :Calendar = Calendar.getInstance()
//                m_year = cal.time.toString()
            }
            listener.onOKClicked(m_year, m_month, m_day)
            dlg.dismiss()
        }
        dlg.show()
    }

    fun setOnOKClickedListener(listener: (String, String, String) -> Unit) {
        this.listener = object: MyDialogOKClickedListener2 {
            override fun onOKClicked(year: String, month: String, day: String) {
                listener(year, month, day)
            }
        }
    }

    interface MyDialogOKClickedListener2 {
        fun onOKClicked(year: String, month: String, day: String)
    }
}