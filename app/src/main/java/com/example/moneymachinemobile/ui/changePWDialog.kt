package com.example.moneymachinemobile.ui

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.user
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class changePWDialog(context: Context) {

    private val dlg = Dialog(context)
    private lateinit var listener : MyDialogOKClickedListener

    lateinit var etCurPW : EditText
    lateinit var etNewPW : EditText
    lateinit var etNewPWCheck : EditText

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_change_pw)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.setCancelable(false)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        etCurPW = dlg.findViewById(R.id.editTextCurPW)
        etNewPW = dlg.findViewById(R.id.editTextNewPW)
        etNewPWCheck = dlg.findViewById(R.id.editTextNewPWCheck)

        val btnOK : Button = dlg.findViewById(R.id.buttonChangeOK)
        btnOK.setOnClickListener {
            ChangePW()
        }

        val btnNG : Button = dlg.findViewById(R.id.buttonChangeNG)
        btnNG.setOnClickListener {
            Toast.makeText(dlg.context, "취소되었습니다.", Toast.LENGTH_LONG).show()
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
        fun onOKClicked(agree: Boolean)
    }

    private fun ChangePW()
    {
        if (etCurPW.text.isEmpty())
        {
            Toast.makeText(dlg.context, "아이디를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etNewPW.text.isEmpty())
        {
            Toast.makeText(dlg.context, "비밀번호를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etNewPWCheck.text.isEmpty())
        {
            Toast.makeText(dlg.context, "이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        if(etCurPW.text.toString() != user.pw)
        {
            Toast.makeText(dlg.context, "기존 비밀번호를 확인해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if(etNewPW.text.toString() != etNewPWCheck.text.toString())
        {
            Toast.makeText(dlg.context, "새로운 비밀번호를 확인해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        val newPW = etNewPW.text.toString()

        val jsonObject = JSONObject()
        jsonObject.put("idx",user.idx)
        jsonObject.put("id", user.id )
        jsonObject.put("pw", newPW)
        jsonObject.put("name", user.name)
        jsonObject.put("email", user.email)
        jsonObject.put("phone", user.phone)
        jsonObject.put("address", user.address)
        val signupdate = user.signupdate.toString()+":00"
        jsonObject.put("signupdate", signupdate)
        val paiddate = user.paiddate.toString()+":00"
        jsonObject.put("paiddate", paiddate)
        jsonObject.put("expertidx", user.expertidx)
        jsonObject.put("profit", user.profit)
        jsonObject.put("totalestimatedassets", user.totalestimatedassets)
        jsonObject.put("access", user.access)
        jsonObject.put("downpayment", user.downpayment)
        jsonObject.put("companyidx", user.companyidx)
        jsonObject.put("expertname", user.expertname)
        val json = jsonObject.toString()

        var changeDone = false
        var changeWait = false

        thread(start = true){

            val callUrl = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/user"
            val url = URL(callUrl)
            val conn  = url.openConnection() as HttpURLConnection

            conn.requestMethod = "PUT"
            conn.setRequestProperty("USER-AGENT", "Mozilla/5.0")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.doOutput = true // POST 로 데이터를 넘겨주겠다는 옵션
            conn.doInput = true

            val wr = OutputStreamWriter(conn.outputStream)
            wr.write(json)
            wr.close()

            if(conn.responseCode == HttpURLConnection.HTTP_OK)
            {
                val responseText: String = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                conn.disconnect()

                if(responseText.contains("false"))
                {
                    changeDone = false
                }
                else
                {
                    user.pw = newPW
                    changeDone = true
                }
            }
            else
            {
                conn.disconnect()
                changeDone = false
            }

            changeWait = true
        }

        while (true)
        {
            if(changeWait)
            {
                if(changeDone)
                {
                    Toast.makeText(dlg.context,"비밀번호가 변경되었습니다.", Toast.LENGTH_LONG).show()
                    break
                }
                else
                {
                    Toast.makeText(dlg.context,"비밀번호 변경에 실패하였습니다.", Toast.LENGTH_LONG).show()
                    break
                }
            }
        }

        dlg.dismiss()
    }
}