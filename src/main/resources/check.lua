-- check_and_occupy.lua
-- 功能：原子检查文件MD5状态并决定秒传/上传/续传/等待
-- 
-- KEYS[1] : file:md5:{md5}        -- 文件状态键
-- KEYS[2] : task:info:{taskId}    -- 上传任务信息键
-- ARGV[1] : md5                   -- 文件MD5（直接传入，避免从key中解析）
-- ARGV[2] : reqTaskId             -- 前端传来的taskId，没有则为空串 ""
-- ARGV[3] : newTaskId             -- 后端生成的新taskId
-- ARGV[4] : userId                -- 当前用户ID
-- ARGV[5] : totalChunks           -- 总分片数
-- ARGV[6] : ttl                   -- 过期时间（秒），如300
--
-- 返回值：{状态码, taskId}
--   {"fast",   ""}         -> 文件已存在，秒传
--   {"new",    newTaskId}  -> 抢占成功，新任务
--   {"resume", reqTaskId}  -> 续传（自己的任务）
--   {"wait",   ""}         -> 等待（别人在上传）
--   {"error",  ""}         -> 异常状态（可选）

-- 从参数列表中获取文件MD5值
local md5        = ARGV[1]
-- 从参数列表中获取前端请求的任务ID（若为空则表示新任务）
local reqTaskId  = ARGV[2]
-- 从参数列表中获取后端生成的新任务ID
local newTaskId  = ARGV[3]
-- 从参数列表中获取当前用户ID
local userId     = ARGV[4]
-- 从参数列表中获取文件总分片数
local totalChunks= ARGV[5]
-- 从参数列表中获取键的过期时间（秒）
local ttl        = ARGV[6]

-- 1. 查询当前文件状态键对应的值，判断文件上传状态
local status = redis.call('GET', KEYS[1])

-- 2. 如果状态为 'done'，表示文件已存在且上传完成，直接返回秒传标识
if status == 'done' then
    return {"fast", ""}
end

-- 3. 如果状态为 'uploading'，表示文件正在上传中，需进一步判断是否可续传
if status == 'uploading' then
    -- 如果前端传来了 taskId，则检查该任务是否属于当前用户
    if reqTaskId ~= '' then
        -- 获取任务详细信息 Hash 表的所有字段和值
        local taskInfo = redis.call('HGETALL', KEYS[2])
        -- taskInfo 格式为 {field1, val1, field2, val2...}，若长度大于0说明任务存在
        if #taskInfo > 0 then
            -- 初始化变量用于存储从 Hash 中解析出的 md5 和 userId
            local taskMd5 = ''
            local taskUser = ''
            -- 遍历 Hash 结果，步长为2，依次取出字段名和字段值
            for i = 1, #taskInfo, 2 do
                -- 如果字段名为 'md5'，则记录其值
                if taskInfo[i] == 'md5' then
                    taskMd5 = taskInfo[i+1]
                -- 如果字段名为 'userId'，则记录其值
                elseif taskInfo[i] == 'userId' then
                    taskUser = taskInfo[i+1]
                end
            end
            -- 校验解析出的 MD5 和用户ID是否与当前请求一致，一致则允许续传
            if taskMd5 == md5 and taskUser == userId then
                return {"resume", reqTaskId}
            end
        end
    end
    -- 若未携带 taskId 或任务不属于当前用户，则返回等待标识
    return {"wait", ""}
end

-- 4. 若状态不存在（即 key 不存在），则原子性地抢占上传权
-- 设置文件状态键为 'uploading'，并指定过期时间
redis.call('SET', KEYS[1], 'uploading' ,'EX', ttl)
-- 设置任务信息 Hash 表，存储 md5、userId、总分片数和创建时间等信息
redis.call('HSET', KEYS[2],
        'md5',         md5,          -- 文件 MD5
        'userId',      userId,       -- 用户 ID
        'totalChunks', totalChunks  -- 总分片数
)
-- 为任务信息键设置过期时间，防止脏数据残留
redis.call('EXPIRE', KEYS[2], ttl)
-- 返回新任务标识及新生成的 taskId
return {"new", newTaskId}