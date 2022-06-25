package com.example.moneymachinemobile.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.moneymachinemobile.R

class tableitemBaseAdapter (context: Context, item: MutableList<TableItemData>) : BaseAdapter() {
    private val mContext = context
    private val mItem = item

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        lateinit var viewHolder : ViewHolder
        var view = convertView
        if (view == null){
            viewHolder = ViewHolder()
            view = LayoutInflater.from(mContext).inflate(R.layout.list_tableitem,parent,false)
            view.tag = viewHolder

            viewHolder.textView1 = view.findViewById(R.id.tableItem1)
            viewHolder.textView1.text = mItem[position].item1

            viewHolder.textView2 = view.findViewById(R.id.tableItem2)
            viewHolder.textView2.text = mItem[position].item2

            viewHolder.textView3 = view.findViewById(R.id.tableItem3)
            viewHolder.textView3.text = mItem[position].item3

            viewHolder.textView4 = view.findViewById(R.id.tableItem4)
            viewHolder.textView4.text = mItem[position].item4

            return view
        }
        else{
            viewHolder = view.tag as ViewHolder
        }
        return  view
    }

    override fun getItem(position: Int) = mItem[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = mItem.size

    inner class ViewHolder{
        lateinit var textView1 : TextView
        lateinit var textView2 : TextView
        lateinit var textView3 : TextView
        lateinit var textView4 : TextView
        lateinit var button : Button
    }
}