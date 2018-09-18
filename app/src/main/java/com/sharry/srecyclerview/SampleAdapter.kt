package com.sharry.srecyclerview

import android.content.Context
import android.view.View
import android.widget.Toast
import com.sharry.librecyclerview.SRecyclerAdapter
import com.sharry.librecyclerview.SViewHolder

/**
 * Created by Sharry on 2018/8/3.
 * Email: SharryChooCHN@Gmail.com
 * Version: 1.0
 * Description:
 */
class SampleAdapter(context: Context, dataSet: MutableList<String>)
    : SRecyclerAdapter<String>(context, dataSet) {

    override fun getLayoutResId(data: String, position: Int): Int = R.layout.recycler_item_test

    override fun convert(holder: SViewHolder, data: String, position: Int) {
        holder.setText(R.id.tv_item_desc, data)
    }

    override fun onItemClick(v: View, position: Int) {
        Toast.makeText(v.context, "$position is clicked.", Toast.LENGTH_SHORT).show()
    }
}