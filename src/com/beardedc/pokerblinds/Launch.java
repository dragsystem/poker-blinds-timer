package com.beardedc.pokerblinds;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Launch extends Activity implements OnClickListener, IReturnFinished
{
	private TextView m_txtTimer;
	private TextView m_BlindBig;
	private TextView m_BlindSmall;
	private CountDownTimerComplex m_timer;
	private AppSettings m_settings;
	private Button m_pause;
	private EditText m_manualBigBlindAlteration;
	private Button m_bigBlindOverride;
	private Button m_Button_Settings = null;
	private String pauseText, startText;

	//*************************************************************************
	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		setupIntents();
		
		setupUI();
		
		updateBlinds();
		
		m_timer = new CountDownTimerComplex(this.getApplicationContext());
		
		m_timer.startTiming((int) m_settings.getSecondsRemaining());
	}

	private void setupUI() 
	{
		m_settings = AppSettings.getSettings(this.getApplicationContext());
		
		m_txtTimer = (TextView) findViewById(R.id.TextTimer);
		m_BlindBig = (TextView) findViewById(R.id.textViewBigBlind);
		m_BlindSmall = (TextView) findViewById(R.id.TextViewSmallBlind);
		m_Button_Settings = (Button) findViewById(R.id.button_settings);
		
		pauseText = getString(R.string.pauseTimer);
		startText = getString(R.string.startTimer);

		//m_bigBlindOverride.setOnClickListener(this);
		
		m_pause = (Button)findViewById(R.id.ButtonPause);
		m_pause.setOnClickListener(this);
		
		m_Button_Settings.setOnClickListener(this);
	}

	// http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
	private void setupIntents() 
	{
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(CountDownTimerComplex.BROADCAST_MESSAGE_TICK);
		filter.addAction(CountDownTimerComplex.BROADCAST_MESSAGE_COMPLETE);		
		BroadcastReceiver mReceiver = new GeneralReceiver(this);
		registerReceiver(mReceiver, filter);
	}
	
	//*************************************************************************
	/**
	 * 
	 */
	private void updateBlinds()
	{
		updateTextView("Big Blind is   : " + m_settings.getCurrentBigBlind(), m_BlindBig);
		updateTextView("Small Blind is : " + (m_settings.getCurrentBigBlind() /2), m_BlindSmall);
	}
	
	//*************************************************************************
	/**
	 * 
	 */
	private void updateTextView(String toOutput, TextView toUpdate)
	{
		toUpdate.setText(toOutput);
	}
	
	//*************************************************************************
    /**
     * 
     */
	private void vibrateThePhone()
	{
		if (m_settings.isVibrateDisabled() == false)
		{
			Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			long lVibratePattern[] = new long[m_settings.getVibrateRepeat() *2];
			int iMilliSeconds;
			int max = m_settings.getVibrateRepeat() * 2;
			for (int i = 0; i < max; i++)
			{
				if (isOdd(i)) {iMilliSeconds = 100;}
				else {iMilliSeconds = 500;}
				lVibratePattern[i] = iMilliSeconds;
			}
			v.vibrate(lVibratePattern, -1);
		}
	}
	
	private boolean isOdd(int i)
	{
		return ((i % 1) == 1);
	}

	//*************************************************************************
	/**
	 * 
	 */
	public void onClick(View v)
	{
		if (v.getId() == R.id.ButtonPause)
		{
			if (m_timer.getIsTimerRunning() == true)
			{
				m_timer.pauseStart();
				m_pause.setText(startText);
			} else
			{
				m_timer.pauseStop();
				m_pause.setText(pauseText);
			}
			
		}else if (v.getId() == R.id.button_settings)
		{
			try{
			Intent settingPrefs = new Intent(this, PreferenceLauncher.class);
			startActivity(settingPrefs);
			} catch (Exception e){
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		/* Commented out as this button is not currently used.
		else if (v.getId() == R.id.butManualBigBlindChange)
		{
			// increase blinds
			String sBigBlindHack = m_manualBigBlindAlteration.getText().toString();
			long lBigBlind = Long.parseLong(sBigBlindHack);
			m_settings.setCurrentBigBlind(lBigBlind);
			m_settings.save();
			
			// update the UI
			updateBlinds(m_settings);
		}
		*/
	}

	//*************************************************************************
	/**
	 * 
	 */
	public void intentReceived(Intent intent)
	{
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			m_timer.switchModeToAlarm();
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
        	m_timer.switchModeToCountdown();
        	m_txtTimer.setText(m_timer.getTimeRemainingUIFormat());
		} else if (intent.getAction().equals(CountDownTimerComplex.BROADCAST_MESSAGE_TICK))
		{
			m_txtTimer.setText(m_timer.getTimeRemainingUIFormat());
		} else if (intent.getAction().equals(CountDownTimerComplex.BROADCAST_MESSAGE_COMPLETE))
		{
			// increase blinds
			m_settings.setCurrentBigBlind( m_settings.getCurrentBigBlind() * 2);
			
			// update the UI
			updateBlinds();
			
			// update the timer to zero values
			String sUpdateValue = m_timer.getTimeRemainingUIFormat();
			m_txtTimer.setText(sUpdateValue);
			
			// notify user
			vibrateThePhone();
			
			// start the timer again
			m_timer.startTiming((int) m_settings.getMinutes() * CountDownTimerComplex.m_iMultiplierMinutesToSeconds);
		}
	}	
	
    //*************************************************************************
    /**
     * make sure any lingering alarms are cancelled
     */
    @Override
	public void onDestroy()
    {    	
    	if (m_timer != null) m_timer.destroy();
    	
    	super.onDestroy();
    }
    
    @Override
	public void onPause()
    {
    	m_settings.setSecondsRemaining(m_timer.getTimeRemainingInSeconds());
    	m_settings.save();
    	super.onPause();
    }
    
    /*
     * This will be used to make sure we read back the values saved
     * from the settings intent. 
     */
    @Override
    public void onStart(){
    	
    	// if we are suppose to do stuff
    	if (m_settings.getApplyUpdateNow()) 
    	{
    		updateBlinds();
    		
    		// destroy current timer
    		m_timer.destroy();
    		m_timer = new CountDownTimerComplex(this);
    		m_timer.startTiming((int) m_settings.getMinutes() * 60);
    		
    		m_settings.setApplyUpdateNow(false);
    		m_settings.save();
    	}
    	super.onStart();
    }
    
}