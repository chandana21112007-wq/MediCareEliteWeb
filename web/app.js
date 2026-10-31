const API = "/api";

async function request(url, options = {}) {
    const response = await fetch(url, options);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "Something went wrong");
    }

    return data;
}

function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.classList.add("show");

    setTimeout(() => {
        toast.classList.remove("show");
    }, 3000);
}

async function loadDashboard() {
    try {
        const [patients, doctors, appointments] = await Promise.all([
            request(`${API}/patients`),
            request(`${API}/doctors`),
            request(`${API}/appointments`)
        ]);

        document.getElementById("patientCount").textContent = patients.length;
        document.getElementById("doctorCount").textContent = doctors.length;
        document.getElementById("appointmentCount").textContent = appointments.length;

        document.getElementById("activeCount").textContent =
            appointments.filter(a => a.status === "Booked").length;

        loadPatientTable(patients);
        loadPatientOptions(patients);
        loadDoctorOptions(doctors);
        displayDoctors(doctors);
        loadAppointmentTable(appointments);

    } catch (error) {
        showToast(error.message);
    }
}

// Display patient details
function loadPatientTable(patients) {
    const table = document.getElementById("patientTable");

    table.innerHTML = patients.map(p => `
        <tr>
            <td>${p.id}</td>
            <td>${escapeHTML(p.name)}</td>
            <td>${p.age}</td>
            <td>${escapeHTML(p.gender)}</td>
            <td>${escapeHTML(p.phone)}</td>
        </tr>
    `).join("");
}

// Fill patient dropdown
function loadPatientOptions(patients) {
    const select = document.getElementById("patientSelect");

    select.innerHTML = '<option value="">Select patient</option>';

    patients.forEach(p => {
        select.innerHTML += `
            <option value="${p.id}">
                ${escapeHTML(p.name)} (ID: ${p.id})
            </option>
        `;
    });
}

// Fill doctor dropdown
function loadDoctorOptions(doctors) {
    const select = document.getElementById("doctorSelect");

    select.innerHTML = '<option value="">Select doctor</option>';

    doctors.forEach(d => {
        select.innerHTML += `
            <option value="${d.id}">
                ${escapeHTML(d.name)} - ${escapeHTML(d.department)}
            </option>
        `;
    });
}

// Display doctor cards
function displayDoctors(doctors) {
    const grid = document.getElementById("doctorGrid");

    if (doctors.length === 0) {
        grid.innerHTML = "<p>No doctors found.</p>";
        return;
    }

    grid.innerHTML = doctors.map(d => `
        <div class="doctor-card">
            <div class="doctor-avatar">Dr</div>
            <h3>${escapeHTML(d.name)}</h3>
            <p>Doctor ID: ${d.id}</p>
            <p>Qualification: ${escapeHTML(d.qualification)}</p>
            <span class="department-tag">
                ${escapeHTML(d.department)}
            </span>
        </div>
    `).join("");
}

// Search doctors by name or department
async function loadDoctors() {
    try {
        const doctors = await request(`${API}/doctors`);
        const keyword = document.getElementById("doctorSearch")
            .value.trim().toLowerCase();

        const filtered = doctors.filter(d =>
            d.name.toLowerCase().includes(keyword) ||
            d.department.toLowerCase().includes(keyword)
        );

        displayDoctors(filtered);

    } catch (error) {
        showToast(error.message);
    }
}

function resetDoctorSearch() {
    document.getElementById("doctorSearch").value = "";
    loadDoctors();
}

// Display appointments
function loadAppointmentTable(appointments) {
    const table = document.getElementById("appointmentTable");

    if (appointments.length === 0) {
        table.innerHTML =
            '<tr><td colspan="8">No appointments available.</td></tr>';
        return;
    }

    table.innerHTML = appointments.map(a => `
        <tr>
            <td>${a.id}</td>
            <td>${escapeHTML(a.patient)}</td>
            <td>${escapeHTML(a.doctor)}</td>
            <td>${escapeHTML(a.department)}</td>
            <td>${escapeHTML(a.date)}</td>
            <td>${escapeHTML(a.time)}</td>
            <td>
                <span class="status ${a.status === "Booked" ? "booked" : "cancelled"}">
                    ${escapeHTML(a.status)}
                </span>
            </td>
            <td>
                ${a.status === "Booked"
                    ? `<button class="cancel-btn"
                        onclick="cancelAppointment(${a.id})">Cancel</button>`
                    : "-"}
            </td>
        </tr>
    `).join("");
}

// Register patient
document.getElementById("patientForm")
    .addEventListener("submit", async function(event) {

        event.preventDefault();

        try {
            const formData = new URLSearchParams(
                new FormData(this)
            );

            const result = await request(`${API}/patients`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            showToast(result.message);
            this.reset();
            await loadDashboard();

        } catch (error) {
            showToast(error.message);
        }
    });

// Book appointment
document.getElementById("appointmentForm")
    .addEventListener("submit", async function(event) {

        event.preventDefault();

        try {
            const formData = new URLSearchParams(
                new FormData(this)
            );

            const result = await request(`${API}/appointments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: formData
            });

            showToast(result.message);
            this.reset();
            await loadDashboard();

        } catch (error) {
            showToast(error.message);
        }
    });

// Cancel appointment
async function cancelAppointment(id) {
    if (!confirm("Are you sure you want to cancel this appointment?")) {
        return;
    }

    try {
        const result = await request(`${API}/appointments/${id}`, {
            method: "DELETE"
        });

        showToast(result.message);
        await loadDashboard();

    } catch (error) {
        showToast(error.message);
    }
}

// Load data when page opens
document.addEventListener("DOMContentLoaded", loadDashboard);
