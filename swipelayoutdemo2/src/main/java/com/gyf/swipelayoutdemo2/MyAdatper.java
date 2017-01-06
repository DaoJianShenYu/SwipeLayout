package com.gyf.swipelayoutdemo2;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MyAdatper extends BaseAdapter {

	
	public MyAdatper(Context context) {
		super();
		this.context = context;
		
		openedItems = new ArrayList<SwipeLayout>();
	}

	private Context context;
	private ArrayList<SwipeLayout> openedItems;

	@Override
	public int getCount() {
		return Cheeses.NAMES.length;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if(convertView == null){
			view = View.inflate(context, R.layout.item_list, null);
		}else {
			view = convertView;
		}
		
		SwipeLayout sl = (SwipeLayout)view;
		
		sl.setOnSwipeListener(new SwipeLayout.OnSwipeListener() {

			@Override
			public void onClose(SwipeLayout layout) {
				openedItems.remove(layout);
			}

			@Override
			public void onOpen(SwipeLayout layout) {
				openedItems.add(layout);
			}

			@Override
			public void onStartOpen(SwipeLayout layout) {
				closeAllItem();

			}

			@Override
			public void onStartClose(SwipeLayout layout) {
			}
		
		});
		
		TextView tv_name = (TextView) view.findViewById(R.id.tv_name);
		tv_name.setText(Cheeses.NAMES[position]);
		
		return view;
	}

	public void closeAllItem() {
		// 关闭所有已经打开的条目
		for (int i = 0; i < openedItems.size(); i++) {
            openedItems.get(i).close(true);
        }

		openedItems.clear();
	}

}
