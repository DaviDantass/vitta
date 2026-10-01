document.addEventListener('DOMContentLoaded', function () {
    const specialtySelect = document.getElementById('specialty');
    const doctorSelect = document.getElementById('doctorId');

    if (specialtySelect && doctorSelect) {
        async function loadDoctors(selectedId = '') {
            doctorSelect.innerHTML = '<option value="">Select a doctor</option>';
            if (!specialtySelect.value) return;

            try {
                const response = await fetch('/doctors/' + encodeURIComponent(specialtySelect.value));
                if (!response.ok) throw new Error('Unable to load doctors');
                const doctors = await response.json();
                doctors.forEach(doctor => {
                    const option = document.createElement('option');
                    option.value = doctor.id;
                    option.textContent = doctor.name;
                    option.selected = String(doctor.id) === String(selectedId);
                    doctorSelect.appendChild(option);
                });
            } catch (error) {
                console.error('Error loading doctors:', error);
            }
        }
        specialtySelect.addEventListener('change', () => loadDoctors());
        loadDoctors(doctorSelect.dataset.selectedId);
    }

    document.querySelectorAll('.pagination a.disabled').forEach(link => {
        link.addEventListener('click', event => event.preventDefault());
    });

    const modal = document.getElementById('deleteModal');
    if (modal) {
        document.querySelectorAll('[href="#deleteModal"]').forEach(trigger => {
            trigger.addEventListener('click', event => {
                event.preventDefault();
                modal.querySelector('input[name="id"]').value = trigger.dataset.id;
                modal.querySelector('form').action = trigger.dataset.url;
                modal.style.display = 'block';
            });
        });
        modal.querySelectorAll('[data-dismiss="modal"]').forEach(button => {
            button.addEventListener('click', () => { modal.style.display = 'none'; });
        });
        modal.addEventListener('click', event => {
            if (event.target === modal) modal.style.display = 'none';
        });
    }
});
