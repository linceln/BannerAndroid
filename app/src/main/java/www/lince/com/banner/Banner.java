package www.lince.com.banner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.PageTransformer;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Scroller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Banner extends FrameLayout {

    private Context context;

    private Handler handler = new Handler();

    private PagerAdapter pagerAdapter;

    private PageTransformer transformer;// 过渡动画

    private List<ImageView> imageViews = new ArrayList<>();

    private List<ImageView> iv_dots = new ArrayList<>();

    private ViewPager vp;

    private LinearLayout linear_dots;

    private int currentItem = 1;// ViewPager当前选中页面

    private boolean isAutoPlaying;// 是否正在播放，手势滑动判断时要用到

    private boolean canAutoPlay = true;// 能否自动播放

    private boolean isAnimated;// 是否设置动画效果

    private int duration = 3;// 自动播放时间间隔（单位：秒）

    private int scrollDuration = 1;// （单位：秒）控制页面切换速度的时间，要小于上面的duration，否则OnPageChangeListener不会触发

    private Drawable dot_default;// 底部指示器图片默认

    private Drawable dot_highlight;// 底部指示器图片高亮

    private Drawable defaultDrawable;// 默认的广告图，无数据时显示

    private Runnable task = new Runnable() {

        @Override
        public void run() {

            if (isAutoPlaying) {// 拖拽时不自动滚动
                vp.setCurrentItem(currentItem);
            }
            currentItem++;
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(duration));
        }
    };

    private Callback callback;
    private OnPageChangeListener pageChangeListener;

    public Banner(Context context) {

        this(context, null);
    }

    public Banner(Context context, AttributeSet attrs) {

        this(context, attrs, 0);
    }

    public Banner(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        if (a.hasValue(R.styleable.Banner_dotDrawableDefault)) {
            dot_default = a.getDrawable(R.styleable.Banner_dotDrawableDefault);
        }
        if (a.hasValue(R.styleable.Banner_dotDrawableHighlight)) {
            dot_highlight = a.getDrawable(R.styleable.Banner_dotDrawableHighlight);
        }
        if (a.hasValue(R.styleable.Banner_defaultDrawable)) {
            defaultDrawable = a.getDrawable(R.styleable.Banner_defaultDrawable);
        }
        a.recycle();

        this.context = context;
        initUI();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Banner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Banner);
        if (a.hasValue(R.styleable.Banner_dotDrawableDefault)) {
            dot_default = a.getDrawable(R.styleable.Banner_dotDrawableDefault);
        }
        if (a.hasValue(R.styleable.Banner_dotDrawableHighlight)) {
            dot_highlight = a.getDrawable(R.styleable.Banner_dotDrawableHighlight);
        }
        if (a.hasValue(R.styleable.Banner_defaultDrawable)) {
            defaultDrawable = a.getDrawable(R.styleable.Banner_defaultDrawable);
        }
        a.recycle();

        this.context = context;
        initUI();
    }

    /**
     * 回调
     */
    public interface Callback {

        /**
         * 对每个item的ImageView做处理
         *
         * @param v
         * @param position
         */
        void onItemView(ImageView v, int position);

        /**
         * item点击事件
         *
         * @param v
         * @param position
         */
        void onItemClicked(ImageView v, int position);
    }

    /**
     * 设置开始自动播放Banner
     */
    public void fly() {

        if (canAutoPlay && !imageViews.isEmpty()) {
            stop();
            isAutoPlaying = true;
            handler.post(task);
        }
    }

    /**
     * 设置自动播放间隔
     *
     * @param second 单位：秒（大于1秒）
     */
    public void setDuration(int second) {

        duration = second;
    }

    /**
     * 设置过渡动画效果
     *
     * @param transformer private class DepthPageTransformer implements ViewPager.PageTransformer { private static final float MIN_SCALE = 0.75f;
     *                    <p>
     *                    public void transformPage(View view, float position) { int pageWidth = view.getWidth();
     *                    <p>
     *                    if (position < -1) { view.setAlpha(0);
     *                    <p>
     *                    } else if (position <= 0) { view.setAlpha(1); view.setTranslationX(0); view.setScaleX(1); view.setScaleY(1);
     *                    <p>
     *                    } else if (position <= 1) { view.setAlpha(1 - position);
     *                    <p>
     *                    view.setTranslationX(pageWidth * -position);
     *                    <p>
     *                    float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position)); view.setScaleX(scaleFactor); view.setScaleY(scaleFactor);
     *                    <p>
     *                    } else { view.setAlpha(0); } } }
     */
    public void setTransformer(PageTransformer transformer) {
        this.transformer = transformer;
        isAnimated = true;
        vp.setPageTransformer(true, transformer);
    }

    public void notifyDataSetChanged(int size) {

        clearData();
        setSize(size, callback);
    }

    /**
     * 设置Banner容量和回调
     *
     * @param size
     * @param callback
     */
    public void setSize(int size, final Callback callback) {

        clearData();
        this.callback = callback;
        canAutoPlay = true;
        if (size <= 0) {
            // 不滚动
            canAutoPlay = false;
            final ImageView imageView = new ImageView(context);
            imageView.setScaleType(ScaleType.FIT_XY);
            // 设置一张默认广告图
            imageView.setImageDrawable(defaultDrawable);
            imageViews.add(imageView);
            pagerAdapter.notifyDataSetChanged();
            vp.setCurrentItem(0, false);
            return;
        }

        if (size == 1) {
            // 不滚动
            canAutoPlay = false;
            final ImageView imageView = new ImageView(context);
            imageView.setScaleType(ScaleType.FIT_XY);
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    if (callback != null) {
                        callback.onItemClicked(imageView, 0);
                    }
                }
            });
            if (callback != null) {
                callback.onItemView(imageView, 0);
            }
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(layoutParams);
            imageViews.add(imageView);
            pagerAdapter.notifyDataSetChanged();
            vp.setCurrentItem(0, false);
            return;
        }

        // B AB A
        // C ABC A
        // D ABCD A
        // E ABCDE A
        // F ABCDEF A
        for (int i = 0; i < size + 2; i++) {
            int j = 0;
            if (i == 0) {// 第0个变成List最后一个
                j = size - 1;
            } else if (i == size + 2 - 1) {// 倒数第二个变成List第0个
                j = 0;
            } else {
                j = i - 1;
            }
            final int k = j;
            final ImageView imageView = new ImageView(context);
            imageView.setScaleType(ScaleType.FIT_XY);
            // 设置点击监听
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    if (callback != null) {
                        callback.onItemClicked(imageView, k);
                    }
                }
            });
            // 设置ImageView的其它属性
            if (callback != null) {
                callback.onItemView(imageView, j);
            }
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(layoutParams);
            imageViews.add(imageView);
        }

        if (dot_default != null && dot_highlight != null) {
            // 指示器
            for (int i = 0; i < size; i++) {
                ImageView iv_dot = new ImageView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.leftMargin = getDp(4);
                params.rightMargin = getDp(4);
                // 指示器的图片
                iv_dot.setImageDrawable(dot_default);
                linear_dots.addView(iv_dot, params);
                iv_dots.add(iv_dot);
            }
        }
        pagerAdapter.notifyDataSetChanged();
        vp.setCurrentItem(currentItem, false);
    }

    private void clearData() {

        imageViews.clear();
        iv_dots.clear();
        linear_dots.removeAllViews();
        currentItem = 1;
    }

    /**
     * 恢复自动播放
     */
    public void resume() {

        if (canAutoPlay && !isAutoPlaying) {
            fly();
        }
    }

    /**
     * 停止自动播放
     */
    public void stop() {

        if (canAutoPlay && isAutoPlaying) {
            isAutoPlaying = false;
            handler.removeCallbacks(task);
        }
    }

    private void initUI() {

        createViews();
        initViewPager();
    }

    private void initViewPager() {

        pagerAdapter = new PagerAdapter() {

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {

                return arg0 == arg1;
            }

            @Override
            public int getCount() {

                return imageViews.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {

                container.addView(imageViews.get(position));
                return imageViews.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {

                container.removeView(imageViews.get(position));
            }
        };
        vp.setAdapter(pagerAdapter);
        vp.setFocusable(true);
        // 改变滑动时间，注意滑动时间要小于自动滑动的间隔，否则无法触发OnPageChangeListener监听
        ViewPagerScroller scroller = new ViewPagerScroller(context);
        scroller.setScrollDuration((int) TimeUnit.SECONDS.toMillis(scrollDuration));
        scroller.initViewPagerScroll(vp);
        vp.addOnPageChangeListener(new ViewPagerOnPageChangeListener());
        vp.setCurrentItem(currentItem, false);
    }

    private void createViews() {
        // ViewPager Match
        vp = new ViewPager(context);
        LayoutParams vpParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        vp.setLayoutParams(vpParams);
        // LinearLayout Bottom
        linear_dots = new LinearLayout(context);
        linear_dots.setGravity(Gravity.CENTER);
        linear_dots.setPadding(0, 0, 0, getDp(6));
        LayoutParams linearParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        linearParams.gravity = Gravity.BOTTOM;
        linear_dots.setLayoutParams(linearParams);
        // 添加ViewPager和LinearLayout到FrameLayout
        this.addView(vp);
        this.addView(linear_dots);
    }

    //  ViewPager被占用的监听接口
    interface OnPageChangeListener {

        void onPageScrollStateChanged(int state);

        void onPageScrolled(int arg0, float arg1, int arg2);

        void onPageSelected(int position);
    }

    public void setOnPageChangeListener(OnPageChangeListener pageChangeListener) {

        this.pageChangeListener = pageChangeListener;
    }

    private class ViewPagerOnPageChangeListener implements ViewPager.OnPageChangeListener {


        private boolean isFirstTime = true;

        @Override
        public void onPageScrollStateChanged(int state) {
            if (pageChangeListener != null) {
                pageChangeListener.onPageScrollStateChanged(state);
            }
            switch (state) {
                case ViewPager.SCROLL_STATE_DRAGGING:
                    isAutoPlaying = false;
                    currentItem = vp.getCurrentItem() + 1;
                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    isAutoPlaying = false;
                    currentItem = vp.getCurrentItem() + 1;
                    break;
                case ViewPager.SCROLL_STATE_IDLE:
                    if (vp.getCurrentItem() == 0) {// 滑到第0个时回到倒数第一个
                        if (isFirstTime && isAnimated) {// 解决添加动画效果之后，第一次往右滑动时出现的问题
                            isFirstTime = false;
                            vp.setPageTransformer(false, null);
                            vp.setCurrentItem(imageViews.size() - 2, false);
                            vp.setPageTransformer(true, transformer);
                        } else {
                            vp.setCurrentItem(imageViews.size() - 2, false);
                        }
                    } else if (vp.getCurrentItem() == imageViews.size() - 1) {// 滑到最后一个时回到第一个
                        vp.setCurrentItem(1, false);
                        currentItem = 1;
                    }
                    currentItem = vp.getCurrentItem() + 1;
                    isAutoPlaying = true;
                    break;
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            if (pageChangeListener != null) {
                pageChangeListener.onPageScrolled(arg0, arg1, arg2);
            }
        }

        @Override
        public void onPageSelected(int position) {

            if (pageChangeListener != null) {
                pageChangeListener.onPageSelected(position);
            }

            if (dot_default != null && dot_highlight != null) {
                // 指示器的图片
                for (int i = 0; i < iv_dots.size(); i++) {
                    iv_dots.get(i).setImageDrawable(dot_default);
                }
                if (position == 0) {// 滑到第0个时跳到倒数第二个，其实是倒数第二个被选中
                    iv_dots.get(iv_dots.size() - 1).setImageDrawable(dot_highlight);
                } else if (position == iv_dots.size() + 1) {// 滑到最后一个时跳到第一个，其实是第1个被选中
                    iv_dots.get(0).setImageDrawable(dot_highlight);
                } else {
                    iv_dots.get(position - 1).setImageDrawable(dot_highlight);
                }
            }
        }
    }


    /**
     * ViewPager 滚动速度设置
     */
    private class ViewPagerScroller extends Scroller {

        private int mScrollDuration;// 滑动速度

        /**
         * 设置速度速度
         *
         * @param duration
         */
        public void setScrollDuration(int duration) {

            this.mScrollDuration = duration;
        }

        public ViewPagerScroller(Context context) {

            super(context);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator) {

            super(context, interpolator);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator, boolean flywheel) {

            super(context, interpolator, flywheel);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {

            super.startScroll(startX, startY, dx, dy, mScrollDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {

            super.startScroll(startX, startY, dx, dy, mScrollDuration);
        }

        public void initViewPagerScroll(ViewPager viewPager) {

            try {
                Field mScroller = ViewPager.class.getDeclaredField("mScroller");
                mScroller.setAccessible(true);
                mScroller.set(viewPager, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getDp(int i) {

        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i, context.getResources().getDisplayMetrics());
    }

    public static class DepthPageTransformer implements PageTransformer {

        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {

            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left
                // page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public static class ZoomOutPageTransformer implements PageTransformer {

        private static final float MIN_SCALE = 0.85f;

        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {

            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

//			Log.e("TAG", view + " , " + position + "");

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1)               // a页滑动至b页 ； a页从 0.0 -1
            // ；b页从1
            // ~
            // 0.0
            { // [-1,1]
                // Modify the default slide transition to shrink the page as
                // well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
}