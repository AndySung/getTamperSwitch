package com.garen.gettamperswitch.ota.lan;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.garen.gettamperswitch.R;
import com.garen.gettamperswitch.ota.OtaHr40;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.leefeng.promptlibrary.PromptButton;
import me.leefeng.promptlibrary.PromptButtonListener;
import me.leefeng.promptlibrary.PromptDialog;
import timber.log.Timber;

public class OTAFIleActivity  extends AppCompatActivity implements Animator.AnimatorListener {
    Button mFab;
    RecyclerView mBookList;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<String> mBooks = new ArrayList<>();
    BookshelfAdapter mBookshelfAdapter;
    androidx.appcompat.widget.Toolbar toolbar;
    private PromptDialog promptDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置窗体为没有标题的模式
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_ota_lan);
        initView();
        Timber.plant(new Timber.DebugTree());
        RxBus.get().register(this);
        initRecyclerView();
        //创建对象
        promptDialog = new PromptDialog(this);
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        mFab = findViewById(R.id.fab);
        mBookList = findViewById(R.id.recyclerview);
        mSwipeRefreshLayout = findViewById(R.id.content_main);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mFab, "translationY", 0, mFab.getHeight() * 2).setDuration(200L);
                objectAnimator.setInterpolator(new AccelerateInterpolator());
                objectAnimator.addListener(OTAFIleActivity.this);
                objectAnimator.start();
            }
        });
        setSupportActionBar(toolbar);
        ActionBar actionBar =  getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    //activity类中的方法
    //添加点击返回箭头事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * @param view    就是我们点击的itemView
     */
    public void myItemClick(View view){
        // 获取itemView的位置
        int position = mBookList.getChildAdapterPosition(view);
        OtaHr40 ota = new OtaHr40(OTAFIleActivity.this);
        final PromptButton confirm = new PromptButton("Sure", new PromptButtonListener() {
            @Override
            public void onClick(PromptButton button) {
                // 升级 HR40 BSP 固件.
                ota.otaUpdate(Constants.DIR_ROOT + mBooks.get(position));
            }
        });
        confirm.setTextColor(Color.parseColor("#DAA520"));
        confirm.setFocusBacColor(Color.parseColor("#FAFAD2"));
        confirm.setDelyClick(true);//点击后，是否再对话框消失后响应按钮的监听事件
        if(ota == null) {
            Toast.makeText(OTAFIleActivity.this, "Failed to detect ota file", Toast.LENGTH_SHORT).show();
            return;
        }else {
            promptDialog.showWarnAlert("Are you sure you want to upgrade？", new PromptButton("Cancel", new PromptButtonListener() {
                @Override
                public void onClick(PromptButton button) {
                }
            }),confirm);
        }
        //Toast.makeText(OTAFIleActivity.this, "点击了 " + mBooks.get(position),Toast.LENGTH_SHORT).show();
    }

    /**
     * 用户选择允许或拒绝后,会回调onRequestPermissionsResult
     * @param requestCode  请求码
     * @param permissions
     * @param grantResults  授权结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    public static void downLoad(String path, Context context)throws Exception
    {
        URL url = new URL(path);
        InputStream is = url.openStream();
        //截取最后的文件名
        String end = path.substring(path.lastIndexOf("."));
        //打开手机对应的输出流,输出到文件中
        OutputStream os = context.openFileOutput("Cache_"+System.currentTimeMillis()+end, Context.MODE_PRIVATE);
        byte[] buffer = new byte[1024];
        int len = 0;
        //从输入六中读取数据,读到缓冲区中
        while((len = is.read(buffer)) > 0)
        {
            os.write(buffer,0,len);
        }
        //关闭输入输出流
        is.close();
        os.close();
    }

    @Subscribe(tags = {@Tag(Constants.RxBusEventType.POPUP_MENU_DIALOG_SHOW_DISMISS)})
    public void onPopupMenuDialogDismiss(Integer type) {
        if (type == Constants.MSG_DIALOG_DISMISS) {
            WebService.stop(this);
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mFab, "translationY", mFab.getHeight() * 2, 0).setDuration(200L);
            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.start();
        }
    }

    @Subscribe(thread = EventThread.IO, tags = {@Tag(Constants.RxBusEventType.LOAD_BOOK_LIST)})
    public void loadBookList(Integer type) {
        Timber.d("loadBookList:" + Thread.currentThread().getName());
        List<String> books = new ArrayList<>();
        File dir = Constants.DIR;
        if (dir.exists() && dir.isDirectory()) {
            String[] fileNames = dir.list();
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    books.add(fileName);
                }
            }
        }
        runOnUiThread(() -> {
            mSwipeRefreshLayout.setRefreshing(false);
            mBooks.clear();
            mBooks.addAll(books);
            mBookshelfAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebService.stop(this);
        RxBus.get().unregister(this);
    }

    @Override
    public void onAnimationStart(Animator animator) {
        WebService.start(OTAFIleActivity.this);
        new PopupMenuDialog(OTAFIleActivity.this).builder().setCancelable(false).setCanceledOnTouchOutside(false).show();
    }

    @Override
    public void onAnimationEnd(Animator animator) {

    }
    @Override
    public void onAnimationCancel(Animator animator) {

    }
    @Override
    public void onAnimationRepeat(Animator animator) {

    }

    void initRecyclerView() {
        mBookshelfAdapter = new BookshelfAdapter();
        mBookList.setHasFixedSize(true);
        mBookList.setLayoutManager(new GridLayoutManager(this, 3));
        mBookList.setAdapter(mBookshelfAdapter);
        RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                RxBus.get().post(Constants.RxBusEventType.LOAD_BOOK_LIST, 0);
            }
        });
    }

    public class BookshelfAdapter extends RecyclerView.Adapter<BookshelfAdapter.MyViewHolder> {
        @NonNull
        @Override
        public BookshelfAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MyViewHolder holder = new MyViewHolder(LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.book_item, parent,
                    false));
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.mTvBookName.setText(mBooks.get(position));
           // holder.mTvBookImg.setImageResource(R.mipmap.ic_ota_cover);
        }

        @Override
        public int getItemCount() {
            return mBooks.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView mTvBookName;
           // ImageView mTvBookImg;
            public MyViewHolder(View view) {
                super(view);
                mTvBookName = view.findViewById(R.id.ota_file_name);
                //mTvBookImg = view.findViewById(R.id.ota_file_img);
            }
        }
    }

}
