-- Sliding Window Rate Limiter
local key = KEYS[1]                         -- Rate limit key (e.g. "ai_translate", "search")
local max_requests = tonumber(ARGV[1])      -- Maximum requests allowed in the time window e.g 300
local window_size_ms = tonumber(ARGV[2])    -- Time window in milliseconds e.g 60000 (1 minute)

-- Get the current time in milliseconds
local now = redis.call('TIME') -- Returns current time as an array [seconds, microseconds elapsed in the current second]
local now_ms = now[1] * 1000 + math.floor(now[2] / 1000)

-- Requests are stored with their current time as scores
-- Remove all entries with scores between 0 and (current_time - window_size)
redis.call('ZREMRANGEBYSCORE', key, 0, now_ms - window_size_ms)

-- Count current requests in the window
local current_request_count = redis.call('ZCARD', key)

if current_request_count < max_requests then
    -- Add the current request, use the current time as the score and a unique identifier as member
    -- Member needs to be unique, although the script is atomic, multiple requests can have the
    -- same timestamp (milliseconds) but differ in microseconds, without this you get a tricky to debug
    -- race condition where multiple requests at the same millisecond are counted as one
    local unique_id = now_ms .. ":" .. math.random(1000000, 9999999)
    redis.call('ZADD', key, now_ms, unique_id)
    redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000))
    return 1
else
    return 0
end


