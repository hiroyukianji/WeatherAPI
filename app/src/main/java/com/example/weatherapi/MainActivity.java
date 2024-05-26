package com.example.weatherapi;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.os.HandlerCompat;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final static String URL_WEATHER ="https://weather.tsukumijima.net/api/forecast?city=";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 縦方向にスクロールバーを設定
        TextView textViewResult = findViewById(R.id.txtResult);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());

        // ラジオボタン
        // 初期値設定
        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radioButton1);

        //
        Button button = findViewById(R.id.btnWeather);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //　ボタンが押されたら、ソフトウェアキーボードを非表示にする.
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                // データ取得処理
                callThreadAndConnect_AsyncTask();
            }
        });
    }

    /**
     * 天気取得ボタン処理
     * 入力された都市コードを取得して、天気取得APIにhttp通信を非同期通信を行いお天気を取得する
     */
    private void callThreadAndConnect_AsyncTask() {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast;

        // 入力されている都市コードを取得
        EditText cityInput = (EditText)findViewById(R.id.editCityCode);
        String cityCode= cityInput.getText().toString();

        if(cityCode.isEmpty()){
            toast = Toast.makeText(context,  "@string/errCityCodeEmpty", duration);
            toast.show();
            return;
        }

        // ラジオグループのオブジェクトを取得
        RadioGroup rg = (RadioGroup)findViewById(R.id.radioGroup);
        // チェックされているラジオボタンの ID を取得
        int id = rg.getCheckedRadioButtonId();
        // チェックされているラジオボタンオブジェクトを取得
        RadioButton radioButton = (RadioButton)findViewById(id);

        // 選択されたラジオボタンを表示
        toast = Toast.makeText(context, radioButton.getText(), duration);
        toast.show();

        //　API内容を表示
        CharSequence text = URL_WEATHER + cityCode;
        toast = Toast.makeText(context, text, duration);
        toast.show();

        // どちらの取得方法が選択されているか、ラジオボタンの確認
        RadioButton radio1 = (RadioButton)findViewById(R.id.radioButton1);
        if(radio1.isChecked()){
            // AsyncTask　を利用した非同期処理の例
            MyAsyncTask myAsyncTask = new MyAsyncTask();
            myAsyncTask.execute(URL_WEATHER + cityCode, "");
        } else {
            // Executorsを使った非同期処理を行う
            asyncExecute(URL_WEATHER + cityCode);
        }
        // 入力クリア
//        cityInput.setText("");
    }

    /**
     * getAPI
     * Http通信：GET
     * @param strUrl
     * @return
     */
    private String getAPI(String strUrl){
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        String result = "";
        String str = "";
        try {
            URL url = new URL(strUrl);
            // 接続先URLへのコネクションを開く．まだ接続されていない
            urlConnection = (HttpURLConnection) url.openConnection();
            // 接続タイムアウトを設定
            urlConnection.setConnectTimeout(10000);
            // レスポンスデータの読み取りタイムアウトを設定
            urlConnection.setReadTimeout(10000);
            // ヘッダーにUser-Agentを設定
            urlConnection.addRequestProperty("User-Agent", "Android");
            // ヘッダーにAccept-Languageを設定
            urlConnection.addRequestProperty("Accept-Language", Locale.getDefault().toString());
            // HTTPメソッドを指定
            urlConnection.setRequestMethod("GET");
            //リクエストボディの送信を許可しない
            urlConnection.setDoOutput(false);
            //レスポンスボディの受信を許可する
            urlConnection.setDoInput(true);
            // 通信開始
            urlConnection.connect();
            // レスポンスコードを取得
            int statusCode = urlConnection.getResponseCode();
            // レスポンスコード200は通信に成功したことを表す
            if (statusCode == 200){
                inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                // 1行ずつレスポンス結果を取得しstrに追記
                result = bufferedReader.readLine();
                while (result != null){
                    str += result;
                    result = bufferedReader.readLine();
                }
                bufferedReader.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // レスポンス結果のJSONをString型で返す
        return str;
    }

    /**
     * postAPI
     * Http通信：POST
     * @param strUrl
     * @return
     */
    private String postAPI(String strUrl){
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        String result = "";
        String str = "";
        try {
            URL url = new URL(strUrl);
            // 接続先URLへのコネクションを開く．まだ接続されていない
            urlConnection = (HttpURLConnection) url.openConnection();
            // リクエストボディに格納するデータ
            // String postData = "name=foge&type=fogefoge";
            String postData = "";
            // 接続タイムアウトを設定
            urlConnection.setConnectTimeout(10000);
            // レスポンスデータの読み取りタイムアウトを設定
            urlConnection.setReadTimeout(10000);
            // ヘッダーにUser-Agentを設定
            urlConnection.addRequestProperty("User-Agent", "Android");
            // ヘッダーにAccept-Languageを設定
            urlConnection.addRequestProperty("Accept-Language", Locale.getDefault().toString());
            // HTTPメソッドを指定
            urlConnection.setRequestMethod("POST");
            //レスポンスボディの受信を許可する
            urlConnection.setDoInput(true);
            // リクエストボディの送信を許可する
            urlConnection.setDoOutput(true);
            // 通信開始
            urlConnection.connect();
            // リクエストボディの書き込みを行う
            outputStream = urlConnection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"));
            bufferedWriter.write(postData);
            bufferedWriter.flush();
            bufferedWriter.close();

            // レスポンスコードを取得
            int statusCode = urlConnection.getResponseCode();
            // レスポンスコード200は通信に成功したことを表す
            if (statusCode == 200){
                inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                // 1行ずつレスポンス結果を取得しstrに追記
                result = bufferedReader.readLine();
                while (result != null){
                    str += result;
                    result = bufferedReader.readLine();
                }
                bufferedReader.close();
            }
            urlConnection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // レスポンス結果のJSONをString型で返す
        return str;
    }
    /**
     * 非同期通信方法1
     * 「AsyncTask」
     * Android11(APIレベル30）で、「AsyncTask」が非推奨にはなったが使用可能
     * ターゲット端末が、Android11以前であれば、「AsyncTask」でも良いのではないか
     */
    private class MyAsyncTask extends AsyncTask<String, Integer, String> {


        // doInBackgroundメソッドの実行前にメインスレッドで実行されます。
        // 非同期処理前に何か処理を行いたい時などに利用。
        @Override
        protected void onPreExecute(){
        }

        // メインスレッドとは別のスレッドで実行されます。
        // 非同期で処理したい内容を記述します。必須で処理内容を必ず記述する
        @Override
        protected String doInBackground(String... params) {
            String strData;
            try {
                // myAsyncTask.execute の引数から取得（必要に応じて使用）
                String strParam0 = params[0];
                String strParam1 = params[1];
                // HTTP通信  GET
                strData = new String(getAPI(strParam0));
                // HTTP通信  POST
                //strData = new String(postAPI(strParam0));
            } catch (Exception e) {
                e.printStackTrace();
                strData = null;
            }
            return strData;
        }

        // メインスレッドで実行されます。
        // 途中経過をメインスレッドに返します。
        // 非同期処理の進行状況をプログレスバーで 表示したい時などに使うことができます。
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        // doInBackgroundメソッドの実行後にメインスレッドで実行されます。
        // 結果をメインスレッドに返します。
        // doInBackgroundメソッドの戻り値をこのメソッドの引数として受け取り、その結果を画面に反映させることができます。
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String response = result;
            TextView textView = findViewById(R.id.txtResult);
            TextView textResult1 = findViewById(R.id.txtResult1);
            TextView textResult2 = findViewById(R.id.txtResult2);
            TextView textResult3 = findViewById(R.id.txtResult3);
            try {
                // JSON 全体
                JSONObject rootJSON = new JSONObject(response);
                textView.setText(rootJSON.toString());

                // JSONの一部を取得する場合

                // 項目が１つだけの場合
                // Executorとは違う情報をjsonから取り出す
                String jsonTitle  =  rootJSON.getString("title");
                textResult1.setText(jsonTitle);

                // 配列データの場合
                // 配列データは、forecastsしかない
                JSONArray forecasts = rootJSON.getJSONArray("forecasts");
                for (int i=0; i < forecasts.length(); i++) {
                    JSONObject person = forecasts.getJSONObject(i);
                    String itemDate = person.getString("date");
                    String itemDateLabel = person.getString("dateLabel");
                    String itemTelop = person.getString("telop");
                    textResult2.setText(itemDate  + " : " + itemDateLabel + " : " + itemTelop);
                }

                // Executorとは違う情報をjsonから取り出す
                JSONObject copyright = rootJSON.getJSONObject("copyright");
                String copyrightTitle = copyright.getString("title");
                String copyrightLink = copyright.getString("link");
                textResult3.setText(copyrightTitle + " : " + copyrightLink );

            } catch (JSONException e) {
//                throw new RuntimeException(e);
                textView.setText("処理出来ませんでした。");
            }
        }
        // 実行順番
        // 1.onPreExecute()     　メインスレッド
        // 2.doInBackground()   　メインスレッドとは別
        // 3.onProgressUpdate() 　メインスレッド　※doInBackground()で、publishProgress()が呼ばれた場合に処理。
        // 4.onPostExecute()      メインスレッド
    }

    /**
     * 非同期通信方法２
     * Android11(APIレベル30）で、「AsyncTask」が非推奨になったため、AsyncTaskとは別の非同期通信方法
     * https://qiita.com/b150005/items/6cd72acc75920be893f6
     * Executorを用いた非同期処理を実装する手順は、以下の通り。
     * 　1.Executorsクラスメソッドを用いてワーカースレッドを生成し、スレッドプールに追加
     *  2.ワーカースレッドでの処理を終えた後にUIスレッドで処理を続行する場合は、HandlerCompatクラスメソッドを用いてHandlerオブジェクトを生成
     *  3.3Runnableインタフェースを実装したクラスを作成し、run()メソッドをオーバーライドして自動的に実行される処理を記述
     * 　4.スレッドプールに、3.で作成したクラスのオブジェクト(=Runnableオブジェクト)を送信
     */
    @UiThread
    public void asyncExecute(String url) {
        // 参考サイトは以下
        // https://qiita.com/b150005/items/6cd72acc75920be893f6
        // Executorを用いた非同期処理を実装する手順は、以下の通り。
        // 1.Executorsクラスメソッドを用いてワーカースレッドを生成し、スレッドプールに追加
        // 2.ワーカースレッドでの処理を終えた後にUIスレッドで処理を続行する場合は、HandlerCompatクラスメソッドを用いてHandlerオブジェクトを生成
        // 3.3Runnableインタフェースを実装したクラスを作成し、run()メソッドをオーバーライドして自動的に実行される処理を記述
        // 4.スレッドプールに、3.で作成したクラスのオブジェクト(=Runnableオブジェクト)を送信
        Looper mainLooper = Looper.getMainLooper();
        Handler handler = HandlerCompat.createAsync(mainLooper);
        BackgroundTask backgroundTask = new BackgroundTask(handler, url);
        ExecutorService executorService  = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundTask);
    }
    /**
     * 非同期処理用のクラス
     * ここでは通信処理を行っている
     */
    private class BackgroundTask implements Runnable {
        private final Handler _handler;
        private final String _url;
        public BackgroundTask(Handler handler, String url) {
            _handler = handler;
            _url = url;
        }
        @WorkerThread
        @Override
        public void run() {
            Log.i("Async-BackgroundTask", "ここに非同期処理を記述する");
            String strData;
            try {
                // HTTP通信  GET
                strData = new String(getAPI(_url));
                // HTTP通信  POST
                // strData = new String(postAPI(_url));
            } catch (Exception e) {
                e.printStackTrace();
                strData = null;
            }

            // UIの処理を行うため、
            PostExecutor postExecutor = new PostExecutor(strData);
            _handler.post(postExecutor);
        }
    }
    /**
     * 結果を画面に表示するクラス
     * ここでは通信結果のJSONデータを取り出して画面に表示している
     */
    private class PostExecutor implements Runnable {
        private final String _response;
        public PostExecutor(String response) {
            _response = response;
        }
        @UiThread
        @Override
        public void run() {
            Log.i("Async-PostExecutor", "ここにUIスレッドで行いたい処理を記述する");
            TextView textView = findViewById(R.id.txtResult);
            TextView textResult1 = findViewById(R.id.txtResult1);
            TextView textResult2 = findViewById(R.id.txtResult2);
            TextView textResult3 = findViewById(R.id.txtResult3);
            try {
                // JSON 全体
                JSONObject rootJSON = new JSONObject(_response);
                textView.setText(rootJSON.toString());

                // JSONの一部を取得する場合

                // 項目が１つだけの場合
                // AsyncTaskとは違う情報をJSONから取り出す
                String jsonPublishingOffice  =  rootJSON.getString("publishingOffice");
                textResult1.setText(jsonPublishingOffice);

                // 配列データの場合
                // 配列データは、forecastsしかない
                JSONArray forecasts = rootJSON.getJSONArray("forecasts");
                for (int i=0; i < forecasts.length(); i++) {
                    JSONObject person = forecasts.getJSONObject(i);
                    String itemDate = person.getString("date");
                    String itemDateLabel = person.getString("dateLabel");
                    String itemTelop = person.getString("telop");
                    textResult2.setText(itemDate  + " : " + itemDateLabel + " : " + itemTelop);
                }

                // AsyncTaskとは違う情報をJSONから取り出す
                JSONObject location = rootJSON.getJSONObject("location");
                String locationArea = location.getString("area");
                String locationPrefecture = location.getString("prefecture");
                String locationDistrict = location.getString("district");
                String locationCity = location.getString("city");
                textResult3.setText( locationArea  + " : " + locationPrefecture+ " : " + locationDistrict + " : " +  locationCity);

            } catch (JSONException e) {
//                throw new RuntimeException(e);
                textView.setText("処理出来ませんでした。");
            }
        }
    }
}