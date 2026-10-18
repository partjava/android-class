package com.example.toutiao.demo;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 视频 + 专辑目录：进程内单例，mock 数据的唯一来源，也是后端接口数据的落点。
 * 频道列表构造后引用稳定（ViewPager2 分页与 feed 适配器持有同一实例），
 * 接口刷新只写缓存、下次启动经 loadCache() 生效，不做会话内重建。
 */
public final class VideoCatalog {
    private static final String PREFS = "video_catalog_v1";
    private static volatile VideoCatalog instance;

    private final Context context;
    private final List<VideoItem> recommend = new ArrayList<>();
    private final List<VideoItem> small = new ArrayList<>();
    private final List<VideoItem> movie = new ArrayList<>();
    private final List<VideoItem> music = new ArrayList<>();
    private final List<VideoAlbum> albums = new ArrayList<>();
    private final Map<String, VideoItem> byPrefixedTitle = new HashMap<>();
    private final Map<String, VideoItem> byRawTitle = new HashMap<>();
    private final Map<String, Integer> coverCache = new HashMap<>();
    private boolean fromApi;

    public static VideoCatalog get(Context context) {
        if (instance == null) {
            synchronized (VideoCatalog.class) {
                if (instance == null) instance = new VideoCatalog(context.getApplicationContext());
            }
        }
        return instance;
    }

    private VideoCatalog(Context context) {
        this.context = context;
        seedMock();
        rebuildIndex();
        migrateLegacyCollections();
        loadCache();
        rebuildIndex();
        migrateLegacyCollections();
    }

    private void migrateLegacyCollections() {
        VideoStore legacy = new VideoStore(context);
        ContentStore content = new ContentStore(context);
        SharedPreferences markers = context.getSharedPreferences("video_collection_migration_v1", Context.MODE_PRIVATE);
        for (List<VideoItem> channel : new List[]{recommend, small, movie, music}) {
            for (VideoItem item : channel) {
                String id = item.getId();
                if (!legacy.isCollected(id) || markers.getBoolean(id, false)) continue;
                try {
                    String key = NewsContract.videoNewsId(item.getTitle());
                    if (!content.contains("saved", key)) content.put("saved", NewsContract.videoSnapshot(item, albumIdOf(item)));
                    // Persist before a user can cancel the migrated collection.
                    if (!markers.edit().putBoolean(id, true).commit()) {
                        android.util.Log.w("VideoCatalog", "收藏迁移标记未保存，将在下次启动重试");
                    }
                } catch (RuntimeException error) {
                    android.util.Log.w("VideoCatalog", "旧收藏迁移失败，保留原记录供下次重试", error);
                }
            }
        }
    }

    public List<VideoItem> recommend() { return recommend; }
    public List<VideoItem> small() { return small; }
    public List<VideoItem> movie() { return movie; }
    public List<VideoItem> music() { return music; }
    public List<VideoAlbum> albums() { return albums; }
    public boolean isFromApi() { return fromApi; }

    public VideoAlbum albumById(String albumId) {
        if (albumId == null) return null;
        for (VideoAlbum album : albums) if (album.getId().equals(albumId)) return album;
        return null;
    }

    /** 视频所属专辑，独立视频返回 null。 */
    public VideoAlbum albumOf(VideoItem item) {
        return item == null ? null : albumById(albumIdOf(item));
    }

    public String albumIdOf(VideoItem item) {
        if (item == null) return "";
        for (VideoAlbum album : albums)
            for (String title : album.getEpisodeTitles())
                if (resolveTitle(title).equals(item.getTitle())) return album.getId();
        return "";
    }

    /** 专辑内全部视频，按集顺序。 */
    public List<VideoItem> albumVideos(String albumId) {
        List<VideoItem> result = new ArrayList<>();
        VideoAlbum album = albumById(albumId);
        if (album == null) return result;
        for (String title : album.getEpisodeTitles()) {
            VideoItem item = byTitle(resolveTitle(title));
            if (item != null) result.add(item);
        }
        return result;
    }

    /** 按getTitle()（带"课程样片 · "前缀）匹配，也兼容原始标题。 */
    public VideoItem byTitle(String displayTitle) {
        if (displayTitle == null) return null;
        VideoItem item = byPrefixedTitle.get(displayTitle);
        return item != null ? item : byRawTitle.get(displayTitle);
    }

    private String resolveTitle(String rawOrPrefixed) {
        VideoItem item = byRawTitle.get(rawOrPrefixed);
        return item != null ? item.getTitle() : rawOrPrefixed;
    }

    private void rebuildIndex() {
        byPrefixedTitle.clear();
        byRawTitle.clear();
        for (List<VideoItem> channel : new List[]{recommend, small, movie, music})
            for (VideoItem item : channel) {
                byPrefixedTitle.put(item.getTitle(), item);
                byRawTitle.put(rawTitle(item.getTitle()), item);
            }
    }

    /** 去掉展示前缀，得到可作专辑集标题的原始标题。 */
    private static String rawTitle(String displayTitle) {
        return displayTitle == null ? "" : displayTitle.replace("课程样片 · ", "");
    }

    // ---------- mock 种子（与后端 MOCK_VIDEOS 保持一致） ----------

    private void seedMock() {
        recommend.add(new VideoItem("纪录片：大国重器是怎样炼成的", "央视新闻", "12:36", "128万次播放",
                "镜头走进重型装备制造车间，记录一台大型设备从零件加工到整机装配的全过程。工人师傅们在微米级的精度要求下，一遍遍测量、打磨。",
                R.drawable.image2, 8600, 326));
        recommend.add(new VideoItem("延时摄影记录城市从黎明到黄昏", "影像志", "04:12", "56万次播放",
                "摄影师在城市最高处架设机位，连续拍摄十二个小时。整座城市的一天被压缩进短短几分钟。",
                R.drawable.image7, 3200, 168));
        recommend.add(new VideoItem("航拍中国：从空中俯瞰大好河山", "航拍视角", "08:45", "92万次播放",
                "镜头掠过奔腾的江河、金黄的麦田和错落的村落，山川河流呈现出与地面完全不同的壮阔面貌。",
                R.drawable.image5, 5400, 271));
        recommend.add(new VideoItem("实拍：消防员火场逆行救人全过程", "现场实录", "06:03", "213万次播放",
                "浓烟从窗口不断涌出。消防员沿着楼道逐层搜救，在五楼卧室内找到一名被困老人，背起老人沿原路撤离，整个过程不到六分钟。",
                R.drawable.img4, 9600, 412));
        recommend.add(new VideoItem("街头采访：你理想中的生活是什么样的", "城市观察", "09:28", "34万次播放",
                "记者在街头随机采访了二十位路人。答案各不相同，但大多朴素而具体。",
                R.drawable.image8, 2100, 486));

        small.add(new VideoItem("萌宠日常：猫咪第一次见到雪的反应", "萌宠日记", "00:15", "320万次播放",
                "第一次见到雪的猫咪先是愣在原地，随后小心翼翼地伸出一只爪子试探，最后还是忍不住扑了进去。",
                R.drawable.image5, 9800, 356));
        small.add(new VideoItem("一分钟学会三道家常菜，新手零失败", "厨房小窍门", "00:58", "88万次播放",
                "西红柿炒蛋、蒜蓉青菜、可乐鸡翅，三步一步不落，照着做就行。",
                R.drawable.img4, 4200, 512));
        small.add(new VideoItem("手艺人用木头做出会走的小机器人", "手工达人", "00:42", "156万次播放",
                "全程不用一颗钉子、一滴胶水，所有零件靠榫卯咬合。上紧发条后，机器人能稳稳走上好几米。",
                R.drawable.image7, 7300, 289));
        small.add(new VideoItem("街头钢琴：路人即兴弹奏惊艳全场", "街头艺术", "00:33", "271万次播放",
                "琴键摆在广场中央，谁都可以坐下来弹一段。这位路人坐下后，围观的人群渐渐安静了。",
                R.drawable.image8, 8900, 401));
        small.add(new VideoItem("慢镜头：雨天窗外的十分钟", "慢镜头", "00:21", "19万次播放",
                "雨滴砸在玻璃上，慢慢汇成一道道水痕。没有旁白，只有雨声。",
                R.drawable.image2, 1200, 87));

        movie.add(new VideoItem("话剧《茶馆》复排上演 幕后纪录", "戏剧观察", "15:20", "45万次播放",
                "本轮复排保留了经典版本的结构，同时在舞台呈现上做了新的尝试。镜头跟拍排练厅里的两个月。",
                R.drawable.image6, 2600, 143));
        movie.add(new VideoItem("电影幕后：一场雨戏是怎么拍出来的", "影视工业", "11:08", "67万次播放",
                "洒水车、灯光组、摄影机轨道，一场看起来只有两分钟的雨戏，背后是四十多人的配合。",
                R.drawable.image2, 3400, 197));
        movie.add(new VideoItem("老片修复：让经典重新清晰起来", "电影资料馆", "18:33", "28万次播放",
                "修复师逐帧处理划痕和噪点，一部两小时的老电影往往需要几个月才能完成。",
                R.drawable.image8, 1800, 96));
        movie.add(new VideoItem("配音演员现场：一个人配出整部剧", "声音工坊", "07:56", "103万次播放",
                "同一个人的声音在不同角色之间来回切换，录音棚外的观众听得目瞪口呆。",
                R.drawable.img4, 6700, 318));
        movie.add(new VideoItem("经典配乐赏析：旋律如何讲故事", "电影音乐", "13:14", "39万次播放",
                "从主题动机到配器选择，拆解几段耳熟能详的旋律是怎么把情绪推上去的。",
                R.drawable.image5, 2300, 152));

        music.add(new VideoItem("国风音乐盛典：传统乐器演绎流行金曲", "音乐现场", "22:41", "74万次播放",
                "古筝、琵琶、笛箫与现代编曲结合，重新演绎了多首流行歌曲。熟悉的旋律换一种方式演奏，别有一番味道。",
                R.drawable.image7, 4800, 236));
        music.add(new VideoItem("民谣现场：一把吉他唱完整个夏天", "livehouse", "05:37", "51万次播放",
                "小小的场地里挤满了人，唱到副歌时全场跟着一起哼。",
                R.drawable.image5, 2900, 174));
        music.add(new VideoItem("交响乐团排练直击：指挥到底在说什么", "古典频道", "09:02", "22万次播放",
                "排练时指挥不断叫停，一遍遍调整某个声部的力度和进入时机。",
                R.drawable.image6, 1500, 118));
        music.add(new VideoItem("草原上的长调：一个人的合唱", "民歌采集", "06:48", "18万次播放",
                "老人在空旷的草原上唱起长调，声音传得很远。录音师说，这种唱法已经很少有人会了。",
                R.drawable.image2, 940, 63));
        music.add(new VideoItem("深夜电台：城市入睡之后的声音", "声音志", "28:15", "63万次播放",
                "凌晨两点的便利店、收摊的夜市、最后一班公交，把这些声音拼在一起，就是一座城市的深夜。",
                R.drawable.image8, 3600, 208));

        albums.add(new VideoAlbum("album-daguo", "大国重器系列", "硬核制造现场实录", "每周五更新",
                R.drawable.image2, new ArrayList<String>() {{
                    add("纪录片：大国重器是怎样炼成的");
                    add("航拍中国：从空中俯瞰大好河山");
                    add("实拍：消防员火场逆行救人全过程");
                }}));
        albums.add(new VideoAlbum("album-city", "城市影像志", "用镜头记录城市的一天", "已完结",
                R.drawable.image7, new ArrayList<String>() {{
                    add("延时摄影记录城市从黎明到黄昏");
                    add("街头采访：你理想中的生活是什么样的");
                    add("街头钢琴：路人即兴弹奏惊艳全场");
                }}));
    }

    // ---------- 接口刷新：只写缓存，下次启动 loadCache() 生效 ----------

    /** 拉取 /videos 与 /albums，两者都成功才持久化；失败静默保持 mock。 */
    public void refreshAsync(Context ctx, Runnable onDone) {
        final String base = CampusNetworkConfig.getApiBaseUrl();
        final JSONArray[] videosBox = new JSONArray[1];
        final JSONArray[] albumsBox = new JSONArray[1];
        final boolean[] failed = {false};
        final Runnable join = () -> {
            if (videosBox[0] != null && albumsBox[0] != null && !failed[0]) {
                JSONObject cache = new JSONObject();
                try {
                    cache.put("videos", videosBox[0]);
                    cache.put("albums", albumsBox[0]);
                    prefs().edit().putString("catalog", cache.toString()).apply();
                } catch (Exception ignored) {
                }
            }
            if (onDone != null) onDone.run();
        };
        ApiClient.get(base + "/videos", new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject response) {
                JSONArray videos = response.optJSONObject("data") == null
                        ? null : response.optJSONObject("data").optJSONArray("videos");
                if (response.optInt("code") != 200 || videos == null) failed[0] = true;
                else videosBox[0] = videos;
                join.run();
            }
            @Override public void onError(String message) { failed[0] = true; join.run(); }
        });
        ApiClient.get(base + "/albums", new ApiClient.Callback() {
            @Override public void onSuccess(JSONObject response) {
                JSONArray albums = response.optJSONObject("data") == null
                        ? null : response.optJSONObject("data").optJSONArray("albums");
                if (response.optInt("code") != 200 || albums == null) failed[0] = true;
                else albumsBox[0] = albums;
                join.run();
            }
            @Override public void onError(String message) { failed[0] = true; join.run(); }
        });
    }

    /** 启动时回放上次成功拉取的接口数据；解析失败自动回落 mock。 */
    private void loadCache() {
        try {
            String raw = prefs().getString("catalog", null);
            if (raw == null || raw.isEmpty()) return;
            JSONObject cache = new JSONObject(raw);
            JSONArray videos = cache.optJSONArray("videos");
            JSONArray albums = cache.optJSONArray("albums");
            if (videos == null || videos.length() == 0 || albums == null) return;
            applyApi(videos, albums);
        } catch (Exception damaged) {
            android.util.Log.w("VideoCatalog", "视频目录缓存解析失败，使用 mock", damaged);
        }
    }

    private void applyApi(JSONArray videos, JSONArray albumRows) throws Exception {
        Map<String, VideoItem> idMap = new HashMap<>();
        Map<String, List<VideoItem>> channels = new HashMap<>();
        for (String key : new String[]{"recommend", "small", "movie", "music"}) channels.put(key, new ArrayList<VideoItem>());
        for (int i = 0; i < videos.length(); i++) {
            JSONObject row = videos.optJSONObject(i);
            if (row == null) continue;
            String title = row.optString("title");
            if (title.isEmpty()) continue;
            VideoItem item = new VideoItem(title,
                    row.optString("source", "离线影像课堂"),
                    row.optString("duration", "00:08"),
                    row.optString("play_count", "本地样片"),
                    row.optString("desc", ""),
                    coverRes(row.optString("cover", "image2")),
                    row.optInt("like_count", 0), row.optInt("comment_count", 0));
            idMap.put(row.optString("id"), item);
            List<VideoItem> channel = channels.get(row.optString("channel", "recommend"));
            if (channel != null) channel.add(item);
        }
        if (idMap.isEmpty()) throw new IllegalStateException("接口视频为空");
        List<VideoAlbum> apiAlbums = new ArrayList<>();
        for (int i = 0; i < albumRows.length(); i++) {
            JSONObject row = albumRows.optJSONObject(i);
            if (row == null) continue;
            List<String> episodes = new ArrayList<>();
            JSONArray ids = row.optJSONArray("video_ids");
            for (int j = 0; ids != null && j < ids.length(); j++) {
                VideoItem item = idMap.get(ids.optString(j));
                if (item != null) episodes.add(rawTitle(item.getTitle()));
            }
            if (episodes.isEmpty()) continue;
            apiAlbums.add(new VideoAlbum(row.optString("id"), row.optString("title"),
                    row.optString("subtitle", ""), row.optString("update_info", ""),
                    coverRes(row.optString("cover", "image2")), episodes));
        }
        recommend.clear(); recommend.addAll(channels.get("recommend"));
        small.clear(); small.addAll(channels.get("small"));
        movie.clear(); movie.addAll(channels.get("movie"));
        music.clear(); music.addAll(channels.get("music"));
        albums.clear();
        albums.addAll(apiAlbums);
        fromApi = true;
        rebuildIndex();
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int coverRes(String name) {
        if (name == null || name.isEmpty()) return R.drawable.image2;
        Integer cached = coverCache.get(name);
        if (cached != null) return cached;
        int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) id = R.drawable.image2;
        coverCache.put(name, id);
        return id;
    }
}
