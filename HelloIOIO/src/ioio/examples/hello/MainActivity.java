package ioio.examples.hello;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.robotar.ioio.LEDSettings;
import com.robotar.ioio.Pins;
import com.robotar.ioio.ServoSettings;

import cz.versarius.xchords.Chord;
import cz.versarius.xchords.ChordBag;
import cz.versarius.xchords.ChordLibrary;
import cz.versarius.xchords.ChordManager;
import cz.versarius.xsong.ChordRef;
import cz.versarius.xsong.Line;
import cz.versarius.xsong.Part;
import cz.versarius.xsong.Song;
import cz.versarius.xsong.XMLSongLoader;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 * 
 * It was extended from IOIOActivity, but because of needed extension from SherlockActivity,
 * I copied IOIOActivity's internals directly into this class.
 */
public class MainActivity extends ActionBarActivity implements IOIOLooperProvider{
	private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

	private static final String KEV_BLUES_XML = "Kev Blues.xml";
	private String lastChosenSong = KEV_BLUES_XML;

	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(
			this, this);
	
	private ToggleButton stateLedButton;
	private TextView title;
	private TextView currentChordView;
	private TextView songText;
	private Button simPedalButton;
	
	private Song song;
	private ChordManager chordManager;
	private boolean guiReady;
	private ToggleButton button_;

	private int lineIdx;
	private int chordIdx;

	/* <code>true</code> for changing next chord with pedal
	 * <code>false</code> for changing next chord with app button
	 */
	private boolean usePedal;
	
	protected boolean ledOn_;

	//private String rootFolder = "/sdcard/";
	
	/**
	 * ServoSettings for the current chord. 
	 */
	private ServoSettings servoSettings;

	/**
	 * LED settings for the current chord.
	 */
	private LEDSettings leds;

	XMLSongLoader songLoader = new XMLSongLoader();
	
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		helper_.create();
		
		// gui items
		setContentView(R.layout.main);
		title = (TextView) findViewById(R.id.title);
		songText = (TextView) findViewById(R.id.currentLineView);
		simPedalButton = (Button) findViewById(R.id.buttonSimPedal);
		currentChordView = (TextView) findViewById(R.id.currentChordView);
		stateLedButton = (ToggleButton) findViewById(R.id.button);

		// init song play attributes
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences pref = getSharedPreferences("preferences", 0);
		usePedal = pref.getBoolean("pref_next_chord_control", true);
		chordIdx = -1;
		lineIdx = 0;
		
		// prepare chord manager
		chordManager = new ChordManager();
		chordManager.initialize();
		LOG.info("Chord manager is initialized?: {}", chordManager.isInitialized());
		
		simPedalButton.setEnabled(false);
		
		// check external media (sdcard) TODO - full status
		boolean storageReadable = FileUtil.isExternalStorageReadable();
		boolean storageWritable = FileUtil.isExternalStorageWritable();
		if (!storageReadable || !storageWritable) {
			LOG.error("cannot access storage - sdcard, readable:" + storageReadable + ", writable: " + storageWritable);
			title.setText("sd error");
			songText.setText("problem with SD storage. " + 
					"cannot access storage - sdcard, readable:" + storageReadable + ", writable: " + storageWritable);
			return;
		} 

		// load servo corrections
		servoSettings = ServoSettings.loadCorrectionsOnAndroid(FileUtil.getCorrections());
		if (!servoSettings.isAnyCorrectionSet()) {
			LOG.error("servo corrections not loaded! : {}", FileUtil.getCorrections().getAbsoluteFile());
			title.setText("cfg error");
			songText.setText("Servo corrections values are not set! (all == 0.0) You should go to Servo Settings and set servo correction values." +
					"Otherwise you can DAMAGE your RoboTar device! After setting the values, please restart this application!");
			// guiReady is still false, so it won't be possible to play the song on RoboTar device. (no damage possible)
			// Currently we demand restart of the app, so it will again check everything.
			// App restart can be avoided, if we set the attribute guiReady=true after ServoSettings change.
			// I prefer restart of the app.
			return; 
		}
		
		// mark the app as ready, RoboTar device will be able to play the songs
		guiReady = true;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && guiReady) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			usePedal = pref.getBoolean("pref_next_chord_control", true);
			LOG.debug("value is : {}", usePedal);
			
			// display or hide next chord button, based on the settings
			if (usePedal) {
				simPedalButton.setVisibility(View.GONE);
			} else {
				simPedalButton.setVisibility(View.VISIBLE);
			}
			
			// load currently selected song
			loadSong();
		}
	}
	
	private void loadSong() {
		// get or create robotar storage dir 
		File rtFolder = FileUtil.getRobotarStorageDir();
		if (rtFolder == null) {
			songText.setText("Problem with robotar storage directory. Is SD attached?");
			return;
		}
		
		// look if we have something 'better' to load than the default song
		Intent intent = getIntent();
		String chosenSongFilename = intent.getStringExtra("myfilename");
		LOG.debug("chosen song: {}", chosenSongFilename);
		if (chosenSongFilename != null && chosenSongFilename.length() != 0) {
			lastChosenSong = chosenSongFilename;
		}
		LOG.debug("songName to load: {}", lastChosenSong);
		song = songLoader.loadSong(new File(rtFolder, lastChosenSong));
		if (song == null) {
			simPedalButton.setEnabled(false);
			title.setText("error");
			songText.setText("cannot find such file: " + lastChosenSong + " in folder: " + rtFolder.getAbsolutePath());
			return;
		}
		title.setText(song.getFullTitle());
		LOG.info("song title: {}", song.getFullTitle());
		
		// display the song
		songText.setText(getSongText(song));
		
		// map chord references with real chords values, add chords to chord manager
		fillWith(song, chordManager);

		// display current chord view
		currentChordView.setText("---");

		simPedalButton.setEnabled(true);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
	    super.onNewIntent(intent);
	    if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			helper_.restart();
		}
	    setIntent(intent);
	}
	
	/**
	 * Quick song view.
	 * TODO better
	 * 
	 * @param song2
	 * @return
	 */
	private CharSequence getSongText(Song song2) {
		StringBuilder sb = new StringBuilder(30);
		for (Part part : song2.getParts()) {
			// check empty lines
			for (Line line : part.getLines()) {
				if (!line.hasAnyChords()) {
					if (line.getText() == null) {
						// totally empty line
						continue;
					}
				} else {
					// put chords above the text
					sb.append(formatChords(line));
				}
				sb.append(line.getText());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Will position chords to the proper places.
	 * first position is 1, not 0!
	 * 
	 * @param line
	 * @return
	 */
	private String formatChords(Line line) {
		StringBuilder sb = new StringBuilder();
		for (ChordRef ref : line.getChords()) {
			int end = sb.length() + 1;
			int position = ref.getPosition();
			if (end < position) {
				for (int i = 0; i< position-end; i++) {
					sb.append(" ");
				}
			}
			String chordName = Chord.getChordName(ref.getChordId());
			
			sb.append(chordName);	
		}
		
		if (line.getText() == null) {
			line.setText(" ");
		}
		
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Sets servos and LEDs to values according to given chord structure.
	 * 
	 * @param chord
	 */
	public void prepareChord(Chord chord) {
		if (chord == null) {
			LOG.error("chord can't be null!!!");
			throw new IllegalArgumentException("error"); // to be able to view the stacktrace - remote debugging :)
		}
		servoSettings.setChord(chord);
		prepareLEDs(new LEDSettings(chord));
	}

	/** 
	 * Release servos and turn off LEDs.
	 */
	public void prepareNoChord() {
		LOG.info("In prepareNoChord");
		servoSettings.setInitialPosition();
		prepareLEDs(new LEDSettings());
	}

	/**
	 * Set values of LEDs structures.
	 * 
	 * @param leds
	 */
	protected void prepareLEDs(LEDSettings leds) {
		this.leds = leds;
		LOG.debug("preparing LED Values on songs page: {}", leds.debugOutput());
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class Looper extends BaseIOIOLooper {
		private final int I2C_PAIR = 0; //IOIO Pair for I2C
		private static final float FREQ = 50.0f;
		private static final int PCA_ADDRESS = 0x40;
		private static final byte PCA9685_MODE1 = 0x00;
		private static final byte PCA9685_PRESCALE = (byte) 0xFE;
		private TwiMaster twi_;

		/** The on-board LED. */
		private DigitalOutput stateLED;
		/** pedal device */
		private DigitalInput pedalButton;
		/** all chord's LEDs */
		private DigitalOutput[][] fretLEDs = new DigitalOutput[6][4];
		/** reference to currently turned on LEDs, to be able to turn them off */
		private DigitalOutput[] fretLEDsTurnedOn = new DigitalOutput[6];

		private boolean lastKnownPedalPosition = true;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException,
		InterruptedException {
			LOG.info("HelloIOIO", "IOIO is connected");

			// on-board pin
			//stateLED = ioio_.openDigitalOutput(IOIO.LED_PIN, true);

			// pedal input setup
			pedalButton = ioio_.openDigitalInput(Pins.PEDAL_PIN, DigitalInput.Spec.Mode.PULL_UP);

			// fret leds output setup
			fretLEDs = prepareLEDs(false);

			// Setup IOIO TWI Pins
			twi_ = ioio_.openTwiMaster(I2C_PAIR, TwiMaster.Rate.RATE_1MHz, false);

			reset();
		}

		/** 
		 * Setup chord LEDs.
		 * 
		 * @param startValue
		 * @return
		 * @throws ConnectionLostException
		 */
		private DigitalOutput[][] prepareLEDs(boolean startValue) throws ConnectionLostException {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 4; j++) {
					// pin matching Pins.java
					fretLEDs[i][j] = ioio_.openDigitalOutput(Pins.getLEDPin(i, j+1), startValue);
				}
			}
			return fretLEDs;
		}

		/** 
		 * Initialize RoboTar IOIO device.
		 * 
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		private void reset() throws ConnectionLostException, InterruptedException {
			// Set prescaler - see PCA9685 data sheet
			LOG.info("HelloIOIO", "Start of the BaseIOIOLooper.reset method");
			float prescaleval = 25000000;
			prescaleval /= 4096;
			prescaleval /= FREQ;
			prescaleval -= 1;
			byte prescale = (byte) Math.floor(prescaleval + 0.5);

			write8(PCA9685_MODE1, (byte) 0x10); // go to sleep... prerequisite to set prescaler
			write8(PCA9685_PRESCALE, prescale); // set the prescaler
			write8(PCA9685_MODE1, (byte) 0x20); // Wake up and set Auto Increment
		}

		/**
		 * Send data to RoboTar device.
		 * 
		 * @param reg
		 * @param val
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		private void write8(byte reg, byte val) throws ConnectionLostException,
		InterruptedException {
			LOG.info("HelloIOIO", "Start of the write8 method");
			byte[] request = {reg, val};
			twi_.writeReadAsync(PCA_ADDRESS, false, request, request.length, null, 0);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException,
		InterruptedException {

			if (!guiReady) {
				LOG.error("The GUI is not ready yet!");
				return;
			}
			//LOG.info("Start of the loop method");
			//stateLED.write(!stateLedButton.isChecked());
			if (usePedal) {
				// initial position
				// high = true, low = false
				boolean pedalInHighPosition = pedalButton.read();

				if (lastKnownPedalPosition == pedalInHighPosition) {
					// no change from last time
					return;
				}
				// save current status of the pedal
				lastKnownPedalPosition = pedalInHighPosition;

				if (!pedalInHighPosition) {
					LOG.info("HelloIOIO", "Pedal is pressed 2");
					// PEDAL IS PRESSED
					
					Chord chord = moveByOneChord();
					if (chord == null) {
						// stop all
						prepareNoChord();
					} else {
						// prepare servo settings and led settings
						prepareChord(chord);
					}
		
				} 
				else {
					LOG.info("Pedal is released 2");
					// PEDAL IS RELEASED
					// reset servos - maybe not needed - we can save one move of servos?
					// we could, if we store old settings and new settings
					// we have to reset those servos, that are not involved in new settings
					// and modify existing, that are in both and set the new ones
					// TODO, for now, let is as it was
					resetAll();
					return;
				}
			} else {
				// progress to next chord is controlled by app button - idx is already set (onclick)
				
				// we are checking and logging the status first
				// if we already play the song, play next chord
				if (chordIdx == -1) {
					// no chord selected - we are not playing the song
					prepareNoChord();
				}
				// prepare servo settings and led settings
				prepareChord(getCurrentChord());

			}
			
			// send the prepared values of servos and leds through ioio to robotar
			// debugging servos and leds values for current chord
			// if in problems, uncomment and investigate
			LOG.debug("got chord: {}" + servoSettings.debugOutput());
			LOG.debug("leds: {}" + leds);

			long timeStart = System.currentTimeMillis();
			// send prepared values to RoboTar device
			// for each string
			for (int i = 0; i < 6; i++) {
				int servoNumber = servoSettings.getServos()[i];
				float servoValue = servoSettings.getValues()[i];
				setServo(servoNumber, servoValue);
				if (leds != null) {
					LOG.debug("leds 2: {}", leds.getLeds());
					if (leds.getLeds() != null) {
						setLED(i, leds.getLeds()[i]);
					}
				}
			}
			long timeEnd = System.currentTimeMillis();
			LOG.debug("It took {} ms to execute 6 servos and LEDs", timeEnd - timeStart);
			

			//PWM Range below is 0.0. to 1.5.  Cycle through each servo channel.
	/*for (int c=0; c<16; c++) {
		for (float p = 1.0f; p>0.7; p-=0.1f) {
			Thread.sleep(200);
			setServo(c, p);
			stateLED.write(ledOn_);
		}

		for (float p=1.0f; p<1.3f; p+=0.1f) {
			Thread.sleep(200);
			setServo(c, p);
			}
		}*/

		}

		/*private void workingKevinsMethod() throws InterruptedException, ConnectionLostException {
			float servoValue = 1.3f;

			Thread.sleep(10);
			//C Chord?
			LOG.info("HelloIOIO", "Pedal is pressed or released");
			Log.i("HelloIOIO", "Pedal is pressed or released");
			setServo(0, servoValue);
			setServo(1, servoValue);
			setServo(2, servoValue);
			setServo(3, servoValue);
			setServo(4, servoValue);
			setServo(5, servoValue);
			setServo(6, servoValue);
			setServo(7, servoValue);
			setServo(8, servoValue);
			setServo(9, servoValue);
			setServo(10, servoValue);
			setServo(11, servoValue);			
			Thread.sleep(300);

			setServo(0, 1.0f);
			setServo(1, 1.0f);
			setServo(2, 1.0f);
			setServo(3, 1.0f);
			setServo(4, 1.0f);
			setServo(5, 1.0f);
			setServo(6, 1.0f);
			setServo(7, 1.0f);
			setServo(8, 1.0f);
			setServo(9, 1.0f);
			setServo(10, 1.0f);
			setServo(11, 1.0f);
			Thread.sleep(300);

		}



		/**
		 * Reset all servos to neutral position.
		 * 
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		public void resetAll() throws ConnectionLostException, InterruptedException {
			// turn off main led
			//stateLedButton.setChecked(false);
			// set all servos to neutral positions
			for (int servo = 0; servo < 12; servo++) {
				// initial = 1.0 + corrections
				LOG.debug("Servo Value: {}", servo);
				LOG.debug("servoSettings: {}", servoSettings.getInitial(servo));
				setServo(servo, servoSettings.getInitial(servo));
				// probably not?
				//setServo(servo, 1.0f);
			}
			turnOffFretLEDs();
			LOG.info("Servos in neutral position default");
		}

		/**
		 * Turn off fret LEDs. 
		 * 
		 * @throws ConnectionLostException
		 */
		private void turnOffFretLEDs() throws ConnectionLostException {
			for (int i = 0; i < 6; i++) {
				for (int j = 0; j < 4; j++) {
					fretLEDs[i][j].write(false);
				}
				fretLEDsTurnedOn[i] = null;
			}
		}

		/**
		 * Set Servo channel and milliseconds input to PulseWidth calculation
		 * 
		 * @param servoNum
		 * @param pos
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		public void setServo(int servoNum, float pos) throws ConnectionLostException, InterruptedException {
			LOG.debug("setServo call: servo: {}, value: {}", servoNum, pos);
			setPulseWidth(servoNum, pos + 1.0f);  //
		}

		/**
		 * Send data to RoboTar device.
		 *  
		 * @param channel
		 * @param ms
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		protected void setPulseWidth(int channel, float ms) throws ConnectionLostException, InterruptedException {
			// Set pulsewidth according to PCA9685 data sheet based on milliseconds value sent from setServo method
			// 4096 steps per cycle, frequency is 50MHz (50 steps per millisecond)
			int pw = Math.round(ms / 1000 * FREQ * 4096);
			// Skip to every 4th address value to turn off the pulse (see datasheet addresses for LED#_OFF_L)
			byte[] request = { (byte) (0x08 + channel * 4), (byte) pw, (byte) (pw >> 8) };
			twi_.writeReadAsync(PCA_ADDRESS, false, request, request.length, null, 0);
		}

		/**
		 * 
		 * @param stringNum 0..5
		 * @param fretNum 1..4
		 * @throws ConnectionLostException
		 */
		public void setLED(int stringNum, int fretNum) throws ConnectionLostException {
			LOG.debug("HelloIOIO", "setLED call: string: {}, fretNum: {}", stringNum, fretNum);
			if (fretNum <= 0) {
				if (fretLEDsTurnedOn[stringNum] != null) {
					// if we know what was last turned on
					fretLEDsTurnedOn[stringNum].write(false);
				} else {
					// turn off all LEDs on this string
					for (int j = 0; j < 4; j++) {
						fretLEDs[stringNum][j].write(false);
					}
				}
				fretLEDsTurnedOn[stringNum] = null;
			} else {
				// turn off last turned on LED on this string
				if (fretLEDsTurnedOn[stringNum] != null) {
					fretLEDsTurnedOn[stringNum].write(false);
				}
				// turn on the one LED on this string
				fretLEDs[stringNum][fretNum-1].write(true);
				fretLEDsTurnedOn[stringNum] = fretLEDs[stringNum][fretNum-1];
			}
		}

		/**
		 * Called when IOIO is disconnected.
		 */
		@Override
		public void disconnected() {
			LOG.info("IOIO disconnected");
		}

		/**
		 * Called if the IOIO has different firmware version.
		 */
		@Override
		public void incompatible() {
			LOG.info("Incompatible firmware version of IOIO");
		}
	};


	private Chord moveByOneChord() {
		chordIdx++;
		Chord curChord = getCurrentChord();
		if (curChord != null) {
			currentChordView.setText(curChord.getName());
		} else {
			chordIdx = -1;
			currentChordView.setText("---");
		}
		return curChord;
	}
	
	/** 
	 * Get current chord in song, which should be played.
	 * 
	 * @return current chord or null
	 */
	public Chord getCurrentChord() {
		Line line = song.getLine(lineIdx);
		if (line == null) {
			lineIdx = 0;
			chordIdx = -1;
			LOG.debug("line is null");
			return null;
		} else {
			LOG.debug("line idx: {}, chord idx: {}", lineIdx, chordIdx);
			if (chordIdx < line.getChords().size()) {
				return line.getChords().get(chordIdx).getChord();
			} else {
				lineIdx++;
				chordIdx = 0;
				return getCurrentChord();
			}
		}
	}

	/**
	 * this should play the song through test button in gui.
	 * the logic through Kevin's pedal is above in loop() 
	 * @param view
	 */
	public void onClick(View view) {
		moveByOneChord();
	}
	
	public boolean fillWith(Song song, ChordManager manager) {
		boolean error = false;
		ChordBag used = song.getUsedChords();
		for (Part part : song.getParts()) {
			// check empty lines
			for (Line line : part.getLines()) {
		 
				for (ChordRef ref : line.getChords()) {
					String libraryName = Chord.getLibraryName(ref.getChordId());
					String chordName = Chord.getChordName(ref.getChordId());
					LOG.debug("libname: {}, chordname: {}", libraryName, chordName);
					//ChordManager pc = new ChordManager();
					//pc.
					Chord chord = used.findByName(chordName);
					if (chord != null) {
						ref.setChord(chord);
						ChordLibrary lib = manager.findByName(libraryName);
						if (lib == null) {
							lib = new ChordLibrary(libraryName);
						}
						Chord existing = lib.findByName(chordName);
						if (existing == null) {
							lib.add(chord);
						} else {
							// TODO - compare the two.. now left the one in library as is...
							LOG.debug("should check content equality of chords - ? {}, {}", libraryName, chordName);
						}
						continue;
					} else {
						// chord was not in usedchords section
						LOG.error("no chord for name: {}", chordName);
					}
					
					/*ChordLibrary lib = manager.findByName(libraryName);
					if (lib == null) {
						LOG.debug("have to create lib: {}", libraryName);
						lib = new ChordLibrary(libraryName);
					}
					Chord chord = lib.findByName(chordName);
					if (chord != null) {
						LOG.debug("chord with name: {} found", chordName);
						ref.setChord(chord);
					} else {
						LOG.error("no chord for name: {}", chordName);
					}
					*/
					/*else {
						// chord was not in usedchords section
						ChordLibrary library = manager.getChordLibraries().get(libraryName);
						if (library != null) {
							Chord existing = library.findByName(chordName);
							if (existing != null) {
								ref.setChord(existing);
								used.add(existing);
								continue;
							}
						}
						
						if (libraryName.startsWith(ChordManager.USER_PREFIX)) {
							// look into current chord buffer on chords page
							RoboTarChordsPage chPage = mainFrame.getChordsPage();
							if (chPage != null) {
								@SuppressWarnings("unchecked")
								DefaultListModel<Chord> model = (DefaultListModel<Chord>)chPage.getChordListModel();
								if (model != null) {
									Chord existing = findByName(model, ref.getChordId()); 
									if (existing != null) {
										ref.setChord(existing);
										used.add(existing);
										continue;
									}
								}
							}
						}
					}*/
				}
			}
		}
		return error;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case R.id.action_songs:
	    		//LOG.debug("open songs ...");
	    		showSongs();
	    		return true;
	        case R.id.action_settings:
	            //LOG.debug("open settings ...");
	        	openSettings();
	            return true;
	        case R.id.servo_corrections:
	        	//LOG.debug("open servo corrections");
	        	openServoCorrections();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	private void openServoCorrections() {
		Intent intent = new Intent(this, ServoSettingsActivity.class);
	    intent.putExtra("servoSettings", servoSettings);
	    //LOG.debug("starting activity with: {}", servoSettings.debugOutputCorrections());
	    startActivityForResult(intent, 1);
	}
	
	private void showSongs() {
		Intent intent = new Intent(this, ShowSongsActivity.class);
	    startActivity(intent);
	}
	
	private void openSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
	    startActivity(intent);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
	        if (data.getExtras().containsKey("servoSettings")){
	            servoSettings = (ServoSettings) data.getSerializableExtra("servoSettings");
	            //LOG.debug("got back from activity: {}", servoSettings.debugOutputCorrections());
	        }
        }
	}
	
	// IOIOActivity internals:
	

	/**
	 * Subclasses should call this method from their own onDestroy() if
	 * overloaded. It takes care of connecting with the IOIO.
	 */
	@Override
	protected void onDestroy() {
		helper_.destroy();
		super.onDestroy();
	}

	/**
	 * Subclasses should call this method from their own onStart() if
	 * overloaded. It takes care of connecting with the IOIO.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		helper_.start();
	}

	/**
	 * Subclasses should call this method from their own onStop() if overloaded.
	 * It takes care of disconnecting from the IOIO.
	 */
	@Override
	protected void onStop() {
		helper_.stop();
		super.onStop();
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * This was originally defined in IOIOActivity and this was overriden.
	 * Now, we don't extend IOIOActivity, therefore simply define it without @Override.
	 * @see ioio.lib.util.android.IOIOActivity#createIOIOLooper()
	 *
	 */
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return createIOIOLooper();
	}

}