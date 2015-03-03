package net.jamyspex.singingkettle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    private Button boilBut;
    private Button getStatusBut;
    private Paint kettleOutline;

    private SVG svg;

    private TextView tempTV;
    private TextView powerStateTV;
    private TextView waterLevelTV;

    private ImageView kettleModelImgView;

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
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                });
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
