package www.lince.com.banner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Banner banner;
    private List<Integer> mData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (banner != null) {
            banner.resume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (banner != null) {
            banner.stop();
        }
    }

    private void initData() {
        mData.add(R.mipmap.intro1);
        mData.add(R.mipmap.intro2);
        mData.add(R.mipmap.intro3);
        //开始自动轮播
        banner.notifyDataSetChanged(mData.size());
        banner.fly();
    }

    private void initUI() {
        banner = (Banner) findViewById(R.id.banner);
        banner.setDuration(2);// 设置轮播间隔
//        banner.setTransformer(new Banner.DepthPageTransformer());// 设置页面切换过渡效果
        banner.setOnPageChangeListener(new Banner.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {

            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageSelected(int position) {

            }
        });
        banner.setSize(mData.size(), new Banner.Callback() {

            @Override
            public void onItemView(ImageView v, int position) {

                // 加载网络图片或者设置本地图片
                v.setImageResource(mData.get(position));
            }

            @Override
            public void onItemClicked(ImageView imageView, int position) {

                // 页点击事件
                Toast.makeText(MainActivity.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
