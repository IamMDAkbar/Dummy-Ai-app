
const validUsername = "TaupheekAkbar";
const validPassword = "Akbar123";
const validUsername1 = "MadyPodle";
const validPassword1 = "Mady123";

document.getElementById("loginForm").addEventListener("submit", function (e) {
    e.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    if (username === validUsername || username === validUsername1 && password === validPassword || password === validPassword1) {
      
        window.location.href = "Profile.html";
    } else {
        window.location.href = "c:\Akady\HTML\LogInterface1.html";
    }
});