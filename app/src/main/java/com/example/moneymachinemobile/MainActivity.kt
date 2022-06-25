package com.example.moneymachinemobile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ebest.api.*
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.data.*
import com.example.moneymachinemobile.ui.account.AccountFragment
import com.example.moneymachinemobile.ui.buySell.BuySellFragment
import com.example.moneymachinemobile.ui.home.HomeFragment
import com.example.moneymachinemobile.ui.setting.SettingFragment
import com.example.moneymachinemobile.ui.yield.YieldFragment
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private var m_nHandle = -1
    private var handler: ProcMessageHandler? = null
    private lateinit var manager: SocketManager

    private val fragmanager = supportFragmentManager
    private val subviewlst = listOf<Triple<Int, String, Int>>(
        Triple(R.id.navi_1, "홈", R.drawable.btn_home_n),
        Triple(R.id.navi_2, "계좌잔고", R.drawable.btn_bank_n),
        Triple(R.id.navi_3, "사자/팔자", R.drawable.btn_sell_n),
        Triple(R.id.navi_4, "수익률", R.drawable.btn_rank_n),
        Triple(R.id.navi_5, "머니플랜", R.drawable.btn_moneyplan_n),
        Triple(R.id.navi_6, "커뮤니티", R.drawable.btn_community_n),
        Triple(R.id.navi_7, "설정", R.drawable.btn_setting_n),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.title = subviewlst[0].second

        /* 하단뷰 버튼 초기화 */
        val lstid = listOf<Triple<Int, String, Int>>(
            subviewlst.get(0),
            subviewlst.get(1),
            subviewlst.get(2),
            subviewlst.get(3),
            subviewlst.get(4),
            subviewlst.get(5),
            subviewlst.get(6),
        )

        for(idx in lstid.indices) {
            val txt = findViewById<LinearLayout>(lstid[idx].first).findViewById<TextView>(R.id.btn_name)
            txt.text = lstid[idx].second

            val icon = findViewById<LinearLayout>(lstid[idx].first).findViewById<ImageView>(R.id.btn_icon)
            icon.setImageResource(lstid[idx].third)
        }

        handler = ProcMessageHandler()
        manager = (application as ApplicationManager).getSockInstance()
        m_nHandle = manager.setHandler(handler as Handler)

        Thread.sleep(500) //계좌정보 가져오기위한 딜레이
        AccountList = getAccountList()
        if(AccountList.isNotEmpty())
        {
            SelectedAccount = AccountList[0]
            requestT0424()
        }

        thread(start = true){
            requestT8430() //조회에 시간이 오래걸릴수있어서 스레드 처리함. 종목명 조회가 안될수도있음.
        }

        thread(start = true){
            GetServerData()
        }

        thread(start = true){
            InitializeRatio()
        }

        serverTimer = timer(period = 10000){
            InitializeYield()
            GetOrder()
        }

        stockTimer = timer(period = 800){
            requestT0424()
            requestT0425()
        }
    }

    lateinit var serverTimer : Timer
    lateinit var stockTimer : Timer

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            try {
                if(DataMngr.Handler().procMsgHandler(applicationContext, msg.what, msg.obj)) {
                    return
                }

                when (msg.what) {

                    API_DEFINE.RECEIVE_DATA -> {
                        val lpDp = msg.obj as DataPacket
                        val trcode = lpDp.strTRCode

                        if(trcode == "t0424"){
                            processT0424(lpDp.pData!!)
                        }
                        else if (trcode == "t0425"){
                            processT0425(lpDp.pData!!)
                        }
                    }
                    // TR조회 끝
                    API_DEFINE.RECEIVE_RELEASE -> {
                        val lpDp = msg.obj as ReleasePacket

                        lpDp.nRqID
                        lpDp.strTrCode
                    }

                    API_DEFINE.RECEIVE_MSG -> {
                        val lpMp = msg.obj as MsgPacket
                        val strMsg = lpMp.strTRCode + " " + lpMp.strMsgCode + lpMp.strMessageData
//                    Toast.makeText(applicationContext,strMsg, Toast.LENGTH_LONG).show()
                    }
                    // 일반적인 에러
                    API_DEFINE.RECEIVE_ERROR -> {
                        val strMsg = msg.obj as String
                        Toast.makeText(applicationContext,strMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            catch (ex : java.lang.Exception)
            {
//                Toast.makeText(applicationContext,ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun onNaviBtnClicked(v: View) {
        val viewId = v.id
        var title = ""
        var fragment : Fragment? = null

        when(viewId)
        {
            subviewlst[0].first -> {
                fragment = HomeFragment()
                title = subviewlst[0].second
            }
            subviewlst[1].first -> {
                fragment = AccountFragment()
                title = subviewlst[1].second
            }
            subviewlst[2].first -> {
                fragment = BuySellFragment()
                title = subviewlst[2].second
            }
            subviewlst[3].first -> {
                fragment = YieldFragment()
                title = subviewlst[3].second
            }
            subviewlst[4].first -> {
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra("url", "https://머니플랜.com")
                startActivity(intent)
                return
            }
            subviewlst[5].first -> {
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra("url", "https://머신홀딩스.com")
                startActivity(intent)
                return
            }
            subviewlst[6].first -> {
                fragment = SettingFragment()
                title = subviewlst[6].second
            }
        }

        /*화면전환*/
        if(fragment != null) {
            fragmanager.beginTransaction().replace(R.id.frameLayout, fragment).commit()
            this.title = title
        }
    }

    private var lastTimeBackPressed : Long = 0
    override fun onBackPressed() {
        if(System.currentTimeMillis() - lastTimeBackPressed >= 1500)
        {
            lastTimeBackPressed = System.currentTimeMillis()
            Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_LONG).show()
        }
        else
        {
            requestT0424()
            SendUserModel()
            serverTimer.cancel()
            manager.logout()
            manager.disconnect()
            finishAffinity()                       // 해당앱의 루트 액티비티를 종료시킨다.
            System.runFinalization()              // 현재 작업중인 쓰레드가 종료되면 종료 시키라는 명령어
            exitProcess(0)
        }
    }

    private fun getAccountList() : java.util.ArrayList<String> {
        val temp : java.util.ArrayList<String> = arrayListOf()

        if(!manager.isConnect()) return arrayListOf()
        val tempList = manager.getAccountList()
        val tempSize = manager.getAccountCount()

        for( i in 0 until tempSize){
            temp.add(tempList?.get(i)?.get(0) as String)  // 계좌번호
//            temp.add(tempList?.get(i)?.get(1) as String)   // 계좌명
//            temp.add(tempList?.get(i)?.get(2) as String)  // 상품유형코드
//            temp.add(tempList?.get(i)?.get(3) as String)  // 관리지점번호
        }

        if(temp.isNotEmpty())
        {
            Toast.makeText(this, "계좌정보를 가져왔습니다.", Toast.LENGTH_SHORT).show()
        }
        return temp
    }

    private fun requestT8430() {

        // DataMngr를 사용하는 방법입니다.
        val t8430 = DataMngr.getInstance(manager, "t8430")!!

        //------------------------------------------------------------------------------------------
        // 입력
        t8430.writeFieldData("t8430InBlock", "gubun    ", "0")

        //------------------------------------------------------------------------------------------
        //   전송
        //   TR 전송 제한으로 인해 조회시 시간을 입력받아 초당전송 제한에 걸리는 문제를 최소화 하기 위해 request에 기능 추가
        //   nLastSec -> -1:즉시전송, 0:초당전송시간이 지난 후에 전송, < 0 : nLastSec초가 지난 후에 전송
        //   fun request( sm : SocketManager, nHandler : Int, bNext: Boolean = false, strContinueKey: String = "", nLaterSec : Int = -1, nTimeOut: Int = 30 ) : Int
        val nRqID = t8430.request(manager, m_nHandle)

        if( nRqID < 0 ) {
//            Toast.makeText( activity?.applicationContext, "TR전송실패(" + nRqID + ")", Toast.LENGTH_LONG ).show()
            return
        }

        //------------------------------------------------------------------------------------------
        // 수신
        t8430.setOnRecvListener(object : DataMngr.OnRecvListener {
            /**
             * 조회 응답
             */
            override fun onData(dm: DataMngr, sBlockName: String) {

            }

            /**
             * 데이터 메세지
             */
            override fun onMsg(dm: DataMngr, sCode: String, sMsg: String, bCriticalError: Boolean) {

            }

            /**
             * 조회한 서비스가 모두 종료되었을 때
             */
            override fun onComplete(dm: DataMngr) {

                if (stockList.isNotEmpty()) {
                    stockList.clear()
                }

                val nCount = t8430.getBlockCount("t8430OutBlock")
                for (i in 0 until nCount) {
                    val 종목명 = dm.readFieldData("t8430OutBlock", "hname     ", i)
                    val 단축코드 = dm.readFieldData("t8430OutBlock", "shcode  ", i)
                    val 확장코드 = dm.readFieldData("t8430OutBlock", "expcode  ", i)
                    val ETF = dm.readFieldData("t8430OutBlock", "etfgubun     ", i)
                    val 상한가 = dm.readFieldData("t8430OutBlock", "uplmtprice     ", i).toLong()
                    val 하한가 = dm.readFieldData("t8430OutBlock", "dnlmtprice      ", i).toLong()
                    val 전일가 = dm.readFieldData("t8430OutBlock", "jnilclose     ", i).toLong()
                    val 주문수량단위 = dm.readFieldData("t8430OutBlock", "memedan      ", i)
                    val 기준가 = dm.readFieldData("t8430OutBlock", "recprice     ", i).toLong()
                    val 구분 = dm.readFieldData("t8430OutBlock", "gubun      ", i)

                    stockList.add(Stock(종목명, 단축코드, 확장코드, ETF, 상한가, 하한가, 전일가, 주문수량단위, 기준가, 구분))
                }
            }
        })
    }

    private fun GetServerData()
    {
        try {
            serverDataDone = false

            val callUrl: String = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/profit?"

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
                        Toast.makeText(this, "오류.", Toast.LENGTH_SHORT).show()
                        break
                    }

                    val jsonObject = JSONObject(line)

                    serverData = ServerData(
                            jsonObject.getString("profit").toInt(),
                            jsonObject.getString("totalestimatedassets").toInt(),
                            jsonObject.getString("memberCount").toInt(),
                    )
                }

                buffered.close()
                conn.disconnect()
            }

            serverDataDone = true

        }
        catch (ex: Exception)
        {
        }
    }

    private fun InitializeYield()
    {
        try {
            yieldDataDone = false

            val callUrl : String = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/expertyield?" + "expertidx=" + user.expertidx;
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
                        Toast.makeText(this, "오류.", Toast.LENGTH_SHORT).show()
                        break
                    }

                    if(yieldList.isNotEmpty())
                    {
                        yieldList.clear()
                    }

                    val jsonArray = JSONArray(line)
                    //val jsonObject = JSONObject(line)

                    for(i in 0 until jsonArray.length())
                    {
                        val jsonObject = JSONArray(line).getJSONObject(i)

                        val ym = YieldModel(
                                jsonObject.getString("expertidx").toInt(),
                                jsonObject.getString("expname"),
                                jsonObject.getString("orderprice").toInt(),
                                jsonObject.getString("price").toInt(),
                                jsonObject.getString("valuation").toInt(),
                                Math.round(jsonObject.getString("yield").toDouble()*100)/100f.toDouble()
                        )
                        yieldList.add(ym)
                    }
                }

                buffered.close()
                conn.disconnect()
            }
            yieldDataDone = true
        }
        catch (ex: Exception)
        {
        }
    }

    private fun InitializeRatio()
    {
        try {
            val callUrl: String = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/ratio"
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
                        Toast.makeText(this, "오류.", Toast.LENGTH_SHORT).show()
                        break
                    }

                    val jsonObject = JSONObject(line)

                    ratioModel = RatioModel(
                            jsonObject.getString("idx").toInt(),
                            jsonObject.getString("ratio").toInt(),
                    )
                }

                buffered.close()
                conn.disconnect()
            }
        }
        catch (ex: Exception)
        {
        }
    }

    private fun GetOrder()
    {
        try {
            orderModelDone = false

            val callUrl : String = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/order?" + "expertidx=" + user.expertidx;

            val url = URL(callUrl)
            val conn  = url.openConnection() as HttpURLConnection

            if(orderModelList.isNotEmpty())
            {
                orderModelList.clear()
            }

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
                    //val jsonObject = JSONObject(line)

                    for(i in 0 until jsonArray.length())
                    {
                        val jsonObject = JSONArray(line).getJSONObject(i)

                        val om = OrderModel(
                                jsonObject.getString("expertidx").toInt(),
                                jsonObject.getString("expcode"),
                                jsonObject.getString("expname"),
                                jsonObject.getString("orderprice").toInt(),
                                0,
                                jsonObject.getString("orderdate"),
                                jsonObject.getString("ordertype"),
                                true,
                                0,
                        )
                        orderModelList.add(om)
                    }
                }

                buyOrderModelList.clear()
                sellOrderModelList.clear()

                for(i in 0 until orderModelList.count())
                {
//                    orderModelList[i].price = stockList.find { o -> o.단축코드 == orderModelList[i].expcode }.현재가

                    if(orderModelList[i].ordertype == "매수")
                    {
                        buyOrderModelList.add(orderModelList[i])
                    }
                    else
                    {
                        sellOrderModelList.add(orderModelList[i])
                    }
                }

                buffered.close()
                conn.disconnect()
            }

            orderModelDone = true
        }
        catch (ex: Exception)
        {
        }
    }

    private fun SendUserModel()
    {
        try {
            val callUrl: String = "http://Moneymachinects-env-1.eba-49j29inb.ap-northeast-2.elasticbeanstalk.com/api/user"

            val jsonObject = JSONObject()
            jsonObject.put("idx", user.idx)
            jsonObject.put("id", user.id)
            jsonObject.put("pw", user.pw)
            jsonObject.put("name", user.name)
            jsonObject.put("email", user.email)
            jsonObject.put("phone", user.phone)
            jsonObject.put("address", user.address)
            jsonObject.put("signupdate", user.signupdate)
            jsonObject.put("paiddate", user.paiddate)
            jsonObject.put("expertidx", user.expertidx)
            jsonObject.put("profit", user.profit)
            jsonObject.put("totalestimatedassets", user.totalestimatedassets)
            jsonObject.put("access", user.access)
            jsonObject.put("downpayment", user.downpayment)
            jsonObject.put("companyidx", user.companyidx)
            jsonObject.put("expertname", user.expertname)

            val json = jsonObject.toString()
            //val body = JsonParser.parseString(jsonObject.toString()) as JsonObject

            thread(start = true){

                val url : URL = URL(callUrl)
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

                val responseCode = conn.responseCode
                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    val responseText: String = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    conn.disconnect()
                }
                conn.disconnect()
            }
        }
        catch (ex: Exception)
        {
        }
    }

    private fun requestT0424() {

        stockDone = false

        val strAccount = SelectedAccount

        if(strAccount.isEmpty()) return

        val strAccPwd : String = AccountPW

        if(strAccPwd.isEmpty()) return

        val strInBlock =
                CommonFunction.makeSpace(strAccount,11) + " " +   // 계좌번호
                        CommonFunction.makeSpace(strAccPwd , 8) + " " +   // 비밀번호
                        CommonFunction.makeSpace(   "1", 1) + " " +   // 단가구분 1 : 평균단가 2 : BEP단가
                        CommonFunction.makeSpace(   "2", 1) + " " +   // 체결구분 0 : 결제기준 2 : 체결기준(잔고가 0이 아닌 종목)
                        CommonFunction.makeSpace(   "0", 1) + " " +   // 단일가구분 0 : 정규장 2 : 시간외단일가
                        CommonFunction.makeSpace(   "1", 1) + " " +   // 제비용미포함여부 0 : 제비용미포함 1 : 제비용포함
                        CommonFunction.makeSpace(    "",22) + " "

        var nRqID = 0

        nRqID = manager.requestData(m_nHandle, "t0424", strInBlock, false, "", 30)
    }

    private fun processT0424(pData: ByteArray) {

        val OutBlockName = arrayOf("t0424OutBlock", "t0424OutBlock1")
        val OutBlockOccursInfo = booleanArrayOf(false, true)
        val OutBlockLenInfo = arrayOf(
                intArrayOf(18, 18, 18, 18, 22, 18, 18),
                intArrayOf(12, 10, 18, 18, 18, 18, 18, 8, 18, 18, 18, 18, 18, 18, 18, 18, 10, 8, 20, 1, 1, 10, 8, 18, 18, 10, 10, 10, 10))

        val map = manager.getDataFromByte(pData, OutBlockName, OutBlockOccursInfo, OutBlockLenInfo, true, "", "B")
        if (map == null) return

        val s1 = map[OutBlockName[0]] as Array<Array<String>>?
        val s2 = map[OutBlockName[1]] as Array<Array<String>>?

        if(s1 == null) return

        val sunamt   = s1[0][0]
        val dtsunik  = s1[0][1]
        val mamt     = s1[0][2]
        val sunamt1  = manager.getCommaValue(s1[0][3])
        val ctxcode  = manager.getCommaValue(s1[0][4])
        val tappamt  = s1[0][5]
        val tdtsunik = s1[0][6]

        accountInfo.추정순자산 = sunamt.toLong()
        accountInfo.매입금액 = mamt.toLong()
        accountInfo.평가금액 = tappamt.toLong()
        accountInfo.평가손익 = tdtsunik.toLong()
        accountInfo.확정손익 = dtsunik.toLong()

        user.totalestimatedassets = accountInfo.추정순자산.toInt()
        user.profit = accountInfo.확정손익.toInt()

        if (s2 != null) {
            if(userSellItemData.isNotEmpty()){
                userSellItemData.clear()
            }
            for (i in s2.indices) {

                val 종목코드 :String = s2[i][0].replace(" ","")
                val 종목명 :String  = s2[i][18].replace(" ","")
                val 잔고수량 :String  = s2[i][2].replace(" ","")
                val 매입금액 = manager.getCommaValue(s2[i][4])
                val 평가금액 = manager.getCommaValue(s2[i][22])
                val 평가손익 = manager.getCommaValue(s2[i][24])
                var 수익률 : String = (s2[i][25].toDouble()/100).toString()
                if(!수익률.contains("-"))
                {
                    수익률 = "+$수익률"
                }
                var 매도신호시간 = ""

                val idx : Int = sellOrderModelList.indexOf{ sellOrderModelList.find {  r -> r.expcode == 종목코드 } }
                if (idx > -1)
                {
                    매도신호시간 = sellOrderModelList[idx].orderdate
                }

                userSellItemData.add(UserSellItemData(SelectedAccount,종목코드, 종목명, 잔고수량, 매입금액, 평가금액, 평가손익, 수익률, 매도신호시간))
            }
        }
        else
        {
            userSellItemData.clear()
        }

        stockDone = true
    }

    private fun requestT0425() {

        signedDone = false

        val strAccount = SelectedAccount
        if(strAccount.isEmpty()) return

        val strAccPwd = AccountPW
        if(strAccPwd.isEmpty()) return

        val strInBlock =
                CommonFunction.makeSpace(strAccount,11) + " " +   // 계좌번호
                        CommonFunction.makeSpace(strAccPwd , 8) + " " +   // 비밀번호
                        CommonFunction.makeSpace("" , 12) + " " +   // 종목번호
                        CommonFunction.makeSpace("0", 1) + " " +   // 체결구분 0 : 결제기준 2 : 체결기준(잔고가 0이 아닌 종목)
                        CommonFunction.makeSpace("0", 1) + " " +   // 매매구분
                        CommonFunction.makeSpace("1", 1) + " " +   // 정렬순서
                        CommonFunction.makeSpace("",10) + " "

        var nRqID = 0

        nRqID = manager.requestData(m_nHandle, "t0425", strInBlock, false, "", 30)
    }

    private fun processT0425(pData: ByteArray) {

        val OutBlockName = arrayOf("t0425OutBlock", "t0425OutBlock1")
        val OutBlockOccursInfo = booleanArrayOf(false, true)
        val OutBlockLenInfo = arrayOf(
                intArrayOf(18, 18, 18, 18, 18, 18, 18, 18, 10),
                intArrayOf(10, 12, 10, 9, 9, 9, 9, 9, 9, 10, 10, 20, 8, 10, 10, 2, 8, 2, 2, 8))

        val map = manager.getDataFromByte(pData, OutBlockName, OutBlockOccursInfo, OutBlockLenInfo, true, "", "B")
        if (map == null) return

        val s1 = map[OutBlockName[0]] as Array<Array<String>>?
        val s2 = map[OutBlockName[1]] as Array<Array<String>>?

        if(s1 == null) return

        val tqty   = s1[0][0] // 총주문수량
        val tcheqty  = s1[0][1] // 총체결수량
        val tordrem     = s1[0][2] // 총미체결수량
        val cmss  = s1[0][3]    // 추정수수료
        val tamt  = s1[0][4]    // 총주문금액
        val tax  = s1[0][7] // 추정제세금

        if (s2 != null) {

            if(allsignedModel.isNotEmpty()){
                allsignedModel.clear()
            }

            if (unSignedItem.isNotEmpty()) {
                unSignedItem.clear()
            }
            if (unSignedModel.isNotEmpty()) {
                unSignedModel.clear()
            }
            if (signedItem.isNotEmpty()) {
                signedItem.clear()
            }
            if (signedModel.isNotEmpty()) {
                signedModel.clear()
            }

            for (i in s2.indices) {

                val 원주문번호  =  s2[i][0].replace(" ","")
                val 종목코드    = s2[i][1].replace(" ","")
                val 종목명 = stockList.find { r -> r.단축코드 == 종목코드 }?.종목명?.replace(" ", "");
                val 주문구분 = s2[i][2].replace(" ","")
                val 주문수량 = s2[i][3].replace(" ","")
                val 주문가격 = s2[i][4].replace(" ","").toLong()
                val 체결수량    =s2[i][5].replace(" ","").toLong()
                val 체결가격 =  s2[i][6].replace(" ","").toLong()
                val 미체결잔량 = s2[i][7].replace(" ","").toLong()
                val 체결상태 = s2[i][9].replace(" ","")
                val 현재가     =s2[i][16].replace(" ","").toLong()

                allsignedModel.add(체결(원주문번호,종목코드,종목명,주문구분,주문수량,주문가격,현재가,체결수량,미체결잔량,체결가격,체결상태))

                if (미체결잔량 > 0) {
                    unSignedModel.add(체결(원주문번호,종목코드,종목명,주문구분,주문수량,주문가격,현재가,체결수량,미체결잔량,체결가격,체결상태))
                    unSignedItem.add(TableItemData(종목명, 주문구분, manager.getCommaValue(현재가), 미체결잔량.toString()))
                } else {
                    signedModel.add(체결(원주문번호,종목코드,종목명,주문구분,주문수량,주문가격,현재가,체결수량,미체결잔량,체결가격,체결상태))
                    signedItem.add(TableItemData(종목명, 주문구분, manager.getCommaValue(현재가), 미체결잔량.toString()))
                }
            }

            signedDone = true
        }
        else
        {
            allsignedModel.clear()
        }
    }
}