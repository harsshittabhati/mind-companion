// ── Theme management ──────────────────────────────
function applyTheme(theme) {
    if (theme === 'dark') {
        document.body.classList.add('dark');
    } else {
        document.body.classList.remove('dark');
    }
    // Update toggle icon if present
    const icon = document.getElementById('themeIcon');
    if (icon) {
        icon.className = theme === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
    }
}

function toggleTheme() {
    const isDark = document.body.classList.contains('dark');
    const newTheme = isDark ? 'light' : 'dark';
    applyTheme(newTheme);

    // Save to server
    const token = localStorage.getItem('token');
    if (token) {
        fetch('/api/user/privacy', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ themePreference: newTheme })
        }).catch(err => console.warn('Failed to save theme:', err));
    }
}

function initTheme() {
    const token = localStorage.getItem('token');
    if (!token) return;
    fetch('/api/user/me', { headers: { 'Authorization': 'Bearer ' + token } })
        .then(r => r.ok ? r.json() : null)
        .then(data => {
            if (data && data.themePreference) {
                applyTheme(data.themePreference);
            }
        })
        .catch(() => {});
}

// Apply theme immediately on load
initTheme();