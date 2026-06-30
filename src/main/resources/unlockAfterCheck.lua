local status=redis.call('GET', KEYS[1])
if status=='uploading' then
    redis.call('SET', KEYS[1], 'done' ,'EX', 60*60*24*30)
    redis.call('DEL', KEYS[2])
elseif status=='done' then
    redis.call('SET', KEYS[1], 'EX', 60*60*24*30)
end