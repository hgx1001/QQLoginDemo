package com.example.administrator.qqlogindemo;

import android.app.AlertDialog;
import android.content.Context;

import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

/**
 * Created by Administrator on 2016/4/11.
 */
public class BaseUiListener implements IUiListener {
    private Context context;
    private String scope;

    public BaseUiListener(Context context, String scope ){
        super();
        this.context = context;
        this.scope = scope;
    }

    protected void doComplete(Object o) {
        //TODO

    }

    @Override
    public void onComplete(Object o) {

        doComplete(o);

    }

    @Override
    public void onError(UiError uiError) {

        new AlertDialog.Builder(context).setTitle("onError").setMessage(uiError.toString())
                .setNegativeButton("知道了",null).create().show();
    }

    @Override
    public void onCancel() {

        new AlertDialog.Builder(context).setTitle("onCancled").setMessage("onCancle")
                .setNegativeButton("知道了",null).create().show();
    }
}
