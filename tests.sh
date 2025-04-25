# 1. Subscribe a Customer
curl -X POST -H "Content-Type: application/json" \
  -d '{"customerNumber": "234774784"}' \
  http://localhost:8080/api/v1/mobile/subscribe

# 2. Request a Loan
curl -X POST -H "Content-Type: application/json" \
  -d '{"customerNumber": "234774784", "amount": 500000}' \
  http://localhost:8080/api/v1/mobile/loans/request

# Wait a few seconds for scoring simulation...

# 3. Check Loan Status
curl http://localhost:8080/api/v1/mobile/loans/status/234774784

# 4. Test Transaction Data Endpoint (via curl - Simulate Scoring Engine Call)
# Replace 'scoring_user:scoring_pwd_123!' with your configured credentials if changed
curl -u 'scoring_user:scoring_pwd_123!' http://localhost:8080/api/v1/lms/transactions/318411216

# 5. Test Concurrent Request (should fail with 409 Conflict if previous request is pending/active)
curl -X POST -H "Content-Type: application/json" \
  -d '{"customerNumber": "234774784", "amount": 1000}' \
  http://localhost:8080/api/v1/mobile/loans/request