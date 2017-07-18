/*
 * Copyright (C) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.fit.samples.basichistoryapi;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;
import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

/**
 * This sample demonstrates how to use the History API of the Google Fit platform to insert data,
 * query against existing data, and remove data. It also demonstrates how to authenticate
 * a user with Google Play Services and how to properly represent data in a {@link DataSet}.
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "BasicHistoryApi";
    private static final int REQUEST_OAUTH = 1;
    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean authInProgress = false;

    public static GoogleApiClient mClient = null;

    public float averageWeightPrev = 0;
    public float averageWeightPrevPrev = 0;
    public long refDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging();
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        buildFitnessClient();
    }

    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or
     *  having multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
            .addApi(Fitness.HISTORY_API)
            .addScope(new Scope(Scopes.FITNESS_BODY_READ))
            .addConnectionCallbacks(
                    new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.i(TAG, "Connected!!!");
                            // Now you can make calls to the Fitness APIs.  What to do?
                            // Look at some data!!

                            new ReadDataTask().execute();
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            // If your connection to the sensor gets lost at some point,
                            // you'll be able to determine the reason and react to it here.
                            if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                            } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                            }
                        }
                    }
            )
            .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult result) {
                    Log.i(TAG, "Google Play services connection failed. Cause: " +
                            result.toString());
                    Snackbar.make(
                            MainActivity.this.findViewById(R.id.main_activity_view),
                            "Exception while connecting to Google Play services: " +
                                    result.getErrorMessage(),
                            Snackbar.LENGTH_INDEFINITE).show();
                }
            })
            .build();
    }


    /**
     * Return a {@link DataReadRequest} for all step count changes in the past week.
     */
    public static DataReadRequest queryFitnessData(long startTime, long endTime) {

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                //.aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                //.bucketByTime(1, TimeUnit.DAYS)
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Log.i(TAG, "Request created!");

        // [END build_read_data_request]
        return readRequest;
    }

    /**
     * Log a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would
     * dump all the data. In this sample, logging also prints to the device screen, so we can see
     * what the query returns, but your app should not log fitness information as a privacy
     * consideration. A better option would be to dump the data you receive to a local data
     * directory to avoid exposing it to other applications.
     */
    public static void printData(DataReadResult dataReadResult) {
        Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
        for (DataSet dataSet : dataReadResult.getDataSets()) {
            dumpDataSet(dataSet);
        }

    }

    // [START parse_dataset]
    private static void dumpDataSet(DataSet dataSet) {

        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());

        DateFormat dateFormat = getTimeInstance();

        Log.i(TAG, "DEBUG String: " + dataSet.getDataSource().toDebugString());
        Log.i(TAG, "Number of Datapoints: " + dataSet.getDataPoints().size());



        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }
    // [END parse_dataset]


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_data) {
            //deleteData();
            return true;
        } else if (id == R.id.action_update_data){
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            MainActivity.this.startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Initialize a custom log class that outputs both to in-app targets and logcat.
     */
    private void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a customized TextView.
        LogView logView = (LogView) findViewById(R.id.sample_logview);

        // Fixing this lint error adds logic without benefit.
        //noinspection AndroidLintDeprecation
        logView.setTextAppearance(this, R.style.Log);

        logView.setBackgroundColor(Color.WHITE);
        msgFilter.setNext(logView);
        Log.i(TAG, "Ready.");
    }

    public class ReadDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            // [START build_read_data_request]
            // Setting a start and end date using a range of 1 week before this moment.
            Calendar cal = Calendar.getInstance();

            Date now = new Date();
            cal.setTime(now);
            cal.set(Calendar.DAY_OF_WEEK, 1);


            long endTime = cal.getTimeInMillis();
            refDate = endTime;

            cal.add(Calendar.WEEK_OF_YEAR, -1);
            long startTime = cal.getTimeInMillis();

            DataReadRequest prevWeek = queryFitnessData(startTime, endTime);

            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mClient, prevWeek).await(1, TimeUnit.MINUTES);

            DataSet ds = dataReadResult.getDataSet(DataType.TYPE_WEIGHT);

            float weightsSum = 0;
            int weightsCount = 0;
            for (DataPoint dp : ds.getDataPoints()) {
                for(Field field : dp.getDataType().getFields()) {
                    weightsCount++;
                    weightsSum += dp.getValue(field).asFloat();
                }
            }
            float prevAverage = (weightsCount == 0 ? 0 : weightsSum / weightsCount);
            averageWeightPrev = prevAverage;
            Log.i(TAG, "WeightsSum: " + weightsSum + " - WeightsCount: " + weightsCount + " - AverageWeight: " + prevAverage);


            printData(dataReadResult);


            endTime = startTime;
            cal.add(Calendar.WEEK_OF_YEAR, -1);
            startTime = cal.getTimeInMillis();

            DataReadRequest weekBefore = queryFitnessData(startTime, endTime);

            dataReadResult =
                    Fitness.HistoryApi.readData(mClient, weekBefore).await(1, TimeUnit.MINUTES);

            ds = dataReadResult.getDataSet(DataType.TYPE_WEIGHT);

            weightsSum = 0;
            weightsCount = 0;
            for (DataPoint dp : ds.getDataPoints()) {
                for(Field field : dp.getDataType().getFields()) {
                    weightsCount++;
                    weightsSum += dp.getValue(field).asFloat();
                }
            }

            float beforeAverage = (weightsCount == 0 ? 0 : weightsSum / weightsCount);
            averageWeightPrevPrev = beforeAverage;

            Log.i(TAG, "WeightsSum: " + weightsSum + " - WeightsCount: " + weightsCount + " - AverageWeight: " + beforeAverage);

            printData(dataReadResult);
            Log.i(TAG, "Weight Change: " + (prevAverage - beforeAverage) + "kg");


            updateWidget();
            return null;
        }
    }

    private void updateWidget() {
        Intent i = new Intent(this, WeiWAProvider.class);
        i.setAction(WeiWAProvider.UPDATE_ACTION);
        i.putExtra("refDate", refDate);
        i.putExtra("averageWeightPrev", averageWeightPrev);
        i.putExtra("averageWeightPrevPrev", averageWeightPrevPrev);
        sendBroadcast(i);
    }

}
