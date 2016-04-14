package com.example.administrator.qqlogindemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.beacon.event.UserAction;
import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.qqgame.client.scene.model.GameModel;
import com.tencent.tauth.Tencent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {

    private Tencent mTencent;
    private Button loginBtn;
    private Button logoutBtn;
    private Button shareBtn;
    private Button getUserInfoBtn;
    private UserInfo userInfo; //用户信息
    private Context context;
    private Handler mhandler = null;

    private Bitmap bitmap;
    //UIListener
    BaseUiListener baseUiListener;

    int versioncode;
    private String versionName;
    private int serverVersionCode;
    private String serverVersionName;
    private ProgressDialog progressDialog = null;
    private int progressLength;
    private int progressTotalLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        baseUiListener = new BaseUiListener(this, "all") {
            @Override
            protected void doComplete(Object o) {
                super.doComplete(o);

                String string = o.toString().replace("，", "\n");
                new AlertDialog.Builder(context).setTitle("onComplete").setMessage(o.toString())
                        .setNegativeButton("知道了", null).create().show();

                try {
                    JSONObject json = (JSONObject) o;
                    String token = json.getString(Constants.PARAM_ACCESS_TOKEN);
                    String expire = json.getString(Constants.PARAM_EXPIRES_IN);
                    String openId = json.getString(Constants.PARAM_OPEN_ID);

                    if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expire) && !TextUtils.isEmpty(openId)) {
                        mTencent.setAccessToken(token, expire);
                        mTencent.setOpenId(openId);

                        UserAction.setUserID(openId);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        mhandler = new Handler(){

           @Override
            public void handleMessage(Message msg){
                switch ( msg.what){
                    case 1 :

                        try {

                            ImageView view = (ImageView) findViewById(R.id.imageView);
                            view.setImageBitmap(bitmap);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;

                    case 2 : {

                        try {
                            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                            if ( packageInfo.versionCode != serverVersionCode || !packageInfo.versionName.equals(serverVersionName)){
                                //TODO update
                                new AlertDialog.Builder(context).setTitle("更新").setMessage("确认更新?").
                                        setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                progressDialog = new ProgressDialog(MainActivity.this);
                                                progressDialog.setTitle("正在下载");
                                                progressDialog.setMessage("请稍后...");
                                                progressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
                                                progressDialog.setCancelable(false);
                                                progressDialog.setProgress(0);

                                                doVersionUpgrade();

                                            }
                                        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                          //do nothing
                                    }
                                }).create().show();
                            }
                            else{
                                //
                                Toast.makeText( context, "已经是最新版本",Toast.LENGTH_SHORT).show();
                            }

                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case 3:
                        progressDialog.setMax(progressTotalLength);
                        progressDialog.setProgress(0);
                        progressDialog.show();
                        break;
                    case 4:
                        progressDialog.setProgress(progressLength);
                        break;

                    case 5:
                        File file = new File(Environment.getExternalStorageDirectory(), Config.UPDATE_SAVENAME);
                        if ( !file.exists() ){
                            return;
                        }
//                        String cmd = "chmod 777" + Environment.getExternalStorageState() + Config.UPDATE_SAVENAME;
//                        try {
//                            Runtime.getRuntime().exec(cmd);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setFlags(intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setDataAndType(Uri.parse("file://"+file.toString()), "application/vnd.android.package-archive");
                        startActivity(intent);
                        finish();
                        break;

                }
           }
        };

        mTencent = Tencent.createInstance("1105250731", this.getApplicationContext());

        UserAction.initUserAction(this);
        InitViews();

        TextView textView = (TextView) findViewById(R.id.version);
        try {
            textView.setText("当前版本号："+Integer.toString(this.getPackageManager().getPackageInfo(getPackageName(),0).versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void doVersionUpgrade() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(Config.UPDATE_SERVER+Config.UPDATE_APKNAME);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setConnectTimeout(5000);
                    conn.setDoInput(true);
                    conn.connect();

                    progressTotalLength = conn.getContentLength();
                    mhandler.sendEmptyMessage(3);

                    InputStream is = conn.getInputStream();

                    //写SD卡
                    FileOutputStream fileOutputStream = null ;
                    if ( is != null ){
                        File file = new File(Environment.getExternalStorageDirectory(), Config.UPDATE_SAVENAME );
                        fileOutputStream = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int ch = -1;
                        progressLength = 0 ;

                        while ((ch = is.read(buf))!=-1){
                            fileOutputStream.write(buf,0,ch);
                            progressLength += ch ;
                            mhandler.sendEmptyMessage(4);

                        }
                        fileOutputStream.flush();
                        if ( fileOutputStream !=null )
                            fileOutputStream.close();
                        is.close();
                    }
                    progressDialog.cancel();

                    mhandler.sendEmptyMessage(5);


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }).start();

    }

    private void InitViews() {
        //TODO：初始化界面控件
        loginBtn = (Button) findViewById(R.id.loginQQBtn);
        loginBtn.setOnClickListener(this);

        logoutBtn = (Button) findViewById(R.id.logoutQQBtn);
        logoutBtn.setOnClickListener(this);

        shareBtn = (Button) findViewById(R.id.shareQQBtn);
        shareBtn.setOnClickListener(this);

        getUserInfoBtn = (Button) findViewById(R.id.getUserInfoBtn);
        getUserInfoBtn.setOnClickListener(this);

        findViewById(R.id.reportBtn1).setOnClickListener(this);
        findViewById(R.id.reportBtn2).setOnClickListener(this);
        findViewById(R.id.checkUpdate).setOnClickListener(this);
        findViewById(R.id.checkIncrementUpdate).setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != mTencent) {
            mTencent.onActivityResultData(requestCode, resultCode, data, baseUiListener);
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void login() {
        if (!mTencent.isSessionValid()) {
            mTencent.login(this, "all", baseUiListener);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginQQBtn:
                login();
                break;
            case R.id.logoutQQBtn:
                logout();
                break;
            case R.id.shareQQBtn:
                shareToQQ();
            case R.id.getUserInfoBtn:
                getUserInfo();
                break;
            case R.id.reportBtn1:
                onreportBtn1Click();
                break;
            case R.id.reportBtn2:
                onReportBtn2Click();
                break;
            case R.id.checkUpdate:
                getServerVersionCode();
                break;
            case R.id.checkIncrementUpdate:
                checkIncrementUpdate();
                break;
        }
    }

    private void checkIncrementUpdate() {

        String sdcardPath = Environment.getExternalStorageDirectory().getPath();
        String oldfileDir = sdcardPath+"/tmp/qqgame_v1.apk";
        String newfileDir = sdcardPath+"/tmp/qqgame_v2.apk";
        String patchfileDir = sdcardPath+"/tmp/qqgame_v1_v2.diff";
        boolean suc= GameModel.restoreAPK(oldfileDir,newfileDir,patchfileDir);
        System.out.println("[makeAPK] suc=" + suc);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        String text = "源文件:"+oldfileDir +"\n" + "diff文件:" + patchfileDir +"\n新文件:"+newfileDir;
        dialog.setMessage(text);
        if( suc ){

            dialog.setTitle("成功");
        }
        else{
           dialog.setTitle("失败");
        }
        dialog.setNegativeButton("确定",null);

        dialog.show();
    }

    private void onReportBtn2Click() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("btn_click","reportBtn11");
        map.put("param_FailCode","123");
        if( UserAction.onUserAction("btnClick2", true, -1, -1, map, false) ){
            System.out.println("report2 success.");
        }
    }

    private void onreportBtn1Click() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("btn_click", "reportBtn");
        if( UserAction.onUserAction("btnClick", true, -1, -1, map, false) ){
            System.out.println("report1 success.");
        }
    }

    private void shareToQQ() {
        Bundle bundle = new Bundle();

//这条分享消息被好友点击后的跳转URL。
        bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://www.baidu.com/");
//分享的标题。注：PARAM_TITLE、PARAM_IMAGE_URL、PARAM_	SUMMARY不能全为空，最少必须有一个是有值的。
        bundle.putString(QQShare.SHARE_TO_QQ_TITLE, "宝贝计划");
//分享的图片URL
        bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_URL,
                "http://img3.cache.netease.com/photo/0005/2013-03-07/8PBKS8G400BV0005.jpg");
//分享的消息摘要，最长50个字
        bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, "点击看看我帅不帅");
//手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替
        bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, "宝贝计划");
//标识该消息的来源应用，值为应用名称+AppId。
        //bundle.putInt(QQShare.SHARE_TO_QQ_TYPE_APP, R.string.APP_ID);

        mTencent.shareToQQ(this, bundle, baseUiListener);
    }

    private void logout() {
        mTencent.logout(this);
    }

    public void getUserInfo() {

        if (mTencent != null) {

            userInfo = new UserInfo(MainActivity.this, mTencent.getQQToken());
            userInfo.getUserInfo(new BaseUiListener(this, "get_simple_user_info") {

                @Override
                public void onComplete(final Object o) {
//                    new AlertDialog.Builder(context).setTitle("onGetUserInfo").setMessage(o.toString())
//                            .setNegativeButton("知道了",null).create().show();

                    final JSONObject json = (JSONObject) o;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                String imageUrl = json.getString("figureurl_qq_2");
                                URL url = new URL(imageUrl);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                                conn.setDoInput(true);
                                conn.connect();

                                InputStream is = conn.getInputStream();
                                bitmap = BitmapFactory.decodeStream(is);
                                is.close();
                                mhandler.sendEmptyMessageDelayed(1,0);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }).start();

                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        if (mTencent != null) {
            mTencent.logout(this);
        }
        super.onDestroy();

    }

    public int getServerVersionCode() {


        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    URL url = new URL(Config.UPDATE_SERVER+ Config.UPDATE_VERJSON ) ;
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setConnectTimeout(5000);

                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader( new InputStreamReader(is));

                    StringBuilder sb = new StringBuilder();

                    String line = null ;

                    while( (line = reader.readLine())!=null){
                        sb.append(line);
                    }
                    String jsonString = sb.toString();

                    JSONArray array = new JSONArray(jsonString);
                    if (array.length()>0){
                        JSONObject obj = array.getJSONObject(0);

                        serverVersionCode = obj.getInt("verCode");
                        serverVersionName = obj.getString("verName");

                        mhandler.sendEmptyMessage(2);
                    }

                }catch ( Exception e )
                {
                    serverVersionCode = -1;
                    serverVersionName = "";
                    e.printStackTrace();
                }
            }
        }).start();


        return  1 ;
    }
}
