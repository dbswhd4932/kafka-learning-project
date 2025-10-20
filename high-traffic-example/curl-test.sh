#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}API 테스트 시작${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 헬스 체크
echo -e "${YELLOW}1. 헬스 체크${NC}"
curl -s "${BASE_URL}/actuator/health" | jq .
echo ""
echo ""

# 게시글 생성
echo -e "${YELLOW}2. 게시글 생성${NC}"
POST_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/posts" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "curl 테스트 게시글",
    "content": "curl로 생성한 게시글입니다.",
    "author": "테스터"
  }')

POST_ID=$(echo $POST_RESPONSE | jq -r '.id')
echo $POST_RESPONSE | jq .
echo -e "${GREEN}생성된 게시글 ID: $POST_ID${NC}"
echo ""
echo ""

# 게시글 조회
echo -e "${YELLOW}3. 게시글 조회${NC}"
curl -s "${BASE_URL}/api/posts/${POST_ID}" | jq .
echo ""
echo ""

# 게시글 목록 조회
echo -e "${YELLOW}4. 게시글 목록 조회 (페이지 0, 크기 5)${NC}"
curl -s "${BASE_URL}/api/posts?page=0&size=5" | jq .
echo ""
echo ""

# 루트 댓글 생성 (2 Depth)
echo -e "${YELLOW}5. 루트 댓글 생성 (2 Depth)${NC}"
COMMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/comments/two-depth" \
  -H "Content-Type: application/json" \
  -d "{
    \"postId\": ${POST_ID},
    \"parentId\": null,
    \"content\": \"curl로 작성한 댓글입니다\",
    \"author\": \"댓글러\"
  }")

COMMENT_ID=$(echo $COMMENT_RESPONSE | jq -r '.id')
echo $COMMENT_RESPONSE | jq .
echo -e "${GREEN}생성된 댓글 ID: $COMMENT_ID${NC}"
echo ""
echo ""

# 대댓글 생성 (2 Depth)
echo -e "${YELLOW}6. 대댓글 생성 (2 Depth)${NC}"
REPLY_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/comments/two-depth" \
  -H "Content-Type: application/json" \
  -d "{
    \"postId\": ${POST_ID},
    \"parentId\": ${COMMENT_ID},
    \"content\": \"curl로 작성한 대댓글입니다\",
    \"author\": \"대댓글러\"
  }")

REPLY_ID=$(echo $REPLY_RESPONSE | jq -r '.id')
echo $REPLY_RESPONSE | jq .
echo -e "${GREEN}생성된 대댓글 ID: $REPLY_ID${NC}"
echo ""
echo ""

# 댓글 목록 조회 (2 Depth)
echo -e "${YELLOW}7. 댓글 목록 조회 (2 Depth)${NC}"
curl -s "${BASE_URL}/api/comments/two-depth?postId=${POST_ID}" | jq .
echo ""
echo ""

# 무한 Depth 댓글 생성
echo -e "${YELLOW}8. 무한 Depth 댓글 생성${NC}"
INFINITE_ROOT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/comments/infinite-depth" \
  -H "Content-Type: application/json" \
  -d "{
    \"postId\": ${POST_ID},
    \"parentId\": null,
    \"content\": \"무한 depth 루트 댓글\",
    \"author\": \"무한댓글러\"
  }")

INFINITE_ROOT_ID=$(echo $INFINITE_ROOT_RESPONSE | jq -r '.id')
echo $INFINITE_ROOT_RESPONSE | jq .
echo -e "${GREEN}생성된 무한 depth 댓글 ID: $INFINITE_ROOT_ID${NC}"
echo ""
echo ""

# Depth 1 생성
echo -e "${YELLOW}9. Depth 1 댓글 생성${NC}"
DEPTH1_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/comments/infinite-depth" \
  -H "Content-Type: application/json" \
  -d "{
    \"postId\": ${POST_ID},
    \"parentId\": ${INFINITE_ROOT_ID},
    \"content\": \"Depth 1 댓글\",
    \"author\": \"사용자1\"
  }")

DEPTH1_ID=$(echo $DEPTH1_RESPONSE | jq -r '.id')
echo $DEPTH1_RESPONSE | jq .
echo ""
echo ""

# Depth 2 생성
echo -e "${YELLOW}10. Depth 2 댓글 생성${NC}"
DEPTH2_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/comments/infinite-depth" \
  -H "Content-Type: application/json" \
  -d "{
    \"postId\": ${POST_ID},
    \"parentId\": ${DEPTH1_ID},
    \"content\": \"Depth 2 댓글\",
    \"author\": \"사용자2\"
  }")

echo $DEPTH2_RESPONSE | jq .
echo ""
echo ""

# 무한 Depth 댓글 목록 조회
echo -e "${YELLOW}11. 무한 Depth 댓글 목록 조회 (트리 구조)${NC}"
curl -s "${BASE_URL}/api/comments/infinite-depth?postId=${POST_ID}" | jq .
echo ""
echo ""

# 댓글 개수 조회
echo -e "${YELLOW}12. 댓글 개수 조회${NC}"
COMMENT_COUNT=$(curl -s "${BASE_URL}/api/comments/count?postId=${POST_ID}")
echo -e "${GREEN}총 댓글 수: $COMMENT_COUNT${NC}"
echo ""
echo ""

# 좋아요 증가
echo -e "${YELLOW}13. 좋아요 증가${NC}"
curl -s -X POST "${BASE_URL}/api/posts/${POST_ID}/like" | jq .
echo ""
echo ""

# 게시글 수정
echo -e "${YELLOW}14. 게시글 수정${NC}"
curl -s -X PUT "${BASE_URL}/api/posts/${POST_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "curl 테스트 게시글 (수정됨)",
    "content": "curl로 수정한 게시글입니다."
  }' | jq .
echo ""
echo ""

# 최종 게시글 상태 확인
echo -e "${YELLOW}15. 최종 게시글 상태 확인${NC}"
curl -s "${BASE_URL}/api/posts/${POST_ID}" | jq .
echo ""
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}테스트 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}생성된 리소스 ID:${NC}"
echo -e "  - 게시글: ${GREEN}${POST_ID}${NC}"
echo -e "  - 댓글: ${GREEN}${COMMENT_ID}${NC}"
echo -e "  - 대댓글: ${GREEN}${REPLY_ID}${NC}"
echo -e "  - 무한 Depth 루트: ${GREEN}${INFINITE_ROOT_ID}${NC}"
echo ""
