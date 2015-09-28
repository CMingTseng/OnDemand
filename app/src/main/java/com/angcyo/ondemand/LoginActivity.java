package com.angcyo.ondemand;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;

import com.angcyo.ondemand.components.RWorkService;
import com.angcyo.ondemand.components.RWorkThread;
import com.angcyo.ondemand.control.RTableControl;
import com.angcyo.ondemand.event.EventLogin;
import com.angcyo.ondemand.event.EventNoNet;
import com.angcyo.ondemand.model.TableCompany;
import com.angcyo.ondemand.model.TableMember;
import com.angcyo.ondemand.model.UserInfo;
import com.angcyo.ondemand.util.PopupTipWindow;
import com.angcyo.ondemand.util.Util;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * Created by angcyo on 15-09-27-027.
 */
public class LoginActivity extends BaseActivity {
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.phone)
    EditText phone;
    @Bind(R.id.pw)
    EditText pw;
    @Bind(R.id.company)
    EditText company;
    @Bind(R.id.login)
    Button login;

    String strPhone, strPw, strCompany;
    UserInfo userInfo;


    @Override
    protected void initView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        setSupportActionBar(toolbar);
        initWindow(R.color.colorAccent);
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));
    }

    @Override
    protected void initAfter() {
        phone.setText("22222222222");
        pw.setText("6666");
        company.setText("顺丰快递");

        if (OdApplication.userInfo != null) {
            launchActivity(MainActivity.class);
            super.onBackPressed();
        }
    }

    @OnClick(R.id.login)
    public void login() {
        if (verifyEdit()) {
            showDialogTip("登录中...");
            RWorkService.addTask(new RWorkThread.TaskRunnable() {
                @Override
                public void run() {
                    if (Util.isNetOk(LoginActivity.this)) {
                        RTableControl.getAllMember();
                        RTableControl.getAllCompany();
                        RTableControl.getAllPlatform();
                        EventBus.getDefault().post(new EventLogin());
                    } else {
                        EventBus.getDefault().post(new EventNoNet());
                    }
                }
            });
        } else {

        }
    }

    private boolean verifyEdit() {
        strPhone = phone.getText().toString();
        strPw = pw.getText().toString();
        strCompany = company.getText().toString();

        if (Util.isEmpty(strPhone) || strPhone.length() != 11) {
            phone.setError("请输入有效手机号码");
            phone.requestFocus();
            return false;
        }
        if (Util.isEmpty(strPw)) {
            pw.setError("请输入密码");
            pw.requestFocus();
            return false;
        }
        if (Util.isEmpty(strCompany)) {
            company.setError("请输入服务商家");
            company.requestFocus();
            return false;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void onEvent(EventLogin event) {
        hideDialogTip();
        for (TableMember member : RTableControl.members) {
            if (member.getPhone().trim().equalsIgnoreCase(strPhone) && member.getPsw().trim().equals(strPw)) {//手机号 和密码 相等
                for (TableCompany company : RTableControl.companys) {
                    if (company.getSid() == member.getId_company() && company.getCaption().trim().equalsIgnoreCase(strCompany)) {//所属公司 相等
                        //登录成功
                        userInfo = new UserInfo();
                        userInfo.member = member;
                        userInfo.company = company;
                        OdApplication.userInfo = userInfo;
                        RWorkService.addTask(new RWorkThread.TaskRunnable() {
                            @Override
                            public void run() {
                                if (Util.isNetOk(LoginActivity.this)) {
                                    RTableControl.updateOnline(OdApplication.userInfo.member.getSid());
                                } else {
                                    EventBus.getDefault().post(new EventNoNet());
                                }
                            }
                        });
                        launchActivity(MainActivity.class);
                        finish();
                        return;
                    }
                }
            }
        }
        PopupTipWindow.showTip(this, "登录失败");
    }
}