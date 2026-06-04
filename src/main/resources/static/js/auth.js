// Взима JWT токена от localStorage
function getToken() {
    return localStorage.getItem('jwt_token');
}

// Декодира payload-а на JWT (само за четене — не верифицира подписа)
function decodeToken() {
    const token = getToken();
    if (!token) return null;
    try {
        return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    } catch {
        return null;
    }
}

// Изгражда хедъри за автентикирани HTTP заявки
function authHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
    };
}

// Изход — изчиства localStorage и пренасочва към страницата за вход
function logout() {
    localStorage.clear();
    window.location.href = '/login';
}

// Проверява дали потребителят е влязъл и евентуално дали ролята съвпада
function checkAuth(expectedRole) {
    if (!getToken()) {
        window.location.href = '/login';
        return false;
    }
    if (expectedRole && localStorage.getItem('user_role') !== expectedRole) {
        window.location.href = '/login';
        return false;
    }
    return true;
}

// Показва съобщение за грешка в посочения елемент
function showError(elementId, message) {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = message;
        el.classList.remove('d-none');
    }
}

// Скрива съобщение за грешка
function hideError(elementId) {
    const el = document.getElementById(elementId);
    if (el) {
        el.classList.add('d-none');
    }
}

// Извлича текста на грешката от JSON отговора на сървъра
async function getErrorMessage(response) {
    try {
        const data = await response.json();
        if (data['съобщение']) return data['съобщение'];
        if (data['грешки']) return Object.values(data['грешки'])[0];
        return data.message || 'Неочаквана грешка от сървъра';
    } catch {
        return 'Грешка при обработка на отговора';
    }
}
