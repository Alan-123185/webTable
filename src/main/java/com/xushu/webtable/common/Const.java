package com.xushu.webtable.common;

import org.springframework.stereotype.Component;

/**
 * 集中管理所有魔法数字和散落常量
 */
public final class Const {
    private Const() {}

    // ==================== 登录结果码 ====================
    public static final int LOGIN_SUCCESS = 0;           // 登录成功
    public static final int LOGIN_NOT_REGISTERED = 1;    // 用户未注册
    public static final int LOGIN_PASSWORD_WRONG = 2;    // 密码错误
    public static final int LOGIN_BANNED = -1;      // 用户被封禁

    // ==================== 转存结果码 ====================
    public static final int STORE_SUCCESS = 0;            // 转存成功
    public static final int STORE_LINK_EXPIRED = 1;       // 链接已过期
    public static final int STORE_FILE_EXISTS = -1;       // 文件已存在
    public static final int STORE_LINK_ERROR = -2;        // 链接错误/文件已被删除
    public static final int STORE_SPACE_INSUFFICIENT = -3;// 存储空间不足

    // ==================== 注册/验证码结果码 ====================
    public static final int REGISTER_SUCCESS = 0;         // 注册成功
    public static final int REGISTER_CODE_WRONG = 1;      // 验证码错误
    public static final int REGISTER_CODE_EXPIRED = -1;   // 验证码过期

    // ==================== HTTP 状态码 ====================
    public static final int HTTP_SUCCESS = 200;           // 成功
    public static final int HTTP_BAD_REQUEST = 400;       // 参数错误
    public static final int HTTP_UNAUTHORIZED = 401;      // 未授权
    public static final int HTTP_FORBIDDEN = 403;         // 权限不足
    public static final int HTTP_NOT_FOUND = 404;         // 资源不存在
    public static final int HTTP_INTERNAL_ERROR = 500;    // 服务端错误
    public static final int HTTP_ACCEPTED = 202;          //已接受待处理

    // ==================== 文件查询默认值 ====================
    public static final int FILE_QUERY_DEFAULT_PAGE = 1;
    public static final int FILE_QUERY_DEFAULT_SIZE = 10;
    public static final int FILE_QUERY_DEFAULT_ORDER = 0;

    // ==================== 文件类型标志位 ====================
    public static final int FILE_TYPE_FILE = 0;           // 普通文件
    public static final int FILE_TYPE_FOLDER = 1;         // 文件夹

    // ==================== RefCount 初始值 ====================
    public static final int REFCOUNT_FIRST_UPLOAD = 1;    // 首次上传
    public static final int REFCOUNT_SECOND_UPLOAD = 0;   // 秒传/转存

    // ==================== 分享链接 ====================
    public static final int SHARE_LINK_LENGTH = 8;        // 分享码长度
    public static final int SHARE_EXPIRY_DAYS = 7;        // 分享有效期（天）

    // ==================== 分页默认值 ====================
    public static final int PAGE_DEFAULT = 1;             // 默认页码
    public static final int SIZE_DEFAULT = 10;            // 默认每页条数
    public static final int PARENT_ID_ROOT = 0;           // 根目录 parentId

    // ==================== Redis Key 前缀 ====================
    public static final String REDIS_UPLOAD_KEY_PREFIX = "webtable:user:upload:";
    public static final String REDIS_USER_CODE_KEY_PREFIX = "webtable:user:code:";
    public static final String REDIS_LOG_INFO_KEY_PREFIX = "webtable:log:queue";
    public static final String REDIS_DEAD_LOG_ERROR_KEY_PREFIX = "webtable:errorLog:queue";
    public static final String REDIS_DEAD_LOG_RETRY_COUNT_KEY = "webtable:errorLog:retryCount";
    public static final String REDIS_FILE_MD5_KEY="webtable:file:md5:";
    public static final String REDIS_FILE_TASK_INFO_KEY="webtable:task:info:";
    public static final String REDIS_DOWNLOAD_LOCK_KEY="webtable:download:lock:";
    public static final String REDIS_LOGIN_INFO_KEY_PREFIX="webtable:login:info:";
    public static final String REDIS_USER_VOLUME="webtable:user:volume";
    public static final String REDIS_USER_BANNED = "webtable:user:banned";

    // ==================== Redis TTL ====================
    public static final int REDIS_UPLOAD_TTL_HOURS = 24;          // 上传任务缓存小时数
    public static final int REDIS_CODE_TTL_MINUTES = 5;
    public static final int REDIS_UPLOAD_TTL_TIME = 60*40;// 验证码缓存分钟数
    public static final int REDIS_LOCK_SECONDS = 60;
    public static final int REDIS_TOKEN_LOSE_HOURS = 1;

    // ==================== HTTP 请求头 ====================
    public static final String HEADER_TOKEN = "token";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";


    // ==================== Content-Disposition 值 ====================
    public static final String DISPOSITION_ATTACHMENT = "attachment; filename=";
    public static final String DISPOSITION_INLINE = "inline; filename=";

    // ==================== Content-Type 值 ====================
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public static final String CONTENT_TYPE_JPEG = "image/jpeg";
    public static final String CONTENT_TYPE_PNG = "image/png";
    public static final String CONTENT_TYPE_GIF = "image/gif";
    public static final String CONTENT_TYPE_BMP = "image/bmp";
    public static final String CONTENT_TYPE_WEBP = "image/webp";
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_TXT = "text/plain";
    public static final String CONTENT_TYPE_MD = "text/markdown";
    public static final String CONTENT_TYPE_BATCH_DOWNLOAD = "application/zip";

    // ==================== JWT Claims ====================
    public static final String JWT_CLAIM_USERNAME = "username";
    public static final String JWT_CLAIM_ID = "id";
    public static final String JWT_CLAIM_ROLE = "role";

    // ==================== MDC Key ====================
    public static final String MDC_TRACE_ID = "traceId";

    // ==================== 文件路径常量 ====================
    public static final String PATH_TEMP = "temp";
    public static final String PATH_UPLOAD = "upload";

    // ==================== 编码 ====================
    public static final String CHARSET_UTF8 = "UTF-8";

    // ==================== 用户身份标示RBAC ====================
    public static final int ROLE_ADMIN = -1;
    public static final int ROLE_USER = 0;
    public static final int ROLE_GUEST = 1;

    // ===================== 日志操作相关 ====================
    public static final int LOG_MAX_SIZE = 100;  //单次最大次数
    public static final int LOG_CLEAN_PERIOD_MS= 1000 * 60 * 60 * 24;

    // ===================== 回收站时间常数 ====================
    public static final int RECYCLE_BIN_EXPIRY_DAYS = 30;

    // ===================== Lua脚本返回常数 ====================
    public static final String LUA_CHECK_FAST="fast";
    public static final String LUA_CHECK_NEW="new";
    public static final String LUA_CHECK_WAIT="wait";
    public static final String LUA_CHECK_ERROR="error";
    public static final String LUA_CHECK_RESUME="resume";
    public static final String LUA_CHECK_DONE="done";
    public static final String LUA_CHECK_UPLOADING="uploading";


    // ===================== redis分布式锁 ====================
    public static final int LOCK_LEASE_TIME_SECONDS = 30;
    public static final int LOCK_RETRY_COUNT = 3;
    public static final int LOCK_RETRY_INTERVAL_MS = 500;

    // ===================== 账号封禁相关=======================
    public static final int BAN_LEVEL_1 = 1;
    public static final int BAN_LEVEL_2 = 2;
    public static final int BAN_LEVEL_3 = 3;
    public static final int BAN_LEVEL_1_DAYS = 7;
    public static final int BAN_LEVEL_2_DAYS = 30;


    //=======================用户状态码=============================
    public static final int USER_STATUS_NORMAL=0;
    public static final int USER_STATUS_BANNED=1;

    //==========================文件状态码=============================
    public static final int FILE_STATUS_NORMAL = 0;
    public static final int FILE_STATUS_DELETED =-1;
    public static final int FILE_STATUS_DELETING = 1;

}
