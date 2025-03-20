<?php
header("Content-Type: application/json");

$servername = "localhost";
$username = "root";
$password = "";
$dbname = "Akady";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit;
}

$data = json_decode(file_get_contents("php://input"), true);

$username = $data["username"];
$email = $data["email"];
$phoneNumber = $data["phoneNumber"];
$password = password_hash($data["password"], PASSWORD_BCRYPT);

$sql = "INSERT INTO Users (username, email, phone_number, password) VALUES ('$username', '$email', '$phoneNumber', '$password')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true]);
} else {
    echo json_encode(["success" => false, "message" => $conn->error]);
}

$conn->close();
?>