/* ===================================================================
   Mari-Fariyaad - GVP Campus Complaint Portal
   Core Interactive Application Logic (Vanilla JavaScript)
   Talks to the real Spring Boot + MySQL backend (session-based auth).
   =================================================================== */

document.addEventListener("DOMContentLoaded", () => {
  initTheme();
  initLanguage();
  bindGlobalEvents();
  refreshAuthNav();
});

// Current active language
let currentLang = localStorage.getItem("gvp_lang") || "gu"; // Default Gujarati
let translations = {};

/* ===================== Small fetch helpers ===================== */

async function apiRequest(url, options = {}) {
  const res = await fetch(url, {
    credentials: "same-origin",
    headers: { "X-Requested-With": "XMLHttpRequest", ...(options.headers || {}) },
    ...options,
  });
  let data = null;
  try {
    data = await res.json();
  } catch (e) {
    data = null;
  }
  if (!res.ok) {
    const message = (data && data.message) || `Request failed (${res.status})`;
    const err = new Error(message);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

async function apiJson(url, method, body) {
  return apiRequest(url, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

async function getCurrentUser() {
  try {
    return await apiRequest("/api/auth/me");
  } catch (e) {
    return null;
  }
}

function showAlert(container, message, type = "danger") {
  if (!container) {
    alert(message);
    return;
  }
  container.innerHTML = `<div class="alert alert-${type} py-2 mb-3">${escapeHtml(message)}</div>`;
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str == null ? "" : String(str);
  return div.innerHTML;
}

/* ===================== Language Translation Manager ===================== */
async function initLanguage() {
  try {
    const res = await fetch(`/lang/${currentLang}.json`);
    translations = await res.json();
    applyTranslations();
  } catch (err) {
    console.warn("Language file fetch failed, using fallback", err);
  }
}

function switchLanguage(langKey) {
  currentLang = langKey;
  localStorage.setItem("gvp_lang", langKey);
  initLanguage();
}

function applyTranslations() {
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (translations[key]) {
      if (el.tagName === "INPUT" || el.tagName === "TEXTAREA") {
        el.placeholder = translations[key];
      } else {
        el.textContent = translations[key];
      }
    }
  });

  document.querySelectorAll(".lang-btn").forEach(btn => {
    if (btn.getAttribute("data-lang") === currentLang) {
      btn.classList.add("btn-primary");
      btn.classList.remove("btn-outline-secondary");
    } else {
      btn.classList.remove("btn-primary");
      btn.classList.add("btn-outline-secondary");
    }
  });
}

/* ===================== Dark Mode Toggle ===================== */
function initTheme() {
  const theme = localStorage.getItem("gvp_theme") || "light";
  document.documentElement.setAttribute("data-bs-theme", theme);
  updateThemeIcon(theme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute("data-bs-theme");
  const target = current === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-bs-theme", target);
  localStorage.setItem("gvp_theme", target);
  updateThemeIcon(target);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById("themeToggleBtn");
  if (!btn) return;
  btn.innerHTML = theme === "dark"
    ? '<i class="bi bi-sun-fill"></i>'
    : '<i class="bi bi-moon-stars-fill"></i>';
}

/* ===================== Auth-aware Navigation ===================== */
async function refreshAuthNav() {
  const authArea = document.getElementById("navAuthArea");
  if (!authArea) return;

  const user = await getCurrentUser();
  if (!user) return; // Leave default (Login/Register) markup as-is

  const dashboardHref = user.role === "ADMIN" ? "/admin-dashboard.html" : "/dashboard.html";
  authArea.innerHTML = `
    <a href="${dashboardHref}" class="btn btn-outline-primary btn-sm fw-semibold">
      <i class="bi bi-speedometer2 me-1"></i> ${escapeHtml(user.fullName.split(" ")[0])}
    </a>
    <button type="button" onclick="handleLogout()" class="btn btn-gvp-primary btn-sm">
      <i class="bi bi-box-arrow-right me-1"></i> Logout
    </button>`;
}

async function handleLogout() {
  try {
    await apiRequest("/api/auth/logout", { method: "POST" });
  } catch (e) {
    // ignore
  }
  window.location.href = "/";
}

/* ===================== Bind Global UI Events ===================== */
function bindGlobalEvents() {
  const themeBtn = document.getElementById("themeToggleBtn");
  if (themeBtn) themeBtn.addEventListener("click", toggleTheme);

  const logoutLinks = document.querySelectorAll("[data-action='logout']");
  logoutLinks.forEach(el => el.addEventListener("click", (e) => { e.preventDefault(); handleLogout(); }));

  const registerForm = document.getElementById("registerForm");
  if (registerForm) registerForm.addEventListener("submit", handleRegisterSubmit);

  const otpForm = document.getElementById("registerOtpForm");
  if (otpForm) otpForm.addEventListener("submit", handleVerifyRegistrationOtp);

  const loginForm = document.getElementById("loginForm");
  if (loginForm) loginForm.addEventListener("submit", handleLoginSubmit);

  const forgotEmailForm = document.getElementById("forgotEmailForm");
  if (forgotEmailForm) forgotEmailForm.addEventListener("submit", handleForgotPasswordEmail);

  const forgotOtpForm = document.getElementById("forgotOtpForm");
  if (forgotOtpForm) forgotOtpForm.addEventListener("submit", handleForgotPasswordOtp);

  const resetPasswordForm = document.getElementById("resetPasswordForm");
  if (resetPasswordForm) resetPasswordForm.addEventListener("submit", handleResetPassword);

  const complaintForm = document.getElementById("complaintRegisterForm");
  if (complaintForm) {
    complaintForm.addEventListener("submit", handleComplaintSubmission);
    prefillComplaintUserInfo();
  }

  const trackForm = document.getElementById("trackComplaintForm");
  if (trackForm) trackForm.addEventListener("submit", handleTrackSearch);

  const profileForm = document.getElementById("profileForm");
  if (profileForm) profileForm.addEventListener("submit", handleProfileUpdate);

  const passwordForm = document.getElementById("changePasswordForm");
  if (passwordForm) passwordForm.addEventListener("submit", handleChangePassword);

  if (document.getElementById("userDashboardContainer")) {
    renderUserDashboard();
  }

  if (document.getElementById("adminDashboardContainer")) {
    guardAdminPageThenRender();
  }

  if (document.getElementById("profilePageContainer")) {
    loadProfile();
  }
}

/* ===================== Registration + OTP ===================== */
let pendingRegistrationEmail = "";

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("registerAlert");
  const submitBtn = form.querySelector("button[type='submit']");

  const payload = {
    fullName: form.fullName.value.trim(),
    email: form.email.value.trim(),
    mobile: form.mobile.value.trim(),
    gender: form.gender.value,
    password: form.password.value,
    confirmPassword: form.confirmPassword.value,
    role: form.role.value,
    department: form.department ? form.department.value : "",
    hostel: form.hostel ? form.hostel.value : "",
    address: form.address ? form.address.value.trim() : "",
  };

  if (payload.password !== payload.confirmPassword) {
    showAlert(alertBox, "Password and Confirm Password do not match.");
    return;
  }

  try {
    if (submitBtn) submitBtn.disabled = true;
    const data = await apiJson("/api/auth/register", "POST", payload);
    pendingRegistrationEmail = payload.email;
    showAlert(alertBox, data.message, "success");

    const otpEmailLabel = document.getElementById("otpEmailLabel");
    if (otpEmailLabel) otpEmailLabel.textContent = payload.email;

    const otpModalEl = document.getElementById("registerOtpModal");
    if (otpModalEl) {
      const modal = new bootstrap.Modal(otpModalEl);
      modal.show();
    }
  } catch (err) {
    showAlert(alertBox, err.message);
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
}

async function handleVerifyRegistrationOtp(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("registerOtpAlert");

  try {
    const data = await apiJson("/api/auth/verify-registration", "POST", {
      email: pendingRegistrationEmail,
      otp: form.otp.value.trim(),
    });
    showAlert(alertBox, data.message, "success");
    setTimeout(() => { window.location.href = data.redirect || "/dashboard.html"; }, 900);
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

/* ===================== Login ===================== */
async function handleLoginSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("loginAlert");
  const submitBtn = form.querySelector("button[type='submit']");

  try {
    if (submitBtn) submitBtn.disabled = true;
    const data = await apiJson("/api/auth/login", "POST", {
      username: form.username.value.trim(),
      password: form.password.value,
    });
    window.location.href = data.redirect || "/dashboard.html";
  } catch (err) {
    showAlert(alertBox, err.message);
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
}

/* ===================== Forgot Password ===================== */
let forgotPasswordEmail = "";

async function handleForgotPasswordEmail(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("forgotAlert");

  try {
    forgotPasswordEmail = form.email.value.trim();
    const data = await apiJson("/api/auth/forgot-password", "POST", { email: forgotPasswordEmail });
    showAlert(alertBox, data.message, "success");
    document.getElementById("forgotStepEmail").classList.add("d-none");
    document.getElementById("forgotStepOtp").classList.remove("d-none");
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

async function handleForgotPasswordOtp(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("forgotAlert");

  try {
    await apiJson("/api/auth/verify-reset-otp", "POST", {
      email: forgotPasswordEmail,
      otp: form.otp.value.trim(),
    });
    document.getElementById("resetOtpHidden").value = form.otp.value.trim();
    document.getElementById("forgotStepOtp").classList.add("d-none");
    document.getElementById("forgotStepReset").classList.remove("d-none");
    showAlert(alertBox, "OTP verified. Please set your new password.", "success");
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

async function handleResetPassword(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("forgotAlert");

  const newPassword = form.newPassword.value;
  const confirmPassword = form.confirmNewPassword.value;
  if (newPassword !== confirmPassword) {
    showAlert(alertBox, "New Password and Confirm Password do not match.");
    return;
  }

  try {
    const data = await apiJson("/api/auth/reset-password", "POST", {
      email: forgotPasswordEmail,
      otp: document.getElementById("resetOtpHidden").value,
      newPassword,
    });
    showAlert(alertBox, data.message, "success");
    setTimeout(() => { window.location.href = "/login.html"; }, 1200);
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

/* ===================== Complaint Submission ===================== */
async function prefillComplaintUserInfo() {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = "/login.html";
    return;
  }
  const nameEl = document.getElementById("complaintFullName");
  const emailEl = document.getElementById("complaintEmail");
  const mobileEl = document.getElementById("complaintMobile");
  const roleEl = document.getElementById("complaintRole");
  if (nameEl) nameEl.value = user.fullName;
  if (emailEl) emailEl.value = user.email;
  if (mobileEl) mobileEl.value = user.mobile;
  if (roleEl) roleEl.value = user.role;
}

async function handleComplaintSubmission(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("complaintAlert");
  const submitBtn = form.querySelector("button[type='submit']");

  const formData = new FormData();
  formData.append("title", form.title.value.trim());
  formData.append("category", form.category.value);
  formData.append("categoryName", form.category.options[form.category.selectedIndex].text);
  formData.append("locationType", form.locationType.value);
  formData.append("building", form.building.value.trim());
  formData.append("floor", form.floor.value.trim());
  formData.append("room", form.room.value.trim());
  formData.append("department", form.department ? form.department.value : "");
  formData.append("hostel", form.hostel ? form.hostel.value : "");
  formData.append("description", form.description.value.trim());
  formData.append("priority", form.priority.value);

  const photoInput = document.getElementById("photoInput");
  if (photoInput && photoInput.files[0]) formData.append("photo", photoInput.files[0]);

  const videoInput = document.getElementById("videoInput");
  if (videoInput && videoInput.files[0]) formData.append("video", videoInput.files[0]);

  try {
    if (submitBtn) submitBtn.disabled = true;
    const complaint = await apiRequest("/api/complaints", { method: "POST", body: formData });

    const successModal = document.getElementById("complaintSuccessModal");
    if (successModal) {
      document.getElementById("newComplaintIdDisplay").textContent = complaint.ticketId;
      const modal = new bootstrap.Modal(successModal);
      modal.show();
    } else {
      alert(`Complaint submitted successfully! Your Ticket ID is: ${complaint.ticketId}`);
      window.location.href = `/track.html?id=${complaint.ticketId}`;
    }
    form.reset();
  } catch (err) {
    showAlert(alertBox, err.message);
  } finally {
    if (submitBtn) submitBtn.disabled = false;
  }
}

/* ===================== Track Complaint ===================== */
function quickTrack() {
  const input = document.getElementById("quickTrackId");
  if (!input) return;
  const id = input.value.trim();
  if (!id) {
    input.focus();
    return;
  }
  window.location.href = `/track.html?id=${encodeURIComponent(id)}`;
}

async function handleTrackSearch(e) {
  if (e) e.preventDefault();
  const query = document.getElementById("trackQueryInput").value.trim();
  if (!query) return;

  const resultContainer = document.getElementById("trackResultsContainer");
  if (!resultContainer) return;

  const user = await getCurrentUser();
  if (!user) {
    resultContainer.innerHTML = `
      <div class="alert alert-warning text-center p-4">
        <i class="bi bi-lock-fill fs-2 mb-2 d-block"></i>
        <h5>Login Required</h5>
        <p class="mb-2">Please login to track your complaint status.</p>
        <a href="/login.html" class="btn btn-gvp-primary btn-sm">Login</a>
      </div>`;
    return;
  }

  try {
    const found = await apiRequest(`/api/complaints/search?query=${encodeURIComponent(query)}`);
    if (!found || found.length === 0) {
      resultContainer.innerHTML = `
        <div class="alert alert-warning text-center p-4">
          <i class="bi bi-exclamation-triangle-fill fs-2 mb-2 d-block"></i>
          <h5>No Complaint Found</h5>
          <p class="mb-0">Please check your Complaint ID (e.g. GVP-2026-101) or registered Email address.</p>
        </div>`;
      return;
    }
    resultContainer.innerHTML = found.map(c => renderComplaintTrackingCard(c)).join("");
  } catch (err) {
    resultContainer.innerHTML = `<div class="alert alert-danger text-center p-4">${escapeHtml(err.message)}</div>`;
  }
}

function renderComplaintTrackingCard(c) {
  const steps = ["Pending", "Assigned", "In Progress", "Resolved"];
  const currentIdx = steps.indexOf(c.status);
  const submittedDate = c.submittedAt ? new Date(c.submittedAt).toLocaleString() : "";

  return `
    <div class="gvp-card p-4 mb-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <div>
          <span class="badge bg-secondary me-2">${escapeHtml(c.ticketId)}</span>
          <span class="badge ${getPriorityBadgeClass(c.priority)}">${escapeHtml(c.priority)} Priority</span>
        </div>
        <span class="badge ${getStatusBadgeClass(c.status)} fs-6">${escapeHtml(c.status)}</span>
      </div>
      <h4 class="fw-bold text-dark mb-2">${escapeHtml(c.title)}</h4>
      <p class="text-muted mb-3"><i class="bi bi-geo-alt-fill text-danger me-1"></i> ${escapeHtml(c.locationType)} - ${escapeHtml(c.building)} (Floor: ${escapeHtml(c.floor || "-")}, Room: ${escapeHtml(c.room || "-")})</p>

      <div class="timeline-stepper my-4">
        ${steps.map((step, idx) => {
          let stateClass = "";
          if (idx < currentIdx) stateClass = "completed";
          else if (idx === currentIdx) stateClass = "active";
          return `
            <div class="timeline-step ${stateClass}">
              <div class="timeline-circle">${idx + 1}</div>
              <div class="timeline-label">${step}</div>
            </div>`;
        }).join("")}
      </div>

      <div class="row g-3 bg-light p-3 rounded-3 border">
        <div class="col-md-6">
          <p class="mb-1"><strong>Submitted By:</strong> ${escapeHtml(c.fullName)} (${escapeHtml(c.role || "")})</p>
          <p class="mb-1"><strong>Email:</strong> ${escapeHtml(c.email)}</p>
          <p class="mb-0"><strong>Category:</strong> ${escapeHtml(c.categoryName || c.category)}</p>
        </div>
        <div class="col-md-6">
          <p class="mb-1"><strong>Date Submitted:</strong> ${escapeHtml(submittedDate)}</p>
          <p class="mb-1"><strong>Assigned Technician:</strong> ${escapeHtml(c.assignedTo || "Pending Assignment")}</p>
          <p class="mb-0"><strong>Details:</strong> ${escapeHtml(c.description)}</p>
        </div>
        ${c.photoUrl ? `<div class="col-12"><a href="${c.photoUrl}" target="_blank" class="btn btn-sm btn-outline-secondary"><i class="bi bi-image me-1"></i> View Attached Photo</a></div>` : ""}
        ${c.videoUrl ? `<div class="col-12"><a href="${c.videoUrl}" target="_blank" class="btn btn-sm btn-outline-secondary"><i class="bi bi-camera-video me-1"></i> View Attached Video</a></div>` : ""}
      </div>
    </div>`;
}

/* ===================== User Dashboard ===================== */
async function renderUserDashboard() {
  const container = document.getElementById("userDashboardContainer");
  if (!container) return;

  const user = await getCurrentUser();
  if (!user) {
    window.location.href = "/login.html";
    return;
  }

  const welcomeEl = document.getElementById("dashboardWelcome");
  if (welcomeEl) welcomeEl.textContent = `Welcome, ${user.fullName} (${user.role === "ADMIN" ? "Administrator" : "User"})`;

  try {
    const stats = await apiRequest("/api/complaints/stats");
    document.getElementById("statTotal").textContent = stats.total;
    document.getElementById("statPending").textContent = stats.pending;
    document.getElementById("statProgress").textContent = stats.inProgress;
    document.getElementById("statResolved").textContent = stats.resolved;

    const list = await apiRequest("/api/complaints");
    const tbody = document.getElementById("userComplaintsTbody");
    if (tbody) {
      if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No complaints filed yet. <a href="/complaint.html">Register a Complaint</a></td></tr>`;
        return;
      }
      tbody.innerHTML = list.map(c => `
        <tr>
          <td><strong>${escapeHtml(c.ticketId)}</strong></td>
          <td>${escapeHtml(c.title)}</td>
          <td><span class="badge ${getPriorityBadgeClass(c.priority)}">${escapeHtml(c.priority)}</span></td>
          <td>${escapeHtml(c.building)}</td>
          <td><span class="badge ${getStatusBadgeClass(c.status)}">${escapeHtml(c.status)}</span></td>
          <td>
            <a href="/track.html?id=${encodeURIComponent(c.ticketId)}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i> View</a>
          </td>
        </tr>
      `).join("");
    }
  } catch (err) {
    console.error(err);
  }
}

/* ===================== Admin Dashboard ===================== */
async function guardAdminPageThenRender() {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = "/login.html";
    return;
  }
  if (user.role !== "ADMIN") {
    window.location.href = "/dashboard.html";
    return;
  }
  renderAdminDashboard();
}

let adminComplaintsCache = [];

async function renderAdminDashboard() {
  const container = document.getElementById("adminDashboardContainer");
  if (!container) return;

  try {
    adminComplaintsCache = await apiRequest("/api/complaints/admin/all");

    const total = adminComplaintsCache.length;
    const pending = adminComplaintsCache.filter(c => c.status === "Pending").length;
    const progress = adminComplaintsCache.filter(c => c.status === "Assigned" || c.status === "In Progress").length;
    const resolved = adminComplaintsCache.filter(c => c.status === "Resolved").length;
    setTextIfExists("adminStatTotal", total);
    setTextIfExists("adminStatPending", pending);
    setTextIfExists("adminStatProgress", progress);
    setTextIfExists("adminStatResolved", resolved);

    renderAdminComplaintsTable(adminComplaintsCache);

    const filterInput = document.getElementById("adminFilterInput");
    if (filterInput && !filterInput.dataset.bound) {
      filterInput.dataset.bound = "true";
      filterInput.addEventListener("input", () => {
        const term = filterInput.value.trim().toLowerCase();
        const filtered = !term ? adminComplaintsCache : adminComplaintsCache.filter(c =>
          c.ticketId.toLowerCase().includes(term) || (c.email && c.email.toLowerCase().includes(term)));
        renderAdminComplaintsTable(filtered);
      });
    }
  } catch (err) {
    console.error(err);
  }
}

function setTextIfExists(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function renderAdminComplaintsTable(list) {
  const tbody = document.getElementById("adminComplaintsTbody");
  if (!tbody) return;

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No complaints found.</td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(c => `
    <tr>
      <td><strong>${escapeHtml(c.ticketId)}</strong></td>
      <td>
        <div class="fw-bold">${escapeHtml(c.title)}</div>
        <small class="text-muted">${escapeHtml(c.categoryName || c.category)}</small>
      </td>
      <td>
        <div>${escapeHtml(c.fullName)}</div>
        <small class="text-muted">${escapeHtml(c.email)} (${escapeHtml(c.role || "")})</small>
      </td>
      <td>${escapeHtml(c.building)}</td>
      <td><span class="badge ${getStatusBadgeClass(c.status)}">${escapeHtml(c.status)}</span></td>
      <td>
        <button onclick="openAdminActionModal('${escapeHtml(c.ticketId)}')" class="btn btn-sm btn-gvp-primary me-1"><i class="bi bi-pencil-square"></i> Action</button>
      </td>
    </tr>
  `).join("");
}

function openAdminActionModal(ticketId) {
  const c = adminComplaintsCache.find(item => item.ticketId === ticketId);
  if (!c) return;

  const modalEl = document.getElementById("adminActionModal");
  if (!modalEl) return;

  document.getElementById("modalComplaintId").value = c.ticketId;
  document.getElementById("modalComplaintTitle").textContent = `${c.ticketId} - ${c.title}`;
  document.getElementById("modalAssignee").value = c.assignedTo || "";
  document.getElementById("modalStatus").value = c.status;

  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

async function saveAdminAction() {
  const ticketId = document.getElementById("modalComplaintId").value;
  const assignee = document.getElementById("modalAssignee").value.trim();
  const newStatus = document.getElementById("modalStatus").value;

  try {
    await apiJson(`/api/complaints/${encodeURIComponent(ticketId)}/status`, "PUT", {
      status: newStatus,
      assignedTo: assignee,
    });
    const modalEl = document.getElementById("adminActionModal");
    if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
    await renderAdminDashboard();
  } catch (err) {
    alert(err.message);
  }
}

/* ===================== Profile ===================== */
async function loadProfile() {
  const user = await getCurrentUser();
  if (!user) {
    window.location.href = "/login.html";
    return;
  }

  const nameHeading = document.getElementById("profileFullNameHeading");
  if (nameHeading) nameHeading.textContent = user.fullName;

  const initials = user.fullName.split(" ").map(p => p[0]).slice(0, 2).join("").toUpperCase();
  const avatar = document.getElementById("profileAvatarInitials");
  if (avatar) avatar.textContent = initials;

  const roleLine = document.getElementById("profileRoleLine");
  if (roleLine) roleLine.textContent = `${user.role === "ADMIN" ? "Administrator" : user.department || "Campus Member"}`;

  const form = document.getElementById("profileForm");
  if (form) {
    form.fullName.value = user.fullName;
    form.email.value = user.email;
    form.mobile.value = user.mobile;
    form.role.value = user.role;
  }
}

async function handleProfileUpdate(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("profileAlert");

  try {
    await apiJson("/api/users/me", "PUT", {
      fullName: form.fullName.value.trim(),
      mobile: form.mobile.value.trim(),
    });
    showAlert(alertBox, "Profile details updated successfully!", "success");
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

async function handleChangePassword(e) {
  e.preventDefault();
  const form = e.target;
  const alertBox = document.getElementById("profileAlert");

  if (form.newPassword.value !== form.confirmPassword.value) {
    showAlert(alertBox, "New Password and Confirm Password do not match.");
    return;
  }

  try {
    const data = await apiJson("/api/users/me/change-password", "POST", {
      currentPassword: form.currentPassword.value,
      newPassword: form.newPassword.value,
    });
    showAlert(alertBox, data.message, "success");
    form.reset();
  } catch (err) {
    showAlert(alertBox, err.message);
  }
}

/* ===================== Helper Badge Classes ===================== */
function getStatusBadgeClass(status) {
  switch (status) {
    case "Pending": return "badge-pending";
    case "Assigned": return "badge-assigned";
    case "In Progress": return "badge-progress";
    case "Resolved": return "badge-resolved";
    case "Rejected": return "badge-rejected";
    default: return "bg-secondary";
  }
}

function getPriorityBadgeClass(priority) {
  switch (priority) {
    case "Low": return "badge-priority-low";
    case "Medium": return "badge-priority-medium";
    case "High": return "badge-priority-high";
    case "Emergency": return "badge-priority-emergency";
    default: return "bg-secondary";
  }
}
