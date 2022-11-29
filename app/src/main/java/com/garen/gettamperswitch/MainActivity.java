package com.garen.gettamperswitch;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IHr40MiscService;
import android.os.IIRService;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements BatteryChangedReceiver.Message{
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1000;
    private BatteryChangedReceiver receiver;
    static String TAG="tamperSwitch";
    static int tamperSwitchPressed = 0;
    static int tamperSwitchReleased = 1;
    static int tamperSwitchInit = -1;
    int status = tamperSwitchInit;
    private static IIRService IR = null;
    private static IHr40MiscService Hr40 = null;

    private static int[] buf_81={0x56,0x01,0xAB,0x00};
    private static int[] buf_82={
            0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,
            0x40,0x00,0x15,0x00,0x40,0x00,0x15,0x00,0x40,0x00,
            0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,
            0x15,0x00,0x15,0x00,0x40,0x00,0x15,0x00,0x40,0x00,
            0x15,0x00,0x40,0x00,0x15,0x00,0x40,0x00,0x15,0x00,
            0x15,0x00,0x15,0x00,0x40,0x00,0x15,0x00,0x15,0x00,
            0x15,0x00,0x15,0x00,0x15,0x00,0x40,0x00,0x15,0x00,
            0x40,0x00,0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,
            0x15,0x00,0x40,0x00,0x15,0x00,0x40,0x00,0x15,0x00,
            0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,
            0x15,0x00,0x15,0x00,0x15,0x00,0x40,0x00,0x15,0x00,
            0x40,0x00,0x15,0x00,0x15,0x00,0x15,0x00,0x15,0x00,
            0x15,0x00,0x40,0x00,0x15,0x00,0x40,0x00,0x15,0x00,
            0x15,0x00};

    /************ AC Detect ****************/
    static int AC_Insert = 0;
    static int AC_PullOut = 1;
    static int AC_DetectInit = -1;
    int AC_DetectStatus = AC_DetectInit;
    int Jni_AC_status = -1;
    int Jni_tamperBtn_status = -1;
    ImageButton battery_icon;
    TextView current_btn_back_light_text,current_screen_btn_back_light,battery_value,battery_status,ir_test_status;
    SeekBar current_btn_back_light_seekbar,current_screen_btn_back_light_seekbar;
    AmountView amountview;
    Button testIR_btn, testIR_more_btn;
    Switch btn_back_light_switch;
    LocationManager locationManager;

    static {    // 静态代码块, 构造一个对象之前被执行，且只会执行一次
       // System.loadLibrary("acStatus"); // load libacStatus.so
//        System.loadLibrary("tamperBtnStatus"); // load libtamperBtnStatus.so
    }
    public native static int getACStatus();  // native 表示 getACStatus 是在 C/C++ 中实现的, 而不是在 Java 中实现的
    public native static int getTamperStatus();

//    public static void main(String[] args) {    // Java 会走 main 函数入口, 但 Activity 不会走 main 入口, 只走 onCreate 函数入口
//
//        Log.d(TAG, "start main . . .");
//        System.loadLibrary("acStatus"); // load libacStatus.so
//    }

    public void initView() {
        battery_icon = findViewById(R.id.battery_icon);
        current_btn_back_light_text = findViewById(R.id.current_btn_back_light_text);
        current_screen_btn_back_light = findViewById(R.id.current_screen_btn_back_light);
        battery_value = findViewById(R.id.battery_value);
        battery_status = findViewById(R.id.battery_status);
        current_btn_back_light_seekbar = findViewById(R.id.current_btn_back_light_seekbar);
        current_screen_btn_back_light_seekbar = findViewById(R.id.current_screen_btn_back_light_seekbar);;
        amountview = findViewById(R.id.amountview);
        testIR_btn = findViewById(R.id.testIR_btn);
        testIR_more_btn = findViewById(R.id.testIR_more_btn);
        ir_test_status = findViewById(R.id.ir_test_status);
        btn_back_light_switch = findViewById(R.id.btn_back_light_switch);
    }

    private void initData() {
        current_screen_btn_back_light_seekbar.setProgress((int)(getScreenBrightness(this)/2.55));
        current_screen_btn_back_light.setText("Current Screen Back Light: " + (int) Math.ceil(getScreenBrightness(this)/2.55) + "%");
    }

    private void setOnClickListen() {
        current_btn_back_light_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //txt_cur.setText("当前进度值:" + progress + "  / 100 ");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(mContext, "触碰SeekBar", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
               // Toast.makeText(mContext, "放开SeekBar", Toast.LENGTH_SHORT).show();
            }
        });

        current_screen_btn_back_light_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current_screen_btn_back_light.setText("Current Screen Back Light: " + progress + "%");
                ModifySettingsScreenBrightness(MainActivity.this,(int)(progress * 2.55));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(mContext, "触碰SeekBar", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Toast.makeText(mContext, "放开SeekBar", Toast.LENGTH_SHORT).show();
                ModifySettingsScreenBrightness(MainActivity.this,(int)(seekBar.getProgress() * 2.55));
            }
        });
        amountview.setOnChangeListener(new AmountView.OnChangeListener() {
            @Override
            public void onChanged(int value) {
                if (amountview.getCurrentValue() == 1){
                    testIR_btn.setText("TESTIR_ONCE");
                    ir_test_status.setText("Send an IR test");
                }else {
                    testIR_btn.setText("TESTIR_MORE_THAN_ONE");
                    ir_test_status.setText("IR send repeat");
                }
            }
        });
        testIR_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (amountview.getCurrentValue() == 1){
                    ir_test_status.setText("IR send once");
                    test_IR_Click("sendOnce",0);
                }else {
                    ir_test_status.setText("IR send repeat");
                    test_IR_Click("sendRepeat", amountview.getCurrentValue());
                }
            }
        });
        testIR_more_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ir_test_status.setText("Continuously send IR test");
                test_IR_Click("keepSending", 0);
            }
        });
        btn_back_light_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    switchBackLight(true);
                }else {
                    switchBackLight(false);
                }
            }
        });
    }

    private void test_IR_Click(String type, int repeatNum) {
        IR = (IIRService)getSystemService(Context.IR_SERVICE);
        if(IR == null){
            Log.d(TAG, "onKeyDown: IR is a null object reference.");
        }else {
            // 为了方便测试，当按下 HR40_POWER 时，会触发 IR 发送事件
            try {
                Log.i(TAG, "IR_open ...");
                IR.IR_open();
                Log.i(TAG, "IR_getFirmwareVersion " + IR.IR_getFirmwareVersion());
                Log.i(TAG, "IR_configCarrier ...");
                IR.IR_configCarrier(40 * 1000, 50);
                Log.i(TAG, "IR_sendPrepareBuffer ...");
                IR.IR_sendPrepareBuffer(buf_81, buf_81.length, buf_82, buf_82.length);
                if(type.equals("sendOnce")) {
                    // 发送一次 IR
                    Log.i(TAG, "IR_startSendOnce ...");
                    IR.IR_startSendOnce();
                } else if(type.equals("sendRepeat")) {
                    // 发送多次 IR
                    Log.i(TAG, "IR_startSend_repeat ...");
                    IR.IR_startSend_repeat(repeatNum);
                } else if(type.equals("keepSending")) {
                    // 持续发送 IR
                    Log.i(TAG, "IR_startSend ...");
                    IR.IR_startSend();
                    if(IR.IR_isSending() == 1) {
                        Log.i(TAG, "Sleep 1 ...");
                        TimeUnit.SECONDS.sleep(1);
                        Log.i(TAG, "IR_stopSend ...");
                        IR.IR_stopSend();
                    }
                }
                Log.i(TAG, "IR_close ...");
                IR.IR_close();
            } catch (RemoteException ex) {
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void switchBackLight(boolean isOpen) {
        Hr40 = (IHr40MiscService)getSystemService(Context.Hr40Misc_SERVICE);
        if(Hr40 == null){
            Log.d(TAG, "onKeyDown: Hr40 is a null object reference.");
        }else {
            // 为了方便测试，当按下 HR40_POWER 时，会触发 IR 发送事件
            try {
                Hr40.Hr40Misc_open();
                if(!Hr40.Hr40Misc_isButtonBacklightOn()) {
                    //btn_back_light_switch.setChecked(true);
                    current_btn_back_light_text.setText("Button back light: On");
                } else {
                    //btn_back_light_switch.setChecked(false);
                    current_btn_back_light_text.setText("Button back light: Off");
                }
                Hr40.Hr40Misc_controlButtonBacklight(isOpen);
            } catch (RemoteException ex) {}
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ix) {}
            try {
                Hr40.Hr40Misc_close();
            } catch (RemoteException ex) {}
        }
    }

    private void initReceiver() {
        if (receiver == null) receiver = new BatteryChangedReceiver();
        registerReceiver(receiver, getIntentFilter());//电池的状态改变广播只能通过动态方式注册
        receiver.setMessage(MainActivity.this);
    }

    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);//This is a sticky broadcast containing the charging state, level, and other information about the battery.
        filter.addAction(Intent.ACTION_BATTERY_LOW);//Indicates low battery condition on the device. This broadcast corresponds to the "Low battery warning" system dialog.
        filter.addAction(Intent.ACTION_BATTERY_OKAY);//This will be sent after ACTION_BATTERY_LOW once the battery has gone back up to an okay state.
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        return filter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allowModifySettings();
        initReceiver();
        initView();
        initData();
        setOnClickListen();


        // set button OnClick callback
        /*ac_Btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View v) {

                Log.d(TAG, "ac_Btn: onClick... ");
//                Jni_AC_status = getACStatus();
//                switch (Jni_AC_status){
//                    case 0: // AC_Insert
//                        ac_ShowStatusTextView.setText("AC Insert");
//                        break;
//                    case 1: // AC_PullOut
//                        ac_ShowStatusTextView.setText("AC Pull Out");
//                        break;
//                    default:
//                        ac_ShowStatusTextView.setText("AC unknown status");
//                        break;
//                }
                Hr40 = (IHr40MiscService)getSystemService(Context.Hr40Misc_SERVICE);
                if(Hr40 == null){
                    Log.d(TAG, "onKeyDown: Hr40 is a null object reference.");
                }else {
                    // 为了方便测试，当按下 HR40_POWER 时，会触发 IR 发送事件
                    try {
                        Hr40.Hr40Misc_open();
                    } catch (RemoteException ex) {}

                    try {
                        Hr40.Hr40Misc_isButtonBacklightOn();
                    } catch (RemoteException ex) {}

                    try {
                        Hr40.Hr40Misc_controlButtonBacklight(true);
                    } catch (RemoteException ex) {}

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ix) {}

                    try {
                        Hr40.Hr40Misc_controlButtonBacklight(false);
                    } catch (RemoteException ex) {}

                    try {
                        Hr40.Hr40Misc_close();
                    } catch (RemoteException ex) {}
                }
            }
        });*/
    }

//    // xml 配置 android:onClick="tamperBtnOnClick"
//    public void tamperBtnOnClick(View v){
//
//        Log.d(TAG, "TamperBtn: onClick... ");
//        Jni_tamperBtn_status = getTamperStatus();
//        switch (Jni_tamperBtn_status){
//            case 0: // tamperPressed
//                Tamper_GetStatusTextView.setText("tamper switch pressed");
//                break;
//            case 1: // tamperReleased
//                Tamper_GetStatusTextView.setText("tamper switch released");
//                break;
//            default:
//                Tamper_GetStatusTextView.setText("tamper switch unknown status");
//                break;
//        }
//    }

    @SuppressLint("WrongConstant")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.d(TAG, "onKeyDown: .... ");
        if( keyCode == KeyEvent.KEYCODE_F1){    // tamper Switch event
            if(event.getAction()==KeyEvent.ACTION_DOWN) {
             //   tamperView.setText("tamper Switch Button pressed");
                this.status = this.tamperSwitchPressed;
                Log.d(TAG, "onKeyDown: pressed ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_HR40_POWER ){ // AC detection event
            if(event.getAction()==KeyEvent.ACTION_DOWN){
         //       acDetectView.setText("AC Insert");
                this.AC_DetectStatus = this.AC_Insert;
                Log.d(TAG, "onKeyDown: POWER down ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_F2 ){ // AC detection event
            if(event.getAction()==KeyEvent.ACTION_DOWN){
           //     acDetectView.setText("AC Insert");
                this.AC_DetectStatus = this.AC_Insert;
                Log.d(TAG, "onKeyDown: AC Insert ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_HR40_MIC_USER_1 ){ //
            if(event.getAction()==KeyEvent.ACTION_DOWN){
                Log.d(TAG, "onKeyDown: KEYCODE_HR40_MIC_USER_1 Down Events.");
            }
        }else{
            Log.d(TAG, "onKeyDown:  " + keyCode + " --> "+ event.toString());
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if( keyCode == KeyEvent.KEYCODE_F1){    // tamper Switch event

            if(event.getAction()==KeyEvent.ACTION_UP){
            //    tamperView.setText("tamper Switch Button released");
                this.status = this.tamperSwitchReleased;
                Log.d(TAG, "onKeyDown: released ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_F2 ){ // AC detection event
            if(event.getAction()==KeyEvent.ACTION_UP){
            //    acDetectView.setText("AC Pull Out");
                this.AC_DetectStatus = this.AC_PullOut;
                Log.d(TAG, "onKeyDown: AC PullOut ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_HR40_POWER ){ // AC detection event
            if(event.getAction()==KeyEvent.ACTION_UP){
          //      acDetectView.setText("AC Insert");
                this.AC_DetectStatus = this.AC_Insert;
                Log.d(TAG, "onKeyDown: POWER Up ");
            }
        }
        else if( keyCode == KeyEvent.KEYCODE_HR40_MIC_USER_1 ){
            if(event.getAction()==KeyEvent.ACTION_UP){
                Log.d(TAG, "onKeyUp: KEYCODE_HR40_MIC_USER_1 Up Events.");
            }
        }
        else{
            Log.d(TAG, "onKeyUp:  " + keyCode + " --> "+ event.toString());
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void getMsg(String str) {
        if(str.startsWith("level:")) {
            String [] power = str.split(":");
            battery_value.setText("Current Battery:" + power[1] + "%");
        }else if(str.startsWith("Status:")) {
            String [] power = str.split(":");
            battery_status.setText(power[1]);
        }
    }

    /**
     * 3.关闭光感，设置手动调节背光模式
     *
     * SCREEN_BRIGHTNESS_MODE_AUTOMATIC 自动调节屏幕亮度模式值为1
     *
     * SCREEN_BRIGHTNESS_MODE_MANUAL 手动调节屏幕亮度模式值为0
     * **/
    public void setScreenManualMode(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            int mode = Settings.System.getInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 5.修改Setting 中屏幕亮度值
     *
     * 修改Setting的值需要动态申请权限 <uses-permission
     * android:name="android.permission.WRITE_SETTINGS"/>
     * **/
    private void ModifySettingsScreenBrightness(Context context,
                                                int birghtessValue) {
        // 首先需要设置为手动调节屏幕亮度模式
        setScreenManualMode(context);

        ContentResolver contentResolver = context.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, birghtessValue);
    }


    /**
     * 1.获取系统默认屏幕亮度值 屏幕亮度值范围（0-255）
     * **/
    private int getScreenBrightness(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        int defVal = 125;
        return Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, defVal);
    }

    private void allowModifySettings() {
        //检测是否拥有写入系统settings的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(MainActivity.this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
                builder.setTitle("请开启修改屏幕亮度权限");
                builder.setTitle("请点击允许开启");
                //拒绝，无法修改
                builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "您已拒绝修改系统Setting的屏幕亮度权限", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 打开允许修改setting权限界面
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        }else{
            Toast.makeText(MainActivity.this, "当前版本为："+ Build.VERSION.SDK_INT +"版本不能低于23", Toast.LENGTH_SHORT).show();
        }
    }
}
