package ioio.examples.hello;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {
	private Context context;
    private String[] texts;
    
    public MyAdapter(Context context) {
        this.context = context;
    }

	public MyAdapter(Context context2, String[] texts2) {
		this(context2);
		this.texts = texts2;
	}

	public int getCount() {
        return 9;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (convertView == null) {
            tv = new TextView(context);
            tv.setLayoutParams(new GridView.LayoutParams(85, 85));
        }
        else {
            tv = (TextView) convertView;
        }

            tv.setText(texts[position]);
        return tv;
    }

	
}
