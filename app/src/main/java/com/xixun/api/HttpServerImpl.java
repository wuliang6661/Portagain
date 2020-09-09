package com.xixun.api;

import android.util.Base64;

import com.google.gson.Gson;
import com.xixun.api.rx.RxResultHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public class HttpServerImpl {

    private static HttpService service;

    /**
     * 获取代理对象
     *
     * @return
     */
    public static HttpService getService() {
        if (service == null)
            service = ApiManager.getInstance().configRetrofit(HttpService.class, HttpService.URL);
        return service;
    }


    /**
     * 上报数据
     */
    public static Observable<String> createOrUpdateOne(String brightness, String wenDu, String shiDu) {
        Map<String, Object> params = new HashMap<>();
        params.put("big_screenId", "12345678");
        params.put("liangDuEnv", brightness);
        params.put("liangDuScreen", brightness);
        params.put("wenDu", wenDu);
        params.put("shiDu", shiDu);
        byte[] bytes = {12, 11};
        String base = Base64.encodeToString(bytes, Base64.DEFAULT);
        params.put("imgBase64", base);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        params.put("time", time);
        Map<String, Object> maps = new HashMap<>();
        maps.put("tag", params);
        return getService().createOrUpdateOne("lingke", "big_screen_params", "chenLu", new Gson().toJson(maps))
                .compose(RxResultHelper.httpRusult());
    }


}
