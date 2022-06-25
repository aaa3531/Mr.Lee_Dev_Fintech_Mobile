package com.example.moneymachinemobile.ui.account

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.ebest.api.CommonFunction
import com.ebest.api.DataPacket
import com.ebest.api.ReleasePacket
import com.ebest.api.SocketManager
import com.example.moneymachinemobile.data.API_DEFINE
import com.example.moneymachinemobile.ApplicationManager
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.data.*
import com.example.moneymachinemobile.data.sellItemBaseAdapter
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timer

class AccountFragment : Fragment() {

    companion object {
        fun newInstance() = AccountFragment()
    }

    private lateinit var account회원명 : TextView
    private lateinit var account추정자산총액 : TextView
    private lateinit var account매입금액 : TextView
    private lateinit var account평가금액 : TextView
    private lateinit var account평가손익 : TextView
    private lateinit var account확정손익 : TextView

    private var m_nHandle = -1
    private var handler: ProcMessageHandler? = null
    lateinit internal var manager: SocketManager

    lateinit var sellItemlistview : ListView
    var sellitem = mutableListOf<UserSellItemData>()

    private lateinit var viewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_account, container, false)

        handler = ProcMessageHandler(this)
        manager = (activity?.application as ApplicationManager).getSockInstance()

        sellItemlistview = root.findViewById(R.id.itemList2)
        account회원명 = root.findViewById(R.id.tv_회원명)
        account추정자산총액 = root.findViewById(R.id.tv_추정자산총액)
        account매입금액 = root.findViewById(R.id.tv매입금액)
        account평가금액 = root.findViewById(R.id.tv평가금액)
        account평가손익 = root.findViewById(R.id.tv평가손익)
        account확정손익 = root.findViewById(R.id.tv확정손익)

        val spinnerAccount =  root.findViewById<Spinner>(R.id.spinnerAccount)
        spinnerAccount.adapter = ArrayAdapter(root.context, android.R.layout.simple_spinner_dropdown_item, AccountList)
        spinnerAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
            ) {
//                val t = (parent.getChildAt(0) as TextView)
//                t.setTextSize(
//                        TypedValue.COMPLEX_UNIT_PX,
//                        ResourceManager.calcFontSize(t.textSize.toInt())
//                )
                SelectedAccount = spinnerAccount.getItemAtPosition(position) as String

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
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
                    InitialAccountInfo()
                    InitialUserSellItem()
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
        viewModel = ViewModelProvider(this).get(AccountViewModel::class.java)
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

    fun setListViewHeightBasedOnChildren(listView:ListView, offset : Int) {
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

    @SuppressLint("HandlerLeak")
    internal inner class ProcMessageHandler(private val activity: AccountFragment) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {

                API_DEFINE.RECEIVE_DATA -> {
                    val lpDp = msg.obj as DataPacket
                }

                // TR조회 끝
                API_DEFINE.RECEIVE_RELEASE -> {
                    val lpDp = msg.obj as ReleasePacket

                    lpDp.nRqID
                    lpDp.strTrCode
                }

                API_DEFINE.RECEIVE_ERROR -> {
                    val strMsg = msg.obj as String
                    Toast.makeText(this.activity.context,strMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun InitialAccountInfo()
    {
        account회원명.text = user.name
        account추정자산총액.text = manager.getCommaValue(accountInfo.추정순자산)
        account매입금액.text = manager.getCommaValue(accountInfo.매입금액)
        account평가금액.text = manager.getCommaValue(accountInfo.평가금액)
        account평가손익.text = manager.getCommaValue(accountInfo.평가손익)
        account확정손익.text = manager.getCommaValue(accountInfo.확정손익)
    }

    private fun InitialUserSellItem()
    {
        if(userSellItemData.isNotEmpty())
        {
            if(sellitem.isNotEmpty()){
                sellitem.clear()
            }

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

            sellItemlistview.adapter = activity?.let { sellItemBaseAdapter(it,sellitem) }
            setListViewHeightBasedOnChildren(sellItemlistview, 0)
        }
        else
        {
            if(sellitem.isNotEmpty()){
                sellitem.clear()
            }

            sellItemlistview.adapter = activity?.let { sellItemBaseAdapter(it,sellitem) }
            setListViewHeightBasedOnChildren(sellItemlistview, 0)
        }
    }
}