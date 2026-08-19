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
    document.getElementById('configuredSpaceName').textContent = anytype.spaceName;
    document.getElementById('apiKeyStatus').textContent = anytype.apiKeyConfigured ? 'Configurada' : 'Nao configurada';
    renderDashboard(dashboard);
}

async function runSync() {
    const button = document.getElementById('syncButton');
    const message = document.getElementById('syncMessage');
    button.disabled = true;
    button.textContent = 'Sincronizando';
    message.textContent = 'Executando...';
    showSyncToast('Sincronização', 'Executando sincronização com o Anytype.', 'info');

    try {
        const result = await getJson('/api/sync', { method: 'POST' });
        if (result.snapshot) {
            message.textContent = `${result.status}: ${result.message} ${result.snapshot.objectCount} objetos, ${result.snapshot.activityDays} dias com atividade inferida.`;
            showSyncToast('Snapshot salvo', message.textContent, 'success');
            const dashboard = await getJson('/api/dashboard/preview');
            renderDashboard(dashboard);
        } else {
            message.textContent = `${result.status}: ${result.message}`;
            showSyncToast('Sincronização', message.textContent, result.status === 'SCHEMA_INVALID' ? 'warning' : 'info');
        }
    } catch (error) {
        message.textContent = `Erro ao executar sincronizacao: ${error.message}`;
        showSyncToast('Erro na sincronização', message.textContent, 'danger');
    } finally {
        button.disabled = false;
        button.textContent = 'Sincronizar';
    }
}

function showPage(pageId) {
    const targetPage = pageId === 'system-status' ? 'system-status' : 'dashboard';
    document.querySelectorAll('.page-section').forEach(section => {
        section.classList.toggle('d-none', section.id !== targetPage);
    });
    document.querySelectorAll('[data-page-link]').forEach(link => {
        link.classList.toggle('active', link.dataset.pageLink === targetPage);
    });
}

function showSyncToast(title, body, tone) {
    const toastElement = document.getElementById('syncToast');
    const dot = toastElement.querySelector('.sync-toast-dot');
    const toneClass = `sync-toast-${tone}`;
    dot.className = `rounded me-2 sync-toast-dot ${toneClass}`;
    document.getElementById('syncToastTitle').textContent = title;
    document.getElementById('syncToastBody').textContent = body;
    document.getElementById('syncToastTime').textContent = new Date().toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit'
    });

    const toast = coreui.Toast.getOrCreateInstance(toastElement, { delay: 8000 });
    toast.show();
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
    container.innerHTML = items.map((item, index) => `
        <div class="col-12 col-sm-6 col-xl-3">
            <article class="card summary-card summary-card-tone-${(index % 4) + 1}">
                <div class="card-body">
                    <span class="summary-caption">${item.caption}</span>
                    <strong class="summary-value">${item.value}</strong>
                    <div>
                        <span class="summary-delta">${item.delta}</span>
                        <span class="summary-label d-block">${item.label}</span>
                    </div>
                </div>
            </article>
        </div>
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
document.querySelectorAll('[data-page-link]').forEach(link => {
    link.addEventListener('click', event => {
        event.preventDefault();
        const pageId = event.currentTarget.dataset.pageLink;
        history.replaceState(null, '', `#${pageId}`);
        showPage(pageId);
    });
});
document.querySelectorAll('[data-dashboard-target]').forEach(link => {
    link.addEventListener('click', event => {
        event.preventDefault();
        const targetId = event.currentTarget.dataset.dashboardTarget;
        history.replaceState(null, '', `#${targetId}`);
        showPage('dashboard');
        document.getElementById(targetId).scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
});

const initialHash = window.location.hash.replace('#', '');
showPage(initialHash === 'system-status' ? 'system-status' : 'dashboard');
loadStatus().catch(error => {
    document.getElementById('healthStatus').textContent = 'Erro';
    document.getElementById('syncMessage').textContent = `Erro ao carregar status: ${error.message}`;
    showSyncToast('Erro ao carregar status', error.message, 'danger');
});
