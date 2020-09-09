package com.xixun.api.rx;

import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 作者 by wuliang 时间 16/11/24.
 */

public class RxResultHelper {
    private static final String TAG = "RxResultHelper";

    public static <T> Observable.Transformer<BaseResult<T>, T> httpRusult() {
        return apiResponseObservable -> apiResponseObservable.flatMap(
                (Func1<BaseResult<T>, Observable<T>>) mDYResponse -> {
                    Log.d(TAG, "call() called with: mDYResponse = [" + mDYResponse + "]");
                    if (mDYResponse.surcess()) {
                        return createData(mDYResponse.getData());
                    } else {
                        return Observable.error(new RuntimeException(mDYResponse.getMsg()));
                    }
                }
        ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }


    private static <T> Observable<T> createData(final T t) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    subscriber.onNext(t);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
