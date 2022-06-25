package com.example.moneymachinemobile

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.moneymachinemobile.data.CompanyModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


class SignUpActivity : AppCompatActivity() {

    private lateinit var etID : EditText
    private lateinit var etPW : EditText
    private lateinit var etName : EditText
    private lateinit var etPhone : EditText
    private lateinit var etEmail : EditText
    private lateinit var etAddress : EditText
    private lateinit var spSubHeadquarters : Spinner
    private lateinit var strSubHeadquarters : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val ab = supportActionBar!!
        ab.title = "회원가입"
        ab.setDisplayShowTitleEnabled(true)
        ab.setDisplayHomeAsUpEnabled(true)

        etID = findViewById(R.id.editTextID)
        etPW = findViewById(R.id.editTextPW)
        etName = findViewById(R.id.editTextName)
        etPhone = findViewById(R.id.editTextPhone)
        etEmail = findViewById(R.id.editTextEmail)
        etAddress = findViewById(R.id.editTextAddress)
        spSubHeadquarters = findViewById(R.id.spinnerSubHeadquarters)

        thread(start = true){
            checkSubHeadQuarters() //쓰레드 안하면 에러
        }

        spSubHeadquarters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                strSubHeadquarters = spSubHeadquarters.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        val button = findViewById<Button>(R.id.ButtonSignUpDone)
        button.setOnClickListener{
            SignUp()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val CMList = ArrayList<CompanyModel>()

    private fun checkSubHeadQuarters()
    {
        val callUrl = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/company?idx=1"

        val url = URL(callUrl)
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
                    Toast.makeText(this, "오류.", Toast.LENGTH_SHORT).show()
                    break
                }

                val jsonArray = JSONArray(line)

                for(i in 0 until jsonArray.length())
                {
                    val jsonObject = JSONArray(line).getJSONObject(i)

                    val cm = CompanyModel(
                            jsonObject.getString("idx").toInt(),
                            jsonObject.getString("name"),
                            jsonObject.getString("id"),
                            jsonObject.getString("pw"),
                    )
                    CMList.add(cm)
                }
            }

            buffered.close()
            conn.disconnect()
        }

        val items = ArrayList<String>()
        for(i in 0 until CMList.count())
        {
            items.add(CMList[i].name)
        }

        runOnUiThread{spSubHeadquarters.adapter = ArrayAdapter(this, R.layout.spinneritem, items)}
    }

    private fun SignUp()
    {

        if (etID.text.isEmpty())
        {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etPW.text.isEmpty())
        {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etName.text.isEmpty())
        {
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etEmail.text.isEmpty())
        {
            Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etPhone.text.isEmpty())
        {
            Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (etAddress.text.isEmpty())
        {
            Toast.makeText(this, "주소를 입력해주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (strSubHeadquarters.isEmpty())
        {
            Toast.makeText(this, "부본사를 선택해주세요.", Toast.LENGTH_LONG).show()
            return
        }

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val today = current.format(formatter)
        val yesterday = current.minusDays(1).format(formatter)

        val callUrl = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/registration"

        val companyidx = CMList.find{ c -> c.name == strSubHeadquarters }?.idx
        if(companyidx == -1)
        {
            return
        }

        val jsonObject = JSONObject()
        jsonObject.put("id", etID.text)
        jsonObject.put("pw", etPW.text)
        jsonObject.put("name", etName.text)
        jsonObject.put("email", etEmail.text)
        jsonObject.put("phone", etPhone.text)
        jsonObject.put("address", etAddress.text)
        jsonObject.put("signupdate", today)
        jsonObject.put("paiddate", yesterday)
        jsonObject.put("companyidx", companyidx)
        jsonObject.put("expertidx", -1)
        val json = jsonObject.toString()
        //val body = JsonParser.parseString(jsonObject.toString()) as JsonObject

        thread(start = true){

            val url = URL(callUrl)
            val conn  = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("USER-AGENT", "Mozilla/5.0")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.useCaches = false
            conn.defaultUseCaches = false
            conn.doOutput = true // POST 로 데이터를 넘겨주겠다는 옵션
            conn.doInput = true

            val wr = DataOutputStream(conn.outputStream)
            wr.writeBytes(json)
            wr.flush()
            wr.close()

            if(conn.responseCode == HttpURLConnection.HTTP_OK)
            {
                val responseText: String = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                conn.disconnect()

                if(responseText.contains("false"))
                {
                    runOnUiThread{
                        Toast.makeText(this, "이미 존재하는 아이디입니다.", Toast.LENGTH_LONG).show()
                    }
                }
                else
                {
                    runOnUiThread{
                        startActivity(Intent(this, SignUpDoneActivity::class.java))
                        finish()
                    }
                }
            }
            conn.disconnect()
        }
    }
}