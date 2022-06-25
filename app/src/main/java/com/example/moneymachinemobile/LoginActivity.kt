package com.example.moneymachinemobile

import MySharedPreferences
import android.content.Intent
import android.os.Bundle
import android.text.BoringLayout
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moneymachinemobile.data.MMID
import com.example.moneymachinemobile.data.MMPW
import com.example.moneymachinemobile.data.UserModel
import com.example.moneymachinemobile.data.user
import com.example.moneymachinemobile.ui.agreeDialog
import com.example.moneymachinemobile.ui.findIDPWDialog
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)

        val autoLogin = findViewById<CheckBox>(R.id.autoLoginCheckBox)
        autoLogin.setOnCheckedChangeListener{ buttonView, isChecked->
            if(isChecked)
            {
                autoLogin.isChecked = true
                MySharedPreferences.setUserAutoLogin(this, true)
            }
            else
            {
                autoLogin.isChecked = false
                MySharedPreferences.setUserAutoLogin(this, false)
            }
        }

        if(MySharedPreferences.getUserId(this).isNotBlank() &&
                MySharedPreferences.getUserPW(this).isNotBlank() &&
                MySharedPreferences.getUserAutoLogin(this))
        {
//            Toast.makeText(this, "자동 로그인 중입니다.", Toast.LENGTH_LONG).show()
            MMID = MySharedPreferences.getUserId(this)
            MMPW = MySharedPreferences.getUserPW(this)
            autoLogin.isChecked = true
            username.setText(MMID)
            password.setText(MMPW)
            LoginCheck()
        }

        val loginbutton : Button = findViewById(R.id.ButtonLogin)
        loginbutton.setOnClickListener{
            MMID = username.text.toString()
            MMPW = password.text.toString()
            if(username.text.isEmpty())
            {
                Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_LONG).show()
            }

            if(password.text.isEmpty())
            {
                Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_LONG).show()
            }

            if(username.text.isNotEmpty() && password.text.isNotEmpty())
            {
                if(autoLogin.isChecked)
                {
                    MySharedPreferences.setUserAutoLogin(this, true)
                    MySharedPreferences.setUserId(this, username.text.toString())
                    MySharedPreferences.setUserPW(this, password.text.toString())
                }
                LoginCheck()
            }
        }

        val signupbutton : Button = findViewById(R.id.ButtonSignUp)
        signupbutton.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val findIDPWbutton : Button = findViewById(R.id.ButtonFindIDPW)
        findIDPWbutton.setOnClickListener{
            val findidpwDlg = findIDPWDialog(this)
            findidpwDlg.start()
        }
    }

    fun Login()
    {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        try {
            //서버연동
            val callUrl: String =
                    "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/user?idx=0&id=$MMID&pw=$MMPW"

            val url : URL = URL(callUrl)
            val conn  = url.openConnection() as HttpURLConnection

            if (conn.responseCode == HttpURLConnection.HTTP_OK)
            {
                val streamReader = InputStreamReader(conn.inputStream, Charset.forName("UTF-8"))
                val buffered = BufferedReader(streamReader)

                while (true)
                {
                    val line = buffered.readLine()?: break

                    if(line.length < 3)
                    {
                        Toast.makeText(this, "아이디와 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                        break
                    }
                    val jsonObject = JSONArray(line).getJSONObject(0)

                    user = UserModel(
                            jsonObject.getString("idx").toInt(),
                            jsonObject.getString("id"),
                            jsonObject.getString("pw"),
                            jsonObject.getString("name"),
                            jsonObject.getString("email"),
                            jsonObject.getString("phone"),
                            jsonObject.getString("address"),
                            LocalDateTime.parse(jsonObject.getString("signupdate"),formatter),
                            LocalDateTime.parse(jsonObject.getString("paiddate"),formatter),
                            jsonObject.getString("expertidx").toInt(),
                            jsonObject.getString("profit").toInt(),
                            jsonObject.getString("totalestimatedassets").toInt(),
                            jsonObject.getString("access").toBoolean(),
                            jsonObject.getString("downpayment").toInt(),
                            jsonObject.getString("companyidx").toInt(),
                            jsonObject.getString("expertname"),
                    )
                }

                buffered.close()
                conn.disconnect()
            }

            if(!user.access)
            {
                Toast.makeText(this, "가입승인이 나지 않았습니다.", Toast.LENGTH_SHORT).show()
                return
            }

            val period = Period.between(user.paiddate?.toLocalDate(), LocalDate.now())

            if(period.isZero)
            {
                Toast.makeText(this, "사용기간이 만료된 계정 입니다.", Toast.LENGTH_SHORT).show()
                return
            }

            serverLoginDone = true;
        }
        catch (ex : Exception)
        {
        }
    }

    var serverLoginDone : Boolean = false
    private fun LoginCheck()
    {
        thread(start = true){
            Login() //쓰레드 안하면 에러
        }

        var waitCount = 0;
        while (true)
        {
            if(serverLoginDone)
            {
                break;
            }
            else
            {
                ++waitCount

                Thread.sleep(100)

                if(waitCount == 10)
                {
                    break;
                }
            }
        }

        //거래동의 안내팝업
        if(serverLoginDone)
        {
            var agree = MySharedPreferences.getUserAgreement(this)

            if(!agree)
            {
                val agreeDlg = agreeDialog(this)
                agreeDlg.setOnOKClickedListener{ content ->
                    agree = content
                    MySharedPreferences.setUserAgreement(this, agree)
                    if(agree)
                    {
                        startActivity(Intent(this, LoginStockActivity::class.java))
                    }
                }
                agreeDlg.start()
            }
            else
            {
                startActivity(Intent(this, LoginStockActivity::class.java))
            }
        }
        else
        {
            Toast.makeText(this, "서버접속 에러", Toast.LENGTH_SHORT).show()
            return
        }
    }
}