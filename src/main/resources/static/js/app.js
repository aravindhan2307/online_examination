// SmartExam Unified Application Logic

// API Configuration URLs
const API_QUESTIONS = '/api/questions';
const API_RESULTS = '/api/results';

// Global Candidate Exam State
let examState = {
    questions: [],
    answers: [], // holds { questionId, selectedOptionIndex }
    currentIndex: 0,
    timerInterval: null,
    totalSeconds: 150, // 2.5 minutes
    remainingSeconds: 150,
    candidateName: '',
    category: ''
};

// Global Admin List State
let adminQuestionsList = [];

// ==========================================
// 1. CANDIDATE EXAM RUNTIME WORKFLOW
// ==========================================

function initExamPage() {
    examState.candidateName = sessionStorage.getItem('candidateName');
    examState.category = sessionStorage.getItem('examCategory');

    if (!examState.candidateName || !examState.category) {
        window.location.href = 'index.html';
        return;
    }

    // Set layout values
    document.getElementById('displayCandidateName').textContent = examState.candidateName;
    document.getElementById('displayCategory').textContent = `Topic: ${formatCategoryName(examState.category)}`;

    // Hook buttons
    document.getElementById('prevBtn').addEventListener('click', navigatePrevious);
    document.getElementById('nextBtn').addEventListener('click', navigateNext);
    document.getElementById('submitExamBtn').addEventListener('click', showSubmitModal);
    
    // Modal buttons
    document.getElementById('cancelSubmitBtn').addEventListener('click', hideSubmitModal);
    document.getElementById('confirmSubmitBtn').addEventListener('click', () => {
        hideSubmitModal();
        submitExam();
    });

    // Fetch Questions
    fetchQuestionsForExam();
}

function formatCategoryName(cat) {
    switch (cat) {
        case 'java': return 'Java Programming';
        case 'web': return 'Web Development (HTML/CSS/JS)';
        case 'general': return 'General Knowledge';
        default: return cat;
    }
}

function fetchQuestionsForExam() {
    // Questions were pre-loaded and stored in sessionStorage by the /api/questions/start
    // call made on the login page — read them directly to avoid a duplicate API round-trip.
    const raw = sessionStorage.getItem('examQuestions');

    if (!raw) {
        alert('No exam questions found. Please return to the home page and start a new session.');
        window.location.href = 'index.html';
        return;
    }

    const data = JSON.parse(raw);

    if (!data || data.length === 0) {
        alert('No questions found in this category in the database! Go to Admin Panel to add some.');
        window.location.href = 'index.html';
        return;
    }

    examState.questions = data;

    // Map answers tracking
    examState.answers = data.map(q => ({
        questionId: q.id,
        selectedOptionIndex: -1
    }));

    renderNavigatorGrid();
    renderQuestion(0);
    startTimer();
}


function renderNavigatorGrid() {
    const grid = document.getElementById('navigatorGrid');
    grid.innerHTML = '';

    examState.questions.forEach((q, idx) => {
        const box = document.createElement('div');
        box.className = 'nav-box';
        box.textContent = idx + 1;
        box.id = `nav-box-${idx}`;
        box.addEventListener('click', () => renderQuestion(idx));
        grid.appendChild(box);
    });
}

function renderQuestion(index) {
    if (index < 0 || index >= examState.questions.length) return;
    
    examState.currentIndex = index;
    const question = examState.questions[index];
    
    // Highlight boxes
    examState.questions.forEach((_, idx) => {
        const box = document.getElementById(`nav-box-${idx}`);
        if (!box) return;
        
        box.className = 'nav-box';
        if (idx === index) {
            box.classList.add('current');
        } else if (examState.answers[idx].selectedOptionIndex !== -1) {
            box.classList.add('answered');
        }
    });

    document.getElementById('questionNumDisplay').textContent = `Question ${index + 1} of ${examState.questions.length}`;
    document.getElementById('questionText').textContent = question.questionText;

    const optionsContainer = document.getElementById('optionsContainer');
    optionsContainer.innerHTML = '';

    const alphaPrefixes = ['A', 'B', 'C', 'D'];
    question.options.forEach((optText, optIdx) => {
        const optionItem = document.createElement('div');
        optionItem.classList.add('option-item');
        
        const userSelection = examState.answers[index].selectedOptionIndex;
        if (userSelection === optIdx) {
            optionItem.classList.add('selected');
        }

        optionItem.innerHTML = `
            <div class="option-prefix">${alphaPrefixes[optIdx]}</div>
            <div class="option-text">${escapeHtml(optText)}</div>
        `;

        optionItem.addEventListener('click', () => {
            examState.answers[index].selectedOptionIndex = optIdx;
            renderQuestion(index);
        });

        optionsContainer.appendChild(optionItem);
    });

    // Toggle navigation triggers
    const isFirst = (index === 0);
    const isLast = (index === examState.questions.length - 1);

    document.getElementById('prevBtn').style.visibility = isFirst ? 'hidden' : 'visible';
    
    if (isLast) {
        document.getElementById('nextBtn').classList.add('hidden');
        document.getElementById('submitExamBtn').classList.remove('hidden');
    } else {
        document.getElementById('nextBtn').classList.remove('hidden');
        document.getElementById('submitExamBtn').classList.add('hidden');
    }
}

function navigateNext() {
    if (examState.currentIndex < examState.questions.length - 1) {
        renderQuestion(examState.currentIndex + 1);
    }
}

function navigatePrevious() {
    if (examState.currentIndex > 0) {
        renderQuestion(examState.currentIndex - 1);
    }
}

function startTimer() {
    examState.remainingSeconds = examState.totalSeconds;
    updateTimerUI();

    examState.timerInterval = setInterval(() => {
        examState.remainingSeconds--;
        updateTimerUI();

        if (examState.remainingSeconds <= 0) {
            clearInterval(examState.timerInterval);
            alert('Timer expired! Submitting...');
            submitExam();
        }
    }, 1000);
}

function updateTimerUI() {
    const minutes = Math.floor(examState.remainingSeconds / 60);
    const seconds = examState.remainingSeconds % 60;
    
    document.getElementById('timerText').textContent = 
        `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

    const fillPercent = (examState.remainingSeconds / examState.totalSeconds) * 100;
    const bar = document.getElementById('timerBar');
    bar.style.width = `${fillPercent}%`;

    if (examState.remainingSeconds <= 30) {
        bar.className = 'timer-progress-fill danger';
    } else if (examState.remainingSeconds <= 75) {
        bar.className = 'timer-progress-fill warning';
    } else {
        bar.className = 'timer-progress-fill';
    }
}

function showSubmitModal() {
    document.getElementById('submitModal').classList.remove('hidden');
}

function hideSubmitModal() {
    document.getElementById('submitModal').classList.add('hidden');
}

function submitExam() {
    clearInterval(examState.timerInterval);

    // Clear the pre-loaded question list so it cannot be re-used without a fresh /start call
    sessionStorage.removeItem('examQuestions');
    sessionStorage.removeItem('examWasReset');

    const payload = {
        userName: examState.candidateName,
        category: examState.category,
        answers: examState.answers
    };

    fetch(`${API_RESULTS}/submit`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Server grading failure');
        }
        return response.json();
    })
    .then(result => {
        sessionStorage.setItem('examResult', JSON.stringify(result));
        window.location.href = 'result.html';
    })
    .catch(err => {
        console.error('Error submitting exam:', err);
        alert('Failed to submit exam. Check backend server logs.');
    });
}

// ==========================================
// 2. GRADED ASSESSMENT RESULTS WORKFLOW
// ==========================================

function renderExamResult() {
    const rawResult = sessionStorage.getItem('examResult');
    if (!rawResult) {
        window.location.href = 'index.html';
        return;
    }

    const result = JSON.parse(rawResult);

    // Profile summary details
    document.getElementById('resCandidateName').textContent = result.userName;
    document.getElementById('resTotalQ').textContent = result.totalQuestions;
    document.getElementById('resCorrectQ').textContent = result.score;
    document.getElementById('resWrongQ').textContent = result.totalQuestions - result.score;

    // Animate SVG Radial circle
    const percentage = result.percentage;
    const percentageText = document.getElementById('percentageVal');
    const circleFill = document.getElementById('percentageCircle');
    
    const circumference = 314;
    const offset = circumference - (circumference * percentage / 100);
    
    setTimeout(() => {
        circleFill.style.strokeDashoffset = offset;
    }, 100);

    // Dynamic number counting
    let count = 0;
    const countTimer = setInterval(() => {
        count += (percentage / 60);
        if (count >= percentage) {
            clearInterval(countTimer);
            percentageText.textContent = `${percentage}%`;
        } else {
            percentageText.textContent = `${Math.floor(count)}%`;
        }
    }, 20);

    // Set performance tier grading labels
    const grade = document.getElementById('gradeLabel');
    if (percentage >= 80) {
        grade.textContent = 'Excellent Work! 🏆';
        grade.style.color = 'var(--color-success)';
    } else if (percentage >= 50) {
        grade.textContent = 'Good Job! 👍';
        grade.style.color = 'var(--color-info)';
    } else {
        grade.textContent = 'Needs Practice! 📚';
        grade.style.color = 'var(--color-error)';
    }

    // Show ARREAR badge when percentage < 50
    const arrearContainer = document.getElementById('arrearBadgeContainer');
    if (arrearContainer && percentage < 50) {
        arrearContainer.style.display = 'block';
    }

    // Render detailed key lists
    const container = document.getElementById('reviewContainer');
    container.innerHTML = '';

    const alphaPrefixes = ['A', 'B', 'C', 'D'];
    result.questionResults.forEach((qRes, idx) => {
        const item = document.createElement('div');
        item.classList.add('review-item');

        const statusBadgeClass = qRes.correct ? 'correct' : 'wrong';
        const statusBadgeIcon = qRes.correct 
            ? '<i class="fa-solid fa-circle-check"></i> Correct' 
            : '<i class="fa-solid fa-circle-xmark"></i> Incorrect';

        let reviewHtml = `
            <div class="review-header">
                <h4 class="review-question">${idx + 1}. ${escapeHtml(qRes.questionText)}</h4>
                <span class="review-status-badge ${statusBadgeClass}">${statusBadgeIcon}</span>
            </div>
            <div class="review-options">
        `;

        qRes.options.forEach((optText, optIdx) => {
            let choiceClass = '';
            if (optIdx === qRes.correctOptionIndex) {
                choiceClass = 'correct-choice';
            } else if (optIdx === qRes.selectedOptionIndex && !qRes.correct) {
                choiceClass = 'incorrect-choice';
            }

            reviewHtml += `
                <div class="review-option ${choiceClass}">
                    <span class="review-option-badge">${alphaPrefixes[optIdx]}</span>
                    <span>${escapeHtml(optText)}</span>
                </div>
            `;
        });

        reviewHtml += `</div>`;

        let feedbackText = '';
        if (qRes.selectedOptionIndex === -1) {
            feedbackText = 'You did not answer this question. The correct answer is highlighted in green.';
        } else if (qRes.correct) {
            feedbackText = 'Excellent! Your selected answer was correct.';
        } else {
            const correctChar = alphaPrefixes[qRes.correctOptionIndex];
            feedbackText = `Your answer was incorrect. You selected option ${alphaPrefixes[qRes.selectedOptionIndex]}, but the correct option was ${correctChar}.`;
        }

        reviewHtml += `<p class="review-feedback-text"><i class="fa-solid fa-circle-info"></i> ${feedbackText}</p>`;
        item.innerHTML = reviewHtml;
        container.appendChild(item);
    });
}

// ==========================================
// 3. ADMIN PORTAL CRUD CONTROLS (admin.html)
// ==========================================

function initAdminPage() {
    fetchAdminQuestions();

    // Hook category filter dropdown
    document.getElementById('categoryFilter').addEventListener('change', filterAdminQuestions);

    // Modal control actions
    document.getElementById('addNewBtn').addEventListener('click', openAddModal);
    document.getElementById('closeModalBtn').addEventListener('click', closeAdminModal);
    document.getElementById('cancelFormBtn').addEventListener('click', closeAdminModal);
    
    // Save Form Submission
    document.getElementById('questionForm').addEventListener('submit', saveQuestionForm);
}

function fetchAdminQuestions() {
    fetch(API_QUESTIONS)
        .then(response => response.json())
        .then(data => {
            adminQuestionsList = data;
            filterAdminQuestions();
        })
        .catch(err => {
            console.error('Error fetching questions:', err);
            const tableBody = document.getElementById('questionsTableBody');
            tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-error"><i class="fa-solid fa-circle-exclamation"></i> Error loading questions from backend! Check database connectivity.</td></tr>`;
        });
}

function filterAdminQuestions() {
    const selectedCategory = document.getElementById('categoryFilter').value;
    
    // JS array filtering acts as browser side streams mapping!
    const filteredList = (selectedCategory === 'all')
        ? adminQuestionsList
        : adminQuestionsList.filter(q => q.category.toLowerCase() === selectedCategory.toLowerCase());

    renderQuestionsTable(filteredList);
}

function renderQuestionsTable(questions) {
    const tableBody = document.getElementById('questionsTableBody');
    tableBody.innerHTML = '';

    if (questions.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No questions found in this category. Click 'Add New Question' to create one!</td></tr>`;
        return;
    }

    const alphaOptions = ['A', 'B', 'C', 'D'];

    questions.forEach(q => {
        const row = document.createElement('tr');
        
        // Setup Category Badge Class
        const badgeClass = `badge-category ${q.category.toLowerCase()}`;
        
        // Format options list
        const correctIndex = q.correctOptionIndex;
        const optionsSpans = [
            `<span class="row-option-span ${correctIndex === 0 ? 'correct' : ''}">A: ${escapeHtml(q.optionA)}</span>`,
            `<span class="row-option-span ${correctIndex === 1 ? 'correct' : ''}">B: ${escapeHtml(q.optionB)}</span>`,
            `<span class="row-option-span ${correctIndex === 2 ? 'correct' : ''}">C: ${escapeHtml(q.optionC)}</span>`,
            `<span class="row-option-span ${correctIndex === 3 ? 'correct' : ''}">D: ${escapeHtml(q.optionD)}</span>`
        ].join(' ');

        row.innerHTML = `
            <td>${q.id}</td>
            <td><span class="${badgeClass}">${formatCategoryName(q.category)}</span></td>
            <td>
                <div class="row-question-text">${escapeHtml(q.questionText)}</div>
                <div class="row-options-list">${optionsSpans}</div>
            </td>
            <td style="font-weight: 600; color: var(--color-success);">
                Option ${alphaOptions[correctIndex]}
            </td>
            <td>
                <div class="table-actions">
                    <button class="btn-icon btn-icon-edit" onclick="openEditModal(${q.id})" title="Edit Question">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button class="btn-icon btn-icon-delete" onclick="deleteQuestionRecord(${q.id})" title="Delete Question">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;

        tableBody.appendChild(row);
    });
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = 'Add New Question';
    document.getElementById('questionId').value = '';
    document.getElementById('questionForm').reset();
    document.getElementById('questionModal').classList.remove('hidden');
}

function closeAdminModal() {
    document.getElementById('questionModal').classList.add('hidden');
}

function openEditModal(id) {
    fetch(`${API_QUESTIONS}/${id}`)
        .then(response => {
            if (!response.ok) throw new Error('Fetch failed');
            return response.json();
        })
        .then(q => {
            document.getElementById('modalTitle').textContent = 'Edit Question';
            document.getElementById('questionId').value = q.id;
            document.getElementById('questionCategory').value = q.category.toLowerCase();
            document.getElementById('questionText').value = q.questionText;
            document.getElementById('optionA').value = q.optionA;
            document.getElementById('optionB').value = q.optionB;
            document.getElementById('optionC').value = q.optionC;
            document.getElementById('optionD').value = q.optionD;
            document.getElementById('correctOption').value = q.correctOptionIndex;

            document.getElementById('questionModal').classList.remove('hidden');
        })
        .catch(err => {
            console.error('Error fetching question detail:', err);
            alert('Failed to retrieve question details for editing.');
        });
}

function saveQuestionForm(e) {
    e.preventDefault();

    const id = document.getElementById('questionId').value;
    const payload = {
        questionText: document.getElementById('questionText').value.trim(),
        optionA: document.getElementById('optionA').value.trim(),
        optionB: document.getElementById('optionB').value.trim(),
        optionC: document.getElementById('optionC').value.trim(),
        optionD: document.getElementById('optionD').value.trim(),
        correctOptionIndex: parseInt(document.getElementById('correctOption').value),
        category: document.getElementById('questionCategory').value
    };

    const isEdit = (id !== '');
    const url = isEdit ? `${API_QUESTIONS}/${id}` : API_QUESTIONS;
    const method = isEdit ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (!response.ok) throw new Error('Saving failed');
        return response.json();
    })
    .then(() => {
        closeAdminModal();
        fetchAdminQuestions(); // Refresh database table values
    })
    .catch(err => {
        console.error('Error saving question:', err);
        alert('Failed to save the question. Check server connection!');
    });
}

function deleteQuestionRecord(id) {
    if (confirm('Are you sure you want to delete this question from the database?')) {
        fetch(`${API_QUESTIONS}/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) throw new Error('Deletion failed');
            fetchAdminQuestions(); // Refresh database table values
        })
        .catch(err => {
            console.error('Error deleting question:', err);
            alert('Failed to delete question record.');
        });
    }
}

// ==========================================
// 4. EXAMINATION LOGS HISTORY (history.html)
// ==========================================

function initHistoryPage() {
    fetchHistoryLogs();

    document.getElementById('refreshLogsBtn').addEventListener('click', fetchHistoryLogs);
}

function fetchHistoryLogs() {
    fetch(API_RESULTS)
        .then(response => response.json())
        .then(data => {
            renderHistoryTable(data);
        })
        .catch(err => {
            console.error('Error fetching history logs:', err);
            const tableBody = document.getElementById('historyTableBody');
            tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-error"><i class="fa-solid fa-circle-exclamation"></i> Error loading history. Make sure server is running.</td></tr>`;
        });
}

function renderHistoryTable(results) {
    const tableBody = document.getElementById('historyTableBody');
    tableBody.innerHTML = '';

    if (results.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No assessment logs found in the database. Take some exams to generate records!</td></tr>`;
        return;
    }

    results.forEach(res => {
        const row = document.createElement('tr');
        
        // Format local date string
        const dateObj = new Date(res.submissionTime);
        const formattedDate = dateObj.toLocaleString();

        const badgeClass = `badge-category ${res.category.toLowerCase()}`;

        row.innerHTML = `
            <td>${res.id}</td>
            <td style="font-weight: 600; color: #fff;">${escapeHtml(res.candidateName)}</td>
            <td><span class="${badgeClass}">${formatCategoryName(res.category)}</span></td>
            <td class="text-center">${res.score} / ${res.totalQuestions}</td>
            <td class="text-center" style="font-weight: 700; color: ${res.percentage >= 50 ? 'var(--color-success)' : 'var(--color-error)'};">
                ${res.percentage}%
                ${res.arrear ? '<br><span style="display:inline-block;margin-top:3px;font-size:.72rem;background:rgba(239,68,68,.15);border:1px solid rgba(239,68,68,.4);color:#f87171;padding:2px 8px;border-radius:1rem;font-weight:700;">ARREAR</span>' : ''}
            </td>
            <td>${formattedDate}</td>
            <td>
                <div class="table-actions">
                    <button class="btn-icon btn-icon-delete" onclick="deleteHistoryRecord(${res.id})" title="Delete Result Log">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;

        tableBody.appendChild(row);
    });
}

function deleteHistoryRecord(id) {
    if (confirm('Are you sure you want to delete this result log from the database?')) {
        fetch(`${API_RESULTS}/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) throw new Error('Deletion failed');
            fetchHistoryLogs(); // Refresh history logs from DB
        })
        .catch(err => {
            console.error('Error deleting result log:', err);
            alert('Failed to delete history entry.');
        });
    }
}

// Safety utility for HTML escaping
function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
