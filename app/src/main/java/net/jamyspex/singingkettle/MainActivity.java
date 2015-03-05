package net.jamyspex.singingkettle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.JSONCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.StringCallback;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {


    public String TAG = "singingKettle";
    public String URL = "wss://singingkettleserver.herokuapp.com/phoneWS";
    public String ID = "jamyspex";
    private Button boilBut;
    private Button getStatusBut;
    private Paint kettleOutline;

    private SVG svg;

    private TextView tempTV;
    private TextView powerStateTV;
    private TextView waterLevelTV;

    private ImageView kettleModelImgView;

    private WebSocket webSocket;
    private Timer heartBeat;

    private RestClient http;
    private Canvas can;
    private Bitmap bm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bm = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        can = new Canvas(bm);

        svg = SVGParser.getSVGFromResource(getResources(), R.drawable.drawing);


        kettleOutline = new Paint();
        kettleOutline.setColor(Color.BLACK);
        kettleOutline.setStrokeWidth(4);

        kettleModelImgView = (ImageView) findViewById(R.id.kettleModelImageView);

        boilBut = (Button) findViewById(R.id.boilButton);
        getStatusBut = (Button) findViewById(R.id.getStatusButton);

        tempTV = (TextView) findViewById(R.id.tempTextView);
        powerStateTV = (TextView) findViewById(R.id.powerStateTextView);
        waterLevelTV = (TextView) findViewById(R.id.waterLevelTextView);

        boilBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                http.get("boil", new RequestParams("id", "jamyspex"), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Toast.makeText(getApplicationContext(), "Kettle boiling!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Toast.makeText(getApplicationContext(), "404 Kettle not found!", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        getStatusBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                webSocket.send("{\"type\":\"heartbeat\", \"id\":\"" + ID + "\"}");
//                http.get("status", new RequestParams("id", "jamyspex"), new JsonHttpResponseHandler() {
//                    private int statusCode;
//                    private Header[] headers;
//                    private byte[] responseBody;
//                    private Throwable error;
//
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                        try {
//                            tempTV.setText("Temperature: " + response.getString("temperature"));
//                            waterLevelTV.setText("Water Level: " + response.getString("waterLevel"));
//                            powerStateTV.setText("Power State: " + response.getString("powerState"));
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
//
//                });
            }
        });

        http.get("status", new RequestParams("id", "jamyspex"), new JsonHttpResponseHandler() {
            private int statusCode;
            private Header[] headers;
            private byte[] responseBody;
            private Throwable error;

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    tempTV.setText("Temperature: " + response.getString("temperature"));
                    waterLevelTV.setText("Water Level: " + response.getString("waterLevel"));
                    powerStateTV.setText("Power State: " + response.getString("powerState"));

                    drawKettleModel();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        });


    }
    @Override
    protected void onStart() {
        super.onStart();
        initWebSocket();
    }

    @Override
    protected void onStop() {
        super.onStop();
        webSocket.close();
        heartBeat.cancel();
    }

    public void initWebSocket()
    {
        AsyncHttpClient.getDefaultInstance().websocket(URL, "my-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket ws) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }

                webSocket = ws;

                JSONObject phoneConnect = new JSONObject();

                try {
                    phoneConnect.put("type", "phoneConnect");
                    phoneConnect.put("id", ID);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                webSocket.send(phoneConnect.toString());

                heartBeat = new Timer();
                heartBeat.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        webSocket.send("{\"type\":\"heartbeat\", \"id\":\"" + ID + "\"}");
                    }
                }, 10000l, 10000l);

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        Log.i(TAG, s);


                        try {

                            JSONObject incoming = new JSONObject(s);

                            String type = incoming.getString("type");

                            if(type.contains("waterLevel"))
                            {
                                waterLevelTV.setText(incoming.getString("waterLevel"));
                            }
                            else if(type.contains("temperature"))
                            {
                                tempTV.setText(incoming.getString("temperature"));
                            }
                            else if(type.contains("powerState"))
                            {
                                powerStateTV.setText(incoming.getString("powerState"));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });

                webSocket.setDataCallback(new DataCallback() {

                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        Log.i(TAG, "onDataAvailable");


                        bb.recycle();
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void drawKettleModel()
    {
        Path outline = new Path();

        Picture picture = svg.getPicture();

        can.drawPicture(picture);

        kettleModelImgView.setImageBitmap(bm);
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
}
