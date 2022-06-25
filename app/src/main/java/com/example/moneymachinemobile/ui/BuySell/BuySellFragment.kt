package com.example.moneymachinemobile.ui.buySell

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import com.ebest.api.*
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.data.DataMngr
import com.example.moneymachinemobile.ApplicationManager
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.*
import com.example.moneymachinemobile.data.buyItemBaseAdapter
import com.example.moneymachinemobile.data.sellItemBaseAdapter
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.concurrent.timer

class BuySellFragment : Fragment() {

    companion object {
        fun newInstance() = BuySellFragment()
    }

    private var m_nHandle = -1
    private var handler: ProcMessageHandler? = null
    private lateinit var manager: SocketManager

    lateinit var sellItemList : ListView
    lateinit var buyItemList : ListView
    lateinit var orderItemList : ListView




    private lateinit var viewModel: BuySellViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_buysell, container, false)

        handler = ProcMessageHandler(this)
        manager = (activity?.application as ApplicationManager).getSockInstance()

        buyItemList = root.findViewById(R.id.buyItemList)
        sellItemList = root.findViewById(R.id.sellItemList)
        orderItemList = root.findViewById(R.id.buysell_orderItemList)

        val allSellButton = root.findViewById<Button>(R.id.buttonAllSell)
        allSellButton.setOnClickListener{
            AllSell()
        }

        uiTimer = timer(period = 500)
        {
            InitializeUI()
        }

        return root
    }

    lateinit var uiTimer : Timer

    fun InitializeUI()
    {
        try {
            activity?.runOnUiThread{
                if(stockDone)
                {
                    InitialUserSellItem()
                }

                if(orderModelDone)
                {
                    InitializeExpertTop3()
                }

                if(signedDone)
                {
                    InitializeSigned()
                }
            }
        }
        catch (ex : Exception)
        {
            Toast.makeText(this.context, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BuySellViewModel::class.java)
        // TODO: Use the ViewModel
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
        uiTimer.cancel()
    }

    fun InitializeExpertTop3()
    {
        if(SelectedAccount.isNotEmpty())
        {
            val buyitem = mutableListOf<UserBuyItemData>()

            if(buyOrderModelList.isNotEmpty())
            {
                for(i in 0 until buyOrderModelList.count())
                    buyitem.add(
                            UserBuyItemData(SelectedAccount,
                                    buyOrderModelList[i].expcode,
                                    buyOrderModelList[i].expname,
                                    buyOrderModelList[i].ordercount.toString(),
                                    buyOrderModelList[i].orderprice.toString(),
                                    buyOrderModelList[i].orderdate)
                    )
            }
            buyItemList.adapter = activity?.let { buyItemBaseAdapter(it, buyitem) }
            setListViewHeightBasedOnChildren(buyItemList, 0)
        }
    }

    fun InitialUserSellItem()
    {
        val sellitem = mutableListOf<UserSellItemData>()

        if(userSellItemData.isNotEmpty())
        {
            if(userSellItemData.isNotEmpty())
            {
                for(i in 0 until userSellItemData.count())
                {
                    sellitem.add(UserSellItemData(SelectedAccount,
                            userSellItemData[i].종목코드,
                            userSellItemData[i].종목명,
                            userSellItemData[i].잔고수량,
                            userSellItemData[i].매입금액,
                            userSellItemData[i].평가금액,
                            userSellItemData[i].평가손익,
                            userSellItemData[i].수익률,
                            userSellItemData[i].매도신호))
                }
            }

            sellItemList.adapter = activity?.let { sellItemBaseAdapter(it,sellitem) }
            setListViewHeightBasedOnChildren(sellItemList, 0)
        }
        else
        {
            if(sellitem.isNotEmpty()){
                sellitem.clear()
            }

            sellItemList.adapter = activity?.let { sellItemBaseAdapter(it,sellitem) }
            setListViewHeightBasedOnChildren(sellItemList, 0)
        }
    }

    fun InitializeSigned()
    {
        val orderItem = mutableListOf<TableItemData>()

        val count = allsignedModel.count()
        for( i in 0 until count) {

            orderItem.add(TableItemData(allsignedModel[i].종목명,allsignedModel[i].주문구분,allsignedModel[i].주문수량,allsignedModel[i].체결가격.toString()))
        }

        orderItemList.adapter = activity?.let { tableitemBaseAdapter(it,orderItem) }
        setListViewHeightBasedOnChildren(orderItemList ,100)
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
            //listItem.measure(0, 0)
            listItem.measure(0, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }

        val params = listView.layoutParams

        params.height = totalHeight + offset
        listView.layoutParams = params

        listView.requestLayout()
    }

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler(private val activity: BuySellFragment) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            if(DataMngr.Handler().procMsgHandler(activity.context, msg.what, msg.obj )) {
                return
            }

            when (msg.what) {

                API_DEFINE.RECEIVE_DATA -> {
                    val lpDp = msg.obj as DataPacket
                    val trcode = lpDp.strTRCode

                    if(trcode == "t0424"){
                        //processT0424(lpDp.pData!!)
                    }
                    else if (trcode!!.contains("CSPAT") || trcode.contains("CFOAT")) {
                        processCSPAT_CFOAT(lpDp.pData, lpDp.strTRCode)
                    }
                }
                // TR조회 끝
                API_DEFINE.RECEIVE_RELEASE -> {
                    val lpDp = msg.obj as ReleasePacket

                    lpDp.nRqID
                    lpDp.strTrCode
                }
                API_DEFINE.RECEIVE_REALDATA -> {
                    val lpRp = msg.obj as RealPacket
                    if (lpRp.strBCCode == "S3_" || lpRp.strBCCode == "K3_") {
                        //processS3_(lpRp.strKeyCode, lpRp.pData);
                    } else if (lpRp.strBCCode == "SC0" || lpRp.strBCCode == "SC1" || lpRp.strBCCode == "SC2" || lpRp.strBCCode == "SC3" || lpRp.strBCCode == "SC4") {
                        val pData = lpRp.pData
                        var nLen = pData!!.size

                        // 주식주문접수
                        if (lpRp.strBCCode == "SC0") {
                            Toast.makeText(this.activity.context,"주문이 완료되었습니다.", Toast.LENGTH_LONG).show()
                            processSC0(pData)

                            // 주식주문체결
                        } else if (lpRp.strBCCode == "SC1") {
                            processSC1(pData)

                            // 주식주문정정
                        } else if (lpRp.strBCCode == "SC2") {
                            processSC2(pData)

                            // 주식주문취소
                        } else if (lpRp.strBCCode == "SC3") {
                            processSC3(pData)

                            // 주식주문거부
                        } else if (lpRp.strBCCode == "SC4") {
                            processSC4(pData)
                        }
                    }
                }
                API_DEFINE.RECEIVE_MSG -> {
                    val lpMp = msg.obj as MsgPacket
                    val strMsg = lpMp.strTRCode + " " + lpMp.strMsgCode + lpMp.strMessageData
//                    Toast.makeText(this.activity.context,strMsg, Toast.LENGTH_LONG).show()
                }
                // 일반적인 에러
                API_DEFINE.RECEIVE_ERROR -> {
                    val strMsg = msg.obj as String
                    Toast.makeText(this.activity.context,strMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun AllSell()
    {
        val builder = AlertDialog.Builder(this.context)
        builder.setTitle("일괄 매도")
        builder.setMessage("전체 일괄 매도합니다.")
        builder.setPositiveButton("확인") { dialoginterface: DialogInterface?, i: Int ->

            val strAccount = SelectedAccount
            if (strAccount.isEmpty()) return@setPositiveButton

            val strAccPwd = AccountPW
            if (strAccPwd.isEmpty()) return@setPositiveButton

            for(i in 0 until userSellItemData.count())
            {
                requestMaemae(userSellItemData[i].종목코드, userSellItemData[i].잔고수량)
            }
        }
        builder.setNeutralButton("취소", null)

        builder.show()
    }

    private fun requestMaemae(code : String , Qty : String) {
        // 계좌번호(20) , 입력비밀번호(8) , 종목번호(12) , 주문수량(16) , 주문가(13.2) , 매매구분(1) , 호가유형코드(2) , 신용거래코드(3) , 대출일(8) , 주문조건구분(1)

        var strAccount = SelectedAccount
        var strPass = AccountPW
        var strJongmok = code
        var strQty = Qty
        var strDanga = ""
        val strHogaCode = "03" // 시장가

        strAccount = manager.makeSpace(strAccount, 20)
        strJongmok = manager.makeSpace(strJongmok, 12)
        strQty = manager.makeZero(strQty, 16)

        if (strDanga.isEmpty()) {
            strDanga = "0"
        }

        strDanga = String.format("%.2f", java.lang.Double.parseDouble(strDanga))
        strDanga = manager.makeZero(strDanga, 13)

        strPass = manager.makeSpace(strPass,8)

        val strInBlock =
                strAccount + strPass + strJongmok + strQty + strDanga + "1" + strHogaCode + "000" + "        " + "0"
        //int nRqID = manager.requestDataAccount(m_nHandle, "CSPAT00600", strInBlock, 0, 'B', "", false, false, false, false, "", 30);
        val nRqID = manager.requestData(m_nHandle, "CSPAT00600", strInBlock, false, "", 30)
    }

    private fun processCSPAT_CFOAT(pData: ByteArray?, TRName: String?) {

        val blockname1  = TRName!! + "OutBlock1"
        val blockname2 = TRName + "OutBlock2"
        val map: Array<Array<String>>?
        val pArray: Array<ByteArray>

        /*
        //방법 1. res 폴더에 TR정보가 담긴 *.res 파일이 있는 경우
        map = manager.getOutBlockDataFromByte(TRName, blockname, pData);
        pArray = manager.getAttributeFromByte(TRName, blockname, pData); // attribute
        */

        //방법2. 프로젝트의 TR정보가 담긴 소스(.kt, .java ... *현재 프로젝트의 TRCODE.kt등 )를 사용하는 경우.
        var OutBlockName: Array<String>? = null
        OutBlockName = arrayOf(blockname1, blockname2)

        var OutBlockOccursInfo: BooleanArray? = null
        var OutBlockLenInfo: Array<IntArray>? = null
        var hashmap: HashMap<*, *>? = null

        when (TRName) {
            "CSPAT00600" -> {
                // ex CSPAT00600.
                //OutBlockName = arrayOf("CSPAT00600OutBlock1", "CSPAT00600OutBlock2")
                OutBlockOccursInfo = booleanArrayOf(false, false)
                OutBlockLenInfo = arrayOf(
                        intArrayOf(5, 20, 8, 12, 16, 13, 1,2,2,1,1,2,3,8,3,1,6,20,10,10,10,10,10,12,1,1),
                        intArrayOf(5, 10, 9, 2, 2, 9, 9, 16, 10, 10, 10, 16, 16, 16, 16, 16, 40, 40)
                )
            }
            "CSPAT00700" -> {
                // ex CSPAT00700.
                //OutBlockName = arrayOf("CSPAT00700OutBlock1", "CSPAT00700OutBlock2")
                OutBlockOccursInfo = booleanArrayOf(false, false)
                OutBlockLenInfo = arrayOf(
                        intArrayOf(5, 20, 8, 12, 16, 2, 1, 13, 2, 6, 20, 10, 10, 10, 10, 10),
                        intArrayOf(5,10,10,9,2,2,9,2,1,1,3,8,1,1,9,16,1,10,10,10,16,16,16,40,40)
                )
            }
            "CSPAT00800" -> {
                // ex CSPAT00800.
                //OutBlockName = arrayOf("CSPAT00800OutBlock1", "CSPAT00800OutBlock2")
                OutBlockOccursInfo = booleanArrayOf(false, false)
                OutBlockLenInfo = arrayOf(
                        intArrayOf(5, 10, 20, 8, 12, 16, 2, 20, 6, 10, 10, 10, 10, 10),
                        intArrayOf(5, 10, 10, 9, 2, 2, 9, 2, 1, 1, 3, 8, 1, 1, 9, 1, 10, 10, 10, 40, 40)
                )
            }

        }
        hashmap = manager.getDataFromByte(
                pData!!,
                OutBlockName,
                OutBlockOccursInfo!!,
                OutBlockLenInfo!!,
                false,
                "",
                "B"
        )
        val o1 = hashmap!![OutBlockName[0]]
        val o2 = hashmap[OutBlockName[1]]

        // OutBlock별 데이터
        val s1: Array<Array<String>>?
        val s2: Array<Array<String>>?
        s1 = (o1 as Array<Array<String>>?)
        s2 = (o2 as Array<Array<String>>?)
        map = s2;

        if (map != null) {
            val strJumunBunho = map[0][1]
//            m_textViewJumunBunho!!.text = strJumunBunho
        }
    }

    // 주문접수
    private fun processSC0(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,10,11,9,8,12,9,40,16,13,1,2,2,1,1,3,8,1,6,20,10,10,10,10,10,
                1,3,1,3,20,1,2,1,20,10,9,10,9,16,16,16,10,16,1,10,10,10,10,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // 주문체결
    private fun processSC1(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
                16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // 주문정정
    private fun processSC2(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
                16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // 주문취소
    private fun processSC3(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
                16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // 주문거부
    private fun processSC4(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
                16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }
}