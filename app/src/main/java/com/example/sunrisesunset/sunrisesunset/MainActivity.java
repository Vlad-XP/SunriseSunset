package com.example.sunrisesunset.sunrisesunset;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {


    public static final String TAG = MainActivity.class.getSimpleName();
    private TextView mTVResult;
    private EditText mETSearch;
    private Button mBSearch;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTVResult = findViewById(R.id.tv_result);
        mETSearch = findViewById(R.id.et_search);
        mBSearch = findViewById(R.id.button_search);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Please wait...");
        mDialog.setCancelable(false);

        mBSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeJsonObjectRequest();
            }
        });
    }

    private void makeJsonObjectRequest() {

        showDialog();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                URLs.GL_API_URL + mETSearch.getText().toString().replaceAll("\\s", "%20") + URLs.INPUT_TYPE + URLs.REQUEST_FIELDS + URLs.AK,
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                try {
                    // Parsing json object response
                    // response will be a json object

                    JSONObject jo = (JSONObject) response.getJSONArray("candidates").get(0);
                    jo = jo.getJSONObject("geometry").getJSONObject("location");

                    JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                            URLs.SS_API_URL + URLs.LAT + jo.getString("lat") + URLs.LNG + jo.getString("lng") + URLs.FOMR,
                            null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {

                            try {
                                // Parsing json object response
                                // response will be a json object

                                JSONObject jb = response.getJSONObject("results");
                                String results = convertTimeFromUTCtoDeviceLocalTime(jb.getString("sunrise")) +
                                        "  " + convertTimeFromUTCtoDeviceLocalTime(jb.getString("sunset"));
                                mTVResult.setText(results);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(),
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                            hideDialog();
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d(TAG, "Error: " + error.getMessage());
                            Toast.makeText(getApplicationContext(),
                                    error.getMessage(), Toast.LENGTH_SHORT).show();
                            // hide the progress dialog
                            hideDialog();
                        }
                    });

                    // Adding request to request queue
                    AppController.getInstance().addToRequestQueue(jsonObjReq);

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                hideDialog();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_SHORT).show();
                // hide the progress dialog
                hideDialog();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);

    }

    private void showDialog() {
        if (!mDialog.isShowing())
            mDialog.show();
    }

    private void hideDialog() {
        if (mDialog.isShowing())
            mDialog.dismiss();
    }

    /**
     * Creates a new request.
     *
     * @param utcTime time in UTC
     */
    private String convertTimeFromUTCtoDeviceLocalTime(String utcTime) {

        // Time converting
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        Date date;
        long timeInMillisec = 0;
        try {
            // time format given by sunrise-sunset.org api
            date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SS:SS").parse(utcTime);
            timeInMillisec = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        timeInMillisec += tz.getOffset(cal.getTimeInMillis());
        cal.setTimeInMillis(timeInMillisec);

        return new SimpleDateFormat("HH:mm:ss").format(cal.getTime());
    }
}
