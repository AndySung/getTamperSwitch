package com.garen.gettamperswitch;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

public class ToastUtils {
    private static Toast toast = null;

    /**
     * 判断toast是否存在，如果存在则更新text，达到避免出现时间叠加的问题
     * @param context 上下文
     * @param text  显示的内容
     * @param duration  显示时长，默认值为Toast.LENGTH_SHORT (2秒)或Toast.LENGTH_LONG(3.5秒)
     */
    public static void toastShow(Context context, String text, int duration) {
        if (toast == null) {
            toast = Toast.makeText(context, text, duration);
        } else {
            toast.setText(text);
        }
        toast.show();
    }

    /**
     * 判断toast是否存在，如果存在则更新text，达到避免出现时间叠加的问题
     * @param context 上下文
     * @param resId  字符串资源文件ID
     * @param duration  显示时长，默认值为Toast.LENGTH_SHORT (2秒)或Toast.LENGTH_LONG(3.5秒)
     */
    public static void toastShow(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        if (toast == null) {
            toast = Toast.makeText(context, context.getResources().getText(resId), duration);
        } else {
            toast.setText(context.getResources().getText(resId));
        }
        toast.show();
    }
}
