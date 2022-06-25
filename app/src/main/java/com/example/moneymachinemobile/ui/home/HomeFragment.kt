package com.example.moneymachinemobile.ui.home

import MySharedPreferences
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ebest.api.*
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.ApplicationManager
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.*
import com.example.moneymachinemobile.data.buyItemBaseAdapter
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timer

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel



    private var m_nHandle = -1
    private var handler: ProcMessageHandler? = null
    private lateinit var manager: SocketManager

    lateinit var orderItemList : ListView
    private lateinit var radioUnSigned : RadioButton
    private lateinit var radioSigned  : RadioButton

    lateinit var tvEstimatedAssets : TextView
    lateinit var tvMemberCount : TextView
    lateinit var tvAccumulatedProfit : TextView
    lateinit var tvExpert : TextView

    var autobuy : Boolean = false
    var bulkSell : Boolean = false
    lateinit var btnControlMode : Button
    lateinit var yieldtop5listview : ListView
    lateinit var experttop3listview : ListView
    lateinit var expertItemlistview : ListView
    lateinit var ivExpert : ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        handler = ProcMessageHandler(this)
        manager = (activity?.application as ApplicationManager).getSockInstance()
        m_nHandle = manager.setHandler(handler as Handler)

        autobuy = MySharedPreferences.getUserAutoBuy(root.context)
        bulkSell = MySharedPreferences.getUserBulkSell(root.context)

        tvEstimatedAssets = root.findViewById(R.id.textViewEstimatedAssets)
        tvMemberCount = root.findViewById(R.id.textViewMemberCount)
        tvAccumulatedProfit = root.findViewById(R.id.textViewAccumulatedProfit)
        tvExpert = root.findViewById(R.id.textViewExpert)
        ivExpert = root.findViewById(R.id.imageViewExpert)
        btnControlMode = root.findViewById(R.id.buttonControlMode)
        orderItemList = root.findViewById(R.id.orderItemList)
        yieldtop5listview = root.findViewById(R.id.yieldTop5ItemList)
        experttop3listview = root.findViewById(R.id.itemList1)
        expertItemlistview = root.findViewById(R.id.expertItemList)

        radioUnSigned = root.findViewById(R.id.radioButtonUnSigned) //미체결 탭
        radioSigned = root.findViewById(R.id.radioButtonSigned) //체결 탭

        radioUnSigned.setOnClickListener{
            if(radioUnSigned.isChecked)
            {
                radioUnSigned.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                radioSigned.isChecked = false
                radioSigned.setTextColor(ContextCompat.getColor(root.context, R.color.lightgray))

                orderItemList.adapter = activity?.let { tableitemBaseAdapter(it, unSignedItem) }
                setListViewHeightBasedOnChildren(orderItemList, 100)
            }
        }

        radioSigned.setOnClickListener{
            if(radioSigned.isChecked)
            {
                radioSigned.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                radioUnSigned.isChecked = false
                radioUnSigned.setTextColor(ContextCompat.getColor(root.context, R.color.lightgray))

                orderItemList.adapter = activity?.let { tableitemBaseAdapter(it, signedItem) }
                setListViewHeightBasedOnChildren(orderItemList, 100)
            }
        }

        val unSignedCancel = root.findViewById<Button>(R.id.buttonUnSignedCancel)
        unSignedCancel.setOnClickListener{
            OrderAllCancel()
        }

        InitializeControl()

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

                if(serverDataDone)
                {
                    InitializeProfit()
                }

                if(yieldDataDone)
                {
                    InitializeYield()
                }

                if(orderModelDone)
                {
                    InitializeExpertTop3()
                    InitializeExpertItem()
                }

                if(signedDone)
                {
                    InitilizeSignedItem()
                }
            }
        }
        catch (ex : Exception)
        {
            Toast.makeText(this.context,ex.message, Toast.LENGTH_LONG).show()
        }

    }

    override fun onResume() {
        super.onResume()
        /* 화면 갱신시 핸들 재연결 */
        m_nHandle = manager.setHandler(handler as Handler)

        if(AccountList.isEmpty())
        {
            AccountList = getAccountList()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        /* 해당 화면을 사용하지 않을떄 핸들값 삭제 */
        manager.deleteHandler(m_nHandle)
        uiTimer.cancel()
    }

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler(private val activity: HomeFragment) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            if(DataMngr.Handler().procMsgHandler(activity.context, msg.what, msg.obj)) {
                return
            }

            when (msg.what) {

                API_DEFINE.RECEIVE_DATA -> {
                    val lpDp = msg.obj as DataPacket
                    val trcode = lpDp.strTRCode

                    if (trcode!!.contains("CSPAT") || trcode.contains("CFOAT")) {
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
                    //Toast.makeText(this.activity.context,strMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setListViewHeightBasedOnChildren(listView: ListView, offset : Int) {
        val listAdapter = listView.adapter
                ?: // pre-condition
                return

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
            Toast.makeText(this.context, "계좌정보를 가져왔습니다.", Toast.LENGTH_SHORT).show()
        }
        return temp
    }

    fun OrderAllCancel()
    {
        val builder = AlertDialog.Builder(this.context)
        builder.setTitle("미체결 주문 취소")
        builder.setMessage("미체결 된 주문을 전부 취소합니다.")
        builder.setPositiveButton("확인") { dialoginterface: DialogInterface?, i: Int ->

            val strAccount = SelectedAccount
            if (strAccount.isEmpty()) return@setPositiveButton

            val strAccPwd = AccountPW
            if (strAccPwd.isEmpty()) return@setPositiveButton

            for (i in 0 until unSignedModel.count()) {
                // DataMngr를 사용하는 방법입니다.
                val CSPAT00800 = DataMngr.getInstance(manager, "CSPAT00800")!!

                //------------------------------------------------------------------------------------------
                // 입력
                CSPAT00800.writeFieldData(
                    "CSPAT00800InBlock1",
                    "OrgOrdNo    ",
                    unSignedModel[i].원주문번호
                )
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "AcntNo   ", strAccount)
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "InptPwd  ", strAccPwd)
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "IsuNo    ", unSignedModel[i].종목코드)
                CSPAT00800.writeFieldData(
                    "CSPAT00800InBlock1",
                    "OrdQty   ",
                    unSignedModel[i].미체결잔량.toString()
                )

                //------------------------------------------------------------------------------------------
                //   전송
                //   TR 전송 제한으로 인해 조회시 시간을 입력받아 초당전송 제한에 걸리는 문제를 최소화 하기 위해 request에 기능 추가
                //   nLastSec -> -1:즉시전송, 0:초당전송시간이 지난 후에 전송, < 0 : nLastSec초가 지난 후에 전송
                //   fun request( sm : SocketManager, nHandler : Int, bNext: Boolean = false, strContinueKey: String = "", nLaterSec : Int = -1, nTimeOut: Int = 30 ) : Int
                val nRqID = CSPAT00800.request(manager, m_nHandle)

                if (nRqID < 0) {
//            Toast.makeText( activity?.applicationContext, "TR전송실패(" + nRqID + ")", Toast.LENGTH_LONG ).show()
                    return@setPositiveButton
                }

                //------------------------------------------------------------------------------------------
                // 수신
                CSPAT00800.setOnRecvListener(object : DataMngr.OnRecvListener {
                    /**
                     * 조회 응답
                     */
                    override fun onData(dm: DataMngr, sBlockName: String) {

                    }

                    /**
                     * 데이터 메세지
                     */
                    override fun onMsg(
                        dm: DataMngr,
                        sCode: String,
                        sMsg: String,
                        bCriticalError: Boolean
                    ) {

                    }

                    /**
                     * 조회한 서비스가 모두 종료되었을 때
                     */
                    override fun onComplete(dm: DataMngr) {

                    }
                })
            }

        }
        builder.setNeutralButton("취소", null)
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun InitializeProfit()
    {
        tvEstimatedAssets.text      = manager.getCommaValue(serverData.totalestimatedassets) + "원"
        tvMemberCount.text          = manager.getCommaValue(serverData.memberCount) + "명"
        tvAccumulatedProfit.text    = manager.getCommaValue(serverData.profit) + "원"
    }

    private fun InitializeControl()
    {
        try {
            //TODO :  전문가 이름 체크
            tvExpert.text = user.expertname

//            if(autobuy)
//            {
//                btnControlMode.text = "자동"
//                ivExpert.visibility = View.VISIBLE //느낌표 보이기
////                StartAutoControlThread() //TODO : MainActivity로 옮겨야 할수도
//            }
//            else
//            {
                btnControlMode.text = "수동"
                ivExpert.visibility = View.INVISIBLE //느낌표 숨기기
//            }
        }
        catch (ex: Exception)
        {
        }
    }

    private fun InitializeYield()
    {
        if(yieldList.isNotEmpty())
        {
            val yieldtop5item = mutableListOf<TableItemData>()

            for(i in 0 until yieldList.count())
            {
                yieldtop5item.add(TableItemData(yieldList[i].expname, manager.getCommaValue(yieldList[i].orderprice), manager.getCommaValue(yieldList[i].valuation), yieldList[i].yield.toString()))
            }

            yieldtop5listview.adapter = activity?.let { tableitemBaseAdapter(it, yieldtop5item) }
            setListViewHeightBasedOnChildren(yieldtop5listview, 100)
        }
    }

    private fun InitializeExpertTop3()
    {
        //TODO : 전문가추천종목 수정
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
                                manager.getCommaValue(buyOrderModelList[i].orderprice),
                                buyOrderModelList[i].orderdate)
                )
            }
            experttop3listview.adapter = activity?.let { buyItemBaseAdapter(it, buyitem) }
            setListViewHeightBasedOnChildren(experttop3listview, 0)
        }
    }

    private fun InitializeExpertItem()
    {
        if(SelectedAccount.isNotEmpty())
        {
            val expertItem = mutableListOf<TableItemData>()

            if(buyOrderModelList.isNotEmpty())
            {
                for(i in 0 until buyOrderModelList.count())
                {
                    expertItem.add(TableItemData(buyOrderModelList[i].expname,
                            buyOrderModelList[i].ordertype,
                            manager.getCommaValue(buyOrderModelList[i].orderprice),
                            buyOrderModelList[i].orderdate,
                    ))
                }
            }

            expertItemlistview.adapter = activity?.let { tableitemBaseAdapter(it, expertItem) }
            setListViewHeightBasedOnChildren(expertItemlistview, 100)
        }
    }

    private fun InitilizeSignedItem()
    {
        if (radioSigned.isChecked)
        {
            orderItemList.adapter = activity?.let { tableitemBaseAdapter(it, signedItem) }
        }
        else
        {
            orderItemList.adapter = activity?.let { tableitemBaseAdapter( it,unSignedItem)}
        }

        setListViewHeightBasedOnChildren(orderItemList, 100)
    }

//    var isAutoControl : Boolean = false;
//    private fun StartAutoControlThread()
//    {
//        isAutoControl = true;
//        thread(isAutoControl){
//            AutoControlThread()
//        }
//    }
//
//    private fun AutoControlThread()
//    {
//        while (isAutoControl) {
//            AutoOrder()
//            Thread.sleep(100)
//        }
//    }

//    private fun GetSellRatioCount(price : Int) : Int
//    {
//        val 구매가능금액 =  (accountInfo.추정순자산 / 100) * ratioModel.ratio;
//        val 구매가능수량 = (구매가능금액 / price).toInt();
//        return 구매가능수량;
//    }

//    private fun AutoOrder()
//    {
//        for(i in 0 until orderModelList.count())
//        {
//            val ordermodel = orderModelList[i]
//
//            if(ordermodel.ordertype == "매수")
//            {
//                ordermodel.ordercount = GetSellRatioCount(ordermodel.orderprice)
//                requestMaemae(ordermodel.expcode, ordermodel.ordercount.toString(), "2")
//            }
//            else if (ordermodel.ordertype == "매도")
//            {
//                val index : Int = userSellItemData.indexOf(userSellItemData.find { o -> o.종목코드 == ordermodel.expcode })
//
//                var count : Int? = 0
//                if(bulkSell)
//                {
//                    count = userSellItemData[index].잔고수량.toInt()
//                }
//                else
//                {
//                    count = orderModelList.find { o -> o.expcode == ordermodel.expcode }?.ordercount
//                    if(count == 0)
//                    {
//                        count = userSellItemData[index].잔고수량.toInt()
//                    }
//                }
//                requestMaemae(ordermodel.expcode, count.toString(), "1")
//            }
//        }
//    }

//    private fun requestMaemae(code : String , Qty : String, strMaemaeGubun: String) {
//        // 계좌번호(20) , 입력비밀번호(8) , 종목번호(12) , 주문수량(16) , 주문가(13.2) , 매매구분(1) , 호가유형코드(2) , 신용거래코드(3) , 대출일(8) , 주문조건구분(1)
//
//        var strAccount = SelectedAccount
//        var strPass = AccountPW
//        var strJongmok = code
//        var strQty = Qty //주문수량
//        var strDanga = ""
//        val strHogaCode = "03" // 시장가
//
//        strAccount = manager.makeSpace(strAccount, 20)
//        strJongmok = manager.makeSpace(strJongmok, 12)
//        strQty = manager.makeZero(strQty, 16)
//
//        if (strDanga.isEmpty()) {
//            strDanga = "0"
//        }
//
//        strDanga = String.format("%.2f", java.lang.Double.parseDouble(strDanga))
//        strDanga = manager.makeZero(strDanga, 13)
//
//        strPass = manager.makeSpace(strPass,8)
//
//        val strInBlock =
//            strAccount + strPass + strJongmok + strQty + strDanga + strMaemaeGubun + strHogaCode + "000" + "        " + "0"
//        //int nRqID = manager.requestDataAccount(m_nHandle, "CSPAT00600", strInBlock, 0, 'B', "", false, false, false, false, "", 30);
//        val nRqID = manager.requestData(m_nHandle, "CSPAT00600", strInBlock, false, "", 30)
//    }

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