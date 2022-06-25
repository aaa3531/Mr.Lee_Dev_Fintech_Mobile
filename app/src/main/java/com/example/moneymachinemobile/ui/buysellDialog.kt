package com.example.moneymachinemobile.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.ebest.api.*
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.ApplicationManager
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.*

class buysellDialog(context: Context, gubun: String, buyData: UserBuyItemData?, sellData: UserSellItemData?){
    private val dlg = Dialog(context)   //부모 액티비티의 context 가 들어감
    private  var mGubun = gubun
    private  var mBuyData = buyData
    private  var mSellData = sellData
    private lateinit var manager: SocketManager
    private val con = context

    lateinit var etQty : EditText

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)   //타이틀바 제거
        dlg.setContentView(R.layout.dialog_buysell)     //다이얼로그에 사용할 xml 파일을 불러옴
        dlg.setCancelable(false)    //다이얼로그의 바깥 화면을 눌렀을 때 다이얼로그가 닫히지 않도록 함

        val tvAccount = dlg.findViewById<TextView>(R.id.tvAccount)
        val tvCode = dlg.findViewById<TextView>(R.id.tvCode)
        val tvName = dlg.findViewById<TextView>(R.id.tvName)
        etQty = dlg.findViewById<EditText>(R.id.EditTextOrderQty)
        val gubun = dlg.findViewById<TextView>(R.id.tvGubun)
        gubun.text = mGubun

        if(mGubun == "매도")
        {
            gubun.setBackgroundColor(Color.BLUE)
            tvAccount.text = mSellData?.계좌
            val tmpCode = mSellData?.종목코드?.replace(" ","")
            tvCode.text = tmpCode
            val tmpName = mSellData?.종목명?.replace(" ", "")
            tvName.text = tmpName
            etQty.setText(mSellData?.잔고수량)
        }
        else
        {
            tvAccount.text = mBuyData?.계좌
            val tmpCode = mBuyData?.종목코드?.replace(" ","")
            tvCode.text = tmpCode
            val tmpName = mBuyData?.종목명?.replace(" ", "")
            tvName.text = tmpName
        }

        val btnOK : Button = dlg.findViewById(R.id.button3)
        btnOK.setOnClickListener {
            Order()
        }

        val btnCancel : Button = dlg.findViewById(R.id.button4)
        btnCancel.setOnClickListener {
            Toast.makeText(dlg.context,"주문이 취소되었습니다.", Toast.LENGTH_LONG).show()
            dlg.dismiss()
        }

        dlg.show()

        m_handler = ProcMessageHandler()
        manager = (this.con.applicationContext as ApplicationManager).getSockInstance()

        m_nHandle = manager.setHandler(m_handler as Handler)
    }

    private fun Order(){

        if(mGubun =="매도")
        {
            requestMaemae("1")
        }
        else
        {
            requestMaemae("2")
        }

    }

    private fun requestMaemae(strMaemaeGubun: String) {
        // 계좌번호(20) , 입력비밀번호(8) , 종목번호(12) , 주문수량(16) , 주문가(13.2) , 매매구분(1) , 호가유형코드(2) , 신용거래코드(3) , 대출일(8) , 주문조건구분(1)

        var strAccount = SelectedAccount
        var strPass = AccountPW
        var strJongmok: String //종목코드
        var strQty = etQty.text.toString()
        var strDanga = ""
        val strHogaCode = "03" // 시장가

        if(strMaemaeGubun == "1")
        {
            strJongmok = mSellData?.종목코드.toString()
        }
        else if (strMaemaeGubun =="2")
        {
            strJongmok = mBuyData?.종목코드.toString()
        }
        else
        {
            return
        }

        strAccount = manager.makeSpace(strAccount, 20)
        strJongmok = manager.makeSpace(strJongmok, 12)
        strQty = manager.makeZero(strQty, 16)

        if (strDanga.isEmpty()) {
            strDanga = "0"
        }

        strDanga = String.format("%.2f", java.lang.Double.parseDouble(strDanga))
        strDanga = manager.makeZero(strDanga, 13)


        strPass = manager.makeSpace(strPass,8)

        //manager.setHeaderInfo(1, "1");

        val strInBlock =
            strAccount + strPass + strJongmok + strQty + strDanga + strMaemaeGubun + strHogaCode + "000" + "        " + "0"
        //int nRqID = manager.requestDataAccount(m_nHandle, "CSPAT00600", strInBlock, 0, 'B', "", false, false, false, false, "", 30);
        val nRqID = manager.requestData(m_nHandle, "CSPAT00600", strInBlock, false, "", 30)
    }

    @SuppressLint("HandlerLeak")
    private inner class ProcMessageHandler : Handler() {
        override fun handleMessage(msg: Message) {

            val msg_type = msg.what
            when (msg_type) {
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
                    }
                    else if (lpRp.strBCCode == "SC0" || lpRp.strBCCode == "SC1" || lpRp.strBCCode == "SC2" || lpRp.strBCCode == "SC3" || lpRp.strBCCode == "SC4") {
                        val pData = lpRp.pData
                        var nLen = pData!!.size

                        // 주식주문접수
                        if (lpRp.strBCCode == "SC0") {
//                            Toast.makeText(dlg.context,"주문이 완료되었습니다.", Toast.LENGTH_LONG).show()

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
                    //val strMsg = lpMp.strTRCode + " " + lpMp.strMsgCode + lpMp.strMessageData
                    if(lpMp.strMessageData?.contains("완료되었습니다") == true)
                    {
                        var agree :Boolean

                        val subdlg = orderDoneDialog(dlg.context)
                        subdlg.setOnOKClickedListener{ content ->
                            agree = content
                            if(agree)
                            {
                                dlg.dismiss()
                            }
                        }
                        subdlg.start()
                    }
                    else
                    {
                    Toast.makeText(dlg.context,lpMp.strMessageData,Toast.LENGTH_LONG).show()
                    }
                }
                // 일반적인 에러
                API_DEFINE.RECEIVE_ERROR -> {
                    val strMsg = msg.obj as String
                    Toast.makeText(dlg.context,strMsg,Toast.LENGTH_LONG).show()
                }

                // 접속종료 또는 재연결
//                -3, 5 -> run { mainView!!.onMessage(msg) }

                else -> {
                }
            }
        }

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


    companion object {
        private var m_nHandle = -1
        private var m_handler: ProcMessageHandler? = null
        private var m_strJongmokCode = ""
    }
}

