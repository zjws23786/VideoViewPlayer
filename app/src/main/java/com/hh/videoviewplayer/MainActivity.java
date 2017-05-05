package com.hh.videoviewplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hh.videoviewplayer.utils.DateTools;
import com.hh.videoviewplayer.utils.DensityUtil;
import com.hh.videoviewplayer.utils.NetworkUtils;
import com.hh.videoviewplayer.widget.FullVideoView;

import java.lang.ref.WeakReference;

/**
 * VideoView视频播放器
 */
public class MainActivity extends Activity implements View.OnClickListener {
    private RelativeLayout mVideoLayout;  //视频区域布局
    private FullVideoView mVideoView; //视频的View
    private LinearLayout topLayout; //视频标题的视图
    private LinearLayout bottomLayout; //视频下面的操作栏
    private TextView mCurrentTimeTv;  //当前视频播放时间
    private TextView mTotalTimeTv;  //视频总时长
    private SeekBar mPosSeekBar;  //进度条
    private SeekBar mVolumeSeekBar;  //音量进度条
    private ImageView mPlayAndPause;  //播放或暂停按钮
    private ImageView mChangeFullScreen;  //屏幕切换按钮
    /*亮度、音量、快进或快退  布局内容*/
    private RelativeLayout volumeLayout;  //音量
    private ImageView playerVolumeIv;   //音量的图标
    private TextView volumePercentageTv; //音量值的变化
    private RelativeLayout brightLayout;  //亮度
    private TextView brightPercentageTv;
    private RelativeLayout progressLayout; //快进快退
    private ImageView progressTimeIv;  //快进快退图标
    private TextView progressTimeTv;  //快进快退进度值
    private RelativeLayout progressbarLayout;


    private int playerWidth; //视频窗口宽
    private int playerHeight; //视频窗口高
    private AudioManager mAudioManager; //音频管理器
    private static final float STEP_VOLUME = 2f;// 协调音量滑动时的步长，避免每次滑动都改变，导致改变过快
    private int currentVolume; //当前音量值
    private int maxVolume;  //最大音量值
    private boolean isFullScreen = false; //是否全屏【true表示全屏】
    private boolean isAdjust = false; //触摸屏幕后是否到达调整手机音量或亮度
    private int threshold = 54; //触摸屏幕到达调整的最小临界值
    private int startX;  //手指视频View的X轴位置
    private int startY;  //手指视频View的Y轴位置
    private float lastX = 0; //手指视频View的X轴位置
    private float lastY = 0; //手指视频View的Y轴位置
    private int playingTime; //视频播放时间
    private int videoTotalTime; //视频播放的总时长
    private float mBrightness; //当前亮度值
    private boolean isClick = true;

    private static final int UPDATE_UI = 1;  //Handler更新UI用的常量
    private static final int HIDE_TIME = 5000;  // 自动隐藏顶部和底部View的时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById();
        setListener();
        init();
    }

    private void findViewById() {
        mVideoLayout = (RelativeLayout) findViewById(R.id.videoLayout);
        mVideoView = (FullVideoView) findViewById(R.id.video_view);
        topLayout = (LinearLayout) findViewById(R.id.top_layout);
        bottomLayout = (LinearLayout) findViewById(R.id.bottom_layout);
        mCurrentTimeTv = (TextView) findViewById(R.id.current_time_tv);
        mTotalTimeTv = (TextView) findViewById(R.id.total_time_tv);
        mPosSeekBar = (SeekBar) findViewById(R.id.pos_seekBar);
        mVolumeSeekBar = (SeekBar) findViewById(R.id.volume_seek);
        mPlayAndPause = (ImageView) findViewById(R.id.pause_img);
        mChangeFullScreen = (ImageView) findViewById(R.id.change_screen);

        volumeLayout = (RelativeLayout) findViewById(R.id.volume_layout);
        playerVolumeIv = (ImageView) findViewById(R.id.player_volume_iv);
        volumePercentageTv = (TextView) findViewById(R.id.volume_percentage_tv);
        brightLayout = (RelativeLayout) findViewById(R.id.bright_layout);
        brightPercentageTv = (TextView) findViewById(R.id.bright_percentage_tv);
        progressLayout = (RelativeLayout) findViewById(R.id.progress_layout);
        progressTimeIv = (ImageView) findViewById(R.id.progress_time_iv);
        progressTimeTv = (TextView) findViewById(R.id.progress_time_tv);
        progressbarLayout = (RelativeLayout) findViewById(R.id.progressbar_layout);


    }

    private void setListener() {
        //播放或暂停
        mPlayAndPause.setOnClickListener(this);
        //屏幕改变按钮
        mChangeFullScreen.setOnClickListener(this);

        //接收对SeekBar进度级别的更改的通知。 还提供了用户什么时候在SeekBar内启动和停止触摸手势的通知。
        mPosSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            //通知进度水平已经改变。
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                updateTextViewWithTimeFormat(mCurrentTimeTv , progress);
            }

            //通知用户已经开始触摸手势。
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeMessages(UPDATE_UI);
            }

            //通知用户已完成触摸手势。
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mVideoView.seekTo(progress);
                mHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        //音量进度条
        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                //设置当前设备音量
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC , progress , 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.pause_img:
                if (mVideoView.isPlaying()){
                    mPlayAndPause.setImageResource(R.drawable.video_start_style);
                    mVideoView.pause();
                    mHandler.removeMessages(UPDATE_UI);
                }else{
                    progressbarLayout.setVisibility(View.GONE);
                    mPlayAndPause.setImageResource(R.drawable.video_stop_style);
                    mVideoView.start();
                    mHandler.sendEmptyMessage(UPDATE_UI);
                }
                break;
            case R.id.change_screen:
                if (isFullScreen){
                    //屏幕竖屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else{
                    //屏幕横屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                break;
        }
    }

    private void init() {
        /** 获取视频播放窗口的尺寸 */
        ViewTreeObserver viewObserver = mVideoView.getViewTreeObserver();
        //当指定视图状态发生改变时触发
        viewObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mVideoView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                playerWidth = mVideoView.getWidth();
                playerHeight = mVideoView.getHeight();
            }
        });

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // 获取系统最大音量
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // 获取当前值
        mVolumeSeekBar.setMax(maxVolume);
        mVolumeSeekBar.setProgress(currentVolume);

        playVideo();
    }

    private void playVideo() {
        //本地视频播放
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/money.mp4";
        //本地视频播放
//        mVideoView.setVideoPath(path);

        path = "http://cntv.vod.cdn.myqcloud.com/flash/mp4video59/TMS/2017/03/29/ee1ec61692974e3b8c2bea762acec1f8_h264418000nero_aac32-4.mp4";
        //网络视频播放
        mVideoView.setVideoURI(Uri.parse(path));
        mVideoView.requestFocus();
//        mVideoView.canPause();
        //当媒体文件被加载，并准备去调用
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                progressbarLayout.setVisibility(View.GONE);
                mVideoView.start();
                if (playingTime != 0){
                    mVideoView.seekTo(playingTime);
                }

                updateTextViewWithTimeFormat(mTotalTimeTv , mVideoView.getDuration());
                mHandler.sendEmptyMessage(UPDATE_UI);
                mHandler.postDelayed(hideRunnable, HIDE_TIME);
            }
        });


        //在播放期间到达媒体文件结束时，注册要调用的回调
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mHandler.removeMessages(UPDATE_UI);
                mVideoView.pause();
                mPlayAndPause.setImageResource(R.drawable.video_start_style);
                mPosSeekBar.setProgress(0);
                mCurrentTimeTv.setText("00:00");
            }
        });

        //在播放或设置过程中发生错误时，注册要调用的回调。 如果没有指定侦听器，
        // 或者如果侦听器返回false，则VideoView将通知用户任何错误。
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            /**
             * @param mp
             * @param what
             * 取值
             * MEDIA_ERROR_UNKNOWN  MEDIA未知的错误
             * MEDIA_ERROR_SERVER_DIED   MEDIA服务器挂掉错误
             * @param extra
             * 取值
             * MEDIA_ERROR_IO    MEDIA IO错误
             * MEDIA_ERROR_MALFORMED    MEDIA畸形错误
             * MEDIA_ERROR_UNSUPPORTED   MEDIA不支持的错误
             * MEDIA_ERROR_TIMED_OUT  MEDIA超时错误
             * @return
             */
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (extra){
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        Toast.makeText(MainActivity.this,"超时",Toast.LENGTH_SHORT).show();
                        progressbarLayout.setVisibility(View.VISIBLE);
                        break;
                    case MediaPlayer.MEDIA_ERROR_IO:
                        Toast.makeText(MainActivity.this,"IO",Toast.LENGTH_SHORT).show();
                        progressbarLayout.setVisibility(View.VISIBLE);
                        break;
                }
                return true;
            }
        });

        mVideoView.setOnTouchListener(mTouchListener);
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    lastY = y;
                    startX = (int) x;
                    startY = (int) y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - lastX;
                    float deltaY = y - lastY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    //XY轴的间距都大于临界值
                    if (absDeltaX > threshold && absDeltaY > threshold){
                        //竖直方向的距离大于水平方向的
                        if (absDeltaX < absDeltaY){
                            isAdjust = true;
                        }else{
                            isAdjust = false;
                        }
                    }else if (absDeltaX < threshold && absDeltaY > threshold){
                        isAdjust = true;
                    }else if (absDeltaX > threshold && absDeltaY < threshold){
                        isAdjust = false;
                    }else{
                        return true;
                    }

                    //进行屏幕亮度或音量的调节
                    if (isAdjust){
                        //亮度【视屏的亮度范围是0~255】
                        if (x < playerWidth/2){
                            brightLayout.setVisibility(View.VISIBLE);
                            if (deltaY > 0){ //屏幕亮度变亮
                                changeBrightness(-absDeltaY);
                            }else{ //屏幕亮度变暗
                                changeBrightness(absDeltaY);
                            }
                        }else{ //音量
                            volumeLayout.setVisibility(View.VISIBLE);
                            if (deltaY > 0){
                                changeVolume(-absDeltaY);
                            }else{
                                changeVolume(absDeltaY);
                            }
                        }
                    }else{
                        progressLayout.setVisibility(View.VISIBLE);
                        if (deltaX > 0) { //快进
                            forward(absDeltaX);
                        } else if (deltaX < 0) { //快退
                            backward(absDeltaX);
                        }
                    }
                    lastX = x;
                    lastY = y;
                    break;
                case MotionEvent.ACTION_UP:
                    brightLayout.setVisibility(View.GONE);
                    volumeLayout.setVisibility(View.GONE);
                    progressLayout.setVisibility(View.GONE);
                    if (Math.abs(x - startX) > threshold
                            || Math.abs(y - startY) > threshold){
                        isClick = false;
                    }
                    lastX = 0;
                    lastY = 0;
                    startX = 0;
                    if (isClick) {
                        showOrHide();
                    }
                    isClick = true;
                    break;
            }
            return true;
        }
    };

    //亮度的变化
    public void changeBrightness(float changY){
        WindowManager.LayoutParams attribute = getWindow().getAttributes();
        mBrightness = attribute.screenBrightness;
//        Log.v("hjz" , mBrightness + "");
        float index = changY / playerHeight;
        mBrightness += index;

        if (mBrightness > 1.0f){
            mBrightness = 1.0f;
        }

        if (mBrightness < 0.01f){
            mBrightness = 0.01f;
        }

        attribute.screenBrightness = mBrightness;
        getWindow().setAttributes(attribute);
        brightPercentageTv.setText((int) (attribute.screenBrightness * 100) + "%");

    }

    //音量的变化
    public void changeVolume(float changY){
        currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // 获取当前值
        if (changY >= DensityUtil.dip2px(this, STEP_VOLUME)) {// 音量调大,注意横屏时的坐标体系,尽管左上角是原点，但横向向上滑动时distanceY为正
            if (currentVolume < maxVolume) {// 为避免调节过快，distanceY应大于一个设定值
                currentVolume++;
            }
            playerVolumeIv.setImageResource(R.mipmap.center_player_volume);
        } else if (changY <= -DensityUtil.dip2px(this, STEP_VOLUME)) {// 音量调小
            if (currentVolume > 0) {
                currentVolume--;
                if (currentVolume == 0) {// 静音，设定静音独有的图片
                    playerVolumeIv.setImageResource(R.mipmap.center_player_silence);
                }
            }
        }
        int percentage = (currentVolume * 100) / maxVolume;
        volumePercentageTv.setText(percentage + "%");
        mVolumeSeekBar.setProgress(currentVolume);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,currentVolume, 0);

    }

    //快进
    private void forward(float deltaX) {
        playingTime = mVideoView.getCurrentPosition();
        videoTotalTime = mVideoView.getDuration();
        //快进了多少
        int forwardTime = (int) (deltaX / playerWidth * videoTotalTime);
        int currentTime = playingTime + forwardTime;
        if (currentTime > videoTotalTime){
            currentTime = videoTotalTime - 10;
        }
        mVideoView.seekTo(currentTime);
        mPosSeekBar.setProgress(currentTime);
        updateTextViewWithTimeFormat(mCurrentTimeTv,currentTime);
        progressTimeIv.setImageResource(R.mipmap.center_player_forward);
        progressTimeTv.setText(DateTools.getTimeStr(currentTime) + "/" + DateTools.getTimeStr(videoTotalTime));
    }

    //快退
    private void backward(float deltaX) {
        playingTime = mVideoView.getCurrentPosition();
        videoTotalTime = mVideoView.getDuration();
        int backwardTime = (int) (deltaX / playerWidth * videoTotalTime);
        int currentTime = playingTime - backwardTime;
        if (currentTime < 0){
            currentTime = 0;
        }
        mVideoView.seekTo(currentTime);
        mPosSeekBar.setProgress(currentTime);
        updateTextViewWithTimeFormat(mCurrentTimeTv,currentTime);
        progressTimeIv.setImageResource(R.mipmap.center_player_backward);
        progressTimeTv.setText(DateTools.getTimeStr(currentTime) + "/" + DateTools.getTimeStr(videoTotalTime));
    }

    //设备配置更改时由系统调用
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //当横屏时
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT , ViewGroup.LayoutParams.WRAP_CONTENT);
            mVolumeSeekBar.setVisibility(View.VISIBLE);
//            mVolumeImg.setVisibility(View.VISIBLE);
            isFullScreen = true;

            //强制移除半屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else{
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT , DensityUtil.dip2px(this , 240f));
            mVolumeSeekBar.setVisibility(View.GONE);
//            mVolumeImg.setVisibility(View.GONE);
            isFullScreen = false;

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }

    private void setVideoViewScale(int width , int height){
        ViewGroup.LayoutParams layoutParams = mVideoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = mVideoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        mVideoLayout.setLayoutParams(layoutParams1);
    }

    private void updateTextViewWithTimeFormat(TextView tv, int milliSecond) {
        int second = milliSecond/1000;
        int hh = second/3600;
        int mm = second%3600/60;
        int ss = second%60;

        String timeStr = null;
        if (hh != 0){
            timeStr = String.format("%02d:%02d:%02d" , hh , mm , ss);
        }else{
            timeStr = String.format("%02d:%02d" , mm , ss);
        }
        tv.setText(timeStr);
    }


    private final MyHandler mHandler = new MyHandler(this);
    public class MyHandler extends Handler {
        WeakReference<Activity > mActivityReference;

        public MyHandler(Activity activity) {
            mActivityReference= new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mActivityReference.get();
            if (activity != null){
                switch (msg.what){
                    case UPDATE_UI:
                        int currentPosition = mVideoView.getCurrentPosition();
                        int totalPosition = mVideoView.getDuration();
                        updateTextViewWithTimeFormat(mCurrentTimeTv , currentPosition);
                        mPosSeekBar.setMax(totalPosition);
                        mPosSeekBar.setProgress(currentPosition);
                        mHandler.sendEmptyMessageDelayed(UPDATE_UI , 500);
                        break;
                    case 2:
                        showOrHide();
                        break;
                }
            }
        }
    }

    private Runnable hideRunnable = new Runnable() {

        @Override
        public void run() {
            showOrHide();
        }
    };

    private void showOrHide() {
        if (topLayout.getVisibility() == View.VISIBLE) {
            topLayout.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.option_leave_from_top);
            animation.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    topLayout.setVisibility(View.GONE);
                }
            });
            topLayout.startAnimation(animation);

            bottomLayout.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this,
                    R.anim.option_leave_from_bottom);
            animation1.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    bottomLayout.setVisibility(View.GONE);
                }
            });
            bottomLayout.startAnimation(animation1);
        } else {
            topLayout.setVisibility(View.VISIBLE);
            topLayout.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.option_entry_from_top);
            topLayout.startAnimation(animation);

            bottomLayout.setVisibility(View.VISIBLE);
            bottomLayout.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this,
                    R.anim.option_entry_from_bottom);
            bottomLayout.startAnimation(animation1);
            mHandler.removeCallbacks(hideRunnable);
            mHandler.postDelayed(hideRunnable, HIDE_TIME);
        }
    }

    private class AnimationImp implements Animation.AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    }

    @Override
    public void onBackPressed() {
        if (isFullScreen){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
