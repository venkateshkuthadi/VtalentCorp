package com.VtalentCorp.controller;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class RegistrationServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // 1️⃣ Get values from form
        String eid = request.getParameter("eid");
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String designation = request.getParameter("designation");
        String dateofjoining = request.getParameter("dateofjoining");
        String address = request.getParameter("address");

        // 2️⃣ Decide salary based on designation
        int salary = 0;
        switch (designation) {
            case "React Developer":
                salary = 60000;
                break;
            case "Java Full Stack Developer":
                salary = 75000;
                break;
            case "Tester":
                salary = 50000;
                break;
            case "Angular Developer":
                salary = 70000;
                break;
            case "Database Administrator":
                salary = 80000;
                break;
            case "Python Developer":
                salary = 72000;
                break;
            default:
                salary = 40000;
        }

        try {
            // 3️⃣ Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 4️⃣ Connect to MySQL
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/vignesh", "root", "root");

            // 5️⃣ Prepare SQL query
            String query = "INSERT INTO registration (eid, name, password, designation, dateofjoining, address, salary) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, eid);
            ps.setString(2, name);
            ps.setString(3, password);
            ps.setString(4, designation);
            ps.setString(5, dateofjoining);
            ps.setString(6, address);
            ps.setInt(7, salary);

            // 6️⃣ Execute query
            int rows = ps.executeUpdate();

            // 7️⃣ Show result
            if (rows > 0) {
                out.println("<h3>✅ Registration Successful!</h3>");
                 
            } else {
                out.println("<h3>❌ Registration Failed. Please try again.</h3>");
            }

            // 8️⃣ Close connection
            ps.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
}
