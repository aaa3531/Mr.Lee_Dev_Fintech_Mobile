package com.example.moneymachinemobile

import MySharedPreferences
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ebest.api.MsgPacket
import com.ebest.api.SocketManager
import com.ebest.api.dialog.Listener
import com.ebest.api.dialog.importSignDialog
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.data.*
import java.util.*
import kotlin.system.exitProcess

class LoginStockActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback, Listener{

    lateinit var certSpinner : Spinner
    private var m_popup :importSignDialog? = null
    private var handler: ProcMessageHandler? = null
    lateinit internal var manager: SocketManager
    private var m_nHandle = -1

    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>, grandResults: IntArray) {
        // 거부한 경우 앱을 종료한다
        for (element in grandResults) {
            if (element == -1) {
                finishAffinity()                       // 해당앱의 루트 액티비티를 종료시킨다.
                System.runFinalization()               // 현재 작업중인 쓰레드가 종료되면 종료 시키라는 명령어
                exitProcess(0)                 // 현재 액티비티를 종료시킨다.
            }
        }
    }

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler(private val activity: LoginStockActivity) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {

//                 TIMEOUT 에러
                API_DEFINE.RECEIVE_TIMEOUTERROR -> {
                    //m_customAnimationDialog!!.dismiss()
                    val strMsg = msg.obj as String
//                    popupNotAble(strMsg)
                }

                // INITECH 핸드세이킹 에러
                API_DEFINE.RECEIVE_INITECHERROR -> {
                    //m_customAnimationDialog!!.dismiss()
                    val strMsg = msg.obj as String
                    Toast.makeText(applicationContext, strMsg, Toast.LENGTH_SHORT).show()
                }


                // 일반적인 에러
                API_DEFINE.RECEIVE_ERROR -> {
                    //m_customAnimationDialog!!.dismiss()
                    val strMsg = msg.obj as String
                    Toast.makeText(applicationContext, strMsg, Toast.LENGTH_SHORT).show()

                }

                // SOCEKT이 연결이 끊어졌다.
                API_DEFINE.RECEIVE_DISCONNECT -> {
                    //val strMsg = msg.obj as String
                    //Toast.makeText(applicationContext, strMsg, Toast.LENGTH_SHORT).show()
                }

                // 서버에서 보내는 시스템 ERROR
                API_DEFINE.RECEIVE_SYSTEMERROR -> {
                    //m_customAnimationDialog!!.dismiss()
                    val pMsg = msg.obj as MsgPacket
                    Toast.makeText(applicationContext, pMsg.strMessageData, Toast.LENGTH_SHORT)
                        .show()
                }

                // SOCKET연결이 실패했다.
                API_DEFINE.RECEIVE_CONNECTERROR -> {
                }

                // SOCKET연결이 성공했다.
                API_DEFINE.RECEIVE_CONNECT -> {

                }

                // TR메세지
                API_DEFINE.RECEIVE_MSG -> {
                    val lpMp = msg.obj as MsgPacket
                    Toast.makeText(
                        applicationContext,
                        lpMp.strTRCode + " " + lpMp.strMsgCode + lpMp.strMessageData,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // LOGIN이 완료됐다.
                API_DEFINE.RECEIVE_LOGINCOMPLETE -> {
                    //m_customAnimationDialog!!.dismiss()
                    //Toast.makeText(applicationContext, msg.obj.toString(), Toast.LENGTH_SHORT).show()

                    //activity.sendActivity(msg)

                    setResult(RESULT_OK)
                    activity.finish()
                    Login()
                }

                // 선택한 공인인증서 정보
                /*
                RECEIVE_SIGN -> {
                    val lpSign = msg.obj as SignPacket
                    if (lpSign.strSubjectName == "") return
                    m_textViewDN.setText(lpSign.strSubjectName)
                    //m_strPolicy = lpSign.strPolicy
                    //m_strIssuerCn = lpSign.strIssuerCn
                    //m_strExpiredTime = lpSign.strExpiredTime
                    //m_strSerialNumberInt = lpSign.strSerialNumberInt
                    manager.setDataString("dn", lpSign.strSubjectName)
                }
                */


                else -> {
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                //toolbar의 back키 눌렀을 때 동작
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login_stock)

        handler = ProcMessageHandler(this)
        manager = (application as ApplicationManager).getSockInstance()
        manager.setRes(this, "res")
        manager.checkPermission(this, handler as Handler)
        m_nHandle = manager.setHandler(handler as Handler)

        val ab = supportActionBar!!
        ab.title = "증권사 연동설정"
        ab.setDisplayShowTitleEnabled(true)
        ab.setDisplayHomeAsUpEnabled(true)

//        val stockID = findViewById<EditText>(R.id.stockID)
//        val stockPW = findViewById<EditText>(R.id.stockPW)
        val stockCertPW = findViewById<EditText>(R.id.stockCertPW)
        val accountPW = findViewById<EditText>(R.id.accountPW)

        val getCertButton = findViewById<Button>(R.id.ButtonGetCert)
        getCertButton.setOnClickListener{
            if (m_popup == null) {

                m_popup = importSignDialog(this)
                m_popup!!.setListener(this)
                m_popup!!.setCancelable(false)
                m_popup!!.show()
            }
        }

        certSpinner = findViewById(R.id.SpinnerCertList)

        ebestCert = MySharedPreferences.getUserStockCert(this)

        val items = checkSign()
        certSpinner.adapter = ArrayAdapter(this, R.layout.spinneritem, items)

        certSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
//                val t = (parent.getChildAt(0) as TextView)
//                t.setTextSize(
//                    TypedValue.COMPLEX_UNIT_PX,
//                    ResourceManager.calcFontSize(t.textSize.toInt())
//                )
                ebestCert = certSpinner.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        if(MySharedPreferences.getUserStockCertPW(this).isNotBlank() &&
                MySharedPreferences.getUserAccountPW(this).isNotBlank() &&
                MySharedPreferences.getUserAutoStockLogin(this))
        {
            ebestCertPW = MySharedPreferences.getUserStockCertPW(this)
            AccountPW = MySharedPreferences.getUserAccountPW(this)

            stockCertPW.setText(ebestCertPW)
            accountPW.setText(AccountPW)

            StockCheck()
        }

        val stockLoginButton = findViewById<Button>(R.id.stockButton)
        stockLoginButton.setOnClickListener{

            ebestCertPW = stockCertPW.text.toString()
            AccountPW = accountPW.text.toString()

            if(stockCertPW.text.isNotEmpty() && accountPW.text.isNotEmpty())
            {
                MySharedPreferences.setUserStockCertPW(this, ebestCertPW)
                MySharedPreferences.setUserAccountPW(this, AccountPW)
            }

            MySharedPreferences.setUserAutoStockLogin(this, true)
            StockCheck()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        manager.deleteHandler(m_nHandle)
    }

    private fun checkSign() : ArrayList<String> {
        val items = ArrayList<String>()

        val temp =  manager.getSignList(this)

        val nCount = temp.size
        for (i in 0 until nCount) {

            val strSubjectName = temp[i].strSubjectName       // DN
//            val strPolicy =  temp.get(i).strPolicy;                // 법용OID
//            val strIssuerCn = temp.get(i).strIssuerCn;            // 발급기관
//            val strExpiredTime = temp.get(i).strExpiredTime;         // 만료일
//            val strSerialNumberInt =  temp.get(i).strSerialNumberInt;      // serial num
//            val strPolicyNumString = temp.get(i).strPolicyNumString;      //

            items.add(strSubjectName)
        }
        return items
    }

    private fun StockCheck()
    {
//       gasangLogin()

        if (ebestCertPW.isEmpty()) {
            Toast.makeText(this, "공인인증 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ebestCert.isEmpty() || ebestCert == "공인인증서를 선택하세요.") {
            Toast.makeText(this, "공인인증서를 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        MySharedPreferences.setUserStockCert(this, ebestCert)

        if (manager.isConnect()) {
            manager.disconnect()
        }

        var nReturn = manager.connect(m_nHandle, 1)
        if (nReturn == 0) {
            nReturn = manager.loginSign(m_nHandle, ebestCertPW, ebestCert)
            Toast.makeText(applicationContext, "증권사 로그인 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun triggerEvent(strEventName: String, strParam: String) {
        if (strEventName == "cancel") {
            m_popup!!.dismiss()
            m_popup = null

        } else if (strEventName == "ok") {
            m_popup!!.dismiss()
            m_popup = null
            //setControl()
            val items = checkSign()
            certSpinner.adapter = ArrayAdapter(this, R.layout.spinneritem, items)
        }
    }

    fun Login()
    {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun gasangLogin()
    {
        val id = "oih7947"
        val password = "inhoo12"

        if (id.isEmpty()) {
            Toast.makeText(this, "ID를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "접속비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (manager.isConnect()) {
            manager.disconnect()
        }

        var nServerGubun = 2
        val iseBestApp = manager.iseBestApp()
        if (iseBestApp)
            nServerGubun = 1


        var nReturn = manager.connect(m_nHandle, nServerGubun)
        if (nReturn == 0) {
            //m_customAnimationDialog!!.show()
            //m_customAnimationDialog!!.setTextMsg("로그인 중입니다. 잠시만 기다리세요.")
            nReturn = manager.loginID(m_nHandle, id, password)
        }

        AccountPW = "0000"
    }
}