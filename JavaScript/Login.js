document.getElementById("loginForm").addEventListener("submit", async function (e) {
    e.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const response = await fetch("backend/Login.php", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
    });

    const result = await response.json();

    if (result.success) {
        localStorage.setItem("user", JSON.stringify(result.user));
        window.location.href = "Profile.html";
    } else {
        alert("Invalid username or password.");
    }
});