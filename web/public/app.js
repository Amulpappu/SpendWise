// SpendWise Web Portal Client Logic
document.addEventListener('DOMContentLoaded', () => {
  // 1. Fetch latest app metadata from API
  fetch('/api/app-info')
    .then(res => res.json())
    .then(data => {
      console.log('SpendWise App Info:', data);
      const metaVersion = document.querySelector('.hero-meta-row .meta-item:nth-child(1) strong');
      const metaSize = document.querySelector('.hero-meta-row .meta-item:nth-child(2) strong');
      if (metaVersion && data.version) {
        metaVersion.textContent = `${data.version} (Latest)`;
      }
      if (metaSize && data.fileSize) {
        metaSize.textContent = data.fileSize;
      }
    })
    .catch(err => {
      console.log('Local fallback info active.');
    });

  // 2. Interactive Statement Password Rule Switcher
  const ruleChips = document.querySelectorAll('.rule-chip');
  const previewFooter = document.querySelector('.sp-footer strong');

  const passwordExamples = {
    0: 'LOHI5779', // Name + A/C
    1: 'LOHI2741', // Name + Mobile
    2: 'HITH6379'  // Name (End) + Mobile (Start)
  };

  ruleChips.forEach((chip, index) => {
    chip.addEventListener('click', () => {
      ruleChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      if (previewFooter && passwordExamples[index]) {
        previewFooter.textContent = passwordExamples[index];
      }
    });
  });

  // 3. Smooth scrolling for anchors
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
      const targetId = this.getAttribute('href');
      if (targetId && targetId !== '#') {
        e.preventDefault();
        const target = document.querySelector(targetId);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }
    });
  });

  // 4. Download click micro-feedback
  const downloadBtns = document.querySelectorAll('a[href="/download"]');
  downloadBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const originalText = btn.innerHTML;
      btn.style.opacity = '0.7';
      setTimeout(() => {
        btn.style.opacity = '1';
      }, 1000);
    });
  });
});
