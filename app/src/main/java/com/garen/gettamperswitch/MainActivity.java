package com.garen.gettamperswitch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.garen.gettamperswitch.ota.DownloadService;
import com.garen.gettamperswitch.ota.OtaHr40;
import com.garen.gettamperswitch.ota.lan.OTAFIleActivity;

import java.util.concurrent.TimeUnit;

import me.leefeng.promptlibrary.PromptDialog;


public class MainActivity extends AppCompatActivity implements BatteryChangedReceiver.Message{
    public DownloadService.DownloadBinder downloadBinder;
    public static String HR40_OTA_PACKET_DIR = "/data/media/0/Download/";
    public static String HR40_OTA_PACKET_NAME = "HR40-OTA-v9.20221201_to_v9.20221202_no_block.zip";

    public ServiceConnection connection=new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder=(DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private Context mContext;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1000;
    private BatteryChangedReceiver receiver;
    static int tamperSwitchPressed = 0;
    static int tamperSwitchReleased = 1;
    static int tamperSwitchInit = -1;
    int status = tamperSwitchInit;
    private static IIRService IR = null;
    private static IHr40MiscService Hr40 = null;
    private static String TAG = "garen--->getTamperSwitch-->:";

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
    TextView current_btn_back_light_text,current_screen_btn_back_light,battery_value,battery_status,ir_test_status,version_code,ota_file_address;
    //TextView ip_address_edit,port_edit;
    SeekBar current_btn_back_light_seekbar,current_screen_btn_back_light_seekbar,ota_seekbar;
    AmountView amountview;
    Button testIR_btn, testIR_more_btn, ota_btn, ota_btn_lan;
    Switch btn_back_light_switch;
    LocationManager locationManager;
    BroadcastReceiver mReceiver;
    PromptDialog dialog;


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
        version_code = findViewById(R.id.version_code);
        //ip_address_edit = findViewById(R.id.ip_address_edit);
        //port_edit = findViewById(R.id.port_edit);
        ota_btn = findViewById(R.id.ota_btn);
        ota_file_address = findViewById(R.id.ota_file_address);
        ota_btn_lan = findViewById(R.id.ota_btn_lan);
    }

    private void initData() {
        mContext = getApplicationContext();
        current_screen_btn_back_light_seekbar.setProgress((int)(getScreenBrightness(this)/2.55));
        current_screen_btn_back_light.setText("Current Screen Back Light: " + (int) Math.ceil(getScreenBrightness(this)/2.55) + "%");
        version_code.setText("version:"+APKVersionInfoUtils.getVersionName(mContext));
        dialog = new PromptDialog(MainActivity.this);
    }

    private void initBroadCast() {
        mReceiver = new BroadcastReceiver();
        // 动态注册广播接受者
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.soft.ota.broadcast");//要接收的广播
        registerReceiver(mReceiver, intentFilter);//注册接收者
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
        ota_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                if(downloadBinder==null){
                    return;
                }
                String url=ota_file_address.getText().toString();
                //String url="http://song0123.com/AndySong/mnzm1.jpg";
                downloadBinder.startDownload(url);
                /*TcpClient tcpClient = new TcpClient(ota,ip_address_edit.getText().toString(), Integer.parseInt(port_edit.getText().toString()));
                new Thread(tcpClient).start();*/
            }
        });
        ota_btn_lan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OTAFIleActivity.class);
                startActivity(intent);
            }
        });
    }

    private String getOtaFileName() {
        if(ota_file_address.getText().toString().indexOf("HR40") == -1) {
            Toast.makeText(mContext, "Please check HR40 download address", Toast.LENGTH_SHORT).show();
        }else {
            String [] otaFileName = ota_file_address.getText().toString().split("HR40");
            return HR40_OTA_PACKET_DIR + "HR40" + otaFileName[1];
        }
        return null;
    }

    private void intService() {
        Intent intent=new Intent(this, DownloadService.class);
        //这一点至关重要，因为启动服务可以保证DownloadService一直在后台运行，绑定服务则可以让MaiinActivity和DownloadService
        //进行通信，因此两个方法的调用都必不可少。
        startService(intent);  //启动服务
        bindService(intent,connection,BIND_AUTO_CREATE);//绑定服务
        /**
         *运行时权限处理：我们需要再用到权限的地方，每次都要检查是否APP已经拥有权限
         * 下载功能，需要些SD卡的权限，我们在写入之前检查是否有WRITE_EXTERNAL_STORAGE权限,没有则申请权限
         */
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
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
        intService();
        initData();
        setOnClickListen();
        initBroadCast();
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_HR40_FAVORITES:
                ToastUtils.toastShow(mContext, "Press HR40_FAVORITES Key", 500);
                Log.i(TAG, "Press HR40_FAVORITES Key");
                break;
            case KeyEvent.KEYCODE_HR40_USER_0:
                ToastUtils.toastShow(mContext, "Press HR40_USER_0 Key", 500);
                Log.i(TAG, "Press HR40_USER_0 Key");
                break;
            case KeyEvent.KEYCODE_HR40_INFO:
                ToastUtils.toastShow(mContext, "Press HR40_INFO Key", 500);
                Log.i(TAG, "Press HR40_INFO Key");
                break;
            case KeyEvent.KEYCODE_HR40_MIC_USER_1:
                ToastUtils.toastShow(mContext, "Press HR40_MIC_USER_1 Key", 500);
                Log.i(TAG, "Press HR40_MIC_USER_1 Key");
                break;
            case KeyEvent.KEYCODE_HR40_MENU:
                ToastUtils.toastShow(mContext, "Press HR40_MENU Key", 500);
                Log.i(TAG, "Press HR40_MENU Key");
                break;
            case KeyEvent.KEYCODE_HR40_GUIDE:
                ToastUtils.toastShow(mContext, "Press HR40_GUIDE Key", 500);
                Log.i(TAG, "Press HR40_GUIDE Key");
                break;
            case KeyEvent.KEYCODE_HR40_FILTER_LIST:
                ToastUtils.toastShow(mContext, "Press HR40_FILTER_LIST Key", 500);
                Log.i(TAG, "Press HR40_FILTER_LIST Key");
                break;
            case KeyEvent.KEYCODE_HR40_EXIT:
                ToastUtils.toastShow(mContext, "Press HR40_EXIT Key", 500);
                Log.i(TAG, "Press HR40_EXIT Key");
                break;
            case KeyEvent.KEYCODE_HR40_VOL_UP:
                ToastUtils.toastShow(mContext, "Press HR40_VOL_UP Key", 500);
                Log.i(TAG, "Press HR40_VOL_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_VOL_DOWN:
                ToastUtils.toastShow(mContext, "Press HR40_VOL_DOWN Key", 500);
                Log.i(TAG, "Press HR40_VOL_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_PREV_CHANNEL:
                ToastUtils.toastShow(mContext, "Press HR40_PREV_CHANNEL Key", 500);
                Log.i(TAG, "Press HR40_PREV_CHANNEL Key");
                break;
            case KeyEvent.KEYCODE_HR40_NEXT_CHANNEL:
                ToastUtils.toastShow(mContext, "Press HR40_NEXT_CHANNEL Key", 500);
                Log.i(TAG, "Press HR40_NEXT_CHANNEL Key");
                break;
            case KeyEvent.KEYCODE_HR40_UP:
                ToastUtils.toastShow(mContext, "Press HR40_UP Key", 500);
                Log.i(TAG, "Press HR40_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_DOWN:
                ToastUtils.toastShow(mContext, "Press HR40_DOWN Key", 500);
                Log.i(TAG, "Press HR40_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_LEFT:
                ToastUtils.toastShow(mContext, "Press HR40_LEFT Key", 500);
                Log.i(TAG, "Press HR40_LEFT Key");
                break;
            case KeyEvent.KEYCODE_HR40_RIGHT:
                ToastUtils.toastShow(mContext, "Press HR40_RIGHT Key", 500);
                Log.i(TAG, "Press HR40_RIGHT Key");
                break;
            case KeyEvent.KEYCODE_HR40_MUTE:
                ToastUtils.toastShow(mContext, "Press HR40_MUTE Key", 500);
                Log.i(TAG, "Press HR40_MUTE Key");
                break;
            case KeyEvent.KEYCODE_HR40_PAGE_DOWN:
                ToastUtils.toastShow(mContext, "Press HR40_PAGE_DOWN Key", 500);
                Log.i(TAG, "Press HR40_PAGE_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_PAGE_UP:
                ToastUtils.toastShow(mContext, "Press HR40_PAGE_UP Key", 500);
                Log.i(TAG, "Press HR40_PAGE_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_GOTO_LIVE:
                ToastUtils.toastShow(mContext, "Press HR40_GOTO_LIVE Key", 500);
                Log.i(TAG, "Press HR40_GOTO_LIVE Key");
                break;
            case KeyEvent.KEYCODE_HR40_SKIP_REWIND:
                ToastUtils.toastShow(mContext, "Press HR40_SKIP_REWIND Key", 500);
                Log.i(TAG, "Press HR40_SKIP_REWIND Key");
                break;
            case KeyEvent.KEYCODE_HR40_PLAY:
                ToastUtils.toastShow(mContext, "Press HR40_PLAY Key", 500);
                Log.i(TAG, "Press HR40_PLAY Key");
                break;
            case KeyEvent.KEYCODE_HR40_SKIP_FORWARD:
                ToastUtils.toastShow(mContext, "Press HR40_SKIP_FORWARD Key", 500);
                Log.i(TAG, "Press HR40_SKIP_FORWARD Key");
                break;
            case KeyEvent.KEYCODE_HR40_ENTER:
                ToastUtils.toastShow(mContext, "Press HR40_ENTER Key", 500);
                Log.i(TAG, "Press HR40_ENTER Key");
                break;
            case KeyEvent.KEYCODE_HR40_MEDIA:
                ToastUtils.toastShow(mContext, "Press HR40_MEDIA Key", 500);
                Log.i(TAG, "Press HR40_MEDIA Key");
                break;
            case KeyEvent.KEYCODE_HR40_HOME_TOPMENU:
                ToastUtils.toastShow(mContext, "Press HR40_HOME_TOPMENU Key", 500);
                Log.i(TAG, "Press HR40_HOME_TOPMENU Key");
                break;
            case KeyEvent.KEYCODE_HR40_NUM:
                ToastUtils.toastShow(mContext, "Press HR40_NUM Key", 500);
                Log.i(TAG, "Press HR40_NUM Key");
                break;
            case KeyEvent.KEYCODE_HR40_POWER:
                ToastUtils.toastShow(mContext, "Press HR40_POWER Key", 500);
                Log.i(TAG, "Press HR40_POWER Key");
                break;
            default:
                Log.d(TAG, "onKeyDown:  " + keyCode + " --> "+ event.toString());
                break;
        }
        return super.onKeyDown(keyCode, event);
    }



    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HR40_FAVORITES:
                ToastUtils.toastShow(mContext, "Release HR40_FAVORITES Key", 500);
                Log.i(TAG, "Release HR40_FAVORITES Key");
                break;
            case KeyEvent.KEYCODE_HR40_USER_0:
                ToastUtils.toastShow(mContext, "Release HR40_USER_0 Key", 500);
                Log.i(TAG, "Release HR40_USER_0 Key");
                break;
            case KeyEvent.KEYCODE_HR40_INFO:
                ToastUtils.toastShow(mContext, "Release HR40_INFO Key", 500);
                Log.i(TAG, "Release HR40_INFO Key");
                break;
            case KeyEvent.KEYCODE_HR40_MIC_USER_1:
                ToastUtils.toastShow(mContext, "Release HR40_MIC_USER_1 Key", 500);
                Log.i(TAG, "Release HR40_MIC_USER_1 Key");
                break;
            case KeyEvent.KEYCODE_HR40_MENU:
                ToastUtils.toastShow(mContext, "Release HR40_MENU Key", 500);
                Log.i(TAG, "Release HR40_MENU Key");
                break;
            case KeyEvent.KEYCODE_HR40_GUIDE:
                ToastUtils.toastShow(mContext, "Release HR40_GUIDE Key", 500);
                Log.i(TAG, "Release HR40_GUIDE Key");
                break;
            case KeyEvent.KEYCODE_HR40_FILTER_LIST:
                ToastUtils.toastShow(mContext, "Release HR40_FILTER_LIST Key", 500);
                Log.i(TAG, "Release HR40_FILTER_LIST Key");
                break;
            case KeyEvent.KEYCODE_HR40_EXIT:
                ToastUtils.toastShow(mContext, "Release HR40_EXIT Key", 500);
                Log.i(TAG, "Release HR40_EXIT Key");
                break;
            case KeyEvent.KEYCODE_HR40_VOL_UP:
                ToastUtils.toastShow(mContext, "Release HR40_VOL_UP Key", 500);
                Log.i(TAG, "Release HR40_VOL_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_VOL_DOWN:
                ToastUtils.toastShow(mContext, "Release HR40_VOL_DOWN Key", 500);
                Log.i(TAG, "Release HR40_VOL_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_PREV_CHANNEL:
                ToastUtils.toastShow(mContext, "Release HR40_PREV_CHANNEL Key", 500);
                Log.i(TAG, "Release HR40_PREV_CHANNEL Key");
                break;
            case KeyEvent.KEYCODE_HR40_NEXT_CHANNEL:
                ToastUtils.toastShow(mContext, "Release HR40_NEXT_CHANNEL Key", 500);
                Log.i(TAG, "Release HR40_NEXT_CHANNEL Key");
                break;
            case KeyEvent.KEYCODE_HR40_UP:
                ToastUtils.toastShow(mContext, "Release HR40_UP Key", 500);
                Log.i(TAG, "Release HR40_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_DOWN:
                ToastUtils.toastShow(mContext, "Release HR40_DOWN Key", 500);
                Log.i(TAG, "Release HR40_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_LEFT:
                ToastUtils.toastShow(mContext, "Release HR40_LEFT Key", 500);
                Log.i(TAG, "Release HR40_LEFT Key");
                break;
            case KeyEvent.KEYCODE_HR40_RIGHT:
                ToastUtils.toastShow(mContext, "Release HR40_RIGHT Key", 500);
                Log.i(TAG, "Release HR40_RIGHT Key");
                break;
            case KeyEvent.KEYCODE_HR40_MUTE:
                ToastUtils.toastShow(mContext, "Release HR40_MUTE Key", 500);
                Log.i(TAG, "Release HR40_MUTE Key");
                break;
            case KeyEvent.KEYCODE_HR40_PAGE_DOWN:
                ToastUtils.toastShow(mContext, "Release HR40_PAGE_DOWN Key", 500);
                Log.i(TAG, "Release HR40_PAGE_DOWN Key");
                break;
            case KeyEvent.KEYCODE_HR40_PAGE_UP:
                ToastUtils.toastShow(mContext, "Release HR40_PAGE_UP Key", 500);
                Log.i(TAG, "Release HR40_PAGE_UP Key");
                break;
            case KeyEvent.KEYCODE_HR40_GOTO_LIVE:
                ToastUtils.toastShow(mContext, "Release HR40_GOTO_LIVE Key", 500);
                Log.i(TAG, "Release HR40_GOTO_LIVE Key");
                break;
            case KeyEvent.KEYCODE_HR40_SKIP_REWIND:
                ToastUtils.toastShow(mContext, "Release HR40_SKIP_REWIND Key", 500);
                Log.i(TAG, "Release HR40_SKIP_REWIND Key");
                break;
            case KeyEvent.KEYCODE_HR40_PLAY:
                ToastUtils.toastShow(mContext, "Release HR40_PLAY Key", 500);
                Log.i(TAG, "Release HR40_PLAY Key");
                break;
            case KeyEvent.KEYCODE_HR40_SKIP_FORWARD:
                ToastUtils.toastShow(mContext, "Release HR40_SKIP_FORWARD Key", 500);
                Log.i(TAG, "Release HR40_SKIP_FORWARD Key");
                break;
            case KeyEvent.KEYCODE_HR40_ENTER:
                ToastUtils.toastShow(mContext, "Release HR40_ENTER Key", 500);
                Log.i(TAG, "Release HR40_ENTER Key");
                break;
            case KeyEvent.KEYCODE_HR40_MEDIA:
                ToastUtils.toastShow(mContext, "Release HR40_MEDIA Key", 500);
                Log.i(TAG, "Release HR40_MEDIA Key");
                break;
            case KeyEvent.KEYCODE_HR40_HOME_TOPMENU:
                ToastUtils.toastShow(mContext, "Release HR40_HOME_TOPMENU Key", 500);
                Log.i(TAG, "Release HR40_HOME_TOPMENU Key");
                break;
            case KeyEvent.KEYCODE_HR40_NUM:
                ToastUtils.toastShow(mContext, "Release HR40_NUM Key", 500);
                Log.i(TAG, "Release HR40_NUM Key");
                break;
            case KeyEvent.KEYCODE_HR40_POWER:
                ToastUtils.toastShow(mContext, "Release HR40_POWER Key", 500);
                Log.i(TAG, "Release HR40_POWER Key");
                break;
            default:
                Log.d(TAG, "onKeyDown:  " + keyCode + " --> "+ event.toString());
                break;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public class BroadcastReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            if (msg.equals("ota_downloading")) {
                dialog.showLoading("Downloading OTA File...");
            }else if (msg.equals("ota_download_success")) {
                dialog.showLoading("OTA File Download Success");
                OtaHr40 ota = new OtaHr40(context);
                if(ota == null) {
                    Toast.makeText(context, "Failed to detect ota file", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    // 升级 HR40 BSP 固件.
                    ota.otaUpdate(getOtaFileName());
                }
                dialog.dismissImmediately();
            }else if (msg.equals("ota_download_failed")) {
                dialog.showLoading("OTA File Download Failed");
                dialog.dismissImmediately();
            }
        }
    }
}
