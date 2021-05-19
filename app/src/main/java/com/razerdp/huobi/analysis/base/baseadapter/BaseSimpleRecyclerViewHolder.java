package com.razerdp.huobi.analysis.base.baseadapter;

import android.view.View;

import androidx.annotation.NonNull;


/**
 * Created by 大灯泡 on 2019/7/30.
 * 懒人必备viewholder
 */
public abstract class BaseSimpleRecyclerViewHolder<T> extends BaseRecyclerViewHolder<T> {


    public BaseSimpleRecyclerViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract int inflateLayoutResourceId();

}
