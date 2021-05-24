package com.razerdp.huobi.analysis.ui.activity;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.utils.SpanUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi_analysis.R;

import butterknife.BindView;

public class GuideActivity extends BaseActivity {

    @BindView(R.id.tv_desc)
    TextView tvDesc;
    @BindView(R.id.tv_step1)
    TextView tvStep1;

    @Override
    public int contentViewLayoutId() {
        return R.layout.activity_guide;
    }

    @Override
    protected void onInitView(View decorView) {
        SpanUtil.create("① 登录 火币全球站 官网；\n" +
                "\n" +
                "② 点击头像-点击“API管理”；\n" +
                "\n" +
                "③ 自定义备注-勾选“读取”（请务必不要勾选交易，否则可能会被人盗用并造成无法挽回的损失）-点击“确认”；\n" +
                "\n" +
                "④ 复制并保存“Access Key”和“Secret Key”；\n" +
                "\n" +
                "⑤ 将获取的Access Key、Secret Key填入 添加用户 对应输入框中。")
                .append("火币全球站")
                .setSpanClickListener(v -> ToolUtil.openInSystemBroswer(self(), "https://www.huobi.pe/zh-cn/"))
                .setTextColor(UIHelper.getColor(R.color.common_blue))
                .setTextStyle(Typeface.DEFAULT_BOLD)
                .append("（请务必不要勾选交易，否则可能会被人盗用并造成无法挽回的损失）")
                .setTextColor(UIHelper.getColor(R.color.common_red))
                .setTextStyle(Typeface.DEFAULT_BOLD)
                .into(tvDesc);

        SpanUtil.create("一、 登录火币全球站官网\n" +
                "登录 火币全球站 官网，若无火币全球站账号请先注册。")
                .append("火币全球站")
                .setSpanClickListener(v -> ToolUtil.openInSystemBroswer(self(), "https://www.huobi.pe/zh-cn/"))
                .setTextColor(UIHelper.getColor(R.color.common_blue))
                .setTextStyle(Typeface.DEFAULT_BOLD)
                .into(tvStep1);

    }


}