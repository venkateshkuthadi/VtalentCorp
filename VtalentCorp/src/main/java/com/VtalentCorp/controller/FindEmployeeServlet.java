package com.VtalentCorp.controller;

import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.time.LocalDate;
import java.time.Month;

public class FindEmployeeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        String empIdParam = request.getParameter("eid");
        String selectedMonth = request.getParameter("month");
        String selectedYear = request.getParameter("year");

        out.println("<html><head><title>Employee Payslip</title>"
                + "<style>"
                + "body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f4f7fc;margin:0;padding:0;}"
                + ".logo{position:absolute;top:0;left:25px;display:flex;align-items:center;gap:10px;}"
                + ".logo img{width:150px;height:100px;}"
                + ".container{width:900px;margin:120px auto 40px auto;background:#fff;"
                + "padding:30px 50px;box-shadow:0 5px 20px rgba(0,0,0,0.1);border-radius:12px;}"
                + "h1,h2{text-align:center;color:#2c3e50;}"
                + ".employee-details{margin-bottom:20px;font-size:16px;color:#333;}"
                + ".tables{display:flex;justify-content:space-between;margin-top:25px;}"
                + "table{width:48%;border-collapse:collapse;}"
                + "th,td{border:1px solid #ddd;padding:8px;text-align:left;font-size:15px;}"
                + "th{background-color:#3498db;color:white;}"
                + "tr:nth-child(even){background-color:#f9f9f9;}"
                + ".total{font-weight:bold;color:#2c3e50;}"
                + ".btn{display:inline-block;background:#3498db;color:white;padding:10px 18px;text-decoration:none;border-radius:6px;margin:10px;}"
                + ".btn:hover{background:#2980b9;}"
                + ".footer{background-color:#2c3e50;color:#fff;margin-top:40px;padding:40px 20px 10px;font-size:14px;}"
                + ".footer-container{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:20px;}"
                + ".footer h3{color:#3498db;margin-bottom:10px;}"
                + ".footer-bottom{text-align:center;border-top:1px solid #444;margin-top:20px;padding-top:10px;font-size:13px;}"
                + "</style>"
                + "<script>function printPayslip(){window.print();}</script>"
                + "</head><body>"
                + "<div class='logo'><img src='logo1.jpeg' alt='Company Logo'></div>"
                + "<div class='container'><h1>Employee Payslip</h1>");

        if (empIdParam == null || empIdParam.isEmpty()) {
            out.println("<h3 style='color:red;text-align:center;'>Employee ID is required!</h3>");
            out.println("</div></body></html>");
            return;
        }

        int eid = Integer.parseInt(empIdParam);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/vignesh", "root", "root");

            String query = "SELECT e.eid, e.name, e.designation, e.dateofjoining, e.address, "
                    + "p.basicpay, p.houserentallowances, p.specialallowances, p.transport, p.pf, p.tax "
                    + "FROM employee e JOIN payroll p ON e.eid = p.eid WHERE e.eid = ?";

            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, eid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double basic = rs.getDouble("basicpay");
                double hra = rs.getDouble("houserentallowances");
                double special = rs.getDouble("specialallowances");
                double transport = rs.getDouble("transport");
                double pf = rs.getDouble("pf");
                double tax = rs.getDouble("tax");

                double gross = basic + hra + special + transport;
                double deductions = pf + tax;
                double net = gross - deductions;

                LocalDate joiningDate = rs.getDate("dateofjoining").toLocalDate();
                int joinYear = joiningDate.getYear();
                int joinMonthValue = joiningDate.getMonthValue();
                int currentYear = LocalDate.now().getYear();

                // Dropdown options
                String[] allMonths = {"january","february","march","april","may","june",
                        "july","august","september","october","november","december"};
                StringBuilder monthOptions = new StringBuilder();
                int selectedYearInt = selectedYear != null ? Integer.parseInt(selectedYear) : currentYear;

                for (int i = 0; i < allMonths.length; i++) {
                    int monthNum = i + 1;
                    if (selectedYear == null) {
                        if (monthNum < joinMonthValue) continue;
                    } else if (selectedYearInt == joinYear && monthNum < joinMonthValue) continue;
                    monthOptions.append("<option value='").append(allMonths[i]).append("'")
                            .append((selectedMonth != null && allMonths[i].equalsIgnoreCase(selectedMonth)) ? " selected" : "")
                            .append(">").append(Character.toUpperCase(allMonths[i].charAt(0)))
                            .append(allMonths[i].substring(1)).append("</option>");
                }

                StringBuilder yearOptions = new StringBuilder();
                for (int y = joinYear; y <= currentYear; y++) {
                    yearOptions.append("<option value='").append(y).append("'")
                            .append((String.valueOf(y).equals(selectedYear)) ? " selected" : "")
                            .append(">").append(y).append("</option>");
                }

                int paidLeaves = 0;
                double leaveDeduction = 0.0;
                double extraHours = 0.0;
                double extraEarnings = 0.0;
                double hourlyRate = 200.0;

                if (selectedMonth != null && selectedYear != null) {
                    // Fetch paid leaves (if any)
                    String leaveQuery = "SELECT paidleaves FROM attendance WHERE eid=? AND month=? AND year=?";
                    PreparedStatement ps2 = con.prepareStatement(leaveQuery);
                    ps2.setInt(1, eid);
                    ps2.setString(2, selectedMonth.toLowerCase());
                    ps2.setInt(3, Integer.parseInt(selectedYear));
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        paidLeaves = rs2.getInt("paidleaves");
                        leaveDeduction = (gross / 30) * paidLeaves;
                        net -= leaveDeduction;
                        deductions += leaveDeduction;
                    }

                    // ✅ Fetch total extra hours from attendance table
                    String extraQuery = "SELECT SUM(extra_hours) AS totalExtra FROM attendance1 "
                            + "WHERE emp_id=? AND MONTH(date)=? AND YEAR(date)=?";
                    PreparedStatement ps3 = con.prepareStatement(extraQuery);
                    ps3.setInt(1, eid);
                    ps3.setInt(2, getMonthNumber(selectedMonth));
                    ps3.setInt(3, Integer.parseInt(selectedYear));
                    ResultSet rs3 = ps3.executeQuery();
                    if (rs3.next()) {
                        extraHours = rs3.getDouble("totalExtra");
                    }

                    // Calculate extra earnings
                    extraEarnings = extraHours * hourlyRate;
                    net += extraEarnings; // add to net pay
                }

                // Display Payslip
                out.println("<form method='get' action='find'>"
                        + "<input type='hidden' name='eid' value='" + eid + "'/>"
                        + "<label>Month:</label><select name='month'>" + monthOptions + "</select>"
                        + "<label>Year:</label><select name='year' onchange='this.form.submit()'>" + yearOptions + "</select>"
                        + "<button type='submit' class='btn'>View Payslip</button></form>"
                        + "<div class='employee-details'><h2>Employee Details</h2>"
                        + "<p><strong>Employee ID:</strong> " + rs.getInt("eid") + "</p>"
                        + "<p><strong>Name:</strong> " + rs.getString("name") + "</p>"
                        + "<p><strong>Designation:</strong> " + rs.getString("designation") + "</p>"
                        + "<p><strong>Date of Joining:</strong> " + rs.getDate("dateofjoining") + "</p>"
                        + "<p><strong>Address:</strong> " + rs.getString("address") + "</p>"
                        + "<p><strong>Total Extra Hours:</strong> " + String.format("%.2f", extraHours) + "</p></div>");

                out.printf("<div class='tables'>"
                        + "<table><tr><th colspan='2'>Earnings</th></tr>"
                        + "<tr><td>Basic Pay</td><td>₹%.2f</td></tr>"
                        + "<tr><td>House Rent Allowance</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Special Allowance</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Transport Allowance</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Extra Hours (%.2f × ₹%.2f)</td><td>₹%.2f</td></tr>"
                        + "<tr class='total'><td>Total Earnings</td><td>₹%.2f</td></tr></table>"
                        + "<table><tr><th colspan='2'>Deductions</th></tr>"
                        + "<tr><td>PF</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Tax</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Leave Deduction (%d Paid Leaves)</td><td>₹%.2f</td></tr>"
                        + "<tr class='total'><td>Total Deductions</td><td>₹%.2f</td></tr>"
                        + "<tr class='total'><td>Net Pay</td><td><b>₹%.2f</b></td></tr></table></div>",
                        basic, hra, special, transport, extraHours, hourlyRate, extraEarnings, gross + extraEarnings,
                        pf, tax, paidLeaves, leaveDeduction, deductions, net);

            } else {
                out.println("<h3 style='color:red;text-align:center;'>No record found for Employee ID: " + eid + "</h3>");
            }

        } catch (Exception e) {
            out.println("<p style='color:red;text-align:center;'>Error: " + e.getMessage() + "</p>");
        }

        out.println("<div class='actions'><a href='payslip.html' class='btn'>Back</a>"
                + "<a href='#' class='btn' onclick='printPayslip()'>Print Payslip</a></div>"
                + "</div><footer class='footer'><div class='footer-container'>"
                + "<div><p>Vtalent Solutions is an IT consultancy firm that takes pride in solving your toughest challenges.</p></div>"
                + "<div><h3>Useful Links</h3><ul><li><a href='#'>Home</a></li><li><a href='#'>About Us</a></li><li><a href='#'>Careers</a></li></ul></div>"
                + "<div><h3>Contact Us</h3><p>Unit 5, The Freehold Industrial Centre<br>Amberly Way, Hounslow</p></div></div>"
                + "<div class='footer-bottom'><p>© Copyright Vtalent Solutions LTD. All Rights Reserved.</p></div>"
                + "</footer></body></html>");
    }

    private int getMonthNumber(String monthName) {
        switch (monthName.toLowerCase()) {
            case "january": return 1;
            case "february": return 2;
            case "march": return 3;
            case "april": return 4;
            case "may": return 5;
            case "june": return 6;
            case "july": return 7;
            case "august": return 8;
            case "september": return 9;
            case "october": return 10;
            case "november": return 11;
            case "december": return 12;
            default: return 0;
        }
    }
}
