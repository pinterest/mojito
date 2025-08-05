-- Sliding Window Rate Limiter
local function sliding_window_rate_limit(KEYS, ARGV)
    local key = KEYS[1]                         -- Rate limit key (e.g. "global", "ai_translate", "search")
    local max_requests = tonumber(ARGV[1])      -- Maximum requests allowed in the time window
    local window_size_ms = tonumber(ARGV[2])    -- Time window in milliseconds

    -- Get the current time in microseconds
    local now = redis.call('TIME') -- Returns current time as an array [seconds, microseconds]
    local now_micros = (now[1] * 1000000) + now[2]

    -- Calculate the cutoff time in microseconds
    local window_size_micros = tonumber(window_size_ms) * 1000
    local cutoff_time = now_micros - window_size_micros

    -- Requests are stored with their current time as scores
    -- Remove all entries with scores between 0 and (current_time - window_size)
    redis.call('ZREMRANGEBYSCORE', key, 0, cutoff_time)

    -- Count current requests in the window
    local current_request_count = redis.call('ZCARD', key)

    -- Check if we're at the limit, if so, deny the request
    if current_request_count >= max_requests then
        return 0
    end

    -- Add the current request, use the current time as the score and unique identifier
    redis.call('ZADD', key, now_micros, now_micros)

    -- Set expiration for automatic cleanup, used to remove expired entries if not already cleaned up above
    redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000))
    return 1
end

return sliding_window_rate_limit(KEYS, ARGV)
