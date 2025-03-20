document.getElementById("signUpForm").addEventListener("submit", function (e) {
    e.preventDefault(); // Prevent form submission

    const phoneNumber = document.getElementById("phoneNumber").value;
    if (phoneNumber) {
        // Redirect with phone number as a query parameter
        window.location.href = `Verify.html?phone=${encodeURIComponent(phoneNumber)}`;
    } else {
        alert("Please enter a valid phone number.");
    }
});