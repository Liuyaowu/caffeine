package com.mobei.caffieine;

import com.github.benmanes.caffeine.cache.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caffeine
 *
 * @author liuyaowu
 * @date 2020-04-09 14:07
 * @remark
 */
public class CaffieineTest {

    public static void main(String[] args) throws InterruptedException {
//        testSimple();

//        testDefFunc();

//        testExpireBySize();

//        testExpireByTime();

//        testExpireByAccess();

//        testReference();

//        testDeleteListener();

        testCount();
    }

    /**
     * 手动添加缓存,查询
     */
    public static void testSimple() {
        //最简单的创建
        Cache<String, String> cacheSimple = Caffeine.newBuilder().build();
        cacheSimple.put("1", "1");
        System.out.println(cacheSimple.getIfPresent("1"));
    }

    /**
     * 缓存中查不到执行指定方法
     */
    public static void testDefFunc() {
        //最简单的创建
        Cache<String, Object> cacheSimple = Caffeine.newBuilder().build();

        // 1.如果缓存中能查到，则直接返回
        // 2.如果查不到，则从我们自定义的getValue方法获取数据，并加入到缓存中
        cacheSimple.get("1", new Function<String, Object>() {
            public String apply(String s) {
                return getValue(s);
            }
        });

        System.out.println(cacheSimple.getIfPresent("1"));
    }

    /**
     * 缓存未命中:从数据库查询
     * @param s
     * @return
     */
    private static String getValue(String s) {
        return s + " : default value";
    }

    /**
     * testDefFunc()中也可以在新建对象的时候添加
     */
    public static void testAutoAdd() {
        Caffeine.newBuilder()
            .build(new CacheLoader<String, String>() {
                @Nullable
                @Override
                public String load(@NonNull String s) throws Exception {
                    return getValue(s);
                }
            });
    }

    /**
     * 过期策略--大小:淘汰并不是按照先后顺序,内部有自己的算法
     */
    public static void testExpireBySize() {
        //实际开发中需要自己配置
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .initialCapacity(2)
                .maximumSize(2)//最大两个:这里会淘汰3个
                .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");
        cache.put("key5", "value5");
        cache.cleanUp();
        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));
        System.out.println(cache.getIfPresent("key4"));
        System.out.println(cache.getIfPresent("key5"));
    }

    /**
     * 过期策略--时间:
     *      expireAfterAccess(long, TimeUnit):
     *           在最后一次访问或者写入后开始计时,在指定的时间后过期.
     *           假如一直有请求访问该key,那么这个缓存将一直不会过期.
     *      expireAfterWrite(long, TimeUnit):
     *          在最后一次写入缓存后开始计时，在指定的时间后过期。
     *      expireAfter(Expiry):
     *          自定义策略,过期时间由Expiry实现独自计算。
     */
    public static void testExpireByTime() throws InterruptedException {
        //实际开发中需要自己配置
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");
        cache.put("key5", "value5");

        cache.cleanUp();

        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));
        System.out.println(cache.getIfPresent("key4"));
        System.out.println(cache.getIfPresent("key5"));

        Thread.sleep(10000);

        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
        System.out.println(cache.getIfPresent("key3"));
        System.out.println(cache.getIfPresent("key4"));
        System.out.println(cache.getIfPresent("key5"));
    }

    /**
     * 基于访问操作的失效
     * @throws InterruptedException
     */
    public static void testExpireByAccess() throws InterruptedException {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .build();

        cache.put("key1", "value1");

        cache.cleanUp();

        Thread.sleep(1*1000);
        System.out.println(cache.getIfPresent("key1"));

        Thread.sleep(1*1000);
        System.out.println(cache.getIfPresent("key1"));

        Thread.sleep(1*1000);
        System.out.println(cache.getIfPresent("key1"));

        Thread.sleep(3*1000);
        System.out.println(cache.getIfPresent("key1"));
    }

    /**
     * 引用:
     *      Caffeine.weakKeys():
     *          使用弱引用存储key.如果没有其他地方对该key有强引用,那么该缓存就会被垃圾回收器回收。
     *      Caffeine.weakValues():
     *          使用弱引用存储value.如果没有其他地方对该value有强引用,那么该缓存就会被垃圾回收器回收。
     *      Caffeine.softValues():
     *          使用软引用存储value。
     */
    public static void testReference() {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .weakValues()
                .build();
        Object value1 = new Object();
        Object value2 = new Object();
        cache.put("key1", value1);
        cache.put("key2", value2);

        value2 = new Object(); // 原对象不再有强引用
        System.gc();
        System.out.println(cache.getIfPresent("key1"));
        System.out.println(cache.getIfPresent("key2"));
    }

    /**
     * 显示删除缓存
     */
    public static void testDeleteCache() {
        Cache<String, Object> cache = Caffeine.newBuilder().build();
        Object value1 = new Object();
        Object value2 = new Object();
        cache.put("key1", value1);
        cache.put("key2", value2);

        // 1、指定key删除
        cache.invalidate("key1");

        // 2、批量指定key删除
        List<String> list = new ArrayList<>();
        list.add("key1");
        list.add("key2");
        cache.invalidateAll(list);//批量清除list中全部key对应的记录

        // 3、删除全部
        cache.invalidateAll();
    }

    /**
     * 添加移除监听器:
     *      为缓存对象添加一个移除监听器,这样当有记录被删除时可以感知到这个事件
     */
    public static void testDeleteListener() throws InterruptedException {
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterAccess(3, TimeUnit.SECONDS)
                .removalListener(new RemovalListener<Object, Object>() {
                    @Override
                    public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
                        System.out.println("key:" + key + ",value:" + value + ",删除原因:" + cause);
                    }
                })
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.invalidate("key1");
        Thread.sleep(2 * 1000);
        cache.cleanUp();
    }

    /**
     * 统计
     */
    public static void testCount() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(3)
                .recordStats()
                .build();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");

        cache.getIfPresent("key1");
        cache.getIfPresent("key2");
        cache.getIfPresent("key3");
        cache.getIfPresent("key4");
        cache.getIfPresent("key5");
        cache.getIfPresent("key6");
        System.out.println(cache.stats());
    }

    public static void testAsync() {
        //异步
        AsyncCache<String, Object> asyncCache = Caffeine.newBuilder().buildAsync();
    }

}
