package com.frank.frecyclerview

import android.content.Context
import android.view.View
import android.widget.Toast
import com.frank.librecyclerview.CommonRecyclerAdapter
import com.frank.librecyclerview.CommonViewHolder

/**
 * Created by Frank on 2018/8/3.
 * Email: frankchoochina@gmail.com
 * Version: 1.0
 * Description:
 */
class TestAdapter(context: Context, dataSet: MutableList<String>)
    : CommonRecyclerAdapter<String>(context, dataSet) {

    override fun getLayoutRes(data: String, position: Int): Int = R.layout.recycler_item_test

    override fun convert(holder: CommonViewHolder, data: String, position: Int) {
        holder.setText(R.id.tv_item_desc, data)
    }

    override fun onItemClick(v: View, position: Int) {
        Toast.makeText(v.context, "$position is clicked.", Toast.LENGTH_SHORT).show()
    }
}