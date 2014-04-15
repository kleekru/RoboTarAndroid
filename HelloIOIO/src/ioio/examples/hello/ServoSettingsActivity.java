package ioio.examples.hello;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.os.Build;

public class ServoSettingsActivity extends ActionBarActivity {
	ArrayAdapter adapter;
	MyAdapter ma;
	Object[] list = { 1, 2 };
    String[] texts = {"aada", "bbdb", "ccc", "ddd", "eee", "fff", "eee", "hhh", "iii"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_servo_settings);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		GridView gridview = (GridView) findViewById(R.id.gridView1);
		//adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, texts);
        //gridview.setAdapter(adapter);
        //ma = new MyAdapter(gridview.getContext(), texts);
        //gridview.setAdapter(ma);
        // neutral/muted/left/right
        
		//gridview.setAdapter(new MyAdapter(this));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.servo_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_servo_settings,
					container, false);
			EditText [][]servos = new EditText[12][4];
			servos[0][0] = (EditText) rootView.findViewById(R.id.editServo1Neutral);
			servos[0][1] = (EditText) rootView.findViewById(R.id.editServo1Muted);
			servos[0][2] = (EditText) rootView.findViewById(R.id.editServo1Left);
			servos[0][3] = (EditText) rootView.findViewById(R.id.editServo1Right);
	        
			servos[1][0] = (EditText) rootView.findViewById(R.id.editServo2Neutral);
			servos[1][1] = (EditText) rootView.findViewById(R.id.editServo2Muted);
			servos[1][2] = (EditText) rootView.findViewById(R.id.editServo2Left);
			servos[1][3] = (EditText) rootView.findViewById(R.id.editServo2Right);
			
			servos[2][0] = (EditText) rootView.findViewById(R.id.editServo3Neutral);
			servos[2][1] = (EditText) rootView.findViewById(R.id.editServo3Muted);
			servos[2][2] = (EditText) rootView.findViewById(R.id.editServo3Left);
			servos[2][3] = (EditText) rootView.findViewById(R.id.editServo3Right);
			
			servos[3][0] = (EditText) rootView.findViewById(R.id.editServo4Neutral);
			servos[3][1] = (EditText) rootView.findViewById(R.id.editServo4Muted);
			servos[3][2] = (EditText) rootView.findViewById(R.id.editServo4Left);
			servos[3][3] = (EditText) rootView.findViewById(R.id.editServo4Right);
			
			servos[4][0] = (EditText) rootView.findViewById(R.id.editServo5Neutral);
			servos[4][1] = (EditText) rootView.findViewById(R.id.editServo5Muted);
			servos[4][2] = (EditText) rootView.findViewById(R.id.editServo5Left);
			servos[4][3] = (EditText) rootView.findViewById(R.id.editServo5Right);
	        
			servos[5][0] = (EditText) rootView.findViewById(R.id.editServo6Neutral);
			servos[5][1] = (EditText) rootView.findViewById(R.id.editServo6Muted);
			servos[5][2] = (EditText) rootView.findViewById(R.id.editServo6Left);
			servos[5][3] = (EditText) rootView.findViewById(R.id.editServo6Right);
			
			servos[6][0] = (EditText) rootView.findViewById(R.id.editServo7Neutral);
			servos[6][1] = (EditText) rootView.findViewById(R.id.editServo7Muted);
			servos[6][2] = (EditText) rootView.findViewById(R.id.editServo7Left);
			servos[6][3] = (EditText) rootView.findViewById(R.id.editServo7Right);
			
			servos[7][0] = (EditText) rootView.findViewById(R.id.editServo8Neutral);
			servos[7][1] = (EditText) rootView.findViewById(R.id.editServo8Muted);
			servos[7][2] = (EditText) rootView.findViewById(R.id.editServo8Left);
			servos[7][3] = (EditText) rootView.findViewById(R.id.editServo8Right);
			
			servos[8][0] = (EditText) rootView.findViewById(R.id.editServo9Neutral);
			servos[8][1] = (EditText) rootView.findViewById(R.id.editServo9Muted);
			servos[8][2] = (EditText) rootView.findViewById(R.id.editServo9Left);
			servos[8][3] = (EditText) rootView.findViewById(R.id.editServo9Right);
	        
			servos[9][0] = (EditText) rootView.findViewById(R.id.editServo10Neutral);
			servos[9][1] = (EditText) rootView.findViewById(R.id.editServo10Muted);
			servos[9][2] = (EditText) rootView.findViewById(R.id.editServo10Left);
			servos[9][3] = (EditText) rootView.findViewById(R.id.editServo10Right);
			
			servos[10][0] = (EditText) rootView.findViewById(R.id.editServo11Neutral);
			servos[10][1] = (EditText) rootView.findViewById(R.id.editServo11Muted);
			servos[10][2] = (EditText) rootView.findViewById(R.id.editServo11Left);
			servos[10][3] = (EditText) rootView.findViewById(R.id.editServo11Right);
			
			servos[11][0] = (EditText) rootView.findViewById(R.id.editServo12Neutral);
			servos[11][1] = (EditText) rootView.findViewById(R.id.editServo12Muted);
			servos[11][2] = (EditText) rootView.findViewById(R.id.editServo12Left);
			servos[11][3] = (EditText) rootView.findViewById(R.id.editServo12Right);
			
			float x = 0.1f;
			for (int i = 0; i < 12; i++) {
				for (int j = 0; j < 4; j++) {
					servos[i][j].setText(Float.toString(x));
					x += 0.1;
				}
			}
			//servos[0][1].setText("0.3");
	        
			return rootView;
		}
	}

}
