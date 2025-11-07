const API_BASE_URL = 'http://localhost:8099';

// Get references to UI elements
const registerSection = document.getElementById('registerSection');
const loginSection = document.getElementById('loginSection');
const chatSection = document.getElementById('chatSection');
const travelPlanSection = document.getElementById('travelPlanSection');
// const profileSection = document.getElementById('profileSection');

const showRegisterBtn = document.getElementById('showRegister');
const showLoginBtn = document.getElementById('showLogin');
const showChatBtn = document.getElementById('showChat');
const showTravelPlanBtn = document.getElementById('showTravelPlan');
// const showProfileBtn = document.getElementById('showProfile');
const logoutBtn = document.getElementById('logout');

const registerForm = document.getElementById('registerForm');
const loginForm = document.getElementById('loginForm');
// Get UI elements
const chatForm = document.getElementById('chatForm');
const chatInput = document.getElementById('chatInput');
const chatOutput = document.getElementById('chatOutput');
const micButton = document.getElementById('micButton'); // Get the microphone button
const speechStatus = document.getElementById('speechStatus');
// const saveTravelPlanBtn = document.getElementById('saveTravelPlanBtn');
// const confirmSavePlanBtn = document.getElementById('confirmSavePlanBtn');

// PCM录音设置
let audioContext = null;
let audioStream = null;
let scriptProcessor = null;
let recording = false;
let recordedChunks = [];
let startTime;
let audioInitialized = false;

// WAV编码函数
function encodeWAV(samples, sampleRate = 48000) {
    const buffer = new ArrayBuffer(44 + samples.length * 2);
    const view = new DataView(buffer);
    
    // WAV文件头
    writeString(view, 0, 'RIFF');
    view.setUint32(4, 36 + samples.length * 2, true);
    writeString(view, 8, 'WAVE');
    writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); // PCM格式
    view.setUint16(22, 1, true); // 单声道
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * 2, true); // 字节率
    view.setUint16(32, 2, true); // 块对齐
    view.setUint16(34, 16, true); // 16位采样
    writeString(view, 36, 'data');
    view.setUint32(40, samples.length * 2, true);
    
    // 将浮点数PCM转换为16位整数PCM
    let offset = 44;
    for (let i = 0; i < samples.length; i++) {
        const s = Math.max(-1, Math.min(1, samples[i]));
        view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
        offset += 2;
    }
    
    return new Blob([view], { type: 'audio/wav' });
}

// 辅助函数：写入字符串到DataView
function writeString(view, offset, string) {
    for (let i = 0; i < string.length; i++) {
        view.setUint8(offset + i, string.charCodeAt(i));
    }
}

// 下载音频文件函数
function downloadAudioFile(audioBlob, filename = 'recorded_audio.wav') {
    const url = URL.createObjectURL(audioBlob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    
    // 清理资源
    setTimeout(() => {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }, 100);
    
    console.log(`音频文件已下载: ${filename}`);
}

// 音频系统初始化 - 纯PCM录制方案
async function setupAudioRecording() {
    try {
        // 关闭之前的资源
        if (audioStream) {
            audioStream.getTracks().forEach(track => track.stop());
        }
        
        if (audioContext) {
            await audioContext.close();
        }
        
        // 创建新的AudioContext - 必须在用户手势后创建
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        
        // 等待音频上下文恢复（如果被暂停）
        if (audioContext.state === 'suspended') {
            await audioContext.resume();
        }
        
        // 获取麦克风权限 - 使用高质量设置
        audioStream = await navigator.mediaDevices.getUserMedia({ 
            audio: {
                channelCount: 1, // 单声道
                sampleRate: 48000, // 48kHz采样率
                echoCancellation: false, // 关闭回声消除，减少处理
                noiseSuppression: false, // 关闭降噪，保持原始音质
                autoGainControl: false, // 关闭自动增益控制
                latency: 0, // 最小延迟
                sampleSize: 16 // 16位采样
            } 
        });
        
        console.log('音频设置完成，采样率:', audioContext.sampleRate);
        
        // 创建音频源
        const source = audioContext.createMediaStreamSource(audioStream);
        
        // 创建脚本处理器用于捕获PCM数据
        // 使用4096的缓冲区大小以获得更好的性能
        scriptProcessor = audioContext.createScriptProcessor(4096, 1, 1);
        
        // 重置录音数据
        recordedChunks = [];
        
        // 处理音频数据
        scriptProcessor.onaudioprocess = (event) => {
            if (!recording) return;
            
            // 获取输入的PCM数据
            const inputData = event.inputBuffer.getChannelData(0);
            
            // 复制数据以避免引用问题
            const chunk = new Float32Array(inputData.length);
            chunk.set(inputData);
            
            // 存储PCM数据块
            recordedChunks.push(chunk);
        };
        
        // 连接节点：source -> scriptProcessor -> destination
        source.connect(scriptProcessor);
        scriptProcessor.connect(audioContext.destination);
        
        audioInitialized = true;
        console.log('PCM音频录制系统初始化完成');
        return true;
        
    } catch (err) {
        console.error('音频设置错误:', err);
        speechStatus.textContent = '音频设置失败: ' + err.message;
        micButton.disabled = true;
        audioInitialized = false;
        return false;
    }
}

// 开始录音
async function startRecording() {
    // 确保音频系统已初始化
    if (!audioInitialized) {
        console.log('首次使用，初始化音频系统...');
        const success = await setupAudioRecording();
        if (!success) {
            speechStatus.textContent = '音频初始化失败';
            return false;
        }
    }
    
    // 确保音频上下文处于活动状态
    if (audioContext.state === 'suspended') {
        await audioContext.resume();
    }
    
    if (!audioContext || !scriptProcessor) {
        console.error('音频系统未正确初始化');
        speechStatus.textContent = '音频系统未正确初始化';
        return false;
    }
    
    recording = true;
    recordedChunks = [];
    startTime = Date.now();
    
    console.log('开始PCM录音...');
    speechStatus.textContent = '录音中...';
    micButton.classList.add('recording');
    
    return true;
}

// 停止录音并处理数据
function stopRecording() {
    if (!recording) return null;
    
    recording = false;
    const duration = Date.now() - startTime;
    
    console.log(`停止录音，时长: ${duration}ms, 数据块数: ${recordedChunks.length}`);
    speechStatus.textContent = '处理中...';
    micButton.classList.remove('recording');
    
    // 处理录制的PCM数据
    return processRecordedData();
}

// 处理录制的PCM数据
function processRecordedData() {
    if (recordedChunks.length === 0) {
        console.error('没有录制到音频数据');
        speechStatus.textContent = '没有录制到音频数据';
        return null;
    }
    
    try {
        // 计算总样本数
        const totalSamples = recordedChunks.reduce((sum, chunk) => sum + chunk.length, 0);
        console.log(`总样本数: ${totalSamples}`);
        
        // 合并所有PCM数据块
        const allSamples = new Float32Array(totalSamples);
        let offset = 0;
        
        for (const chunk of recordedChunks) {
            allSamples.set(chunk, offset);
            offset += chunk.length;
        }
        
        // 使用音频上下文的实际采样率
        const sampleRate = audioContext.sampleRate;
        console.log(`使用采样率: ${sampleRate}Hz`);
        
        // 编码为WAV
        const wavBlob = encodeWAV(allSamples, sampleRate);
        
        // 生成带时间戳的文件名
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
        // const filename = `pcm_recording_${timestamp}.wav`;
        
        // // 下载音频文件
        // downloadAudioFile(wavBlob, filename);
        
        // console.log('PCM WAV文件已生成，大小:', wavBlob.size, 'bytes');
        
        return wavBlob;
        
    } catch (error) {
        console.error('处理PCM数据错误:', error);
        speechStatus.textContent = '处理失败: ' + error.message;
        return null;
    }
}

// 发送音频进行识别
async function sendAudioForRecognition(audioBlob) {
    if (!audioBlob) {
        console.error('没有有效的音频数据');
        speechStatus.textContent = '没有有效的音频数据';
        return;
    }
    
    console.log('开始语音识别...');
    speechStatus.textContent = '识别语音中...';
    
    try {
        const formData = new FormData();
        formData.append('audioFile', audioBlob, 'audio.wav');

        const response = await fetch(`${API_BASE_URL}/speech/recognize`, {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP错误! 状态: ${response.status}, 消息: ${errorText}`);
        }

        const result = await response.json();
        console.log('识别结果:', result);
        
        if (result.data) {
            chatInput.value = result.data;
            speechStatus.textContent = '识别完成';
        } else {
            speechStatus.textContent = '识别结果为空';
        }
    } catch (error) {
        console.error('语音识别错误:', error);
        speechStatus.textContent = `识别错误: ${error.message}`;
    }
}

// 录音按钮事件
micButton.addEventListener('mousedown', async (event) => {
    // 阻止事件冒泡，确保用户手势被正确识别
    event.preventDefault();
    
    try {
        if (!recording) {
            const started = await startRecording();
            if (!started) {
                speechStatus.textContent = '开始录音失败';
            }
        }
    } catch (error) {
        console.error('开始录音错误:', error);
        speechStatus.textContent = '开始录音失败: ' + error.message;
    }
});

micButton.addEventListener('mouseup', async (event) => {
    event.preventDefault();
    
    try {
        if (recording) {
            const audioBlob = stopRecording();
            if (audioBlob) {
                await sendAudioForRecognition(audioBlob);
            }
        }
    } catch (error) {
        console.error('停止录音错误:', error);
        speechStatus.textContent = '停止录音失败: ' + error.message;
    }
});

// 点击事件作为备用手势
micButton.addEventListener('click', (event) => {
    // 防止重复处理
    event.preventDefault();
});
// 新增：美观渲染 JSON 行程计划
function renderPlanJson(jsonStr, outputContainer = chatJsonOutput) {
    let plan;
    try {
        plan = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
    } catch (e) {
        if (outputContainer) {
            outputContainer.textContent = '行程计划解析失败：' + e.message;
        }
        return;
    }
    // 结构化展示主要字段，兼容 camelCase 与 snake_case
    const planName = plan.plan_name || plan.planName || '未命名计划';
    const destination = plan.destination || plan.destination_city || '';
    const startDateRaw = plan.start_date || plan.startDate || '';
    const endDateRaw = plan.end_date || plan.endDate || '';
    const formatDate = (d) => Array.isArray(d) ? d.join('-') : d;
    const startDate = formatDate(startDateRaw);
    const endDate = formatDate(endDateRaw);
    const budget = plan.budget ?? plan.totalBudget ?? '';
    const travelers = plan.travelers ?? plan.people ?? '';
    const activities = plan.activities || plan.activity_list || [];
    const notes = plan.notes || plan.remark || '';
    
    if (outputContainer) {
        outputContainer.innerHTML = `
            <div class="plan-card">
                <h4>${planName}</h4>
                <p><strong>目的地：</strong> ${destination}</p>
                <p><strong>时间：</strong> ${startDate} ~ ${endDate}</p>
                <p><strong>预算：</strong> ${budget}</p>
                <p><strong>人数：</strong> ${travelers}</p>
                <h5>活动安排：</h5>
                <ul>
                    ${Array.isArray(activities) && activities.length > 0 ? activities.map(item => `<li>${item}</li>`).join('') : '<li>无详细安排</li>'}
                </ul>
                ${notes ? `<h6>备注：</h6><p>${notes}</p>` : ''}
            </div>
        `;
    }
}

// 初始化页面时只设置事件监听器，不初始化音频
document.addEventListener('DOMContentLoaded', () => {
    const chatJsonOutput = document.getElementById('chatJsonOutput');

    console.log('页面加载完成，音频系统将在首次点击时初始化');
    speechStatus.textContent = '点击麦克风按钮开始录音';
    updateNavigation();
});

// Function to fetch and display travel plans
async function fetchAndDisplayTravelPlans() {
    const savedTravelPlansDiv = document.getElementById('savedTravelPlans');
    savedTravelPlansDiv.innerHTML = 'Loading travel plans...'; // Clear previous content and show loading

    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${API_BASE_URL}/travel-plan`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (result.code === 200 && result.data) {
            displayTravelPlans(result.data);
        } else {
            savedTravelPlansDiv.innerHTML = `<p>Error fetching travel plans: ${result.message || 'Unknown error'}</p>`;
        }
    } catch (error) {
        console.error('Error fetching travel plans:', error);
        savedTravelPlansDiv.innerHTML = '<p>Failed to load travel plans.</p>';
    }
}

function displayTravelPlans(plans) {
    const savedTravelPlansDiv = document.getElementById('savedTravelPlans');
    savedTravelPlansDiv.innerHTML = ''; // Clear previous content

    if (plans.length === 0) {
        savedTravelPlansDiv.innerHTML = '<p>No travel plans saved yet.</p>';
        return;
    }

    plans.forEach(plan => {
        const planElement = document.createElement('div');
        planElement.classList.add('travel-plan-item');
        planElement.innerHTML = `
            <h3>${plan.planName || 'Untitled Plan'}</h3>
            <p><strong>Destination:</strong> ${plan.destination || ''}</p>
            <p><strong>Dates:</strong> ${plan.startDate || ''} - ${plan.endDate || ''}</p>
            <p><strong>Budget:</strong> ${plan.budget ?? ''} | <strong>Travelers:</strong> ${plan.travelers ?? ''}</p>
            <p><strong>Preferences:</strong> ${plan.preferences || ''}</p>
            <p><strong>Details:</strong> <pre>${plan.details ? JSON.stringify(plan.details, null, 2) : ''}</pre></p>
            <p><strong>Create Time:</strong> ${plan.createTime || ''}</p>
            <p><strong>Update Time:</strong> ${plan.updateTime || ''}</p>
            <button class="view-plan-btn" data-plan-id="${plan.id}">View Details</button>
            <button class="delete-plan-btn" data-plan-id="${plan.id}">Delete</button>
        `;
        savedTravelPlansDiv.appendChild(planElement);
    });

    // Attach event listeners for view, edit, delete buttons (will be implemented in later steps)
    document.querySelectorAll('.view-plan-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            const planId = event.target.dataset.planId;
            // Logic to view plan details (e.g., in a modal or new section)
            console.log('View plan:', planId);
        });
    });

    document.querySelectorAll('.edit-plan-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            const planId = event.target.dataset.planId;
            // Logic to edit plan (will be implemented in a later step)
            console.log('Edit plan:', planId);
        });
    });

    document.querySelectorAll('.delete-plan-btn').forEach(button => {
        button.addEventListener('click', (event) => {
            const planId = event.target.dataset.planId;
            deleteTravelPlan(planId);
        });
    });
}

const travelPlanForm = document.getElementById('travelPlanForm');

const registerMessage = document.getElementById('registerMessage');
const loginMessage = document.getElementById('loginMessage');
const profileUsername = document.getElementById('profileUsername');
const profileEmail = document.getElementById('profileEmail');
const profileMessage = document.getElementById('profileMessage');
const travelPlanOutput = document.getElementById('travelPlanOutput');

let currentChatId = null;
let currentPlanJson = '';

// 强制 JSON 总结提示词（仅输出严格 JSON）
const SUMMARIZE_PROMPT = `请基于当前对话总结一个完整的旅游计划并只输出严格的 JSON，字段必须使用 snake_case，与下列字段完全一致，不要输出任何解释或多余文本：\n\n{\n  "plan_name": "...",\n  "destination": "...",\n  "start_date": "YYYY-MM-DD",\n  "end_date": "YYYY-MM-DD",\n  "budget": 0.00,\n  "travelers": 1,\n  "preferences": ["...","..."],\n  "details": {\n    "transport": [],\n    "lodging": [],\n    "attractions": [],\n    "restaurants": [],\n    "daily_schedule": []\n  }\n}\n\n注意：\n- 只输出 JSON 字符串，无任何前后缀、无代码块、无注释；\n- 日期必须是 YYYY-MM-DD；\n- "budget" 是数字（可带两位小数）；\n- "preferences" 推荐使用字符串数组；\n- "details" 必须是对象（可包含任意结构化字段）。`;

// Helper: display message in an element
function displayMessage(el, msg, isError = false) {
    if (!el) return;
    el.textContent = msg;
    el.style.color = isError ? 'red' : 'black';
}

// Fetch user profile
async function getUserProfile() {
    const token = localStorage.getItem('token');
    if (!token) {
        displayMessage(profileMessage, '请先登录。', true);
        return;
    }
    try {
        const resp = await fetch(`${API_BASE_URL}/user/profile`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const result = await resp.json();
        if (result.code === 200 && result.data) {
            profileUsername.textContent = result.data.username || '';
            profileEmail.textContent = result.data.email || '';
            displayMessage(profileMessage, '');
        } else {
            displayMessage(profileMessage, result.message || '无法获取用户信息', true);
        }
    } catch (e) {
        displayMessage(profileMessage, `网络错误: ${e.message}`, true);
    }
}

// Function to show a specific section and hide others
function showSection(section) {
    registerSection.classList.add('hidden');
    loginSection.classList.add('hidden');
    chatSection.classList.add('hidden');
    travelPlanSection.classList.add('hidden');
    // profileSection.classList.add('hidden');
    section.classList.remove('hidden');
}

// Function to update navigation buttons based on login status
function updateNavigation() {
    const token = localStorage.getItem('token');
    if (token) {
        showRegisterBtn.style.display = 'none';
        showLoginBtn.style.display = 'none';
        showChatBtn.style.display = 'inline-block';
        showTravelPlanBtn.style.display = 'inline-block';
        // showProfileBtn.style.display = 'inline-block';
        logoutBtn.style.display = 'inline-block';
        showSection(chatSection); // Default to chat section after login
    } else {
        showRegisterBtn.style.display = 'inline-block';
        showLoginBtn.style.display = 'inline-block';
        showChatBtn.style.display = 'none';
        showTravelPlanBtn.style.display = 'none';
        // showProfileBtn.style.display = 'none';
        logoutBtn.style.display = 'none';
        showSection(loginSection); // Default to login section if not logged in
    }
}

// Event Listeners for navigation buttons
showRegisterBtn.addEventListener('click', () => showSection(registerSection));
showLoginBtn.addEventListener('click', () => showSection(loginSection));
showChatBtn.addEventListener('click', () => showSection(chatSection));
showTravelPlanBtn.addEventListener('click', () => {
    showSection(travelPlanSection);
    fetchAndDisplayTravelPlans(); // Fetch and display plans when the section is shown
});
// showProfileBtn.addEventListener('click', () => {
//     showSection(profileSection);
//     getUserProfile();
// });

// Register
registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('registerUsername').value.trim();
    const password = document.getElementById('registerPassword').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    try {
        const resp = await fetch(`${API_BASE_URL}/user/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email })
        });
        const result = await resp.json();
        if (result.code === 200) {
            registerMessage.textContent = '注册成功，请登录。';
            registerMessage.style.color = 'green';
            showSection(loginSection);
        } else {
            registerMessage.textContent = result.message || '注册失败';
            registerMessage.style.color = 'red';
        }
    } catch (e) {
        registerMessage.textContent = `网络错误: ${e.message}`;
        registerMessage.style.color = 'red';
    }
});

// Login
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    try {
        const resp = await fetch(`${API_BASE_URL}/user/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const result = await resp.json();
        if (result.code === 200 && result.data && result.data.token) {
            localStorage.setItem('token', result.data.token);
            loginMessage.textContent = '登录成功。';
            loginMessage.style.color = 'green';
            updateNavigation();
        } else {
            loginMessage.textContent = result.message || '登录失败';
            loginMessage.style.color = 'red';
        }
    } catch (e) {
        loginMessage.textContent = `网络错误: ${e.message}`;
        loginMessage.style.color = 'red';
    }
});

// Logout
logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('token');
    updateNavigation();
});

// // 保存与确认按钮事件
// saveTravelPlanBtn.addEventListener('click', async () => {
//     const token = localStorage.getItem('token');
//     if (!token) {
//         displayMessage(travelPlanOutput, '请先登录。', true);
//         return;
//     }
//     if (!currentChatId) {
//         displayMessage(travelPlanOutput, '请先在 Chat 标签与 AI 互动生成方案。', true);
//         return;
//     }
//     try {
//         const resp = await fetch(`${API_BASE_URL}/ai/chat`, {
//             method: 'POST',
//             headers: {
//                 'Content-Type': 'application/json',
//                 'Authorization': `Bearer ${token}`
//             },
//             body: JSON.stringify({ message: SUMMARIZE_PROMPT, chatId: currentChatId })
//         });
//         const result = await resp.json();
//         if (result.code === 200 && result.data) {
//             currentPlanJson = result.data;
//             travelPlanOutput.textContent = currentPlanJson;
//             confirmSavePlanBtn.disabled = false;
//         } else {
//             displayMessage(travelPlanOutput, result.message || '总结失败', true);
//         }
//     } catch (e) {
//         displayMessage(travelPlanOutput, `网络错误: ${e.message}`, true);
//     }
// });

// confirmSavePlanBtn.addEventListener('click', async () => {
//     const token = localStorage.getItem('token');
//     if (!token) {
//         displayMessage(travelPlanOutput, '请先登录。', true);
//         return;
//     }
//     if (!currentPlanJson || !currentPlanJson.trim()) {
//         displayMessage(travelPlanOutput, '请先点击“Save Travel Plan”获取 JSON 总结。', true);
//         return;
//     }
//     try {
//         const resp = await fetch(`${API_BASE_URL}/travel-plan/confirm`, {
//             method: 'POST',
//             headers: {
//                 'Content-Type': 'application/json',
//                 'Authorization': `Bearer ${token}`
//             },
//             body: currentPlanJson
//         });
//         const result = await resp.json();
//         if (result.code === 200 && result.data) {
//             displayMessage(travelPlanOutput, `保存成功！计划ID：${result.data}`);
//             confirmSavePlanBtn.disabled = true;
//             currentPlanJson = '';
//             fetchAndDisplayTravelPlans();
//         } else {
//             displayMessage(travelPlanOutput, result.message || '保存失败', true);
//         }
//     } catch (e) {
//         displayMessage(travelPlanOutput, `网络错误: ${e.message}`, true);
//     }
// });

// AI Chat
chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const message = e.target.chatInput.value;
    e.target.chatInput.value = '';

    chatOutput.innerHTML += `<p><strong>You:</strong> ${message}</p>`;

    if (!currentChatId) {
        currentChatId = crypto.randomUUID();
    }

    // Create a placeholder for the AI's response
    const aiResponseContainer = document.createElement('div');
    aiResponseContainer.classList.add('ai-response'); // Add a class for styling if needed
    chatOutput.appendChild(aiResponseContainer);
    chatOutput.scrollTop = chatOutput.scrollHeight;

    try {
        await streamChatResponse(message, currentChatId, aiResponseContainer);
    } catch (error) {
        aiResponseContainer.innerHTML += `<p style="color: red;"><strong>Error:</strong> ${error.message}</p>`;
    }
});

// New function for streaming chat response
async function streamChatResponse(message, chatId, aiResponseContainer) {
    const token = localStorage.getItem('token');
    if (!token) {
        displayMessage(loginMessage, 'Authentication required. Please log in.', true);
        updateNavigation();
        throw new Error('No authentication token found.');
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };

    const requestBody = JSON.stringify({ message, chatId });

    const url = `${API_BASE_URL}/ai/chat/sse-emitter`;

    let accumulatedContent = ''; // To store the full response for final Markdown rendering
    let currentDisplayedContent = ''; // To store content for incremental display
    let isPlanJsonRendered = false; // Flag to check if a plan JSON was rendered for the entire stream

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: headers,
            body: requestBody
        });

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = `HTTP error! status: ${response.status}`;
            try {
                const errorJson = JSON.parse(errorText);
                errorMessage = errorJson.message || errorMessage;
            } catch (parseError) {
                errorMessage = errorText || errorMessage;
            }
            throw new Error(errorMessage);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                break;
            }
            const chunk = decoder.decode(value, { stream: true });
+            console.log('Received SSE chunk:', chunk);
             // If a plan JSON was already rendered, skip further processing of chunks
             if (isPlanJsonRendered) {
                 break;
             }

             // Process each line in the chunk
+            chunk.split('\n').forEach(line => {
+                console.log('Processing line:', line);
            }); // 修正：此处应为表达式而非语句块
+            // reset iterator to actual logic (duplicate for correctness?)
             chunk.split('\n').forEach(line => {

                if (line.startsWith('data:')) {
                    const content = line.substring(5).trimStart();
                    try {
                        const parsedOuterContent = JSON.parse(content);
                        if (parsedOuterContent.code === 200 && parsedOuterContent.data) {
                            try {
                                const innerData = JSON.parse(parsedOuterContent.data);
                                console.log('innerData parsed:', innerData);
                                if (innerData.plan_name && innerData.destination) { // Check for travel plan specific keys
                                    console.log('Detected plan JSON, rendering to chatJsonOutput');
                                    renderPlanJson(innerData, chatJsonOutput);
                                    isPlanJsonRendered = true; // Set flag for the entire stream
                                    chatOutput.scrollTop = chatOutput.scrollTop;
                                    // If a plan JSON is rendered, we stop processing this line and the rest of the stream
                                    return; // Exit forEach for this line
                                }
                            } catch (innerParseError) {
                                // Not a valid JSON, treat as regular chat content
                            }
                        }
                    } catch (e) {
                        // Not a valid JSON or not a travel plan JSON, treat as regular chat content
                    }

                    // Only process as regular chat content if no plan JSON was rendered for the entire stream yet
                    if (!isPlanJsonRendered) {
                        accumulatedContent += content + '\n'; // Accumulate for final Markdown with newline
                        currentDisplayedContent += content + '\n'; // Accumulate for incremental display with newline
                        aiResponseContainer.innerHTML = marked.parse(currentDisplayedContent); // Display incrementally with Markdown
                        chatOutput.scrollTop = chatOutput.scrollHeight;
                    }
                }
            });
        } // End of while loop

        if (!isPlanJsonRendered) { // Only do final Markdown parse if no plan JSON was rendered for the entire stream
            aiResponseContainer.innerHTML = marked.parse(accumulatedContent);
        }

    } catch (error) {
        console.error('Stream chat failed:', error);
        throw error;
    }
}

// AI Travel Plan
let currentEditingPlanId = null; // To store the ID of the plan being edited

    async function editTravelPlan(planId, planContent) {
        const travelPlanInput = document.getElementById('travelPlanInput');
        const generatePlanBtn = document.getElementById('generatePlanBtn');

        travelPlanInput.value = planContent;
        generatePlanBtn.textContent = 'Update Plan';
        currentEditingPlanId = planId;

        // Scroll to the travel plan input section
        travelPlanInput.scrollIntoView({ behavior: 'smooth' });
    }

    if (travelPlanForm) travelPlanForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const travelPlanInput = document.getElementById('travelPlanInput').value;
        const token = localStorage.getItem('token');

        if (!token) {
            alert('请先登录。');
            return;
        }

        if (currentEditingPlanId) {
            // Update existing plan
            try {
                const response = await fetch(`${API_BASE_URL}/travel-plan/${currentEditingPlanId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ content: travelPlanInput })
                });

                if (response.ok) {
                    alert('旅行计划更新成功！');
                    document.getElementById('travelPlanInput').value = '';
                    document.getElementById('generatePlanBtn').textContent = 'Generate and Save Plan';
                    currentEditingPlanId = null;
                    fetchAndDisplayTravelPlans();
                } else {
                    const errorData = await response.json();
                    alert(`更新旅行计划失败: ${errorData.message || response.statusText}`);
                }
            } catch (error) {
                console.error('更新旅行计划时出错:', error);
                alert('更新旅行计划时发生网络错误。');
            }
        } else {
            // Original logic for generating and saving a new plan
            const travelPlanOutput = document.getElementById('travelPlanOutput');

            if (!travelPlanInput.trim()) {
                displayMessage(travelPlanOutput, 'Please describe your travel preferences.', true);
                return;
            }

            travelPlanOutput.innerHTML = 'Generating and saving your travel plan...';
            travelPlanOutput.style.color = 'black';

            try {
                // First, generate the personalized plan (using the existing /travel-plan/personalized endpoint)
                const generateResponse = await fetch(`${API_BASE_URL}/travel-plan/personalized?userInput=${encodeURIComponent(travelPlanInput)}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    }
                });
                const generateResult = await generateResponse.json();

                if (generateResult.code !== 200) {
                    displayMessage(travelPlanOutput, `Error generating plan: ${generateResult.message || 'Unknown error'}`, true);
                    return;
                }

                const generatedPlanContent = generateResult.data; // Assuming data contains the generated plan string

                // Then, save the generated plan
                const saveResponse = await fetch(`${API_BASE_URL}/travel-plan`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ title: 'New Travel Plan', content: generatedPlanContent })
                });
                const saveResult = await saveResponse.json();

                if (saveResult.code === 200) {
                    displayMessage(travelPlanOutput, 'Travel plan generated and saved successfully!');
                    document.getElementById('travelPlanInput').value = ''; // Clear input
                    fetchAndDisplayTravelPlans(); // Refresh the list of plans
                } else {
                    displayMessage(travelPlanOutput, `Error saving plan: ${saveResult.message || 'Unknown error'}`, true);
                }

            } catch (error) {
                console.error('Error generating or saving travel plan:', error);
                displayMessage(travelPlanOutput, 'Failed to generate or save travel plan.', true);
            }
        }
    })

    async function deleteTravelPlan(planId) {
        if (!confirm('确定要删除此旅行计划吗？')) {
            return;
        }

        const token = localStorage.getItem('token');
        if (!token) {
            alert('请先登录。');
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/travel-plan/${planId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                alert('旅行计划删除成功！');
                fetchAndDisplayTravelPlans(); // Refresh the list
            } else {
                const errorData = await response.json();
                alert(`删除旅行计划失败: ${errorData.message || response.statusText}`);
            }
        } catch (error) {
            console.error('删除旅行计划时出错:', error);
            alert('删除旅行计划时发生网络错误。');
        }
    }
// 新增：模式选择控件与按钮逻辑
const chatModeSelect = document.getElementById('chatMode');
const generatePlanBtn = document.getElementById('generatePlanBtn');
const confirmPlanBtn = document.getElementById('confirmPlanBtn');
const generateBudgetBtn = document.getElementById('generateBudgetBtn');
const confirmBudgetBtn = document.getElementById('confirmBudgetBtn');
const chatJsonOutput = document.getElementById('chatJsonOutput');
const budgetPlanSelectModal = document.getElementById('budgetPlanSelectModal');
const closeBudgetModal = document.getElementById('closeBudgetModal');
const budgetPlanList = document.getElementById('budgetPlanList');
const selectBudgetPlanBtn = document.getElementById('selectBudgetPlanBtn');

// 预算分析按钮点击弹窗逻辑
// console.log('Attempting to attach event listener to generateBudgetBtn'); // 移除调试日志
if (generateBudgetBtn) {
    // console.log('generateBudgetBtn found, attaching click listener'); // 移除调试日志
    generateBudgetBtn.addEventListener('click', () => {
        // console.log('generateBudgetBtn clicked!'); // 移除调试日志
        openBudgetPlanModal();
    });
} else {
    console.log('generateBudgetBtn not found during event listener attachment'); // 修改调试日志
}

chatModeSelect.addEventListener('change', () => {
    // console.log('chatModeSelect changed to:', chatModeSelect.value); // 移除调试日志
    if (chatModeSelect.value === 'plan') {
        generatePlanBtn.style.display = 'block';
        generateBudgetBtn.style.display = 'none';
    } else if (chatModeSelect.value === 'budget') {
        generatePlanBtn.style.display = 'none';
        generateBudgetBtn.style.display = 'block';
        openBudgetPlanModal();
        // console.log('generateBudgetBtn display set to block'); // 移除调试日志
    }
});

let selectedBudgetPlanId = null;
let lastPlanJson = '';
let lastBudgetJson = '';

function updateChatModeButtons() {
    if (chatModeSelect.value === 'plan') {
        generatePlanBtn.style.display = '';
        confirmPlanBtn.style.display = '';
        generateBudgetBtn.style.display = 'none';
        confirmBudgetBtn.style.display = 'none';
        chatJsonOutput.textContent = '';
    } else {
        generatePlanBtn.style.display = 'none';
        confirmPlanBtn.style.display = 'none';
        generateBudgetBtn.style.display = '';
        confirmBudgetBtn.style.display = '';
        chatJsonOutput.textContent = '';
    }
}

chatModeSelect.addEventListener('change', updateChatModeButtons);

// 页面加载时立即执行一次，以设置初始按钮状态
updateChatModeButtons();

// 生成行程计划
generatePlanBtn.addEventListener('click', async () => {
    // 改为调用后端 /travel-plan/generate 接口，传递 chatId
    const token = localStorage.getItem('token');
    if (!token) {
        alert('请先登录');
        return;
    }
    const chatId = currentChatId || crypto.randomUUID();
    try {
        const resp = await fetch(`${API_BASE_URL}/travel-plan/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ chatId })
        });
        const result = await resp.json();
        if (result.code === 200 && result.data) {
            lastPlanJson = result.data;
            if (renderPlanJson) {
                renderPlanJson(lastPlanJson, chatJsonOutput);
            } else {
                console.error('renderPlanJson is not available on window');
            }
            // 美观渲染 JSON 行程计划
            confirmPlanBtn.disabled = false;
        } else {
            chatJsonOutput.textContent = '后端未能生成有效行程计划。';
            confirmPlanBtn.disabled = true;
        }
    } catch (e) {
        chatJsonOutput.textContent = '生成行程计划失败：' + e.message;
        confirmPlanBtn.disabled = true;
    }
});

// 确认行程计划
confirmPlanBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        alert('请先登录');
        return;
    }
    if (!lastPlanJson) {
        alert('请先生成行程计划');
        return;
    }
    try {
        const resp = await fetch(`${API_BASE_URL}/travel-plan/confirm`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: lastPlanJson
        });
        const result = await resp.json();
        if (result.code === 200) {
            alert('行程计划已保存！');
            chatJsonOutput.textContent = '';
            confirmPlanBtn.disabled = true;
            fetchAndDisplayTravelPlans();
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (e) {
        alert('保存失败：' + e.message);
    }
});

// 生成预算分析弹窗逻辑
function openBudgetPlanModal() {
    // console.log('openBudgetPlanModal called'); // 移除调试日志
    budgetPlanSelectModal.classList.remove('hidden');
    // 加载所有行程计划
    budgetPlanList.innerHTML = '加载中...';
    const token = localStorage.getItem('token');
    fetch(`${API_BASE_URL}/travel-plan`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    }).then(resp => resp.json()).then(result => {
        if (result.code === 200 && result.data) {
            budgetPlanList.innerHTML = '';
            result.data.forEach(plan => {
                const div = document.createElement('div');
                div.className = 'budget-plan-item';
                div.innerHTML = `<input type="radio" name="budgetPlan" value="${plan.id}"> ${plan.planName} (${plan.destination})`;
                budgetPlanList.appendChild(div);
            });
        } else {
            budgetPlanList.innerHTML = '暂无行程计划';
        }
    });
}

closeBudgetModal.addEventListener('click', () => {
    budgetPlanSelectModal.classList.add('hidden');
});

// 生成预算分析
// 选择行程后弹窗关闭，自动调用后端生成预算分析接口
selectBudgetPlanBtn.addEventListener('click', async () => {
    const selected = document.querySelector('input[name="budgetPlan"]:checked');
    if (selected) {
        selectedBudgetPlanId = selected.value;
        budgetPlanSelectModal.classList.add('hidden');
        // 自动调用后端生成预算分析接口
        const token = localStorage.getItem('token');
        if (!token) {
            alert('请先登录');
            return;
        }
        try {
            // 这里假设后端 /travel-plan/{id}/budget/generate 返回预算分析 JSON
            const resp = await fetch(`${API_BASE_URL}/travel-plan/${selectedBudgetPlanId}/budget/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ planId: selectedBudgetPlanId })
            });
            const result = await resp.json();
            if (result.code === 200 && result.data) {
                lastBudgetJson = result.data;
                renderBudgetJson(lastBudgetJson);
                confirmBudgetBtn.disabled = false;
            } else {
                chatJsonOutput.textContent = '后端未能生成有效预算分析。';
                confirmBudgetBtn.disabled = true;
            }
        } catch (e) {
            chatJsonOutput.textContent = '生成预算分析失败：' + e.message;
            confirmBudgetBtn.disabled = true;
        }
    } else {
        alert('请选择一个行程计划');
    }
});

// 保存预算分析
confirmBudgetBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        alert('请先登录');
        return;
    }
    if (!lastBudgetJson || !selectedBudgetPlanId) {
        alert('请先生成预算分析并选择行程');
        return;
    }
    try {
        const resp = await fetch(`${API_BASE_URL}/travel-plan/${selectedBudgetPlanId}/budget`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: lastBudgetJson
        });
        const result = await resp.json();
        if (result.code === 200) {
            alert('预算分析已保存！');
            chatJsonOutput.textContent = '';
            confirmBudgetBtn.disabled = true;
            fetchAndDisplayTravelPlans();
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (e) {
        alert('保存失败：' + e.message);
    }
});

// 新增：美观渲染预算分析 JSON
function renderBudgetJson(jsonStr) {
    let budget;
    try {
        budget = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
    } catch (e) {
        chatJsonOutput.textContent = '预算分析解析失败：' + e.message;
        return;
    }
    // 结构化展示主要字段
    chatJsonOutput.innerHTML = `
        <div class="budget-card">
            <h3>预算分析结果</h3>
            <pre>${JSON.stringify(budget, null, 2)}</pre>
        </div>
    `;
}

// 保存预算分析
confirmBudgetBtn.addEventListener('click', async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        alert('请先登录');
        return;
    }
    if (!lastBudgetJson || !selectedBudgetPlanId) {
        alert('请先生成预算分析并选择行程');
        return;
    }
    try {
        const resp = await fetch(`${API_BASE_URL}/travel-plan/${selectedBudgetPlanId}/budget`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: lastBudgetJson
        });
        const result = await resp.json();
        if (result.code === 200) {
            alert('预算分析已保存！');
            chatJsonOutput.textContent = '';
            confirmBudgetBtn.disabled = true;
            fetchAndDisplayTravelPlans();
        } else {
            alert('保存失败：' + (result.message || '未知错误'));
        }
    } catch (e) {
        alert('保存失败：' + e.message);
    }
});