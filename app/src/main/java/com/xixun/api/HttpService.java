package com.xixun.api;


import com.xixun.api.rx.BaseResult;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

/**
 * Created by wuliang on 2017/3/9.
 * <p>
 * 此处存放后台服务器的所有接口数据
 */

public interface HttpService {

    String URL = "https://editor.olmap.cn/";


    /**
     * 上报数据
     */
    @FormUrlEncoded
    @POST("dataOperate/createOrUpdateOne")
    Observable<BaseResult<String>> createOrUpdateOne(@Field("eId") String eId, @Field("dataSetId") String dataSetId,
                                                     @Field("cUserId") String cUserId, @Field("data") String data);

}
