package com.example.cookerwhistlecounter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "RIYAZ";
    private SoundMeter sm;
    private Thread thread;
    private static boolean isMicRecStoped = false;
    private static final int SAMPLE_DELAY = 160;
    private boolean micRecording = false;
    private MediaPlayer player;
    private int previousWhistleCount = 0;
    TextView av;
    ConstraintLayout alert;
    TextView alertMsg;

    int whistleCount;
    TextView amp;
    TextView freq;
    TextView samp;
    TextView miss;

    private void checkRecordPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkRecordPermission();
        player = MediaPlayer.create(this, R.raw.alert_sound);
        player.setLooping(true);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        alertMsg = findViewById(R.id.alertMessage);
        alert = findViewById(R.id.alert);
        av = findViewById(R.id.whistleCount);
        whistleCount = Integer.parseInt(av.getText().toString());
        amp = findViewById(R.id.amp);
        freq = findViewById(R.id.freq);
        samp = findViewById(R.id.samp);
        miss = findViewById(R.id.miss);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int samp_cnt = 0, miss_cnt = 0;
    private static final double AMP_MIN = 1500.0, FREQ_MIN = 1200.0;

    private int getUserRequestedCount() {
        EditText av = findViewById(R.id.numOfWhistles);
        if(av == null) {
            return -1;
        }
        return Integer.parseInt(av.getText().toString());
    }

    private void incrementWhistleCount() {
        if(av == null) {
            return;
        }
        whistleCount++;
        Log.d(TAG, "Inside IncrementWhistleCount(), whistleCount ="+whistleCount);
        av.setText(String.valueOf(whistleCount));
        if(whistleCount >= getUserRequestedCount()) {
            if(!player.isPlaying()) {
                player.start();
                alertMsg.setText("Turn Off Cooker it’s "+whistleCount+" Whistles");
                alert.setVisibility(View.VISIBLE);

//                try{
//                    Snackbar.make(getWindow().getCurrentFocus(), "Replace with your own action", Snackbar.LENGTH_LONG)
//                            .setAction("Action", null).show();
//                } catch (Exception e){
//                    e.printStackTrace();
//                }

            }
        }
    }

    private void handleSample(double amp, double freq) {
        if(amp > AMP_MIN && freq > FREQ_MIN) {
            samp_cnt++;
            if(samp_cnt > 5) {
                miss_cnt = 0;
            }
        } else {
            miss_cnt++;
            if(miss_cnt > 3) {
                if(samp_cnt > 10) {
                    Log.d(TAG, "Inside handleSample(), whistleCount ="+whistleCount+1+"| With miss count = "+miss_cnt+" & samp count = "+samp_cnt);

                    incrementWhistleCount();
                }
                samp_cnt = 0;
            }
        }
    }

    private void startRecording() {
        isMicRecStoped = false;
        sm = new SoundMeter();
        sm.start();
        thread = new Thread(new Runnable() {
            public void run() {
                while (thread != null && !thread.isInterrupted()) {
                    try {
                        Thread.sleep(SAMPLE_DELAY);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(sm != null) {
                                double amplitude = sm.getAmplitude();
                                handleSample(amplitude, sm.freq);

                                amp.setText("amp = " + amplitude);
                                freq.setText("freq = " + sm.freq);
                                samp.setText("samp = " + samp_cnt);
                                miss.setText("miss = " + miss_cnt);

//                                av.setText("amplitude = " + String.valueOf(amplitude) +
//                                        " freq = " + String.valueOf(sm.freq) +
//                                        " samp = " + String.valueOf(samp_cnt) +
//                                        " miss = " + String.valueOf(miss_cnt));
                            }
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void stopRecording() {
        isMicRecStoped = true;
        try {
            if(sm != null) {
                sm.stop();
                sm = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        thread.interrupt();
        thread = null;
    }

    public void onButton(View view) {
        Button bt = findViewById(R.id.reset);
        if(micRecording == false) {
            startRecording();
            bt.setText("Stop");
            bt.setBackgroundTintList(this.getResources().getColorStateList(R.color.stop_button_color));

        } else{
            stopRecording();
            bt.setText("Start");
            bt.setBackgroundTintList(this.getResources().getColorStateList(R.color.start_button_color));

            samp_cnt = 0;
            miss_cnt = 0;
            previousWhistleCount = whistleCount;
            whistleCount = 0;
            alert.setVisibility(View.GONE);

            av.setText(String.valueOf(whistleCount));
            TextView whistleCountView = findViewById(R.id.previousWhistleCount);
            whistleCountView.setText("Previous Whistle Count = "+ previousWhistleCount);
            samp.setText("samp = " + samp_cnt);
            miss.setText("miss = " + miss_cnt);
            if(player.isPlaying()) {
                player.pause();
            }
        }
        micRecording = !micRecording;
    }
}
