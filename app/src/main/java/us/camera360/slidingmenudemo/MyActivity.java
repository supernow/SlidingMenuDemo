package us.camera360.slidingmenudemo;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;


public class MyActivity extends Activity implements View.OnTouchListener {
    /**
     * 手指每单位时间滑动200个像素
     */
    private static final int SPEED = 200;
    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * menu的layout
     */
    private LinearLayout mMenuLayout;
    /**
     * content的layout
     */
    private LinearLayout mContentLayout;
    /**
     * menu的layout的Paramters
     */
    private LinearLayout.LayoutParams mMenuParams;
    /**
     * menu完全显示的时候给content的宽度值
     */
    private int mMenuPadding = 80;
    /**
     * menu最多滑到左边缘，值由menu布局的宽度决定，marginLeft到达此值之后，不能在减少
     */
    private int mLeftEdge;
    /**
     * 测速度的对象
     */
    private VelocityTracker mVelocityTracker;
    /**
     * 手指按下的X坐标
     */
    private float mXDown;
    /**
     * 手指移动时候的X坐标
     */
    private float mXMove;
    /**
     * 手指抬起的X坐标
     */
    private float mXUp;
    /**
     * menu是否再显示
     */
    private boolean mIsMenuVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        initViewsAndValues();
        mContentLayout.setOnTouchListener(this);
    }

    /**
     * 初始化menu和content并且设置他们的位置
     */
    private void initViewsAndValues() {
        //得到windowManager
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //得到屏幕宽度
        mScreenWidth = window.getDefaultDisplay().getWidth();
        //找到控件
        mMenuLayout = (LinearLayout) findViewById(R.id.menu);
        mContentLayout = (LinearLayout) findViewById(R.id.content);
        //得到menu的paramter
        mMenuParams = (LinearLayout.LayoutParams) mMenuLayout.getLayoutParams();
        //将menu的宽度设置为屏幕宽度减去mMenuPading
        mMenuParams.width = mScreenWidth - mMenuPadding;
        //左边缘的值复制为menu宽度的负数，这样的话就可以将menu隐藏
        mLeftEdge = -mMenuParams.width;
        //将margin设置为mLeftEdge
        mMenuParams.leftMargin = mLeftEdge;
        //将content显示再屏幕上
        mContentLayout.getLayoutParams().width = mScreenWidth;
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        createVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //记录X坐标
                mXDown = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                mXMove = event.getRawX();
                int distanceX = (int) (mXMove - mXDown);
                if (mIsMenuVisible) {
                    mMenuParams.leftMargin = distanceX;
                } else {
                    mMenuParams.leftMargin = mLeftEdge + distanceX;
                }
                if (mMenuParams.leftMargin < mLeftEdge) {
                    //因为leftEdge是负数，就是menu已经隐藏完毕了，不能再往左隐藏了
                    mMenuParams.leftMargin = mLeftEdge;
                } else if (mMenuParams.leftMargin > 0) {
                    //menu显示完全了，不能再往右移动了
                    mMenuParams.leftMargin = 0;
                }
                mMenuLayout.setLayoutParams(mMenuParams);
                break;
            case MotionEvent.ACTION_UP:
                mXUp = event.getRawX();
                if (wantToShowMenu()) {
                    if (shouldScrollToMenu()) {
                        scrollToMenu();
                    } else {
                        //条件不满足，把menu继续隐藏掉
                        scrollToContent();
                    }
                } else if (wantToShowContent()) {
                    if (shouldScrollToContent()) {
                        scrollToContent();
                    } else {
                        scrollToMenu();
                    }
                }
                break;
        }
        recycleVelocityTracker();
        return true;
    }

    /**
     * 创建VelocityTracker对象，针对于content的界面的滑动事件
     *
     * @param event
     */
    private void createVelocityTracker(MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 判断手势是不是想要显示Content && menu处于显示状态
     *
     * @return
     */
    private boolean wantToShowContent() {
        return mXUp - mXDown < 0 && mIsMenuVisible;
    }

    /**
     * 是不是要显示menu && menu处于隐藏状态
     *
     * @return
     */
    private boolean wantToShowMenu() {
        return mXUp - mXDown > 0 && !mIsMenuVisible;
    }

    /**
     * 是否应该滑动出menu
     *
     * @return
     */
    private boolean shouldScrollToMenu() {
        return mXUp - mXDown > mScreenWidth / 2 || getScrollVelocity() > SPEED;
    }

    /**
     * 是否应该让content全部显示出来
     *
     * @return
     */
    private boolean shouldScrollToContent() {
        return mXDown - mXUp < mScreenWidth / 2 || getScrollVelocity() > SPEED;
    }

    /**
     * 显示出menu
     */
    private void scrollToMenu() {
        new ScrollAsyncTask().execute(30);
    }

    /**
     * 隐藏掉menu
     */
    private void scrollToContent() {
        new ScrollAsyncTask().execute(-30);
    }


    /**
     * 得到手指滑动速度，每秒移动多少单位像素
     *
     * @return
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getXVelocity();
        return Math.abs(velocity);
    }

    /**
     * 回收VelocityTracker对象。
     */
    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    class ScrollAsyncTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer[] speed) {
            //得到当前margin
            int leftMargin = mMenuParams.leftMargin;
            //不断更改margin的值
            while (true) {
                leftMargin = leftMargin + speed[0];
                if (leftMargin > 0) {
                    leftMargin = 0;
                    break;
                }
                if (leftMargin < mLeftEdge) {
                    leftMargin = mLeftEdge;
                    break;
                }
                publishProgress(leftMargin);
                sleep();
            }
            if (speed[0] > 0) {
                mIsMenuVisible = true;
            } else {
                mIsMenuVisible = false;
            }
            return leftMargin;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mMenuParams.leftMargin = integer;
            mMenuLayout.setLayoutParams(mMenuParams);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mMenuParams.leftMargin = values[0];
            mMenuLayout.setLayoutParams(mMenuParams);
        }
    }

    /**
     * sleep线程睡眠一下
     */
    private void sleep() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
