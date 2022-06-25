package com.example.moneymachinemobile.ui

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.example.moneymachinemobile.R

class autobuyDialog(context: Context) {

    private val dlg = Dialog(context)
    private lateinit var listener : MyDialogOKClickedListener

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_autobuy)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.setCancelable(false)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        val btnOK : Button = dlg.findViewById(R.id.buttonAutoBuyOK)
        btnOK.setOnClickListener {
            Toast.makeText(dlg.context,"확인을 눌렀습니다", Toast.LENGTH_LONG).show()
            listener.onOKClicked(true)
            dlg.dismiss()
        }

        val btnNG : Button = dlg.findViewById(R.id.buttonAutoBuyNG)
        btnNG.setOnClickListener {
            Toast.makeText(dlg.context,"취소를 눌렀습니다", Toast.LENGTH_LONG).show()
            listener.onOKClicked(false)
            dlg.dismiss()
        }
        dlg.show()
    }

    fun setOnOKClickedListener(listener: (Boolean) -> Unit) {
        this.listener = object: MyDialogOKClickedListener {
            override fun onOKClicked(agree: Boolean) {
                listener(agree)
            }
        }
    }

    interface MyDialogOKClickedListener {
        fun onOKClicked(agree : Boolean)
    }
}