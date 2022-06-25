package com.example.moneymachinemobile.data

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.ui.buysellDialog

class sellItemBaseAdapter (context: Context, item: MutableList<UserSellItemData>) : BaseAdapter(){
    private val mContext = context
    private val mItem = item

    @SuppressLint("CutPasteId")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        lateinit var viewHolder : ViewHolder
        var view = convertView
        if (view == null){
            viewHolder = ViewHolder()
            view = LayoutInflater.from(mContext).inflate(R.layout.list_sellitem,parent,false)

            viewHolder.button = view.findViewById(R.id.buttonSell)

            viewHolder.tv종목명 = view.findViewById(R.id.tvSell종목명)
            viewHolder.tv잔고수량 = view.findViewById(R.id.tvSell잔고수량)
            viewHolder.tv매입금액 = view.findViewById(R.id.tvSell매입금액)
            viewHolder.tv평가금액 = view.findViewById(R.id.tvSell평가금액)
            viewHolder.tv평가손익 = view.findViewById(R.id.tvSell평가손익)
            viewHolder.tv수익률 = view.findViewById(R.id.tvSell수익률)
            viewHolder.tv매도신호시간 = view.findViewById(R.id.tvSell신호시간)

            view.tag = viewHolder
            viewHolder.tv종목명.text = mItem[position].종목명
            viewHolder.tv잔고수량.text = mItem[position].잔고수량
            viewHolder.tv매입금액.text = mItem[position].매입금액
            viewHolder.tv평가금액.text = mItem[position].평가금액
            viewHolder.tv평가손익.text = mItem[position].평가손익
            viewHolder.tv수익률.text = mItem[position].수익률
            viewHolder.tv매도신호시간.text = mItem[position].매도신호

            val bu : Button = view.findViewById(R.id.buttonSell)
            bu.setOnClickListener{

                val dlg = buysellDialog(mContext,"매도", null, mItem[position])
                dlg.start()
            }

            return view
        }else{
            viewHolder = view.tag as ViewHolder
        }
        return  view
    }

    override fun getItem(position: Int) = mItem[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = mItem.size

    inner class ViewHolder{
        lateinit var tv종목명 : TextView
        lateinit var tv잔고수량 : TextView
        lateinit var tv매입금액 : TextView
        lateinit var tv평가금액: TextView
        lateinit var tv평가손익 : TextView
        lateinit var tv수익률 : TextView
        lateinit var tv매도신호시간 : TextView
        lateinit var button : Button
    }
}

