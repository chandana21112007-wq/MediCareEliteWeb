import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class HospitalServer {

    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Doctor> doctors = new ArrayList<>();
    static ArrayList<Appointment> appointments = new ArrayList<>();

    static int patientId = 1;
    static int appointmentId = 1;

    static class Patient {
        int id, age;
        String name, gender, phone;

        Patient(int id, String name, int age, String gender, String phone) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.phone = phone;
        }
    }

    static class Doctor {
        int id;
        String name, department, qualification;

        Doctor(int id, String name, String department, String qualification) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.qualification = qualification;
        }
    }

    static class Appointment {
        int id;
        Patient patient;
        Doctor doctor;
        String date, time, status;

        Appointment(int id, Patient patient, Doctor doctor,
                    String date, String time) {
            this.id = id;
            this.patient = patient;
            this.doctor = doctor;
            this.date = date;
            this.time = time;
            this.status = "Booked";
        }
    }

    static void loadDoctors() {
        doctors.add(new Doctor(101, "Dr. Arun", "Cardiology", "MD"));
        doctors.add(new Doctor(102, "Dr. Priya", "Neurology", "MD"));
        doctors.add(new Doctor(103, "Dr. Ravi", "Orthopedics", "MS"));
        doctors.add(new Doctor(104, "Dr. Sneha", "Dermatology", "MD"));
        doctors.add(new Doctor(105, "Dr. Kiran", "Pediatrics", "MD"));
        doctors.add(new Doctor(106, "Dr. Meera", "Gynecology", "MS"));
        doctors.add(new Doctor(107, "Dr. Raj", "ENT", "MS"));
        doctors.add(new Doctor(108, "Dr. Anu", "Ophthalmology", "MS"));
        doctors.add(new Doctor(109, "Dr. Vikram", "General Medicine", "MD"));
        doctors.add(new Doctor(110, "Dr. Divya", "Dentistry", "BDS"));
    }

    // Method overloading 1
    static Appointment bookAppointment(Patient p, Doctor d) {
        return bookAppointment(p, d, "Not specified", "Not specified");
    }

    // Method overloading 2
    static Appointment bookAppointment(Patient p, Doctor d, String date) {
        return bookAppointment(p, d, date, "Not specified");
    }

    // Method overloading 3
    static Appointment bookAppointment(Patient p, Doctor d,
                                       String date, String time) {
        Appointment a = new Appointment(
            appointmentId++, p, d, date, time
        );
        appointments.add(a);
        return a;
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    static Map<String, String> readForm(HttpExchange exchange)
            throws IOException {
        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String, String> data = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                : "";

            data.put(key, value);
        }

        return data;
    }

    static void send(HttpExchange exchange, int status, String response)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static String patientsJSON() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);

            if (i > 0) json.append(",");

            json.append("{")
                .append("\"id\":").append(p.id).append(",")
                .append("\"name\":\"").append(escape(p.name)).append("\",")
                .append("\"age\":").append(p.age).append(",")
                .append("\"gender\":\"").append(escape(p.gender)).append("\",")
                .append("\"phone\":\"").append(escape(p.phone)).append("\"")
                .append("}");
        }

        return json.append("]").toString();
    }

    static String doctorsJSON() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);

            if (i > 0) json.append(",");

            json.append("{")
                .append("\"id\":").append(d.id).append(",")
                .append("\"name\":\"").append(escape(d.name)).append("\",")
                .append("\"department\":\"").append(escape(d.department)).append("\",")
                .append("\"qualification\":\"").append(escape(d.qualification)).append("\"")
                .append("}");
        }

        return json.append("]").toString();
    }

    static String appointmentsJSON() {
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < appointments.size(); i++) {
            Appointment a = appointments.get(i);

            if (i > 0) json.append(",");

            json.append("{")
                .append("\"id\":").append(a.id).append(",")
                .append("\"patient\":\"").append(escape(a.patient.name)).append("\",")
                .append("\"doctor\":\"").append(escape(a.doctor.name)).append("\",")
                .append("\"department\":\"").append(escape(a.doctor.department)).append("\",")
                .append("\"date\":\"").append(escape(a.date)).append("\",")
                .append("\"time\":\"").append(escape(a.time)).append("\",")
                .append("\"status\":\"").append(escape(a.status)).append("\"")
                .append("}");
        }

        return json.append("]").toString();
    }

    static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (method.equals("GET") && path.equals("/api/doctors")) {
                send(exchange, 200, doctorsJSON());
            }

            else if (method.equals("GET") && path.equals("/api/patients")) {
                send(exchange, 200, patientsJSON());
            }

            else if (method.equals("GET") && path.equals("/api/appointments")) {
                send(exchange, 200, appointmentsJSON());
            }

            else if (method.equals("POST") && path.equals("/api/patients")) {
                Map<String, String> data = readForm(exchange);

                String name = data.getOrDefault("name", "").trim();
                String gender = data.getOrDefault("gender", "").trim();
                String phone = data.getOrDefault("phone", "").trim();
                int age = Integer.parseInt(data.getOrDefault("age", "0"));

                if (name.isEmpty() || gender.isEmpty()
                        || phone.isEmpty() || age < 1 || age > 120) {
                    send(exchange, 400, "{\"message\":\"Enter valid patient details.\"}");
                    return;
                }

                Patient p = new Patient(
                    patientId++, name, age, gender, phone
                );

                patients.add(p);

                send(exchange, 201,
                    "{\"message\":\"Patient registered successfully!\",\"id\":" + p.id + "}");
            }

            else if (method.equals("POST") && path.equals("/api/appointments")) {
                Map<String, String> data = readForm(exchange);

                int pId = Integer.parseInt(data.get("patientId"));
                int dId = Integer.parseInt(data.get("doctorId"));

                Patient patient = null;
                Doctor doctor = null;

                for (Patient p : patients) {
                    if (p.id == pId) patient = p;
                }

                for (Doctor d : doctors) {
                    if (d.id == dId) doctor = d;
                }

                if (patient == null || doctor == null) {
                    send(exchange, 400, "{\"message\":\"Select a valid patient and doctor.\"}");
                    return;
                }

                String date = data.getOrDefault("date", "").trim();
                String time = data.getOrDefault("time", "").trim();

                if (date.isEmpty() || time.isEmpty()) {
                    send(exchange, 400, "{\"message\":\"Enter date and time.\"}");
                    return;
                }

                Appointment a = bookAppointment(
                    patient, doctor, date, time
                );

                send(exchange, 201,
                    "{\"message\":\"Appointment booked successfully!\",\"id\":" + a.id + "}");
            }

            else if (method.equals("DELETE")
                    && path.startsWith("/api/appointments/")) {
                int id = Integer.parseInt(
                    path.substring("/api/appointments/".length())
                );

                for (Appointment a : appointments) {
                    if (a.id == id && a.status.equals("Booked")) {
                        a.status = "Cancelled";
                        send(exchange, 200,
                            "{\"message\":\"Appointment cancelled successfully!\"}");
                        return;
                    }
                }

                send(exchange, 404,
                    "{\"message\":\"Appointment not found or already cancelled.\"}");
            }

            else {
                servePage(exchange, path);
            }

        } catch (Exception e) {
            send(exchange, 400,
                "{\"message\":\"Request failed: " + escape(e.getMessage() == null
                    ? "Invalid request" : e.getMessage()) + "\"}");
        }
    }

    static void servePage(HttpExchange exchange, String path)
            throws IOException {
        if (path.equals("/")) path = "/index.html";

        String fileName;

        if (path.equals("/index.html")) {
            fileName = "index.html";
        } else if (path.equals("/style.css")) {
            fileName = "style.css";
        } else if (path.equals("/app.js")) {
            fileName = "app.js";
        } else {
            send(exchange, 404, "{\"message\":\"Page not found\"}");
            return;
        }

        Path file = Paths.get("web", fileName);

        if (!Files.exists(file)) {
            send(exchange, 404, "{\"message\":\"Frontend file not found\"}");
            return;
        }

        String contentType = fileName.endsWith(".html")
            ? "text/html; charset=UTF-8"
            : fileName.endsWith(".css")
                ? "text/css; charset=UTF-8"
                : "application/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        loadDoctors();

        
int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", HospitalServer::handle);
        server.start();

        System.out.println("MediCare Elite server started!");
        System.out.println("Open http://localhost:8080");
    }
}
