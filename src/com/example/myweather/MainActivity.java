package com.example.myweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.zip.GZIPInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.widget.Toast;
import com.example.bean.TodayWeather;
import com.example.util.NetUtil;
import com.example.util.PinYin;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    private ImageView mUpdateBtn,weatherImg,pmImg,mCitySelect;
    private TextView cityTv,timeTv,shiduTv,weekTv,pmTv,qualityTv,tempertureTv,tianqiTv,fengliTv;

    void initView(){
        cityTv =(TextView) findViewById(R.id.content_city_name);
        timeTv =(TextView) findViewById(R.id.content_publish_time);
        shiduTv =(TextView) findViewById(R.id.content_humidity);
        weekTv =(TextView) findViewById(R.id.content_date);
        pmTv =(TextView) findViewById(R.id.content_pm_describe);
        qualityTv =(TextView) findViewById(R.id.content_pm_value);
        tempertureTv =(TextView) findViewById(R.id.content_temperature);
        tianqiTv =(TextView) findViewById(R.id.content_weather_describe1);
        fengliTv =(TextView) findViewById(R.id.content_weather_describe2);
        weatherImg = (ImageView)findViewById(R.id.content_weather_pic);
        pmImg = (ImageView)findViewById(R.id.content_pm_pic);
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        shiduTv.setText("N/A");
        weekTv.setText("N/A");
        pmTv.setText("N/A");
        qualityTv.setText("N/A");
        tempertureTv.setText("N/A");
        tianqiTv.setText("N/A");
        fengliTv.setText("N/A");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        mCitySelect = (ImageView)findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);
        initView();
        /*weatherImg = (ImageView)findViewById(R.id.title_share);
        weatherImg.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this,ShareActivity.class);
				startActivity(intent);
			}
		});*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORK_NULL) {
            Log.d("myweather", "Internet connected!");
            Toast.makeText(MainActivity.this, "网OK!", Toast.LENGTH_LONG).show();
        } else {
            Log.d("myweather", "Not connected");
            Toast.makeText(MainActivity.this, "网挂了!", Toast.LENGTH_LONG).show();
        }
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



    public static final int UPDATE = 0;
    public static final int RESPONSE = 1;

    public void onClick(View view) {
        if (view.getId() == R.id.title_update_btn) {
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");
            Log.d("myweather", cityCode);
            queryWeatherCode(cityCode);

            } else if(view.getId()== R.id.title_city_manager){
            	Intent i = new Intent(this,SelectCity.class);
            	startActivity(i);
            }
        }



    public void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myweather", address);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HttpClient httpClient = new DefaultHttpClient();
                            HttpGet httpGet = new HttpGet(address);
                            HttpResponse httpResponse = httpClient.execute(httpGet);
                            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                                HttpEntity entity = httpResponse.getEntity();

                                InputStream responseStream = entity.getContent();
                                responseStream = new GZIPInputStream(responseStream);

                                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                                StringBuilder response = new StringBuilder();
                                String str;
                                while ((str = reader.readLine()) != null) {
                                    response.append(str);
                                }
                                parseXML(response.toString());
                                Log.d("myweather",response.toString());
        
/*
                                Message msg = new Message();
                                msg.what = UPDATE;
                                msg.obj = response.toString();
                                handler.sendMessage(msg);
*/
                            }
                        } catch (Exception e)

                        {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE:
                    updateWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };


    public TodayWeather parseXML(String xmldata) {

        TodayWeather todayWeather = new TodayWeather();
        try {
            int fengxiangCount = 0;
            int windCount = 0;
            int temCount = 0;
            int dateCount = 0;
            int highCount = 0;
            int lowCount = 0;
            int typeCount = 0;
            int humidityCount = 0;
            int updatetimeCount = 0;
            int pm25Count = 0;
            int qualityCount = 0;
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myapp", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("city")) {
                            eventType = xmlPullParser.next();
                            todayWeather.setCity(xmlPullParser.getText());
                            Log.d("city:", xmlPullParser.getText());
                        } else if (xmlPullParser.getName().equals("updatetime") && updatetimeCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setUpdatetime(xmlPullParser.getText());
                            Log.d("updatetime", xmlPullParser.getText());
                            updatetimeCount++;
                        } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setHigh(xmlPullParser.getText().substring(3));
                            Log.d("high", xmlPullParser.getText().substring(3));
                            highCount++;
                        } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setLow(xmlPullParser.getText().substring(3));
                            Log.d("low", xmlPullParser.getText().substring(3));
                            lowCount++;
                        } else if (xmlPullParser.getName().equals("wendu") && temCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setTemperature(xmlPullParser.getText());
                            Log.d("tem", xmlPullParser.getText());
                            temCount++;
                        } else if (xmlPullParser.getName().equals("fengli") && windCount == 0) {
                            eventType = xmlPullParser.next();
                            Log.d("wind", xmlPullParser.getText());
                            todayWeather.setWind(xmlPullParser.getText());
                            windCount++;
                        } else if (xmlPullParser.getName().equals("pm25") && pm25Count == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setPm25(xmlPullParser.getText());
                            Log.d("pm2.5", xmlPullParser.getText());
                            pm25Count++;
                        } else if (xmlPullParser.getName().equals("quality") && qualityCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setQuality(xmlPullParser.getText());
                            Log.d("quality", xmlPullParser.getText());
                            qualityCount++;
                        } else if (xmlPullParser.getName().equals("shidu") && humidityCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setHumidity(xmlPullParser.getText());
                            Log.d("humidity", xmlPullParser.getText());
                            humidityCount++;
                        } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                            eventType = xmlPullParser.next();
                            todayWeather.setClimate(xmlPullParser.getText());
                            Log.d("type", xmlPullParser.getText());
                            typeCount++;
                        } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
/*
                            Calendar cal = Calendar.getInstance();
                            int date = cal.get(Calendar.DATE);
*/
                            eventType = xmlPullParser.next();
                            todayWeather.setDate(xmlPullParser.getText());
/*
                            Log.d("date:", String.valueOf(date));
*/
                            dateCount++;
/*
                            Pattern pattern = Pattern.compile("(^\\d+)");
                            Matcher matcher = pattern.matcher(xmlPullParser.nextText());
                            if (matcher.find()) {
                                Log.d("find:", String.valueOf(matcher.find()));
                                if (Integer.parseInt(matcher.group(0)) == date) {
                                    Pattern p2 = Pattern.compile("(ÐÇÆÚ.)");
                                    Matcher m2 = p2.matcher(xmlPullParser.nextText());
                                    if (m2.find()) {
                                        String week = m2.group(0);
                                        Log.d("week:", week);
                                    }
                                }
                            }
*/
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;

                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (todayWeather != null) {
            Message msg= new Message();
            msg.what=UPDATE;
            msg.obj=todayWeather;
            handler.sendMessage(msg);
            Log.d("ALL", todayWeather.toString());
        }
            return todayWeather;
    }

    public void updateWeather(TodayWeather today) {
    	int pm25 = Integer.parseInt(today.getPm25().trim());
    	String pmImgStr = "0_50";
    	if(pm25>50&&pm25<101){
    		pmImgStr = "51_100";
    	} else if(pm25>100&&pm25<151){
    		pmImgStr = "101_150";
    	} else if(pm25>150&&pm25<201){
    		pmImgStr = "151_200";
    	} else if(pm25>200&&pm25<301){
    		pmImgStr = "201_300";
    	} else if(pm25>300){
    		pmImgStr = "greater_300";
    	}
    	String climateImg = "biz_plugin_weather_"+ PinYin.convertToPinYin(today.getClimate());
    	pmImgStr = "biz_plugin_weather_"+pmImgStr;
    	Class aClass = R.drawable.class;
    	int pmId = -1;
    	int climateId = -1;
    	try{
    		Field pmfield = aClass.getField(pmImgStr);
    		Object pmValue = pmfield.get(new Integer(0));
    		pmId = (Integer)pmValue;
    		
    		Field clfield = aClass.getField(climateImg);
    		Object clValue = clfield.get(new Integer(0));
    		climateId = (Integer)clValue;
    	} catch(Exception e){
    		if(-1==pmId){
    			pmId = R.drawable.biz_plugin_weather_0_50;
    		}
    		if(-1==climateId){
    			climateId = R.drawable.biz_plugin_weather_qing;
    		}
    	} finally{
    		Drawable drawable = getResources().getDrawable(pmId);
    		pmImg.setImageDrawable(drawable);
    		drawable = getResources().getDrawable(climateId);
    		weatherImg.setImageDrawable(drawable);
    	
	        Log.d("xml",today.toString());
	        cityTv.setText(today.getCity());
	        timeTv.setText(today.getUpdatetime() + "发布");
	        shiduTv.setText("湿度：" + today.getHumidity());
	        pmTv.setText(today.getPm25());
	        qualityTv.setText(today.getQuality());
	        weekTv.setText(today.getDate());
	        tempertureTv.setText(today.getTemperature()+"℃"+"("+today.getLow() + "~" + today.getHigh()+")");
	        tianqiTv.setText(today.getClimate());
	        fengliTv.setText(today.getWind());
	        Toast.makeText(MainActivity.this,"更新成功",Toast.LENGTH_SHORT).show();
    	}
    }
}