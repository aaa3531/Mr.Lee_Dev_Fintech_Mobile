package com.example.moneymachinemobile.ui.yield

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.icu.text.SimpleDateFormat
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ebest.api.CommonFunction
import com.ebest.api.DataPacket
import com.ebest.api.MsgPacket
import com.ebest.api.SocketManager
import com.example.moneymachinemobile.data.DataMngr
import com.example.moneymachinemobile.ApplicationManager
import com.example.moneymachinemobile.MainActivity
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.*
import com.example.moneymachinemobile.ui.calendarDialog
import kotlinx.android.synthetic.main.activity_login_stock.*
import org.json.JSONArray
import java.lang.Exception
import java.util.*
import kotlin.math.roundToInt

class YieldFragment : Fragment() {

    private lateinit var yieldViewModel: YieldViewModel

    private var m_nHandle = -1
    private var handler: ProcMessageHandler? = null
    private lateinit var manager: SocketManager

    private lateinit var startDate  : String
    private lateinit var endDate : String

    lateinit var tvEstimatedAssets : TextView
    lateinit var tvMemberCount : TextView
    lateinit var tvAccumulatedProfit : TextView
    lateinit var yieldItemList : ListView

    lateinit var tempAccount : String

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        yieldViewModel =
                ViewModelProvider(this).get(YieldViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_yield, container, false)

        handler = ProcMessageHandler(this)
        manager = (activity?.application as ApplicationManager).getSockInstance()

        tempAccount = SelectedAccount

        val spinnerAccount = root.findViewById<Spinner>(R.id.spinnerAccount)
        spinnerAccount.adapter = activity?.let { ArrayAdapter(it, android.R.layout.simple_spinner_dropdown_item, AccountList) }
        spinnerAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
            ) {
                tempAccount = spinnerAccount.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        tvEstimatedAssets = root.findViewById(R.id.textViewEstimatedAssets)
        tvMemberCount = root.findViewById(R.id.textViewMemberCount)
        tvAccumulatedProfit = root.findViewById(R.id.textViewAccumulatedProfit)
        yieldItemList = root.findViewById(R.id.yieldItemList)

        val etStartDate = root.findViewById<EditText>(R.id.editTextStartDate)
        val etEndDate = root.findViewById<EditText>(R.id.editTextEndDate)

        val now = System.currentTimeMillis()
        val date = Date(now)
        val simpleYear = SimpleDateFormat("yyyy")
        val simpleMonth = SimpleDateFormat("MM")
        val simpleDay = SimpleDateFormat("dd")
        val curYear = simpleYear.format(date)
        val curMonth = simpleMonth.format(date)
        val curDay = simpleDay.format(date)
        startDate = curYear + curMonth + curDay
        etStartDate.setText("$curYear/$curMonth/$curDay")
        endDate = curYear + curMonth + curDay
        etEndDate.setText("$curYear/$curMonth/$curDay")

        val startDateButton = root.findViewById<ImageButton>(R.id.startDateButton)
        startDateButton.setOnClickListener{
            val dlg = calendarDialog(root.context)
            dlg.setOnOKClickedListener{ year, month, day ->
                startDate = year + month + day
                etStartDate.setText("$year/$month/$day")
            }
            dlg.start()
        }

        val endDateButton = root.findViewById<ImageButton>(R.id.endDateButton)
        endDateButton.setOnClickListener{
            val dlg = calendarDialog(root.context)
            dlg.setOnOKClickedListener{ year, month, day ->
                endDate = year + month + day
                etEndDate.setText("$year/$month/$day")
            }
            dlg.start()
        }

        val searchButton = root.findViewById<Button>(R.id.yieldSearchButton)
        searchButton.setOnClickListener{

            val strMsg = startDate + " ~ " + endDate
            pro = ProgressDialog.show(this.context, "수익률을 조회합니다.", strMsg)

            Yield()
        }

        InitializeProfit()

        return root
    }

    var pro:ProgressDialog? = null
    var m_next : Boolean = false
    var m_contKey : String = ""

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler(private val activity: YieldFragment) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            if(DataMngr.Handler().procMsgHandler(activity.context, msg.what, msg.obj )) {
                return
            }

            when (msg.what) {

                API_DEFINE.RECEIVE_DATA -> {
                    val lpDp = msg.obj as DataPacket

                    if (lpDp.strTRCode == "CDPCQ04700") {
                        processCDPCQ04700(lpDp.pData!!)

                        if(lpDp.strCont == 'Y'.toByte()){
                            m_next = true
                            m_contKey = lpDp.strContKey
                            requestCDPCQ04700()
                        }
                        else{
                            m_next = false
                            m_contKey = ""
                            CalcYield()
                        }
                    }
                }

                API_DEFINE.RECEIVE_MSG -> {
                    val lpMp = msg.obj as MsgPacket
                    val strMsg = lpMp.strTRCode + " " + lpMp.strMsgCode + lpMp.strMessageData
                    pro?.cancel()
                    Toast.makeText(context, lpMp.strMessageData, Toast.LENGTH_SHORT).show()
                }

                API_DEFINE.RECEIVE_ERROR -> {
                    val strMsg = msg.obj as String
                    if (strMsg.contains("전송횟수를 초과")) {
                        pro?.cancel()
                        Toast.makeText(context, "수익률 조회 실패. : 요청 전송횟수 초과", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        pro?.cancel()
                        Toast.makeText(context, strMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                // SOCEKT이 연결이 끊어졌다.
                API_DEFINE.RECEIVE_DISCONNECT -> {
                    val strMsg = msg.obj as String
                }

                // 서버에서 보내는 시스템 ERROR
                API_DEFINE.RECEIVE_SYSTEMERROR -> {
                    val pMsg = msg.obj as MsgPacket
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        /* 화면 갱신시 핸들 재연결 */
        m_nHandle = manager.setHandler(handler as Handler)
    }

    override fun onDestroy() {
        super.onDestroy()
        /* 해당 화면을 사용하지 않을떄 핸들값 삭제 */
        manager.deleteHandler(m_nHandle)
    }

    @SuppressLint("SetTextI18n")
    private fun InitializeProfit()
    {
        tvEstimatedAssets.text      = manager.getCommaValue(serverData.totalestimatedassets) + "원"
        tvMemberCount.text          = manager.getCommaValue(serverData.memberCount) + "명"
        tvAccumulatedProfit.text    = manager.getCommaValue(serverData.profit) + "원"
    }

    fun Yield()
    {
        if(dataList.isNotEmpty()) dataList.clear()
        if(yieldTableList.isNotEmpty()) yieldTableList.clear()

        yieldItemList.adapter = activity?.let { tableitemBaseAdapter(it,yieldTableList) }
        setListViewHeightBasedOnChildren(yieldItemList, 200)

        requestCDPCQ04700()
    }

    private fun requestCDPCQ04700() {

        if(tempAccount.isEmpty()) return

        if(AccountPW.isEmpty()) return

        val strInBlock =
                        CommonFunction.makeSpace("00001",5) +     //RecCnt
                        CommonFunction.makeSpace("3" , 1) +     //QryTp
                        CommonFunction.makeSpace(tempAccount, 20) +   // AcntNo
                        CommonFunction.makeSpace(AccountPW, 8) +    // Pwd
                        CommonFunction.makeSpace(startDate, 8) +    // QrySrtDt
                        CommonFunction.makeSpace(endDate, 8) +    // QryEndDt
                        CommonFunction.makeZero("", 10) +    // SrtNo
                        CommonFunction.makeZero("01", 2) +    // PdptnCode
                        CommonFunction.makeZero("", 2) +    // IsuLgclssCode
                        CommonFunction.makeSpace("",12)              // IsuNo

        if(m_next)
        {
            Thread.sleep(150)
            val nRqID = manager.requestData(m_nHandle, "CDPCQ04700", strInBlock, true, m_contKey, 30)
        }
        else
        {
            val nRqID = manager.requestData(m_nHandle, "CDPCQ04700", strInBlock, false, "", 30)
        }
    }

    val dataList = mutableListOf<거래내역>()
    val yieldTableList = mutableListOf<TableItemData>()

    private fun processCDPCQ04700(pData: ByteArray) {

        val OutBlockName = arrayOf("CDPCQ04700OutBlock1", "CDPCQ04700OutBlock2", "CDPCQ04700OutBlock3", "CDPCQ04700OutBlock4", "CDPCQ04700OutBlock5")
        val OutBlockOccursInfo = booleanArrayOf(false, false, true, false, false)
        val OutBlockLenInfo = arrayOf(
                intArrayOf(5, 1, 20, 8, 8, 8, 10, 2, 2, 12),
                intArrayOf(5, 40),
                intArrayOf(20, 8, 10, 50, 4, 40, 20, 16, 16, 25, 16, 16, 16, 16, 16, 40, 10, 40, 13, 16, 15, 16, 16, 16, 16, 16, 9, 10, 12, 16, 16, 16, 26, 16, 16, 16, 3, 40, 16, 16, 13, 16, 16, 16, 16, 16,
                        20, 20, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 8, 3, 24, 24, 21, 21, 40, 20, 16, 16, 40, 8, 15, 21, 21, 21, 21),
                intArrayOf(5, 16, 16, 16),
                intArrayOf(5, 16, 16, 16, 16, 16, 16, 16, 16, 16, 19, 25, 16, 16, 16, 16, 25)
        )

        val map = manager.getDataFromByte(pData, OutBlockName, OutBlockOccursInfo, OutBlockLenInfo, false, "", "B")
        if (map == null) return

        val s1 = map[OutBlockName[0]] as Array<Array<String>>?
        val s2 = map[OutBlockName[1]] as Array<Array<String>>?
        val s3 = map[OutBlockName[2]] as Array<Array<String>>?
        val s4 = map[OutBlockName[3]] as Array<Array<String>>?
        val s5 = map[OutBlockName[4]] as Array<Array<String>>?

        if(s3 != null)
        {
            for (i in s3.indices) {

                val 거래일자 : String = s3[i][1].replace(" ", "")
                val 거래유형 : String = s3[i][3].replace(" ", "")
                val 거래수량 : String = s3[i][7].replace(" ", "")
                val 종목명   : String = s3[i][17].replace(" ", "")
                val 거래단가 : String = s3[i][18].replace(" ", "")
                val 외화거래금액 : String = s3[i][64].replace(" ", "")
                val 외화수수료: String = s3[i][20].replace(" ", "")
                val 외화세금합계금액 : String = s3[i][32].replace(" ", "")

                dataList.add(거래내역(거래일자, 거래유형, 종목명, 외화수수료.toDouble(), 거래단가.toDouble(), 거래수량.toLong(), 외화거래금액.toDouble(), 외화세금합계금액.toDouble()))
            }
        }
    }

    fun CalcYield()
    {
        val 매수List = mutableListOf<거래내역>()
        val 매도List = mutableListOf<거래내역>()

        for(i in 0 until dataList.count())
        {
            if(dataList[i].거래유형 == "매수")
            {
                매수List.add(dataList[i])
            }
            else if(dataList[i].거래유형 == "매도")
            {
                매도List.add(dataList[i])
            }
        }

        val f매수List =  mutableListOf<거래내역>()

        val temp매수 = 매수List.groupBy { i -> i.종목명 }.toList()
        for(i in 0 until  temp매수.count())
        {
            val 거래일자 = temp매수[i].second[0].거래일자
            val 거래유형 = temp매수[i].second[0].거래유형
            val 외화수수료 = temp매수[i].second.sumOf {it.외화수수료 }
            val 거래수량 = temp매수[i].second.sumOf {it.거래수량 }
            val 거래단가 = temp매수[i].second.sumOf {it.거래단가 }
            val 거래금액 = temp매수[i].second.sumOf {it.거래금액 }
            val 세금합계금액 = temp매수[i].second.sumOf {it.세금합계금액 }

            f매수List.add(거래내역(거래일자, 거래유형, temp매수[i].first, 외화수수료, 거래단가, 거래수량, 거래금액, 세금합계금액))
        }

        val f매도List =  mutableListOf<거래내역>()

        val temp매도 = 매도List.groupBy { i -> i.종목명 }.toList()
        for(i in 0 until  temp매도.count())
        {
            val 거래일자 = temp매도[i].second[0].거래일자
            val 거래유형 = temp매도[i].second[0].거래유형
            val 외화수수료 = temp매도[i].second.sumOf {it.외화수수료 }
            val 거래수량 = temp매도[i].second.sumOf {it.거래수량 }
            val 거래단가 = temp매도[i].second.sumOf {it.거래단가 }
            val 거래금액 = temp매도[i].second.sumOf {it.거래금액 }
            val 세금합계금액 = temp매도[i].second.sumOf {it.세금합계금액 }

            f매도List.add(거래내역(거래일자, 거래유형, temp매도[i].first, 외화수수료, 거래단가, 거래수량, 거래금액, 세금합계금액))
        }

        for( i in 0 until f매수List.count())
        {
            val 매수 = f매수List[i]
            val 매도 = f매도List.find { i -> i.종목명 == 매수.종목명 }
            if(매도 != null)
            {
                val 종목명 = 매도.종목명
                val 총매도금액 = 매도.거래금액 - 매도.외화수수료 - 매도.세금합계금액
                val 총매수금액 = 매수.거래금액 + 매수.외화수수료 + 매수.세금합계금액
                val 추정실현손익 =  총매도금액 - 총매수금액
                val 수익률 = (추정실현손익 / 매수.거래금액 * 100 * 100).roundToInt() /100.0
                val 매도금액 = 매도.거래금액

                yieldTableList.add(TableItemData(종목명, manager.getCommaValue(추정실현손익.toLong()), manager.getCommaValue(매도금액.toLong()), 수익률.toString())) //TODO: 검증 필요
            }
        }

        yieldItemList.adapter = activity?.let { tableitemBaseAdapter(it,yieldTableList) }
        setListViewHeightBasedOnChildren(yieldItemList, 200)

        pro?.cancel()
    }

    fun setListViewHeightBasedOnChildren(listView: ListView, offset : Int) {
        val listAdapter = listView.adapter

        if (listAdapter == null) {
            // pre-condition
            return
        }

        var totalHeight = 0

        for (i in 0 until listAdapter.count)
        {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(0, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams

        params.height = totalHeight + offset
        listView.layoutParams = params

        listView.requestLayout()
    }
}
