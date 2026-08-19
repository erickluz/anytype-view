async function getJson(url, options) {
    const response = await fetch(url, options);
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        const message = payload && payload.message ? payload.message : `HTTP ${response.status}`;
        throw new Error(message);
    }
    return payload;
}

async function loadStatus() {
    const [health, anytype, dashboard] = await Promise.all([
        getJson('/api/health'),
        getJson('/api/anytype/status'),
        getJson('/api/dashboard/preview')
    ]);

    document.getElementById('healthStatus').textContent = health.status;
    document.getElementById('spaceName').textContent = anytype.spaceName;
    document.getElementById('apiKeyStatus').textContent = anytype.apiKeyConfigured ? 'Configurada' : 'Nao configurada';
    renderDashboard(dashboard);
}

async function runSync() {
    const button = document.getElementById('syncButton');
    const message = document.getElementById('syncMessage');
    button.disabled = true;
    message.textContent = 'Executando...';

    try {
        const result = await getJson('/api/sync', { method: 'POST' });
        if (result.snapshot) {
            message.textContent = `${result.status}: ${result.message} ${result.snapshot.objectCount} objetos, ${result.snapshot.activityDays} dias com atividade inferida.`;
            const dashboard = await getJson('/api/dashboard/preview');
            renderDashboard(dashboard);
        } else {
            message.textContent = `${result.status}: ${result.message}`;
        }
    } catch (error) {
        message.textContent = `Erro ao executar sincronizacao: ${error.message}`;
    } finally {
        button.disabled = false;
    }
}

function renderDashboard(dashboard) {
    document.getElementById('previewNote').textContent = dashboard.note;
    renderSummary(dashboard.summary);
    renderBars('activityBars', dashboard.activity, '#2563eb');
    renderBars('conceptTrend', dashboard.conceptTrend, '#0f766e');
    renderUnderstanding(dashboard.understanding);
    renderTopics(dashboard.topics);
    renderCheckpoints(dashboard.checkpoints);
}

function renderSummary(items) {
    const container = document.getElementById('summaryGrid');
    container.innerHTML = items.map(item => `
        <article class="summary-card">
            <span>${item.caption}</span>
            <strong>${item.value}</strong>
            <em>${item.delta}</em>
            <span>${item.label}</span>
        </article>
    `).join('');
}

function renderBars(elementId, points, color) {
    const max = Math.max(...points.map(point => point.value), 1);
    const container = document.getElementById(elementId);
    container.innerHTML = points.map(point => {
        const height = Math.max((point.value / max) * 170, 8);
        return `
            <div class="bar-group">
                <div class="bar-value">${point.value}</div>
                <div class="bar" style="height: ${height}px; background: ${color};"></div>
                <div class="bar-label">${point.label}</div>
            </div>
        `;
    }).join('');
}

function renderUnderstanding(items) {
    const total = items.reduce((sum, item) => sum + item.value, 0);
    let cursor = 0;
    const segments = items.map(item => {
        const start = cursor;
        const size = total === 0 ? 0 : (item.value / total) * 360;
        cursor += size;
        return `${item.color} ${start}deg ${cursor}deg`;
    });

    document.getElementById('understandingDonut').style.background = `conic-gradient(${segments.join(', ')})`;
    document.getElementById('understandingLegend').innerHTML = items.map(item => `
        <div class="legend-item">
            <span><i class="legend-dot" style="background: ${item.color};"></i>${item.label}</span>
            <strong>${item.value}</strong>
        </div>
    `).join('');
}

function renderTopics(topics) {
    document.getElementById('topicTable').innerHTML = topics.map(topic => `
        <div class="topic-row">
            <div>
                <strong>${topic.name}</strong>
                <span class="topic-meta">${topic.concepts} conceitos</span>
            </div>
            <div>
                <div class="progress">
                    <div class="progress-fill" style="width: ${topic.progressPercent}%;"></div>
                </div>
                <span class="topic-meta">${topic.progressPercent}% de maturidade percebida</span>
            </div>
            <div class="topic-meta">
                ${topic.lowUnderstanding} em nivel baixo<br>
                ${topic.daysSinceCheckpoint} dias sem checkpoint
            </div>
        </div>
    `).join('');
}

function renderCheckpoints(checkpoints) {
    document.getElementById('checkpointList').innerHTML = checkpoints.map(checkpoint => `
        <div class="checkpoint-item">
            <div>
                <strong>${checkpoint.topic}</strong>
                <span>${checkpoint.age} sem revisao · ${checkpoint.perceivedLevel}</span>
            </div>
            <strong>${checkpoint.sellability}/10</strong>
        </div>
    `).join('');
}

document.getElementById('syncButton').addEventListener('click', runSync);
loadStatus().catch(error => {
    document.getElementById('healthStatus').textContent = 'Erro';
    document.getElementById('syncMessage').textContent = `Erro ao carregar status: ${error.message}`;
});
