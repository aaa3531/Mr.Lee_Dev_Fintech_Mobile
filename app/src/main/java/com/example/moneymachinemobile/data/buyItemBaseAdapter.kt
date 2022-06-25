package com.example.moneymachinemobile.data

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.moneymachinemobile.R
import com.example.moneymachinemobile.ui.buysellDialog

class buyItemBaseAdapter(context: Context, item: MutableList<UserBuyItemData>) : BaseAdapter(){
    private val mContext = context
    private val mItem = item

    @SuppressLint("CutPasteId")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        lateinit var viewHolder : ViewHolder
        var view = convertView
        if (view == null){
            viewHolder = ViewHolder()
            view = LayoutInflater.from(mContext).inflate(R.layout.list_buyitem,parent,false)
            viewHolder.button = view.findViewById(R.id.buttonBuy)

            viewHolder.tv종목명 = view.findViewById(R.id.tvBuyName)
            viewHolder.tv추천가 = view.findViewById(R.id.tvBuyPrice)
            viewHolder.tv매수신호시간 = view.findViewById(R.id.tvBuyTime)

            view.tag = viewHolder
            viewHolder.tv종목명.text = mItem[position].종목명
            viewHolder.tv추천가.text = mItem[position].추천가
            viewHolder.tv매수신호시간.text = mItem[position].매수신호

            val bu : Button = view.findViewById(R.id.buttonBuy)
            bu.setOnClickListener{

                val dlg = buysellDialog(mContext,"매수",mItem[position],null)
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
        lateinit var tv추천가 : TextView
        lateinit var tv매수신호시간 : TextView
        lateinit var button : Button
    }
}

