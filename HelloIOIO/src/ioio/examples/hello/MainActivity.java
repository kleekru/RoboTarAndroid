package ioio.examples.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robotar.ioio.LEDSettings;
import com.robotar.ioio.Pins;
import com.robotar.ioio.ServoSettings;

import cz.versarius.xchords.Chord;
import cz.versarius.xchords.ChordManager;
import cz.versarius.xsong.Line;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import ioio.examples.hello.R;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.util.Log;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity {
	private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

	private ToggleButton stateLedButton;
	private TextView sampleLineText;
	private Button simPedalButton;
	private TextView currentChordView;
	private int chordIdx;
	
	private SongSample songSample;
	private ChordManager chordManager;
	private boolean guiReady;
	private ToggleButton button_;
	
	protected boolean ledOn_;

	
	/**
	 * ServoSettings for the current chord. 
	 */
	private ServoSettings servoSettings;
	
	/**
	 * LED settings for the current chord.
	 */
	private LEDSettings leds;
	
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		stateLedButton = (ToggleButton) findViewById(R.id.button);
		sampleLineText = (TextView) findViewById(R.id.currentLineView);
		simPedalButton = (Button) findViewById(R.id.buttonSimPedal);
		currentChordView = (TextView) findViewById(R.id.currentChordView);
		chordIdx = -1;
		songSample = new SongSample();
		chordManager = new ChordManager();
		chordManager.initialize();
		LOG.info("HelloIOIO", "chord manager is initialized?: {}", chordManager.isInitialized());
		Log.i("HelloIOIO", "chord manager is initialized"+ chordManager.isInitialized());
		// map chord references with real chords values from chord manager
		songSample.fillWith(chordManager);
		
		// display sample line
		sampleLineText.setText(songSample.getSong().getLine(0).getText());
		currentChordView.setText("---");
		
		guiReady = true;
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
		LOG.debug("HelloIOIO", "preparing LED Values on songs page: {}", leds.debugOutput());
		Log.i("HelloIOIO", "preparing LED Values on songs page: "+ leds.debugOutput());
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
			Log.i("HelloIOIO", "IOIO is connected");
			
			// on-board pin
			stateLED = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
							
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
			Log.i("HelloIOIO", "Start of the BaseIOIOLooper.reset method");
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
			Log.i("HelloIOIO", "Start of the write8 method");
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
			
			//LOG.info("Start of the loop method");
			stateLED.write(!stateLedButton.isChecked());

			// initial position
			// high = true, low = false
			boolean pedalInHighPosition = pedalButton.read();
			//LOG.debug("current position of pedal is: {}", pedalInHighPosition);

			//LOG.debug("lastPedalPosition: {}", lastKnownPedalPosition);
			if (lastKnownPedalPosition == pedalInHighPosition) {
				// no change from last time
				return;
			}
			
			if (!pedalInHighPosition) {
				LOG.info("HelloIOIO", "Pedal is pressed");
				Log.i("HelloIOIO", "Pedal is pressed");
				// PEDAL IS PRESSED
				stateLedButton.setChecked(true);

				// we are checking and logging the status first
				if (!guiReady) {
					LOG.error("The GUI is not ready yet!");
					Log.e("HelloIOIO", "guit is not ready yet!");
				} else {
					// if we already play the song, play next chord
					if (chordIdx == -1) {
						// no chord selected - we are not playing the song, quit
						prepareNoChord();
						return;
					}
					// prepare servo settings and led settings
					prepareChord(getCurrentChord());
					
					// debugging servos and leds values for current chord
					// if in problems, uncomment and investigate
					LOG.debug("HelloIOIO", "got chord: {}" + servoSettings.debugOutput());
					LOG.debug("HelloIOIO", "leds: {}" + leds);
					Log.d("HelloIOIO", "got chord: " + servoSettings.debugOutput());
					Log.d("HelloIOIO", "leds: " + leds);
					
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
					LOG.debug("HelloIOIO", "It took {} ms to execute 6 servos and LEDs", timeEnd - timeStart);
					Log.d("HelloIOIO", "To execute 6 servos and LEDs took ms: " + (timeEnd - timeStart));
				}
			} else {
				LOG.info("Pedal is released");
				// PEDAL IS RELEASED
				// reset servos
				resetAll();
			}	
			// save current status of the pedal
			/*
			lastKnownPedalPosition = pedalInHighPosition;
			
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
			*/

			/*//PWM Range below is 0.0. to 1.5.  Cycle through each servo channel.
			for (int c=0; c<16; c++) {
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
			
			//resetAll();
			
		}
		
		/**
		 * Reset all servos to neutral position.
		 * 
		 * @throws ConnectionLostException
		 * @throws InterruptedException
		 */
		public void resetAll() throws ConnectionLostException, InterruptedException {
			// turn off main led
			stateLedButton.setChecked(false);
			// set all servos to neutral positions
			for (int servo = 0; servo < 12; servo++) {
				// initial = 1.0 + corrections
				setServo(servo, servoSettings.getInitial(servo));
				// probably not?
				//setServo(servo, 1.0f);
			}
			turnOffFretLEDs();
			LOG.info("HelloIOIO", "Servos in neutral position default");
			Log.i("HelloIOIO","resetAll - Servos in neutral position default");
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
			LOG.debug("HelloIOIO", "setServo call: servo: {}, value: {}", servoNum, pos);
			Log.i("HelloIOIO", "setServo call: " + "ServoNum:" + servoNum +"Servo Value: "+ pos);
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
			Log.i("HelloIOIO", "setLED call: " +"setLED"+ stringNum + "FretNum:"+ fretNum);
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
			LOG.info("HelloIOIO", "IOIO disconnected");
			Log.i("HelloIOIO", "IOIO disconnected");
		}

		/**
		 * Called if the IOIO has different firmware version.
		 */
		@Override
		public void incompatible() {
			LOG.info("HelloIOIO", "Incompatible firmware version of IOIO");
			Log.i("HelloIOIO", "Incompatible firmware version of IOIO");
		}
	};


	/** 
	 * Get current chord in song, which should be played.
	 * 
	 * @return current chord or null
	 */
	public Chord getCurrentChord() {
		Line line = songSample.getSong().getLine(0);
		if (line == null) {
			throw new IllegalArgumentException("line is not available!");
		}
		if (chordIdx < line.getChords().size()) {
			return line.getChords().get(chordIdx).getChord();
		}
		return null;
	}
	

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.android.IOIOActivity#createIOIOLooper()
	 *
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	/**
	 * this should play the song through test button in gui.
	 * the logic through Kevin's pedal is above in loop() 
	 * @param view
	 */
	public void onClick(View view) {
		chordIdx++;
		Chord curChord = getCurrentChord();
		if (curChord != null) {
			currentChordView.setText(curChord.getName());
		} else {
			chordIdx = -1;
			currentChordView.setText("---");
		}
	}
}