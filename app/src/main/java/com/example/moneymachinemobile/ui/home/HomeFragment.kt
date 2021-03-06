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

        radioUnSigned = root.findViewById(R.id.radioButtonUnSigned) //????????? ???
        radioSigned = root.findViewById(R.id.radioButtonSigned) //?????? ???

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
        /* ?????? ????????? ?????? ????????? */
        m_nHandle = manager.setHandler(handler as Handler)

        if(AccountList.isEmpty())
        {
            AccountList = getAccountList()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        /* ?????? ????????? ???????????? ????????? ????????? ?????? */
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
                // TR?????? ???
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

                        // ??????????????????
                        if (lpRp.strBCCode == "SC0") {
                            Toast.makeText(this.activity.context,"????????? ?????????????????????.", Toast.LENGTH_LONG).show()
                            processSC0(pData)

                            // ??????????????????
                        } else if (lpRp.strBCCode == "SC1") {
                            processSC1(pData)

                            // ??????????????????
                        } else if (lpRp.strBCCode == "SC2") {
                            processSC2(pData)

                            // ??????????????????
                        } else if (lpRp.strBCCode == "SC3") {
                            processSC3(pData)

                            // ??????????????????
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
                // ???????????? ??????
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
            temp.add(tempList?.get(i)?.get(0) as String)  // ????????????
//            temp.add(tempList?.get(i)?.get(1) as String)   // ?????????
//            temp.add(tempList?.get(i)?.get(2) as String)  // ??????????????????
//            temp.add(tempList?.get(i)?.get(3) as String)  // ??????????????????
        }

        if(temp.isNotEmpty())
        {
            Toast.makeText(this.context, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show()
        }
        return temp
    }

    fun OrderAllCancel()
    {
        val builder = AlertDialog.Builder(this.context)
        builder.setTitle("????????? ?????? ??????")
        builder.setMessage("????????? ??? ????????? ?????? ???????????????.")
        builder.setPositiveButton("??????") { dialoginterface: DialogInterface?, i: Int ->

            val strAccount = SelectedAccount
            if (strAccount.isEmpty()) return@setPositiveButton

            val strAccPwd = AccountPW
            if (strAccPwd.isEmpty()) return@setPositiveButton

            for (i in 0 until unSignedModel.count()) {
                // DataMngr??? ???????????? ???????????????.
                val CSPAT00800 = DataMngr.getInstance(manager, "CSPAT00800")!!

                //------------------------------------------------------------------------------------------
                // ??????
                CSPAT00800.writeFieldData(
                    "CSPAT00800InBlock1",
                    "OrgOrdNo    ",
                    unSignedModel[i].???????????????
                )
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "AcntNo   ", strAccount)
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "InptPwd  ", strAccPwd)
                CSPAT00800.writeFieldData("CSPAT00800InBlock1", "IsuNo    ", unSignedModel[i].????????????)
                CSPAT00800.writeFieldData(
                    "CSPAT00800InBlock1",
                    "OrdQty   ",
                    unSignedModel[i].???????????????.toString()
                )

                //------------------------------------------------------------------------------------------
                //   ??????
                //   TR ?????? ???????????? ?????? ????????? ????????? ???????????? ???????????? ????????? ????????? ????????? ????????? ?????? ?????? request??? ?????? ??????
                //   nLastSec -> -1:????????????, 0:????????????????????? ?????? ?????? ??????, < 0 : nLastSec?????? ?????? ?????? ??????
                //   fun request( sm : SocketManager, nHandler : Int, bNext: Boolean = false, strContinueKey: String = "", nLaterSec : Int = -1, nTimeOut: Int = 30 ) : Int
                val nRqID = CSPAT00800.request(manager, m_nHandle)

                if (nRqID < 0) {
//            Toast.makeText( activity?.applicationContext, "TR????????????(" + nRqID + ")", Toast.LENGTH_LONG ).show()
                    return@setPositiveButton
                }

                //------------------------------------------------------------------------------------------
                // ??????
                CSPAT00800.setOnRecvListener(object : DataMngr.OnRecvListener {
                    /**
                     * ?????? ??????
                     */
                    override fun onData(dm: DataMngr, sBlockName: String) {

                    }

                    /**
                     * ????????? ?????????
                     */
                    override fun onMsg(
                        dm: DataMngr,
                        sCode: String,
                        sMsg: String,
                        bCriticalError: Boolean
                    ) {

                    }

                    /**
                     * ????????? ???????????? ?????? ??????????????? ???
                     */
                    override fun onComplete(dm: DataMngr) {

                    }
                })
            }

        }
        builder.setNeutralButton("??????", null)
        builder.show()
    }

    @SuppressLint("SetTextI18n")
    private fun InitializeProfit()
    {
        tvEstimatedAssets.text      = manager.getCommaValue(serverData.totalestimatedassets) + "???"
        tvMemberCount.text          = manager.getCommaValue(serverData.memberCount) + "???"
        tvAccumulatedProfit.text    = manager.getCommaValue(serverData.profit) + "???"
    }

    private fun InitializeControl()
    {
        try {
            //TODO :  ????????? ?????? ??????
            tvExpert.text = user.expertname

//            if(autobuy)
//            {
//                btnControlMode.text = "??????"
//                ivExpert.visibility = View.VISIBLE //????????? ?????????
////                StartAutoControlThread() //TODO : MainActivity??? ????????? ?????????
//            }
//            else
//            {
                btnControlMode.text = "??????"
                ivExpert.visibility = View.INVISIBLE //????????? ?????????
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
        //TODO : ????????????????????? ??????
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
//        val ?????????????????? =  (accountInfo.??????????????? / 100) * ratioModel.ratio;
//        val ?????????????????? = (?????????????????? / price).toInt();
//        return ??????????????????;
//    }

//    private fun AutoOrder()
//    {
//        for(i in 0 until orderModelList.count())
//        {
//            val ordermodel = orderModelList[i]
//
//            if(ordermodel.ordertype == "??????")
//            {
//                ordermodel.ordercount = GetSellRatioCount(ordermodel.orderprice)
//                requestMaemae(ordermodel.expcode, ordermodel.ordercount.toString(), "2")
//            }
//            else if (ordermodel.ordertype == "??????")
//            {
//                val index : Int = userSellItemData.indexOf(userSellItemData.find { o -> o.???????????? == ordermodel.expcode })
//
//                var count : Int? = 0
//                if(bulkSell)
//                {
//                    count = userSellItemData[index].????????????.toInt()
//                }
//                else
//                {
//                    count = orderModelList.find { o -> o.expcode == ordermodel.expcode }?.ordercount
//                    if(count == 0)
//                    {
//                        count = userSellItemData[index].????????????.toInt()
//                    }
//                }
//                requestMaemae(ordermodel.expcode, count.toString(), "1")
//            }
//        }
//    }

//    private fun requestMaemae(code : String , Qty : String, strMaemaeGubun: String) {
//        // ????????????(20) , ??????????????????(8) , ????????????(12) , ????????????(16) , ?????????(13.2) , ????????????(1) , ??????????????????(2) , ??????????????????(3) , ?????????(8) , ??????????????????(1)
//
//        var strAccount = SelectedAccount
//        var strPass = AccountPW
//        var strJongmok = code
//        var strQty = Qty //????????????
//        var strDanga = ""
//        val strHogaCode = "03" // ?????????
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
        //?????? 1. res ????????? TR????????? ?????? *.res ????????? ?????? ??????
        map = manager.getOutBlockDataFromByte(TRName, blockname, pData);
        pArray = manager.getAttributeFromByte(TRName, blockname, pData); // attribute
        */

        //??????2. ??????????????? TR????????? ?????? ??????(.kt, .java ... *?????? ??????????????? TRCODE.kt??? )??? ???????????? ??????.
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
        // OutBlock??? ?????????
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

    // ????????????
    private fun processSC1(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
            16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // ????????????
    private fun processSC2(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
            16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // ????????????
    private fun processSC3(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
            16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }

    // ????????????
    private fun processSC4(pData: ByteArray) {

        val nColLen = intArrayOf(10,11,8,6,1,1,1,3,8,3,16,2,3,9,16,12,12,3,3,8,1,9,4,1,1,4,4,6,1,18,2,2,2,1,4,4,41,2,2,2,3,11,9,40,12,40,10,10,10,16,13,16,13,16,16,16,16,4,10,1,2,
            16,9,12,1,16,16,16,16,13,16,12,12,1,2,3,2,2,8,3,20,3,9,3,20,1,2,7,9,16,16,16,16,16,16,16,16,16,6,20,10,10,10,10,10,16,1,6,1,1,9,9,16,16,16,16,16,16,16,16,13,16,16,16,16,16,16,16,16,16)
        val bAttributeInData = false
        val strArray = manager.getDataFromByte(pData, nColLen, bAttributeInData)
        val nRowCount = strArray?.size
        val nColCount = nColLen.size
    }
}