package com.fengmap.FMDemoNavigationAdvance.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


import com.fengmap.FMDemoNavigationAdvance.R;
import com.fengmap.FMDemoNavigationAdvance.location.StepCountLocationService;
import com.fengmap.FMDemoNavigationAdvance.utils.ConvertUtils;
import com.fengmap.FMDemoNavigationAdvance.utils.ViewHelper;
import com.fengmap.FMDemoNavigationAdvance.widget.ImageViewCheckBox;
import com.fengmap.android.analysis.navi.FMActualNavigation;
import com.fengmap.android.analysis.navi.FMNaviOption;
import com.fengmap.android.analysis.navi.FMNavigationInfo;
import com.fengmap.android.analysis.navi.FMSimulateNavigation;
import com.fengmap.android.analysis.navi.OnFMNavigationListener;
import com.fengmap.android.map.FMViewMode;
import com.fengmap.android.map.geometry.FMGeoCoord;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.map.marker.FMLocationMarker;
import com.fengmap.android.widget.FMSwitchFloorComponent;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;

import java.net.MalformedURLException;

/**
 * 导航实例
 * <p>模拟真实导航如何使用地图进行导航显示
 *
 * @author yangbin@fengmap.com
 * @version 2.1.0
 */
public class FMNavigationApplication extends BaseActivity implements
        View.OnClickListener,
        ImageViewCheckBox.OnCheckStateChangedListener,
        OnFMNavigationListener {
    // 约束过的定位标注
    private FMLocationMarker mHandledMarker;

    // 是否为第一人称
    private boolean mIsFirstView = true;

    // 是否为跟随状态
    private boolean mHasFollowed = true;

    // 总共距离
    private double mTotalDistance;

    // 楼层切换控件
    private FMSwitchFloorComponent mSwitchFloorComponent;

    // 上一次文字描述
    private String mLastDescription;

    private SpeechSynthesizer mTts;
    public static FMActualNavigation sActualNavigation;
/*****************************************/
    private boolean flag = true;
    private boolean isLocating = false;
    private MyReceiver receiver;
    private boolean isFirstLocating = true;
    /*************************************************/


    private synchronized void setFlag() {
        flag = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_app);
        mEndCoord = new FMGeoCoord(1, new FMMapCoord(12961662.565714367, 4861818.338024983));
        createSynthesizer();
    }

    @Override
    public void onMapInitSuccess(String path) {
        super.onMapInitSuccess(path);

        if (mSwitchFloorComponent == null) {
            initSwitchFloorComponent();
        }

        ViewHelper.setViewCheckedChangeListener(FMNavigationApplication.this, R.id.btn_3d, this);
        ViewHelper.setViewCheckedChangeListener(FMNavigationApplication.this, R.id.btn_locate, this);
        ViewHelper.setViewCheckedChangeListener(FMNavigationApplication.this, R.id.btn_view, this);

        // 创建模拟导航对象
    //    mNavigation = new FMSimulateNavigation(mFMMap);
        mNavigation=new FMActualNavigation(mFMMap);


        // 创建模拟导航配置对象
        mNaviOption = new FMNaviOption();

        // 设置跟随模式，默认跟随
        mNaviOption.setFollowPosition(mHasFollowed);

        // 设置跟随角度（第一人视角），默认跟随
        mNaviOption.setFollowAngle(mIsFirstView);

        // 点移距离视图中心点超过最大距离5米，就会触发移动动画；若设为0，则实时居中
        mNaviOption.setNeedMoveToCenterMaxDistance(NAVI_MOVE_CENTER_MAX_DISTANCE);

        // 设置导航开始时的缩放级别，true: 导航结束时恢复开始前的缩放级别，false：保持现状
        mNaviOption.setZoomLevel(NAVI_ZOOM_LEVEL, false);

        // 设置配置
        mNavigation.setNaviOption(mNaviOption);

        // 设置导航监听接口
        mNavigation.setOnNavigationListener(this);

        // 路径规划
        analyzeNavigation(mStartCoord, mEndCoord);

        // 总长
        mTotalDistance = mNavigation.getSceneRouteLength();

        isMapLoaded = true;
        sActualNavigation=(FMActualNavigation)mNavigation;
    }


    @Override
    public void startNavigation() {
//        FMSimulateNavigation simulateNavigation = (FMSimulateNavigation) mNavigation;
//        // 3米每秒。
//        simulateNavigation.simulate(3.0f);
        FMActualNavigation actualNavigation=(FMActualNavigation)mNavigation;
        //sActualNavigation=actualNavigation;
        actualNavigation.start();
//        FMGeoCoord my = new FMGeoCoord(1,
//                new FMMapCoord(12961777.666, 4861899));
        ///*********起点：  12961647.576796599, 4861814.63807118
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FMNavigationApplication.sActualNavigation.locate(new FMGeoCoord(1,
                        new FMMapCoord(12961650.576796599, 4861814.63807118)),270);
            }
        },2000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FMNavigationApplication.sActualNavigation.locate(new FMGeoCoord(1,
                        new FMMapCoord(12961654.576796599,4861814.63807118)),270);
            }
        },6000);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FMNavigationApplication.sActualNavigation.locate(new FMGeoCoord(1,
                        new FMMapCoord(12961658.576796599,4861819.63807118)),320);
            }
        },10000);



    }

    /**
     * 更新约束定位点
     *
     * @param coord 坐标
     */
    private void updateHandledMarker(FMGeoCoord coord, float angle) {
        if (mHandledMarker == null) {
            mHandledMarker = ViewHelper.buildLocationMarker(coord.getGroupId(), coord.getCoord(), angle);
            mLocationLayer.addMarker(mHandledMarker);
        } else {
            mHandledMarker.updateAngleAndPosition(coord.getGroupId(), angle, coord.getCoord());
        }
    }


    @Override
    public void updateLocateGroupView() {
        int groupSize = mFMMap.getFMMapInfo().getGroupSize();
        int position = groupSize - mFMMap.getFocusGroupId();
        mSwitchFloorComponent.setSelected(position);
    }

    /**
     * 设置地图2、3D效果
     */
    private void setViewMode() {
        if (mFMMap.getCurrentFMViewMode() == FMViewMode.FMVIEW_MODE_2D) {
            mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_3D);
        } else {
            mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_2D);
        }
    }

    /**
     * 设置是否为第一人称
     *
     * @param enable true 第一人称
     *               false 第三人称
     */
    private void setViewState(boolean enable) {
        this.mIsFirstView = !enable;
        setFloorControlEnable();
    }

    /**
     * 设置跟随状态
     *
     * @param enable true 跟随
     *               false 不跟随
     */
    private void setFollowState(boolean enable) {
        mHasFollowed = enable;
        setFloorControlEnable();
    }

    /**
     * 设置楼层控件是否可用
     */
    private void setFloorControlEnable() {
        if (getFloorControlEnable()) {
            mSwitchFloorComponent.close();
            mSwitchFloorComponent.setEnabled(false);
        } else {
            mSwitchFloorComponent.setEnabled(true);
        }
    }

    /**
     * 楼层控件是否可以使用。
     */
    private boolean getFloorControlEnable() {
        return mHasFollowed || mIsFirstView;
    }


    /**
     * 更新行走距离和文字导航。
     */
    private void updateWalkRouteLine(FMNavigationInfo info) {
        // 剩余时间
        int timeByWalk = ConvertUtils.getTimeByWalk(info.getSurplusDistance());

        // 导航路段描述
        String description = info.getNaviText();

        String viewText = getResources().getString(R.string.label_walk_format, info.getSurplusDistance(),
                timeByWalk, description);

        ViewHelper.setViewText(FMNavigationApplication.this, R.id.txt_info, viewText);

        if (!description.equals(mLastDescription)) {
            mLastDescription = description;
            startSpeaking(mLastDescription);
        }
    }

    private void updateNavigationOption() {
        mNaviOption.setFollowAngle(mIsFirstView);
        mNaviOption.setFollowPosition(mHasFollowed);
    }

    @Override
    public void onCheckStateChanged(View view, boolean isChecked) {
        switch (view.getId()) {
            case R.id.btn_3d: {
                setViewMode();
            }
            break;
            case R.id.btn_view: {
                setViewState(isChecked);
            }
            break;
            case R.id.btn_locate: {
                setFollowState(isChecked);
                try{
                    GetLocation();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            break;
            default:
                break;
        }
    }

    /**
     * 楼层切换控件初始化
     */
    private void initSwitchFloorComponent() {
        mSwitchFloorComponent = new FMSwitchFloorComponent(this);
        //最多显示6个
        mSwitchFloorComponent.setMaxItemCount(6);
        mSwitchFloorComponent.setEnabled(false);
        mSwitchFloorComponent.setOnFMSwitchFloorComponentListener(new FMSwitchFloorComponent.OnFMSwitchFloorComponentListener() {
            @Override
            public boolean onItemSelected(int groupId, String floorName) {
                mFMMap.setFocusByGroupId(groupId, null);
                return true;
            }
        });

        mSwitchFloorComponent.setFloorDataFromFMMapInfo(mFMMap.getFMMapInfo(), mFMMap.getFocusGroupId());

        addSwitchFloorComponent();
    }

    /**
     * 添加楼层切换按钮
     */
    private void addSwitchFloorComponent() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout viewGroup = (FrameLayout) findViewById(R.id.layout_group_control);
        viewGroup.addView(mSwitchFloorComponent, lp);
    }


    /**
     * 创建语音合成SpeechSynthesizer对象
     */
    private void createSynthesizer() {
        //1.创建 SpeechSynthesizer 对象, 第二个参数： 本地合成时传 InitListener
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        //2.合成参数设置，详见《 MSC Reference Manual》 SpeechSynthesizer 类
        //设置发音人（更多在线发音人，用户可参见科大讯飞附录13.2
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "100");//设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
    }

    /**
     * 开始语音合成
     *
     * @param inputStr 语音合成文字
     */
    private void startSpeaking(String inputStr) {
        mTts.stopSpeaking();
        mTts.startSpeaking(inputStr, null);
    }

    @Override
    public void onBackPressed() {
        if (!isMapLoaded)
            return;

        if (mTts != null) {
            mTts.destroy();
        }

        // 释放资源
        mNavigation.clear();
        mNavigation.release();

        super.onBackPressed();
    }

    @Override
    public void onCrossGroupId(final int lastGroupId, final int currGroupId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFMMap.setFocusByGroupId(currGroupId, null);
                updateLocateGroupView();
            }
        });
    }

    @Override
    public void onWalking(final FMNavigationInfo navigationInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 更新定位标志物
                updateHandledMarker(navigationInfo.getPosition(), navigationInfo.getAngle());

                // 更新路段显示信息
                updateWalkRouteLine(navigationInfo);

                // 更新导航配置
                updateNavigationOption();
            }
        });
    }

    @Override
    public void onComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String description = "到达目的地";
                String info = getResources().getString(R.string.label_walk_format, 0f,
                        0, description);
                ViewHelper.setViewText(FMNavigationApplication.this, R.id.txt_info, info);

                startSpeaking(description);
            }
        });
    }

    //定位
    public void GetLocation(/*View source*/) throws MalformedURLException {
        log("点击定位了");
        if (flag) {
            log("flag is true");
            setFlag();
            if (!isLocating) {
                log("locating 这里");
               // viewHandler.sendEmptyMessage(LOCATION_START);
                startService(new Intent(this, StepCountLocationService.class));
                receiver = new MyReceiver();
                IntentFilter filter = new IntentFilter();
                filter.setPriority(30);
//                filter.addAction("com.example.amap.service.LocationService");
                filter.addAction("com.example.amap.service.StepCountLocationService");
                registerReceiver(receiver, filter);
                isLocating = true;
            } else {
               // viewHandler.sendEmptyMessage(LOCATION_CLOST);
                unregisterReceiver(receiver);
                //结束服务，如果想让服务一直运行就注销此句
                stopService(new Intent(this, StepCountLocationService.class));
                isLocating = false;
                isFirstLocating = true;
                log("停了");
            }
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    flag = true;
                }
            }, 2000);
        }


    }

    void log(String s){
        Log.v("AAAA",s);
    }

    //获取广播数据
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            double ax = bundle.getDouble("ax");
            double ay = bundle.getDouble("ay");
            int az = bundle.getInt("az");
            int astate = bundle.getInt("astate");
            Log.v("AAAA","x="+ax+"y="+ay+"z="+az);
            switch (astate) {
                case 10086:
                    Log.v("AAAA","x="+ax+"y="+ay+"z="+az);

                    FMNavigationApplication.sActualNavigation.locate(new FMGeoCoord(1,
                            new FMMapCoord(ax, ay)),270);

//                    viewHandler.sendEmptyMessage(LOCATION_ING);
//                    if (isFirstLocating) {
//                        ShowToast(R.string.location_success);
//                        isFirstLocating = false;
//                    }
//                    Message msg = new Message();
//                    msg.what=10086;
//                    msg.setData(bundle);//mes利用Bundle传递数据
//                    Point mapPoint = new Point(LocationToMapX(ax), LocationToMapY(ay));
//                    ShowLog(mapPoint.toString());
//                    MyPoint newMyPoint = new MyPoint(MapToMyPointX(mapPoint.getX()), MapToMyPointY(mapPoint.getY()), az);
//                    ShowLog("zzz:"+newMyPoint.toString());
//                    if (locateMyPoint!=null && !newMyPoint.equal(locateMyPoint)) {//注意是不等号= =
//                        Bundle addBundle = new Bundle();
//                        addBundle.putBoolean("ischanged", true);
//                        setResultExtras(addBundle);
//                    }
//                    viewHandler.sendMessage(msg);

                    break;
//                case LOCATION_NET_ERROR:
////					abortBroadcast();
//                    viewHandler.sendEmptyMessage(LOCATION_NET_ERROR);
//                    break;
//                case LOCATION_NO_IN_MAP:
////					abortBroadcast();
//                    viewHandler.sendEmptyMessage(LOCATION_NO_IN_MAP);
//                    break;
//                case LOCATION_LOCATION_IP_ERROR:
////					abortBroadcast();
//                    viewHandler.sendEmptyMessage(LOCATION_LOCATION_IP_ERROR);
//                    break;
//                case LOCATION_LOCATION_IP_NOSET:
////					abortBroadcast();
//                    viewHandler.sendEmptyMessage(LOCATION_LOCATION_IP_NOSET);
//                    break;
//                default:
////					abortBroadcast();
//                    break;
            }
        }
    }

}
