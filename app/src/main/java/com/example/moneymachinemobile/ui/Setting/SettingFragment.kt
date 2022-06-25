package com.example.moneymachinemobile.ui.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moneymachinemobile.*
import com.example.moneymachinemobile.data.user
import com.example.moneymachinemobile.ui.*
import java.time.format.DateTimeFormatter

class SettingFragment : Fragment() {

    companion object {
        fun newInstance() = SettingFragment()
    }

    private lateinit var viewModel: SettingViewModel

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_setting, container, false)

        val tvName = root.findViewById<TextView>(R.id.tvUserName)
        tvName.text = user.name

        val tvPaidDate = root.findViewById<TextView>(R.id.tvPaidDate)
        tvPaidDate.text = user.paiddate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val stockButton = root.findViewById<Button>(R.id.ButtonStock)
        stockButton.setOnClickListener {

            MySharedPreferences.setUserAutoStockLogin(root.context, false)
            ConnectStock()
        }

        val changePWButton = root.findViewById<Button>(R.id.ButtonChangePW)
        changePWButton.setOnClickListener {
            val changePWDlg = changePWDialog(root.context)
            changePWDlg.start()
        }

        val autobuyRadioButton = root.findViewById<RadioButton>(R.id.radioButtonAutobuy)
        val manualbuyRadioButton = root.findViewById<RadioButton>(R.id.radioButtonManualBuy)

        val bulkRadioButton = root.findViewById<RadioButton>(R.id.radioButtonAllSell)
        val installmentRadioButton = root.findViewById<RadioButton>(R.id.radioButtonPartSell)

        if(MySharedPreferences.getUserAutoBuy(root.context))
        {
            autobuyRadioButton.isChecked = true
            manualbuyRadioButton.isChecked = false
        }
        else
        {
            autobuyRadioButton.isChecked = false
            manualbuyRadioButton.isChecked = true
        }

        if(MySharedPreferences.getUserBulkSell(root.context))
        {
            bulkRadioButton.isChecked = true
            installmentRadioButton.isChecked = false
        }
        else
        {
            bulkRadioButton.isChecked = false
            installmentRadioButton.isChecked = true
        }

        autobuyRadioButton.setOnClickListener{

            var agree :Boolean

            val agreeDlg = this.context?.let { autobuyDialog(it) }
            agreeDlg?.setOnOKClickedListener{ content ->
                agree = content
                if(agree)
                {
                    MySharedPreferences.setUserAutoBuy(root.context, true)
//                    autobuyRadioButton.isChecked = true
//                    manualbuyRadioButton.isChecked = false
                }
                else
                {
                    MySharedPreferences.setUserAutoBuy(root.context, false  )
//                    autobuyRadioButton.isChecked = false
//                    manualbuyRadioButton.isChecked = true
                }
            }
            agreeDlg?.start()
        }

        manualbuyRadioButton.setOnClickListener{
            var agree :Boolean

            val agreeDlg = this.context?.let { autobuyDialog(it) }
            agreeDlg?.setOnOKClickedListener{ content ->
                agree = content
                if(agree)
                {
                    MySharedPreferences.setUserAutoBuy(root.context, false  )
//                    autobuyRadioButton.isChecked = false
//                    manualbuyRadioButton.isChecked = true
                }
                else
                {
                    MySharedPreferences.setUserAutoBuy(root.context, true)
//                    autobuyRadioButton.isChecked = true
//                    manualbuyRadioButton.isChecked = false
                }
            }
            agreeDlg?.start()
        }

        autobuyRadioButton.setOnClickListener{
            var agree :Boolean

            val agreeDlg = this.context?.let { autobuyDialog(it) }
            agreeDlg?.setOnOKClickedListener{ content ->
                agree = content
                if(agree)
                {
                    MySharedPreferences.setUserAutoBuy(root.context, true)
//                    autobuyRadioButton.isChecked = true
//                    manualbuyRadioButton.isChecked = false
                }
                else
                {
                    MySharedPreferences.setUserAutoBuy(root.context, false  )
//                    autobuyRadioButton.isChecked = false
//                    manualbuyRadioButton.isChecked = true
                }
            }
            agreeDlg?.start()
        }

        bulkRadioButton.setOnClickListener{
//            MySharedPreferences.setUserBulkSell(root.context, true)
        }
        installmentRadioButton.setOnClickListener{
//            MySharedPreferences.setUserBulkSell(root.context, false)
        }

        val serviceCenterButton = root.findViewById<Button>(R.id.ButtonServiceCenter)
        serviceCenterButton.setOnClickListener {

            val intent = Intent(this.context, WebViewActivity::class.java)
            intent.putExtra("url","https://helpsite.kr")
            startActivity(intent)
        }

        val kakaoButton = root.findViewById<Button>(R.id.ButtonKAKAO)
        kakaoButton.setOnClickListener {
            val intent = Intent(this.context, WebViewActivity::class.java)
            intent.putExtra("url","https://open.kakao.com/o/sCBRfD7c")
            startActivity(intent)
        }

        val logoutButton = root.findViewById<Button>(R.id.ButtonLogOut)
        logoutButton.setOnClickListener {
            MySharedPreferences.setUserAutoLogin(root.context, false)
            MySharedPreferences.setUserAutoStockLogin(root.context, false)
            MySharedPreferences.setUserAgreement(root.context, false)
            MySharedPreferences.setUserStockCert(root.context, "")
            MySharedPreferences.setUserStockCertPW(root.context, "")
            MySharedPreferences.setUserAccountPW(root.context, "")

            LogOut()
        }

        val autoLoginSW = root.findViewById<Switch>(R.id.switchAutoLogin)
        if (MySharedPreferences.getUserAutoLogin(root.context))
        {
            autoLoginSW.isChecked = true
        }
        autoLoginSW.setOnClickListener{
            if(autoLoginSW.isChecked){
                MySharedPreferences.setUserAutoLogin(root.context, true)
            }
            else{
                MySharedPreferences.setUserAutoLogin(root.context, false)
            }
        }

        val autoStockLoginSW = root.findViewById<Switch>(R.id.switchStockAutoLogin)
        if (MySharedPreferences.getUserAutoStockLogin(root.context))
        {
            autoStockLoginSW.isChecked = true
        }
        autoStockLoginSW.setOnClickListener{
            if(autoStockLoginSW.isChecked){
                MySharedPreferences.setUserAutoStockLogin(root.context, true)
            }
            else{
                MySharedPreferences.setUserAutoStockLogin(root.context, false)
                MySharedPreferences.setUserAgreement(root.context, false)
            }
        }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingViewModel::class.java)
        // TODO: Use the ViewModel
    }

    private fun LogOut()
    {
        startActivity(Intent(this.context, LoginActivity::class.java))
    }

    private fun ConnectStock()
    {
        startActivity(Intent(this.context, LoginStockActivity::class.java))
    }
}