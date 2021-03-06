/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplowtrackerdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import com.snowplowanalytics.snowplow.tracker.classic.Executor;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplowtrackerdemo.utils.DemoUtils;
import com.snowplowanalytics.snowplowtrackerdemo.utils.TrackerEvents;

import java.util.concurrent.TimeUnit;

/**
 * Classic Demo Activity.
 */
public class ClassicDemo extends Activity {

    private Tracker tracker;
    private Button _startButton;
    private EditText _uriField;
    private RadioGroup _type, _security, _collection;
    private RadioButton _radioGet, _radioHttp;
    private TextView _logOutput, _eventsCreated, _eventsSent, _emitterOnline, _emitterStatus,
            _databaseSize, _sessionIndex;

    private int eventsCreated = 0;
    private int eventsSent = 0;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classic_demo);
        setupTrackerListener();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (tracker != null) {
            tracker.pauseEventTracking();
            tracker = null;
        }
        DemoUtils.resetExecutor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tracker != null) {
            tracker.getSession().setIsBackground(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tracker != null) {
            tracker.getSession().setIsBackground(false);
        }
    }

    /**
     * Builds and sets up the Tracker listener for the demo.
     */
    private void setupTrackerListener() {

        _startButton = (Button)findViewById(R.id.btn_lite_start);
        _uriField    = (EditText)findViewById(R.id.emitter_uri_field);
        _type        = (RadioGroup)findViewById(R.id.radio_send_type);
        _security    = (RadioGroup)findViewById(R.id.radio_send_security);
        _collection  = (RadioGroup)findViewById(R.id.radio_data_collection);
        _radioGet   = (RadioButton)findViewById(R.id.radio_get);
        _radioHttp  = (RadioButton)findViewById(R.id.radio_http);
        _logOutput  = (TextView)findViewById(R.id.log_output);
        _eventsCreated  = (TextView)findViewById(R.id.created_events);
        _eventsSent     = (TextView)findViewById(R.id.sent_events);
        _emitterOnline  = (TextView)findViewById(R.id.online_status);
        _emitterStatus  = (TextView)findViewById(R.id.emitter_status);
        _databaseSize   = (TextView)findViewById(R.id.database_size);
        _sessionIndex   = (TextView)findViewById(R.id.session_index);

        context = getApplicationContext();

        _logOutput.setMovementMethod(new ScrollingMovementMethod());
        _logOutput.setText("");

        tracker = DemoUtils.getAndroidTrackerClassic(context, getCallback());

        makePollingUpdater(context);

        _collection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final RadioGroup radioGroup, final int i) {
                DemoUtils.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (i == R.id.radio_data_on) {
                            tracker.resumeEventTracking();
                        } else if (i == R.id.radio_data_off) {
                            tracker.pauseEventTracking();
                        }
                    }
                });
            }
        });

        _startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Emitter e = tracker.getEmitter();
                String uri = _uriField.getText().toString();
                HttpMethod method = _type.getCheckedRadioButtonId() ==
                        _radioGet.getId() ? HttpMethod.GET : HttpMethod.POST;
                RequestSecurity security = _security.getCheckedRadioButtonId() ==
                        _radioHttp.getId() ? RequestSecurity.HTTP : RequestSecurity.HTTPS;

                if (!e.getEmitterStatus()) {
                    e.setEmitterUri(uri);
                    e.setRequestSecurity(security);
                    e.setHttpMethod(method);
                }

                if (!uri.equals("")) {
                    eventsCreated += 28;
                    _eventsCreated.setText("Made: " + eventsCreated);
                    TrackerEvents.trackAll(tracker);
                } else {
                    updateLogger("URI field empty!\n");
                }
            }
        });
    }

    /**
     * Returns the Emitter Request Callback.
     */
    private RequestCallback getCallback() {
        return new RequestCallback() {
            @Override
            public void onSuccess(int successCount) {
                updateLogger("Emitter Send Success:\n " +
                        "- Events sent: " + successCount + "\n");
                updateEventsSent(successCount);
            }
            @Override
            public void onFailure(int successCount, int failureCount) {
                updateLogger("Emitter Send Failure:\n " +
                        "- Events sent: " + successCount + "\n " +
                        "- Events failed: " + failureCount + "\n");
                updateEventsSent(successCount);
            }
        };
    }

    /**
     * Updates the logger with a message.
     *
     * @param message the message to add to the log.
     */
    private void updateLogger(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _logOutput.append(message);

            }
        });
    }

    /**
     * Updates the events sent counter.
     *
     * @param count the amount of successful events
     */
    private void updateEventsSent(final int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                eventsSent += count;
                _eventsSent.setText("Sent: " + eventsSent);
            }
        });
    }

    /**
     * Starts a polling updater which will fetch
     * and update the UI.
     *
     * @param context the activity context
     */
    private void makePollingUpdater(final Context context) {
        DemoUtils.scheduleRepeating(new Runnable() {
            @Override
            public void run() {
                boolean isOnline = Util.isOnline(context);
                Emitter e = tracker.getEmitter();
                boolean isRunning = e.getEmitterStatus();
                long dbSize = e.getEventStore().getSize();
                int sessionIndex = tracker.getSession().getSessionIndex();
                updateEmitterStats(isOnline, isRunning, dbSize, sessionIndex);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Updates the various UI elements based on information
     * about the Tracker and Emitter.
     *
     * @param isOnline is the device online
     * @param isRunning is the emitter running
     * @param dbSize the database event size
     */
    private void updateEmitterStats(final boolean isOnline, final boolean isRunning, final long dbSize,
                                    final int sessionIndex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String online = isOnline ? "Online: yes" : "Online: no";
                _emitterOnline.setText(online);
                String status = isRunning ? "Running: yes" : "Running: no";
                _emitterStatus.setText(status);
                _databaseSize.setText("DB Size: " + dbSize);
                _sessionIndex.setText("Session #: " + sessionIndex);

                if (isRunning) {
                    String startButtonText = _startButton.getText().toString();
                    if (startButtonText.equals("Start!") || startButtonText.equals("Running  .")) {
                        _startButton.setText("Running.  ");
                    } else if (startButtonText.equals("Running.  ")) {
                        _startButton.setText("Running . ");
                    } else {
                        _startButton.setText("Running  .");
                    }
                } else {
                    _startButton.setText("Start!");
                }
            }
        });
    }
}
