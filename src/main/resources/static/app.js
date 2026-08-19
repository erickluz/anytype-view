async function getJson(url, options) {
    const response = await fetch(url, options);
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        const message = payload && payload.message ? payload.message : `HTTP ${response.status}`;
        throw new Error(message);
    }
    return payload;
}

const PAGE_IDS = ['dashboard', 'checkpoints-page', 'topics-page', 'concepts-page', 'system-status'];
const knowledgeViews = {};

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
            Object.keys(knowledgeViews).forEach(key => delete knowledgeViews[key]);
            const currentPage = window.location.hash.replace('#', '');
            if (['checkpoints-page', 'topics-page', 'concepts-page'].includes(currentPage)) {
                loadKnowledgeView(currentPage);
            }
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
    const targetPage = PAGE_IDS.includes(pageId) ? pageId : 'dashboard';
    document.querySelectorAll('.page-section').forEach(section => {
        section.classList.toggle('d-none', section.id !== targetPage);
    });
    document.querySelectorAll('[data-page-link]').forEach(link => {
        link.classList.toggle('active', link.dataset.pageLink === targetPage);
    });
    if (['checkpoints-page', 'topics-page', 'concepts-page'].includes(targetPage)) {
        loadKnowledgeView(targetPage);
    }
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
    renderProblemIndicators(dashboard.problemIndicators || []);
    renderSummary(dashboard.summary);
    renderBars('activityBars', dashboard.activity, '#2563eb');
    renderActivityHistory(dashboard.activityHistory || []);
    renderBars('conceptTrend', dashboard.conceptTrend, '#0f766e');
    renderUnderstanding(dashboard.understanding);
    renderTopics(dashboard.topics);
    renderCheckpoints(dashboard.checkpoints);
}

function renderActivityHistory(points) {
    const container = document.getElementById('activityHistory');
    if (!points.length) {
        container.innerHTML = '<p class="text-body-secondary mb-0">Ainda não há histórico de atividade.</p>';
        return;
    }

    const values = points.map(point => point.value).filter(value => value > 0).sort((a, b) => a - b);
    const levelFor = value => {
        if (value === 0 || values.length === 0) return 0;
        const percentile = values.indexOf(value) / values.length;
        return Math.min(4, Math.floor(percentile * 4) + 1);
    };
    const firstDate = new Date(`${points[0].date}T00:00:00`);
    const offset = firstDate.getDay();
    const cells = [...Array(offset).fill(null), ...points];
    const weeks = Array.from({ length: Math.ceil(cells.length / 7) }, (_, index) => cells.slice(index * 7, index * 7 + 7));
    const monthLabels = [];
    let previousMonth = -1;

    const grid = weeks.map((week, weekIndex) => `<div class="contribution-week">${week.map(point => {
        if (!point) return '<span class="contribution-day contribution-day-empty" aria-hidden="true"></span>';
        const date = new Date(`${point.date}T00:00:00`);
        const month = date.getMonth();
        if (month !== previousMonth) {
            monthLabels.push({ week: weekIndex, label: date.toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '') });
            previousMonth = month;
        }
        const label = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
        const count = point.value;
        return `<span class="contribution-day contribution-level-${levelFor(count)}" tabindex="0" data-tooltip="${label}: ${count} ${count === 1 ? 'alteração inferida' : 'alterações inferidas'}" aria-label="${label}: ${count} alterações inferidas"></span>`;
    }).join('')}</div>`).join('');

    const labels = monthLabels.map(item => `<span style="grid-column: ${item.week + 1}">${item.label}</span>`).join('');
    container.innerHTML = `
        <div class="contribution-scroll">
            <div class="contribution-months">${labels}</div>
            <div class="contribution-content">
                <div class="contribution-weekdays" aria-hidden="true"><span></span><span>Seg</span><span></span><span>Qua</span><span></span><span>Sex</span><span></span></div>
                <div class="contribution-grid">${grid}</div>
            </div>
            <div class="contribution-footer"><span>Menos</span><i class="contribution-day contribution-level-0"></i><i class="contribution-day contribution-level-1"></i><i class="contribution-day contribution-level-2"></i><i class="contribution-day contribution-level-3"></i><i class="contribution-day contribution-level-4"></i><span>Mais</span></div>
        </div>
    `;
}

function renderProblemIndicators(items) {
    const container = document.getElementById('problemIndicators');
    const currentReadings = items.slice(0, 3);
    const volatility = items[3];
    container.innerHTML = currentReadings.map(item => `
        <article class="current-reading current-reading-${item.tone}">
            <div class="d-flex align-items-baseline justify-content-between gap-3">
                <span class="problem-label">${item.label}</span>
                <strong class="current-reading-value">${item.value}</strong>
            </div>
            <p class="problem-context mb-0">${item.context}</p>
        </article>
    `).join('');

    const volatilityContainer = document.getElementById('volatilityIndicator');
    volatilityContainer.innerHTML = volatility ? `
        <span class="volatility-label">${volatility.label}: <strong>${volatility.value}</strong></span>
        <span>${volatility.context}</span>
    ` : '';
}

function renderSummary(items) {
    const container = document.getElementById('summaryGrid');
    container.innerHTML = items.map(item => `
        <div class="col-6">
            <article class="card summary-card">
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
            <div class="topic-name">
                <strong>${topic.name}</strong>
                <span class="topic-meta">${topic.concepts} conceitos</span>
            </div>
            <div class="topic-primary-progress">
                <div class="d-flex justify-content-between gap-2">
                    <span class="topic-progress-label">Maturidade</span>
                    <strong>${topic.progressPercent}%</strong>
                </div>
                <div class="progress">
                    <div class="progress-fill" style="width: ${topic.progressPercent}%;"></div>
                </div>
                <span class="topic-meta">${topic.matureConcepts} de ${topic.concepts} em nível intermediário ou forte</span>
            </div>
            <div class="topic-details">
                ${renderTopicDetail('Iniciado', topic.initiatedConcepts, topic.initiatedPercent)}
                ${renderTopicDetail('Forte', topic.strongConcepts, topic.strongPercent)}
                ${renderTopicDetail('Com checkpoint', topic.checkpointCoveredConcepts, topic.checkpointCoveragePercent)}
                <span class="topic-meta">${topic.lowUnderstanding} em nível baixo · ${checkpointRecency(topic.daysSinceCheckpoint)}</span>
            </div>
        </div>
    `).join('');
}

function renderTopicDetail(label, count, percent) {
    return `
        <span>${label} <strong>${percent}%</strong> <small>(${count})</small></span>
    `;
}

function checkpointRecency(daysSinceCheckpoint) {
    if (daysSinceCheckpoint < 0) {
        return 'sem checkpoint encontrado';
    }
    if (daysSinceCheckpoint === 0) {
        return 'checkpoint trabalhado hoje';
    }
    return `${daysSinceCheckpoint} dias desde checkpoint`;
}

function renderCheckpoints(checkpoints) {
    document.getElementById('checkpointList').innerHTML = checkpoints.map(checkpoint => `
        <div class="checkpoint-item">
            <div>
                <strong>${checkpoint.topic}</strong>
                <span>Trabalhado em ${checkpoint.workedAt} · ${checkpoint.perceivedLevel} · há ${checkpoint.age}</span>
            </div>
            <div class="checkpoint-score">
                <span>Vendabilidade</span>
                <strong>${checkpoint.sellability}/10</strong>
            </div>
        </div>
    `).join('');
}

async function loadKnowledgeView(pageId) {
    if (knowledgeViews[pageId]) {
        renderKnowledgeView(pageId, knowledgeViews[pageId]);
        return;
    }

    const endpoint = {
        'checkpoints-page': '/api/dashboard/checkpoints',
        'topics-page': '/api/dashboard/topics',
        'concepts-page': '/api/dashboard/concepts'
    }[pageId];
    try {
        const view = await getJson(endpoint);
        knowledgeViews[pageId] = view;
        renderKnowledgeView(pageId, view);
    } catch (error) {
        const note = document.getElementById({
            'checkpoints-page': 'checkpointsNote',
            'topics-page': 'topicsNote',
            'concepts-page': 'conceptsNote'
        }[pageId]);
        note.textContent = `Erro ao carregar dados: ${error.message}`;
        showSyncToast('Erro ao carregar dados', error.message, 'danger');
    }
}

function renderKnowledgeView(pageId, view) {
    if (pageId === 'checkpoints-page') {
        document.getElementById('checkpointsNote').textContent = view.note;
        renderDetailMetrics('checkpointMetrics', view.metrics);
        renderCheckpointDetails(view.items);
    }
    if (pageId === 'topics-page') {
        document.getElementById('topicsNote').textContent = view.note;
        renderDetailMetrics('topicMetrics', view.metrics);
        renderTopicDetails(view.items);
    }
    if (pageId === 'concepts-page') {
        document.getElementById('conceptsNote').textContent = view.note;
        renderDetailMetrics('conceptMetrics', view.metrics);
        renderConceptDetails(view.items);
    }
}

function renderDetailMetrics(elementId, metrics) {
    document.getElementById(elementId).innerHTML = metrics.map(metric => `
        <div class="col-6 col-md-3">
            <article class="card detail-metric">
                <div class="card-body">
                    <span>${metric.label}</span>
                    <strong>${metric.value}</strong>
                    <small>${metric.context}</small>
                </div>
            </article>
        </div>
    `).join('');
}

function renderCheckpointDetails(items) {
    const query = normalizeText(document.getElementById('checkpointSearch').value);
    const filter = document.getElementById('checkpointFilter').value;
    const filtered = items.filter(item => {
        const matchesQuery = !query || normalizeText(`${item.name} ${item.topic} ${item.status}`).includes(query);
        const matchesFilter = filter === 'all'
            || (filter === 'gaps' && item.hasGaps)
            || (filter === 'no-application' && !item.hasPracticalApplication);
        return matchesQuery && matchesFilter;
    });
    document.getElementById('checkpointResultCount').textContent = `${filtered.length} de ${items.length} checkpoints`;
    document.getElementById('checkpointDetailList').innerHTML = filtered.map(item => `
        <article class="detail-row checkpoint-detail-row">
            <div class="detail-main">
                <strong>${item.topic}</strong>
                <span>${item.name}</span>
            </div>
            <div class="detail-secondary">Trabalhado em ${item.workedAt} · ${activityAge(item.daysSinceWorked)}</div>
            <div class="detail-tags">
                <span class="insight-badge">${item.perceivedLevel}</span>
                <span class="insight-badge">Vendabilidade ${item.sellability}/10</span>
                <span class="insight-badge">${item.connectedConcepts} conceitos ligados</span>
                ${item.hasGaps ? '<span class="insight-badge insight-badge-warning">Com lacunas</span>' : ''}
                ${item.hasPracticalApplication ? '<span class="insight-badge insight-badge-success">Aplicação prática</span>' : ''}
            </div>
        </article>
    `).join('') || emptyListMessage('Nenhum checkpoint corresponde aos filtros atuais.');
}

function renderTopicDetails(items) {
    const query = normalizeText(document.getElementById('topicSearch').value);
    const filter = document.getElementById('topicFilter').value;
    const filtered = items.filter(item => {
        const matchesQuery = !query || normalizeText(item.name).includes(query);
        const matchesFilter = filter === 'all'
            || (filter === 'low' && item.lowUnderstanding > 0)
            || (filter === 'no-checkpoint' && item.concepts > 0 && item.checkpointCount === 0)
            || (filter === 'subtopic' && normalizeText(item.type) === 'subtema');
        return matchesQuery && matchesFilter;
    });
    document.getElementById('topicResultCount').textContent = `${filtered.length} de ${items.length} temas e subtemas`;
    document.getElementById('topicDetailList').innerHTML = filtered.map(item => `
        <article class="detail-row topic-detail-row">
            <div class="detail-main">
                <strong>${item.name}</strong>
                <span>${item.type}${item.priority === null ? '' : ` · prioridade ${item.priority}`}</span>
            </div>
            <div class="detail-progress">
                <div class="d-flex justify-content-between gap-2">
                    <span>Maturidade</span><strong>${item.maturityPercent}%</strong>
                </div>
                <div class="progress"><div class="progress-fill" style="width: ${item.maturityPercent}%;"></div></div>
                <small>${item.matureConcepts} de ${item.concepts} em nível intermediário ou forte</small>
            </div>
            <div class="detail-tags">
                <span class="insight-badge">${item.concepts} conceitos</span>
                <span class="insight-badge">${item.lowUnderstanding} em nível baixo</span>
                <span class="insight-badge">${item.checkpointCount} checkpoints</span>
                <span class="insight-badge">${item.subtopics} subtemas</span>
                <span class="insight-badge">${checkpointRecency(item.daysSinceCheckpoint)}</span>
            </div>
        </article>
    `).join('') || emptyListMessage('Nenhum tema corresponde aos filtros atuais.');
}

function renderConceptDetails(items) {
    const query = normalizeText(document.getElementById('conceptSearch').value);
    const understandingFilter = document.getElementById('conceptUnderstandingFilter').value;
    const checkpointFilter = document.getElementById('conceptCheckpointFilter').value;
    const filtered = items.filter(item => {
        const searchText = `${item.name} ${item.topics.join(' ')}`;
        const matchesQuery = !query || normalizeText(searchText).includes(query);
        const low = isLowUnderstanding(item.understanding);
        const mature = !low;
        const matchesUnderstanding = understandingFilter === 'all'
            || (understandingFilter === 'low' && low)
            || (understandingFilter === 'mature' && mature);
        const matchesCheckpoint = checkpointFilter === 'all'
            || (checkpointFilter === 'with' && item.hasCheckpoint)
            || (checkpointFilter === 'without' && !item.hasCheckpoint);
        return matchesQuery && matchesUnderstanding && matchesCheckpoint;
    });
    document.getElementById('conceptResultCount').textContent = `${filtered.length} de ${items.length} conceitos`;
    document.getElementById('conceptDetailList').innerHTML = filtered.map(item => `
        <article class="detail-row concept-detail-row">
            <div class="detail-main">
                <strong>${item.name}</strong>
                <span>${item.topics.length ? item.topics.join(' · ') : 'Sem tema associado'}</span>
            </div>
            <div class="detail-tags">
                <span class="insight-badge ${isLowUnderstanding(item.understanding) ? 'insight-badge-warning' : 'insight-badge-success'}">${item.understanding}</span>
                <span class="insight-badge">Veredito ${item.verdict}</span>
                ${item.priority === null ? '' : `<span class="insight-badge">Prioridade ${item.priority}</span>`}
                <span class="insight-badge">${item.hasCheckpoint ? 'Com checkpoint' : 'Sem checkpoint'}</span>
            </div>
            <div class="detail-secondary">Alterado em ${item.lastModifiedAt} · ${activityAge(item.daysSinceActivity)}</div>
        </article>
    `).join('') || emptyListMessage('Nenhum conceito corresponde aos filtros atuais.');
}

function emptyListMessage(message) {
    return `<p class="detail-empty mb-0">${message}</p>`;
}

function normalizeText(value) {
    return (value || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim()
        .toLowerCase();
}

function isLowUnderstanding(value) {
    const normalized = normalizeText(value);
    return normalized.includes('desconhecido') || normalized.includes('basico');
}

function activityAge(days) {
    if (days < 0) {
        return 'sem data de atividade';
    }
    if (days === 0) {
        return 'trabalhado hoje';
    }
    return `há ${days} dias`;
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

[
    ['checkpointSearch', 'input', () => renderCheckpointDetails(knowledgeViews['checkpoints-page'].items)],
    ['checkpointFilter', 'change', () => renderCheckpointDetails(knowledgeViews['checkpoints-page'].items)],
    ['topicSearch', 'input', () => renderTopicDetails(knowledgeViews['topics-page'].items)],
    ['topicFilter', 'change', () => renderTopicDetails(knowledgeViews['topics-page'].items)],
    ['conceptSearch', 'input', () => renderConceptDetails(knowledgeViews['concepts-page'].items)],
    ['conceptUnderstandingFilter', 'change', () => renderConceptDetails(knowledgeViews['concepts-page'].items)],
    ['conceptCheckpointFilter', 'change', () => renderConceptDetails(knowledgeViews['concepts-page'].items)]
].forEach(([elementId, eventName, handler]) => {
    document.getElementById(elementId).addEventListener(eventName, () => {
        const pageId = elementId.startsWith('checkpoint')
            ? 'checkpoints-page'
            : elementId.startsWith('topic')
                ? 'topics-page'
                : 'concepts-page';
        if (knowledgeViews[pageId]) {
            handler();
        }
    });
});

const initialHash = window.location.hash.replace('#', '');
showPage(PAGE_IDS.includes(initialHash) ? initialHash : 'dashboard');
window.addEventListener('hashchange', () => {
    const pageId = window.location.hash.replace('#', '');
    showPage(PAGE_IDS.includes(pageId) ? pageId : 'dashboard');
});
loadStatus().catch(error => {
    document.getElementById('healthStatus').textContent = 'Erro';
    document.getElementById('syncMessage').textContent = `Erro ao carregar status: ${error.message}`;
    showSyncToast('Erro ao carregar status', error.message, 'danger');
});
