local UUID=ARGV[1]

local moretime=ARGV[2]

if redis.call('get', KEYS[1]) == UUID then
     return redis.call('expire', KEYS[1], moretime)
 else
     return 0 end