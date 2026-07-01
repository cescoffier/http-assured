document.addEventListener('DOMContentLoaded', function () {
    initVersionSwitcher();
    initCopyButtons();
    initTableOfContents();
});

/* === Version Switcher === */
function initVersionSwitcher() {
    var btn = document.getElementById('version-btn');
    var dropdown = document.getElementById('version-dropdown');
    if (!btn || !dropdown) return;

    fetch('/versions.json')
        .then(function (r) { return r.json(); })
        .then(function (data) {
            btn.textContent = findCurrentLabel(data) || 'Version';
            dropdown.innerHTML = data.versions.map(function (v) {
                return '<a href="' + v.url + '" role="menuitem">' + v.label + '</a>';
            }).join('');
        })
        .catch(function () {
            btn.style.display = 'none';
        });

    btn.addEventListener('click', function () {
        var open = dropdown.classList.toggle('open');
        btn.setAttribute('aria-expanded', String(open));
    });

    document.addEventListener('click', function (e) {
        if (!e.target.closest('#version-switcher')) {
            dropdown.classList.remove('open');
            btn.setAttribute('aria-expanded', 'false');
        }
    });
}

function findCurrentLabel(data) {
    var path = window.location.pathname;
    for (var i = 0; i < data.versions.length; i++) {
        if (path.startsWith(data.versions[i].url)) {
            return data.versions[i].label;
        }
    }
    return null;
}

/* === Copy Buttons === */
function initCopyButtons() {
    var blocks = document.querySelectorAll('.content pre');
    blocks.forEach(function (pre) {
        var btn = document.createElement('button');
        btn.className = 'copy-btn';
        btn.textContent = 'Copy';
        btn.addEventListener('click', function () {
            var code = pre.querySelector('code');
            var text = code ? code.textContent : pre.textContent;
            navigator.clipboard.writeText(text).then(function () {
                btn.textContent = 'Copied!';
                setTimeout(function () { btn.textContent = 'Copy'; }, 2000);
            });
        });
        pre.style.position = 'relative';
        pre.appendChild(btn);
    });
}

/* === Table of Contents === */
function initTableOfContents() {
    var tocList = document.getElementById('toc-links');
    if (!tocList) return;

    var headings = document.querySelectorAll('.content h2, .content h3');
    headings.forEach(function (h) {
        if (!h.id) {
            h.id = h.textContent.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-');
        }
        var li = document.createElement('li');
        var a = document.createElement('a');
        a.href = '#' + h.id;
        a.textContent = h.textContent;
        if (h.tagName === 'H3') {
            a.style.paddingLeft = '0.75rem';
        }
        li.appendChild(a);
        tocList.appendChild(li);
    });
}
