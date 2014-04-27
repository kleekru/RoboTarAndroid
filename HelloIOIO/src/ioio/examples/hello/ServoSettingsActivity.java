package ioio.examples.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robotar.ioio.ServoSettings;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;

public class ServoSettingsActivity extends ActionBarActivity {
	private static final Logger LOG = LoggerFactory.getLogger(ServoSettingsActivity.class);

	ArrayAdapter adapter;
	MyAdapter ma;
	Object[] list = { 1, 2 };
    String[] texts = {"aada", "bbdb", "ccc", "ddd", "eee", "fff", "eee", "hhh", "iii"};
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_servo_settings);

		// prepare corrections.xml file on the /sdcard path 
		// (outside from assets)
		if (!FileUtil.correctionsExists()) {
			FileUtil.copyCorrections(this);
		}
		
		// display
		if (savedInstanceState == null) {
			PlaceholderFragment pf = new PlaceholderFragment();
            pf.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, pf).commit();
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
			openSettings();
			return true;
		}
		setCorrResult();
		
		return super.onOptionsItemSelected(item);
	}

	private void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
	    startActivity(intent);
	}
	
	protected void setCorrResult() {
		PlaceholderFragment pf = (PlaceholderFragment) getSupportFragmentManager().getFragments().get(0);
		pf.updateSettings();
		//LOG.debug("corrs to save: {}", pf.servoSettings.debugOutputCorrections());
		
		// save it to the file on SD
		ServoSettings.saveCorrectionsAs(FileUtil.getCorrections(), pf.servoSettings);
		
		// pass it back as result of the activity
		Intent intent = getIntent();
		intent.putExtra("servoSettings", pf.servoSettings);
		//LOG.debug("sending back: {}", pf.servoSettings.debugOutputCorrections());
		setResult(RESULT_OK, intent);
		finish();
	}
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		private ServoSettings servoSettings;
		EditText [][]servos = new EditText[12][4];

		public PlaceholderFragment() {
		}
		@Override
		public void setArguments(Bundle args) {
			super.setArguments(args);
			servoSettings = (ServoSettings) args.getSerializable("servoSettings");
			LOG.info("got in fragment: {}", servoSettings.debugOutputCorrections());
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_servo_settings,
					container, false);
			// load content of the corrections file
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

			// set UI edit boxes with loaded values from file
			float[][] scorr = servoSettings.getCorrections();
			for (int i = 0; i < 12; i++) {
				for (int j = 0; j < 4; j++) {
					servos[i][j].setText(Float.toString(scorr[i][j]));
				}
			}
	        
			return rootView;
		}
		
		@Override
		public void onResume() {
		    super.onResume();
		    LOG.info("resume in fragment");
		    
		}
		
		@Override
	    public void onSaveInstanceState(Bundle outState) {
	        super.onSaveInstanceState(outState);
		    LOG.info("save instance"); 
	        
		    updateSettings();
			outState.putSerializable("servoSettings", servoSettings);
			
			
	    }
		
		public void updateSettings() {
			// set value from UI edit boxes to servoSettings object
		    float[][] scorr = new float[12][4];
			for (int i = 0; i < 12; i++) {
				for (int j = 0; j < 4; j++) {
					scorr[i][j] = Float.parseFloat(servos[i][j].getText().toString());
				}
			}
			servoSettings.setCorrections(scorr);
		}
	}

}
