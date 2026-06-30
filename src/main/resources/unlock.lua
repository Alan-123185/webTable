local UUID=ARGV[1]

if redis.call('get', KEYS[1]) == UUID then
    return redis.call('del', KEYS[1])
else
    return 0
end