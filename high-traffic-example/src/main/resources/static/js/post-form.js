// 게시글 작성 폼 스크립트

document.addEventListener('DOMContentLoaded', function() {
    const postForm = document.getElementById('postForm');

    if (postForm) {
        postForm.addEventListener('submit', handlePostSubmit);
    }
});

async function handlePostSubmit(e) {
    e.preventDefault();

    const formData = new FormData(e.target);
    const data = {
        title: formData.get('title'),
        content: formData.get('content'),
        author: formData.get('author')
    };

    // 유효성 검사
    if (!data.title || !data.content || !data.author) {
        alert('모든 필드를 입력해주세요.');
        return;
    }

    try {
        const response = await fetch('/api/posts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            const post = await response.json();
            alert('게시글이 작성되었습니다.');
            // 작성한 게시글 상세 페이지로 이동
            window.location.href = `/posts/${post.id}`;
        } else {
            const error = await response.json();
            alert('게시글 작성 실패: ' + error.message);
        }
    } catch (error) {
        console.error('게시글 작성 실패:', error);
        alert('게시글 작성에 실패했습니다.');
    }
}
