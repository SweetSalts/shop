package cn.cie.utils;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RojerAlone on 2017/6/13.
 * 封装了redis操作，用protostuff进行序列化和反序列化
 * 如果要插入对象，需要传入插入对象的Class类型
 */
@Component
public class RedisUtil<T> implements InitializingBean {

    private JedisPool jedisPool;

    private RuntimeSchema schema;

    private static final String REDIS_URL = "redis://localhost:6379/6";

    public static final String EVERYDAY = "everyday";

    public static final String KINDS = "kinds";

    public static final String NEWESTGAME = "newestgame";

    public static final String PRE_UP_GAMES = "preupgames";

    /**
     * 存放一条数据
     * @param key
     * @param value
     * @return
     */
    public String put(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.set(key, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 存放一条定时过期的数据
     * @param key
     * @param value
     * @param timeout
     * @return
     */
    public String putEx(String key, String value, int timeout) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.setex(key, timeout, value);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 根据key获取value
     * @param key
     * @return
     */
    public String get(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.get(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 存放一个对象
     * @param key
     * @param value
     * @return
     */
    public String putObject(String key, T value) {
        this.setSchema(value.getClass());
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[] bytes = setBytes(value);
            return  jedis.set(key.getBytes(), bytes);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 存放一个定时过期的对象
     * @param key
     * @param value
     * @param timeout
     * @return
     */
    public String putObjectEx(String key, T value, int timeout) {
        this.setSchema(value.getClass());
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[] bytes = setBytes(value);
            return  jedis.setex(key.getBytes(), timeout, bytes);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 根据key获取对应的对象
     * @param key
     * @return
     */
    public T getObject(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[] bytes = jedis.get(key.getBytes());
            return getBytes(bytes);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 根据key删除数据
     * @param key
     * @return
     */
    public long delete(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.del(key);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 向列表头部添加数据
     * @param key
     * @param values
     * @return
     */
    public long lpushObject(String key, Class clazz, Object... values) {
        if (values.length == 0) {
            return 0;
        }
        this.setSchema(clazz);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[][] bytes = new byte[values.length][];
            int index = 0;
            for (Object value : values) {
                bytes[index] = setBytes(value);
                index++;
            }
            return jedis.lpush(key.getBytes(), bytes);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 从队列头部出队一个元素，如果没有，则会阻塞 timeout 秒后返回null
     * 如果 timeout 为0，那么会一直阻塞直到有元素
     * @param timeout 阻塞的时间，单位为秒
     * @param key
     * @param clazz
     * @return
     */
    public T blpopObject(int timeout, String key, Class clazz) {
        this.setSchema(clazz);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            List<byte[]> bytes = jedis.blpop(timeout, key.getBytes());
            if (bytes == null || bytes.size() == 0) {
                return null;
            }
            return getBytes(bytes.get(1));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 向列表尾部添加数据
     * @param key
     * @param values
     * @return
     */
    public long rpushObject(String key, Class clazz, Object... values) {
        if (values.length == 0) {
            return 0;
        }
        this.setSchema(clazz);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[][] bytes = new byte[values.length][];
            int index = 0;
            for (Object value : values) {
                bytes[index] = setBytes(value);
                index++;
            }
            return jedis.rpush(key.getBytes(), bytes);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 向列表尾部添加某个时间点删除的数据
     * @param key
     * @param time  unix时间戳
     * @param values
     * @return
     */
    public long rpushObjectExAtTime(String key, Class clazz, long time, Object... values) {
        if (values.length == 0) {
            return 0;
        }
        this.setSchema(clazz);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[][] bytes = new byte[values.length][];
            int index = 0;
            for (Object value : values) {
                bytes[index] = setBytes(value);
                index++;
            }
            long res = jedis.rpush(key.getBytes(), bytes);
            jedis.expireAt(key.getBytes(), time);      // 手动设置过期时间
            return res;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 在列表尾部添加一个定期删除的数据
     * @param key
     * @param clazz
     * @param timeout
     * @param values
     * @return
     */
    public long rpushObjectEx(String key, Class clazz, int timeout, Object... values) {
        if (values.length == 0) {
            return 0;
        }
        this.setSchema(clazz);
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            byte[][] bytes = new byte[values.length][];
            int index = 0;
            for (Object value : values) {
                bytes[index] = setBytes(value);
                index++;
            }
            long res = jedis.rpush(key.getBytes(), bytes);
            jedis.expire(key, timeout);
            return res;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 获取列表中所有数据,ruguo
     * @param key
     * @return
     */
    public List<T> lall(String key, Class clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // 0表示第一个元素，-1表示最后一个元素
            List<byte[]> bytes = jedis.lrange(key.getBytes(), 0, -1);
            List<T> res = new ArrayList<T>();
            if (bytes == null || bytes.size() == 0) {
                return res;
            }
            this.setSchema(clazz);
            for (byte[] b : bytes) {
                res.add(getBytes(b));
            }
            return res;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 如果要从redis中查询对象，那么必须调用这个方法设置对象的class
     * @param clazz
     */
    private void setSchema(Class clazz) {
        this.schema = RuntimeSchema.createFrom(clazz);
    }

    /**
     * 用protostuff将对象序列化为bytes
     * @param value
     * @return
     */
    private byte[] setBytes(Object value) {
        return ProtostuffIOUtil.toByteArray(value, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
    }

    /**
     * 从bytes中反序列化为对象
     * @param bytes
     * @return
     */
    private T getBytes(byte[] bytes) {
        if (bytes != null) {
            Object object = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(bytes, object, schema);
            return (T) object;
        } else {
            return null;
        }
    }

    /**
     * spring注入之后会自动调用这个方法
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        jedisPool = new JedisPool(REDIS_URL);
    }
}
