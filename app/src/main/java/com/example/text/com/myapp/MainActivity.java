package com.example.text.com.myapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private InstrumentView instrumentView;
    private TextView tvProgress;
    private SeekBar sbProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instrumentView = (InstrumentView) findViewById(R.id.instrument_view);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        sbProgress = (SeekBar) findViewById(R.id.sbProgress);

        setListener();
    }

    private void setListener() {

        instrumentView.setDividerValues(90, 120, 140, 180);

        final int max = 210;
        final int min = 50;
        final int defult = 85;

        setValue(defult);

        sbProgress.setMax(max - min);
        sbProgress.setProgress(defult - min);
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setValue(i + min);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setValue(int i) {
        tvProgress.setText(String.valueOf(i));
        instrumentView.setVelocity(i);
    }

}
