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
		LOG.info("chord manager is initialized?: {}", chordManager.isInitialized());
		// map chord references with real chords values from chord manager
		songSample.fillWith(chordManager);
		
		// display sample line
		sampleLineText.setText(songSample.getSong().getLine(0).getText());
		currentChordView.setText("---");
		
		guiReady = true;
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 *
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. *
		private DigitalOutput led_;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 *
		@Override
		protected void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(0, true);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 *
		@Override
		public void loop() throws ConnectionLostException {
			led_.write(!button_.isChecked());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
	*/
	
	public void prepareChord(Chord chord) {
		if (chord == null) {
			LOG.error("chord can't be null!!!");
			throw new IllegalArgumentException("error"); // to be able to view the stacktrace - remote debugging :)
		}
		servoSettings.setChord(chord);
		prepareLEDs(new LEDSettings(chord));
	}

	public void prepareNoChord() {
		servoSettings.setInitialPosition();
		prepareLEDs(new LEDSettings());
	}
	
	protected void prepareLEDs(LEDSettings leds) {
		this.leds = leds;
		LOG.debug("preparing LED Values on songs page: {}", leds.debugOutput());
	}
	
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.android.IOIOActivity#createIOIOLooper()
	 *
	}*/
	//@Override
	//public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		// we don't need to use connectionType and extra - we have only 1 ioio device
		
		// this is basically copy'n'paste from robotarpcconsole, and then modified...
		class Looper extends BaseIOIOLooper {
			private final int I2C_PAIR = 0; //IOIO Pair for I2C
			private static final float FREQ = 50.0f;
			private static final int PCA_ADDRESS = 0x40;
			private static final byte PCA9685_MODE1 = 0x00;
			private static final byte PCA9685_PRESCALE = (byte) 0xFE;
			private TwiMaster twi_;
			
			private DigitalOutput stateLED;
			private DigitalInput pedalButton;
			// all the leds
			private DigitalOutput[][] fretLEDs = new DigitalOutput[6][4];
			// reference to actually turned on leds, to be able to turn them off
			private DigitalOutput[] fretLEDsTurnedOn = new DigitalOutput[6];
			
			private boolean lastKnownPedalPosition = true;
			
			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {
				LOG.info("IOIO is connected");
				
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

			private DigitalOutput[][] prepareLEDs(boolean startValue) throws ConnectionLostException {
				for (int i = 0; i < 6; i++) {
					for (int j = 0; j < 4; j++) {
						// pin matching Pins.java
						fretLEDs[i][j] = ioio_.openDigitalOutput(Pins.getLEDPin(i, j+1), startValue);
					}
				}
				return fretLEDs;
			}
			
			private void reset() throws ConnectionLostException, InterruptedException {
				// Set prescaler - see PCA9685 data sheet
				LOG.info("Start of the BaseIOIOLooper.reset method");
				float prescaleval = 25000000;
				prescaleval /= 4096;
				prescaleval /= FREQ;
				prescaleval -= 1;
				byte prescale = (byte) Math.floor(prescaleval + 0.5);
				
				write8(PCA9685_MODE1, (byte) 0x10); // go to sleep... prerequisite to set prescaler
				write8(PCA9685_PRESCALE, prescale); // set the prescaler
				write8(PCA9685_MODE1, (byte) 0x20); // Wake up and set Auto Increment
			}
			
			private void write8(byte reg, byte val) throws ConnectionLostException,
				InterruptedException {
				LOG.info("Start of the write8 method");
				byte[] request = {reg, val};
				twi_.writeReadAsync(PCA_ADDRESS, false, request, request.length, null, 0);
			}
		
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
					LOG.info("Pedal is pressed");
					System.out.println("Pedal is pressed");
					// PEDAL IS PRESSED
					stateLedButton.setChecked(true);

					// we are checking and logging the status first
					if (!guiReady) {
						LOG.error("The GUI is not yet ready!");
					} else {
						// if we already play the song, play next chord
						if (chordIdx == -1) {
							// no chord selected - we are not playing the song, quit
							prepareNoChord();
							return;
						}
						// sets servo settings and led settings
						prepareChord(getCurrentChord());
						
						// debugging logs... in problems, uncomment
						System.out.println("got chord: {}" + servoSettings.debugOutput());
						System.out.println("leds: {}" + leds);
						
						long timeStart = System.currentTimeMillis();
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
					}
				} else {
					LOG.info("Pedal is released");
					// PEDAL IS RELEASED
					// turn off led
					stateLedButton.setChecked(false);
					// reset servos
					resetAll();
					
				} 

				// save current status of the pedal
				lastKnownPedalPosition = pedalInHighPosition;
				
				/*
				 //TODO what is this?
				//PWM Range below is 0.0. to 1.5.  Cycle through each servo channel.
				for (int c=0; c<16; c++) {
					for (float p = 1.5f; p>0.0; p-=0.5f) {
						Thread.sleep(200);
						setServo(c, p);
						led_.write(ledOn_);
					}
				
					for (float p=0.0f; p<1.5f; p+=0.5f) {
						Thread.sleep(200);
						setServo(c, p);
					}
				}*/
				
			}
			
			/**
			 * Reset all servos to neutral position.
			 * 
			 * @throws ConnectionLostException
			 * @throws InterruptedException
			 */
			public void resetAll() throws ConnectionLostException, InterruptedException {
				stateLedButton.setChecked(false);
				for (int servo = 0; servo < 12; servo++) {
					setServo(servo, servoSettings.getInitial(servo));
				}
				turnOffFretLEDs();
				LOG.info("Servos in neutral position default");
			}

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
				LOG.debug("setLED call: string: {}, fretNum: {}", stringNum, fretNum);
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
			
			@Override
			public void disconnected() {
				LOG.info("IOIO disconnected");
			}

			@Override
			public void incompatible() {
				LOG.info("Incompatible firmware version of IOIO");
			}
		};


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