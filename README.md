````
HttpClient.getInstance()
    .url(url)
    .tag(tag) //Activity, Fragment, Presenter 中最好都加上 tag(this)
    .addParam(key, value)
    .addParam(key, value)
    .addParam(key, value)
    .addUrlParam(key, value) //用于 post 时还想把参数拼在 url 上
    .addHeader(key, value)
    .addHeader(key, value)

    .jsonBody(String json) // MediaType : application/json

    .cacheControl(cacheControl) //缓存
    .addContent(String name, String filename, Bitmap/byte[]/File content) //post 上传文件
    .callbackOn(Schedulers.mainThread()) // optional
    .get(new HttpCallback<T1, T2>() { //T1, T2 支持 Bean, String, JSONObject
        @Override
        protected void onSuccess(int code, T1 data) {

        }

        @Override
        protected void onFailure(int code, T2 data) {

        }

        @Override
        protected void onError(Exception e) {

        }

        @Override
        protected void onFinal() {
    
        }
    }); //get() or post() return httpRequest

HttpClient.getInstance().init(httpClientConfig);  
````

务必调用 cancel()，建议在 BaseActivity, BaseFragment, BasePresenter onDestroy 中调用 cancel(this)

````
HttpClient.getInstance().cancel(tag); //tag or httpRequest, httpRequest's default tag is self
````


post 请求，如果有 addContent, MediaType 会使用 multipart/form-data；如果有 jsonBody, MediaType 会使用 application/json；否则 MediaType 会使用 application/x-www-form-urlencoded


````
HttpClient.getInstance().init(context);
HttpClient.getInstance().init(context, okHttpClient); //optional
````

````
//optional util
Schedulers.mainThread().schedule(new SchedulerTask(request, new Runnable() {
    @Override
    public void run() {
        HttpCallback.super.dispatchOnSuccess(code, data);
    }
}));
````

如果有用 Schedulers.xxx.schedule(...) 也应该配套调用 Schedulers.cancel(tag)


可以直接用 HttpClient，不过还是建议再进行一层简单的继承封装，可以参考 Demo（XHttpClient, XHttpCallback）

 

 

CacheControl

CacheControl缓存以 url 为 key（key 不包含任何参数，就是 .url(url) 传进来的 url，同一个 url 不同参数，会互相覆盖）

````
new CacheControl.Builder()
    .readCacheWhen(...) //默认值 ReadCacheWhen.AT_ONCE 表示立即读取缓存；ReadCacheWhen.ON_ERROR 表示请求失败才读取缓存
    .maxAge(int seconds) //缓存有效时间，默认值 Integer.MAX_VALUE
    .noCache() //表示不读取缓存
    .noStore() //表示不保存缓存

    .noDiskStore() //表示不允许磁盘缓存（就是只有内存缓存）

    .requestComparator(RequestComparator comparator) //判断是否命中缓存，可以自定义，默认是判断 params headers 的所有 key value

    .responseComparator(ResponseComparator comparator) //判断是否跟最后的缓存一样，一样的话则不回调 onSuccess，可以自定义，默认为空，不进行 headers 比对

    .build()
````

命中缓存会触发 onCache 回调，不影响继续网络请求，请求成功后，如果与最后一次 cache 结果一样，则不回调 onSuccess，如果不一样会继续回调 onSuccess