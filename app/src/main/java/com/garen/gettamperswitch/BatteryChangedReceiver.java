package com.garen.gettamperswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

/**
 * 电量改变的广播接收者
 */
public class BatteryChangedReceiver extends BroadcastReceiver {
	private static String TAG = BatteryChangedReceiver.class.getSimpleName();
	private Message message;
	@Override
	public void onReceive(Context context, Intent intent) {
		message.getMsg("Battery States");
		/**
		 * 先判断是否正在充电
		 * ***/
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
		switch (status) {
			case BatteryManager.BATTERY_STATUS_CHARGING:  //2
				message.getMsg("Status: Charging");
				Log.i(TAG,"正在充电：" + status);
				break;
			case BatteryManager.BATTERY_STATUS_DISCHARGING:		//3
				message.getMsg("Status: DisCharging");
				Log.i(TAG,"正在放电：" + status);
				break;
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:	//4
				message.getMsg("Status: Not Charging");
				Log.i(TAG,"未充电：" + status);
				break;
			case BatteryManager.BATTERY_STATUS_FULL:		//5
				message.getMsg("Status: Full");
				Log.i(TAG,"充满电：" + status);
				break;
			case BatteryManager.BATTERY_STATUS_UNKNOWN:		//1
				message.getMsg("Status: unknown");
				Log.i(TAG,"未知道状态：" + status);
				break;
		}

		/**
		 * 判断充电方式
		 * ***/
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC);
		Log.i(TAG,"plugged： " + plugged);
		switch (plugged) {
			case BatteryManager.BATTERY_PLUGGED_AC:		//1
				if(status == 2) {
					message.getMsg("Plugged：AC Power");
					message.getMsg("Status+Plugged: Charging battery By AC");
				}else {
					message.getMsg("Plugged：AC Power");
					message.getMsg("Status+Plugged: AC Power");
				}
				Log.i(TAG,"AC供电");
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:		//2
				message.getMsg("Plugged：USB charger");
				Log.i(TAG,"USB充电");
				break;
			case BatteryManager.BATTERY_PLUGGED_WIRELESS:		//4
				message.getMsg("Plugged：Wireless charger");
				Log.i(TAG,"无线充电");
				break;
		}


		/**
		 * 判断电池健康状态
		 * ***/
		int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
		switch (health) {
			case BatteryManager.BATTERY_HEALTH_GOOD:
				message.getMsg("health: Good（"+health+"）");
				Log.i(TAG,"良好");
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				message.getMsg("health: Overheat（"+health+"）");
				Log.i(TAG,"过热");
				break;
			case BatteryManager.BATTERY_HEALTH_DEAD:
				message.getMsg("health: Dead（"+health+"）");
				Log.i(TAG,"没电");
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				message.getMsg("health: Over voltage（"+health+"）");
				Log.i(TAG,"过电压");
				break;
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				message.getMsg("health: unknown（"+health+"）");
				Log.i(TAG,"未知错误");
				break;
		}

		/**
		 * 判断电池状态变化
		 * ***/
		switch (intent.getAction()) {
			case Intent.ACTION_BATTERY_CHANGED://"android.intent.action.BATTERY_CHANGED"
				Log.i(TAG, "==============电池电量改变：BATTERY_CHANGED_ACTION");
				Log.i(TAG, "status: " + intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));//如BATTERY_STATUS_CHARGING 正在充电
				Log.i(TAG, "health: " + intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));//如BATTERY_HEALTH_COLD
				//Log.i(TAG, "present: " + intent.getIntExtra(BatteryManager.EXTRA_PRESENT, -1));
				Log.i(TAG, "level: " + intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));	//当前剩余电量
				Log.i(TAG, "scale: " + intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));	//电量最大值
				//电量百分比
				float batteryPct = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) / (float) intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				Log.d(TAG, "batteryPct=" + batteryPct * 100 +"%");		//电量百分比

				Log.i(TAG, "voltage: " + intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));			//电池电压
				Log.i(TAG, "temperature: " + intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));	//电池温度
				Log.i(TAG, "technology: " + intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));			//描述当前电池技术。比如，对于锂电池是Li-ion

				//message.getMsg("status: "  + intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));				//电池状态（是否在充电）
				//message.getMsg("health: " + intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));					//电池健康状态（有良好，过电..）
				message.getMsg("level: " + intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));		//电池当前电量值
				message.getMsg("scale: " + intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1));		//电池总电量
				message.getMsg("voltage: " + intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));	//电池伏数
				//message.getMsg("temperature: " + intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));		//电池温度
				//message.getMsg("technology: " + intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));			//描述当前电池技术
				break;
			case Intent.ACTION_BATTERY_LOW:// "android.intent.action.BATTERY_LOW"
				Log.i(TAG, "电池电量低：ACTION_BATTERY_LOW");
				message.getMsg("Battery Power：Low power");
				break;
			case Intent.ACTION_BATTERY_OKAY:// "android.intent.action.BATTERY_OKAY"
				Log.i(TAG, "电池已经从电量低恢复为正常：ACTION_BATTERY_OKAY");
				message.getMsg("Battery Power：Power okay(not low anymore)");
				break;
			case Intent.ACTION_POWER_CONNECTED:
				Log.i(TAG, "ACTION_POWER_CONNECTED:" + Intent.ACTION_POWER_CONNECTED);
				message.getMsg("Power: Connected");
				break;
			case Intent.ACTION_POWER_DISCONNECTED:
				Log.i(TAG, "ACTION_POWER_DISCONNECTED:" + Intent.ACTION_POWER_DISCONNECTED);
				message.getMsg("Power: DISConnected");
				break;
			default:
				break;

		}
	}

	public interface Message{
		public void getMsg(String str);
	}

	public void setMessage(Message message){
		this.message = message;
	}

}