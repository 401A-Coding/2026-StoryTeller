package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.local.prefs.PrefsUtils;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.activity.MainActivity;
import com.example.storyteller.ui.activity.CharacterActivity;
import com.example.storyteller.ui.activity.MaterialActivity;
import com.example.storyteller.ui.activity.PlotTreeActivity;
import com.example.storyteller.ui.activity.StoryGenerateActivity;
import com.example.storyteller.ui.adapter.StoryAdapter;
import java.util.List;

public class HomeFragment extends BaseFragment {

    private RecyclerView rvStory;
    private TextView tvCurrentNovel;
    private StoryDao storyDao;
    private StoryAdapter adapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view) {
        // 功能卡片点击
        view.findViewById(R.id.card_story_generate)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), StoryGenerateActivity.class)));
        view.findViewById(R.id.card_character)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), CharacterActivity.class)));
        view.findViewById(R.id.card_plot_tree)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), PlotTreeActivity.class)));
        view.findViewById(R.id.card_material)
            .setOnClickListener(v -> startActivity(new Intent(requireContext(), MaterialActivity.class)));

        view.findViewById(R.id.btn_switch_novel).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_BOOKSHELF);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 本地数据存储演示（直接写入测试故事，不走 AI）
        rvStory = view.findViewById(R.id.rv_story);
        tvCurrentNovel = view.findViewById(R.id.tv_current_novel);
        Button btnAdd = view.findViewById(R.id.btn_add_demo_story);

        rvStory.setLayoutManager(new LinearLayoutManager(getContext()));

        btnAdd.setOnClickListener(v -> {
            int count = insertTestStories();
            if (count > 0) {
                Toast.makeText(getContext(), "已新增 " + count + " 篇测试故事", Toast.LENGTH_SHORT).show();
                refreshList();
            } else {
                Toast.makeText(getContext(), "新增失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());
        seedTestStoriesIfEmpty();
        refreshList();
        refreshCurrentNovel();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCurrentNovel();
    }

    private void refreshList() {
        List<Story> stories = storyDao.getAllStories();
        if (adapter == null) {
            adapter = new StoryAdapter(requireContext(), stories);
            rvStory.setAdapter(adapter);
        } else {
            adapter.setData(stories);
        }
    }

    private void refreshCurrentNovel() {
        if (tvCurrentNovel == null) {
            return;
        }
        String selectedId = PrefsUtils.getInstance(requireContext()).getString(StoryAdapter.PREF_SELECTED_STORY_ID, "");
        if (selectedId == null || selectedId.isEmpty()) {
            tvCurrentNovel.setText("当前小说：未选择");
            return;
        }
        try {
            int storyId = Integer.parseInt(selectedId);
            Story story = storyDao.getStoryById(storyId);
            tvCurrentNovel.setText(story == null ? "当前小说：未选择" : "当前小说：" + story.getTitle());
        } catch (NumberFormatException e) {
            tvCurrentNovel.setText("当前小说：未选择");
        }
    }

    private void seedTestStoriesIfEmpty() {
        if (!storyDao.getAllStories().isEmpty()) {
            return;
        }
        insertTestStories();
    }

    private int insertTestStories() {
        long now = System.currentTimeMillis();
        Story[] stories = new Story[]{
                new Story("雨夜旧书店", "深夜的小巷里，一家旧书店忽然亮起灯光。林若曦推门而入，发现店主似乎早就认识她，而角落里那本没有封面的书，正写着她的名字。她翻开第一页，里面记录的不是书目，而是她小时候最害怕的那场大火。随着雨声越来越急，书页开始一页页显出新的字迹，像是在提醒她，真正被遗忘的不是这家书店，而是她自己。\n\n店主没有多解释，只说这本书会在合适的时候替人找回记忆。林若曦原本只想避雨，却被迫在书店里读完了一段又一段像是自己过去写下的句子：搬家前的老宅、离开的母亲、某个深夜消失的朋友、以及一场她始终不愿回想的事故。每一段文字都像一根细线，把她拉回一个被尘封的时间点。她开始怀疑，自己这些年所有的“忘记”，也许并不是因为年岁太久，而是有人刻意让她遗忘。\n\n当她想把书带走时，店里的灯突然全部熄灭，只剩柜台上的铃铛在雨声里轻轻作响。店主这才告诉她，书店只会在“被选中的人”出现时亮灯，而书页的内容并不会一次说完，它只会一点点揭开真相。林若曦站在黑暗里，第一次意识到，自己或许不是来躲雨的，而是来找回一段本该属于自己的过去。", "悬疑", now),
                new Story("风铃山谷", "顾言跟着一张褪色的地图来到山谷，遇见了沉默寡言的守林人阿烈。山谷入口挂满风铃，每当风吹过，铃声就会指向不同的方向，仿佛在替迷路的人做选择。阿烈告诉他，山谷深处藏着一座早已废弃的观测站，那里保存着一份关于失踪旅人的名单。顾言越往里走，越觉得自己像是在追查别人的过去，也像是在寻找自己失去的那段记忆。\n\n山谷里的路并不长，却总会在同一块青石前岔开。每一次顾言以为自己选对了方向，风铃都会从背后响起，把他带回起点。阿烈始终跟在不远处，既不阻拦，也不主动解释，只在夜里生火时说，山谷会把每个来访者最想逃开的东西翻出来。顾言原本以为自己只是来找一个失踪的旅行团，后来却在废弃观测站里看到一本泛黄的记录册，里面竟有自己童年时的姓名和日期。那一刻他突然明白，这趟旅程真正的目的不是追踪失踪者，而是让他重新面对一段被自己弄丢的离别。\n\n当山谷尽头的风铃全部同时响起时，阿烈第一次露出复杂的神情。他说这里的每个人最后都会做一个选择：继续往前，或者回头承认自己真正想找的东西。顾言站在山谷最高处，看着雾气从林梢间慢慢散开，终于决定不再把“寻找”当成借口，而是正视那段一直没敢承认的过去。", "冒险", now + 1),
                new Story("星河列车", "苏浅搭上了开往星河尽头的列车，车厢里每一位乘客都带着一个未说出口的愿望。列车长纪衡微笑着说，车票的代价是你的一个记忆，而车窗外的星光会把这些记忆变成漂浮的碎片。苏浅在第七节车厢里遇见了一位始终不肯摘下手套的老人，对方声称自己曾经在终点站下过车，却又在下一站重新回到了列车上。随着列车穿过一片没有边界的光海，苏浅逐渐意识到，这趟旅程真正要到达的不是宇宙尽头，而是她一直不敢面对的那段告别。\n\n列车每经过一座星站，广播里都会播报一则与乘客有关的秘密。有人在座位下发现故乡的编号，有人在窗上看见死去亲人的影子，也有人在餐车里吃到只属于童年的味道。苏浅原本只是想去终点站寻找一封迟到的信，却在一次次穿梭中发现，纪衡像是早已知道她会来，甚至知道她会在某个时刻忘记自己真正要寻找的人。那位戴手套的老人告诉她，所有乘客都在用记忆交换一件东西，有人换回勇气，有人换回歉意，而有的人只是想换回继续生活的资格。\n\n当列车驶入最亮的那片光海时，苏浅终于明白，终点站并不是一个地名，而是一次与过去的正式告别。她在车窗里看见另一个自己，也看见纪衡轻轻摘下帽檐，像在等待一场早已写好的结局。", "科幻", now + 2)
        };

        int inserted = 0;
        for (Story story : stories) {
            long id = storyDao.insertStory(story);
            if (id > 0) {
                inserted++;
            }
        }
        return inserted;
    }
}
