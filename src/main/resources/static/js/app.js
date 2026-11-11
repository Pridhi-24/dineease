// Small progressive enhancements. The app works without JavaScript except these conveniences.
document.addEventListener('click', e => {
  if (e.target.closest('[data-burger]')) document.getElementById('nl').classList.toggle('open');
  const eye = e.target.closest('[data-eye]');
  if (eye) { const i = eye.parentElement.querySelector('input'); i.type = i.type === 'password' ? 'text' : 'password'; }
});
document.addEventListener('change', e => {
  if (e.target.matches('.opt input')) { // highlight chosen seating card
    document.querySelectorAll('.opt').forEach(o => o.classList.remove('sel'));
    e.target.closest('.opt').classList.add('sel');
  }
});
document.addEventListener('submit', e => { // <form data-confirm="Are you sure?">
  const msg = e.target.dataset.confirm;
  if (msg && !confirm(msg)) e.preventDefault();
});
document.querySelectorAll('.toast').forEach(t => setTimeout(() => t.remove(), 3500));
