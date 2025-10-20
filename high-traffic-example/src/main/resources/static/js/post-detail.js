// 게시글 상세 페이지 스크립트

document.addEventListener('DOMContentLoaded', function() {
    // 좋아요 버튼
    const likeBtn = document.querySelector('.btn-like');
    if (likeBtn) {
        likeBtn.addEventListener('click', handleLike);
    }

    // 댓글 작성 폼
    const commentForm = document.getElementById('commentForm');
    if (commentForm) {
        commentForm.addEventListener('submit', handleCommentSubmit);
    }

    // 답글 버튼들
    const replyBtns = document.querySelectorAll('.reply-btn');
    replyBtns.forEach(btn => {
        btn.addEventListener('click', handleReplyBtnClick);
    });

    // 답글 취소 버튼들
    const cancelBtns = document.querySelectorAll('.cancel-reply');
    cancelBtns.forEach(btn => {
        btn.addEventListener('click', handleCancelReply);
    });

    // 답글 작성 폼들
    const replyForms = document.querySelectorAll('.reply-form');
    replyForms.forEach(form => {
        form.addEventListener('submit', handleReplySubmit);
    });

    // 삭제 버튼들
    const deleteBtns = document.querySelectorAll('.delete-btn');
    deleteBtns.forEach(btn => {
        btn.addEventListener('click', handleDelete);
    });
});

// 좋아요 처리
async function handleLike(e) {
    const postId = e.currentTarget.dataset.postId;

    try {
        const response = await fetch(`/api/posts/${postId}/like`, {
            method: 'POST'
        });

        if (response.ok) {
            const data = await response.json();
            console.log('좋아요 응답 데이터:', data);

            const likeCount = document.querySelector('.like-count');
            if (likeCount) {
                likeCount.textContent = data.likeCount;
            }

            // 버튼 애니메이션
            e.currentTarget.style.transform = 'scale(1.1)';
            setTimeout(() => {
                e.currentTarget.style.transform = 'scale(1)';
            }, 200);
        } else {
            const errorText = await response.text();
            console.error('좋아요 API 응답 실패:', response.status, errorText);
            alert('좋아요 처리에 실패했습니다.');
        }
    } catch (error) {
        console.error('좋아요 처리 실패:', error);
        alert('좋아요 처리에 실패했습니다.');
    }
}

// 댓글 작성
async function handleCommentSubmit(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        postId: parseInt(formData.get('postId')),
        parentId: null,
        content: formData.get('content'),
        author: formData.get('author')
    };

    try {
        const response = await fetch('/api/comments/two-depth', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            alert('댓글이 작성되었습니다.');
            location.reload();
        } else {
            const error = await response.json();
            alert('댓글 작성 실패: ' + error.message);
        }
    } catch (error) {
        console.error('댓글 작성 실패:', error);
        alert('댓글 작성에 실패했습니다.');
    }
}

// 답글 버튼 클릭
function handleReplyBtnClick(e) {
    const commentId = e.currentTarget.dataset.commentId;
    const replyForm = document.getElementById(`replyForm-${commentId}`);

    // 다른 답글 폼은 모두 닫기
    document.querySelectorAll('.reply-form-wrapper').forEach(wrapper => {
        if (wrapper.id !== `replyForm-${commentId}`) {
            wrapper.style.display = 'none';
        }
    });

    // 현재 답글 폼 토글
    if (replyForm.style.display === 'none' || replyForm.style.display === '') {
        replyForm.style.display = 'block';
    } else {
        replyForm.style.display = 'none';
    }
}

// 답글 취소
function handleCancelReply(e) {
    const replyFormWrapper = e.currentTarget.closest('.reply-form-wrapper');
    replyFormWrapper.style.display = 'none';

    // 폼 초기화
    const form = replyFormWrapper.querySelector('form');
    form.reset();
}

// 답글 작성
async function handleReplySubmit(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        postId: parseInt(formData.get('postId')),
        parentId: parseInt(formData.get('parentId')),
        content: formData.get('content'),
        author: formData.get('author')
    };

    try {
        const response = await fetch('/api/comments/two-depth', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            alert('답글이 작성되었습니다.');
            location.reload();
        } else {
            const error = await response.json();
            alert('답글 작성 실패: ' + error.message);
        }
    } catch (error) {
        console.error('답글 작성 실패:', error);
        alert('답글 작성에 실패했습니다.');
    }
}

// 댓글/답글 삭제
async function handleDelete(e) {
    const commentId = e.currentTarget.dataset.commentId;

    if (!confirm('정말 삭제하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/api/comments/${commentId}`, {
            method: 'DELETE'
        });

        if (response.ok || response.status === 204) {
            alert('삭제되었습니다.');
            location.reload();
        } else {
            alert('삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('삭제 실패:', error);
        alert('삭제에 실패했습니다.');
    }
}
