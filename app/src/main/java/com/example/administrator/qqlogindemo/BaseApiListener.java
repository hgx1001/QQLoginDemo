package com.example.administrator.qqlogindemo;

import android.app.AlertDialog;
import android.content.Context;

import com.tencent.open.utils.HttpUtils;
import com.tencent.tauth.IRequestListener;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

/**
 * Created by Administrator on 2016/4/11.
 */
public class BaseApiListener implements IRequestListener {

    private Context context;
    public BaseApiListener(Context context) {
        this.context = context;
    }

    @Override
    public void onComplete(JSONObject jsonObject) {

        showResult("onComplete",jsonObject.toString());

        doComplete(jsonObject);

    }

    protected void doComplete(JSONObject jsonObject) {

    }

    @Override
    public void onIOException(IOException e) {

        showResult("onIoExceptio",e.toString());
    }

    @Override
    public void onMalformedURLException(MalformedURLException e) {

        showResult("onMalformedURLException",e.toString());
    }

    @Override
    public void onJSONException(JSONException e) {

    }

    @Override
    public void onConnectTimeoutException(ConnectTimeoutException e) {

    }

    @Override
    public void onSocketTimeoutException(SocketTimeoutException e) {

    }

    @Override
    public void onNetworkUnavailableException(HttpUtils.NetworkUnavailableException e) {

    }

    @Override
    public void onHttpStatusException(HttpUtils.HttpStatusException e) {

    }

    @Override
    public void onUnknowException(Exception e) {

    }

    private void showResult(String title, String text){

        new AlertDialog.Builder(this.context).setTitle(title).setMessage(text)
                .setNegativeButton("知道了",null).create().show();
    }
}
