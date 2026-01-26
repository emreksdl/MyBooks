# Rate Limiter Testing Guide

## Task 4: Rate Limiting Implementation

### Rate Limits Configuration

| Endpoint | Max Requests | Time Window | Purpose |
|----------|--------------|-------------|---------|
| `/api/auth/login` | 5 | 60 seconds | Prevent brute-force login attacks |
| `/api/auth/register` | 3 | 60 seconds | Prevent spam registrations |
| `/api/*` (other) | 100 | 60 seconds | Prevent API abuse |

---

## Testing with Postman

### Test 1: Login Rate Limiting

**Step 1:** Make 5 failed login attempts quickly:

```
POST http://localhost:8080/api/auth/login?authType=jwt
Content-Type: application/json

{
  "email": "test@test.com",
  "password": "wrong_password"
}
```

**Expected Results:**
- Attempt 1-5: `401 Unauthorized` (bad credentials)
- Attempt 6+: `429 Too Many Requests` with rate limit message

**Response Headers:**
```
X-RateLimit-Remaining: 4  (after 1st attempt)
X-RateLimit-Remaining: 3  (after 2nd attempt)
...
X-RateLimit-Remaining: 0  (after 5th attempt)
Retry-After: 60           (after 6th attempt)
```

**Response Body (after 6th attempt):**
```json
{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "error": "RATE_LIMIT_EXCEEDED",
  "retryAfter": 60,
  "remainingAttempts": 0,
  "timestamp": 1706281234567
}
```

---

### Test 2: Check Rate Limit Status

```
GET http://localhost:8080/api/admin/rate-limit/status
```

**Response:**
```json
{
  "ipAddress": "127.0.0.1",
  "loginRemaining": 2,
  "loginBlocked": false
}
```

**When blocked:**
```json
{
  "ipAddress": "127.0.0.1",
  "loginRemaining": 0,
  "loginBlocked": true,
  "retryAfterSeconds": 45
}
```

---

### Test 3: Reset Rate Limit (Admin Only)

```
POST http://localhost:8080/api/admin/rate-limit/reset?ipAddress=127.0.0.1&limitType=login
Authorization: Bearer <admin_jwt_token>
```

**Response:**
```json
{
  "message": "Rate limit reset successfully",
  "ipAddress": "127.0.0.1",
  "limitType": "login"
}
```

---

## Testing with cURL

### Rapid Fire Test (Bash script):

```bash
#!/bin/bash

# Test login rate limiting
echo "Testing login rate limiting..."

for i in {1..7}; do
  echo "Attempt $i:"
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}' \
    -w "\nHTTP Status: %{http_code}\n" \
    -s | head -n 5
  echo "---"
  sleep 1
done
```

---

## Log Output

After rate limit is exceeded, check logs:

```bash
cat logs/security.log | grep RATE_LIMIT
```

**Expected output:**
```
2026-01-26 16:00:00.123 WARN  c.e.m.service.SecurityLogger - RATE_LIMIT_EXCEEDED: ip=127.0.0.1, endpoint=/api/auth/login
```

---

## Algorithm: Sliding Window

The rate limiter uses a **sliding window** algorithm:

1. Store timestamps of each request
2. Remove timestamps older than the time window
3. Count remaining timestamps
4. If count ≥ limit, reject request
5. Otherwise, add new timestamp and allow request

### Example:
```
Time window: 60 seconds
Max requests: 5
Current time: 16:00:00

Timestamps:
- 15:59:05 ← Removed (outside window)
- 15:59:30 ← Kept
- 15:59:45 ← Kept
- 15:59:55 ← Kept
- 16:00:00 ← Current (4 in window, allow)

Next request at 16:00:05:
- 15:59:30 ← Kept
- 15:59:45 ← Kept
- 15:59:55 ← Kept
- 16:00:00 ← Kept
- 16:00:05 ← Current (5 in window, allow)

Next request at 16:00:10:
- 15:59:30 ← Kept
- 15:59:45 ← Kept
- 15:59:55 ← Kept
- 16:00:00 ← Kept
- 16:00:05 ← Kept
- 16:00:10 ← BLOCKED (6 in window, deny!)
```

---

## Cleanup Mechanism

- Automatic cleanup runs every 5 minutes
- Removes entries older than 10 minutes
- Prevents memory leaks
- Thread-safe (ConcurrentHashMap)

---

## Production Considerations

### Current Implementation (In-Memory):
✅ Simple
✅ Fast
✅ No external dependencies
❌ Not shared across multiple servers
❌ Lost on restart

### For Production (Redis):
Consider using Redis for:
- Distributed rate limiting (multiple servers)
- Persistence across restarts
- Centralized management

**Redis Example:**
```java
// Replace in-memory map with Redis
@Autowired
private RedisTemplate<String, String> redisTemplate;

// Increment counter with expiration
redisTemplate.opsForValue().increment(key);
redisTemplate.expire(key, Duration.ofSeconds(60));
```

---

## Security Best Practices

1. **Different limits for different endpoints**
    - Login: Strict (5/min)
    - Registration: Moderate (3/min)
    - API: Generous (100/min)

2. **Track by IP address**
    - Handles proxy headers (X-Forwarded-For)
    - Protects against distributed attacks

3. **Informative responses**
    - Return `429 Too Many Requests`
    - Include `Retry-After` header
    - Tell user how long to wait

4. **Logging**
    - Log all rate limit violations
    - Monitor for suspicious patterns
    - Alert on repeated violations

---

## Common Attack Scenarios

### Scenario 1: Brute-Force Login
**Attack:** Try 1000 passwords for one account
**Defense:** After 5 attempts, block for 60 seconds
**Result:** Attacker limited to 5 passwords/minute = 300/hour

### Scenario 2: Credential Stuffing
**Attack:** Try leaked credentials from other sites
**Defense:** Same as brute-force
**Result:** Severely limited attack speed

### Scenario 3: API Scraping
**Attack:** Automated scraping of data
**Defense:** 100 requests/minute limit
**Result:** Slow down attacker, normal users unaffected

---

## Monitoring Dashboard (Future Enhancement)

Track:
- Total blocked requests per hour
- Top blocked IP addresses
- Most targeted endpoints
- Average requests per user

Alert when:
- Single IP blocked > 10 times/hour
- Total blocked requests > 100/hour
- Unusual spike in login attempts