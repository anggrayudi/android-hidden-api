package com.anggrayudi.hiddenapi.sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

/**
 * Created by Anggrayudi on 11/03/2016.
 */
class Adapter(private val models: List<Model> = emptyList()) : BaseAdapter() {

    private class ViewHolder(
        val source: TextView,
        val result: TextView,
        val desc: TextView
    )

    override fun getCount() = models.size

    override fun getItem(position: Int) = models[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.list_layout, parent, false)
            view.tag = ViewHolder(
                view.findViewById(R.id.source),
                view.findViewById(R.id.result),
                view.findViewById(R.id.desc)
            )
        }
        val holder = view!!.tag as ViewHolder
        val (source, result, description) = models[position]
        holder.source.text = source
        holder.result.visibility = if (result.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.result.text = "Result = $result"
        holder.desc.visibility = if (description.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.desc.text = description
        return view
    }
}