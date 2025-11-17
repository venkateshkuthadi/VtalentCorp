package com.VtalentCorp.controller;

import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.time.LocalDate;

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
                + ".container{width:900px;margin:120px auto 40px auto;background:#fff;"
                + "padding:30px 50px;box-shadow:0 5px 20px rgba(0,0,0,0.1);border-radius:12px;}"
                + ".employee-details{margin-bottom:20px;font-size:16px;color:#333;}"
                + ".tables{display:flex;justify-content:space-between;margin-top:25px;}"
                + "table{width:48%;border-collapse:collapse;}"
                + "th,td{border:1px solid #ddd;padding:8px;text-align:left;font-size:15px;}"
                + "th{background-color:#3498db;color:white;}"
                + ".btn{background:#3498db;color:white;padding:10px 18px;text-decoration:none;border-radius:6px;}"
                + "</style>"
                + "<script>function printPayslip(){window.print();}</script>"
                + "</head><body><div class='container'>");

        if (empIdParam == null || empIdParam.isEmpty()) {
            out.println("<h3 style='color:red;text-align:center;'>Employee ID is required!</h3>");
            out.println("</div></body></html>");
            return;
        }

        int eid = Integer.parseInt(empIdParam);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/vignesh", "root", "root");

            // Get employee and payroll data
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

                String[] allMonths = {"january","february","march","april","may","june",
                        "july","august","september","october","november","december"};

                StringBuilder monthOptions = new StringBuilder();
                StringBuilder yearOptions = new StringBuilder();

                int selectedYearInt = selectedYear != null ? Integer.parseInt(selectedYear) : currentYear;

                for (int i = 0; i < allMonths.length; i++) {
                    int monthNum = i + 1;
                    if (selectedYear == null) {
                        if (monthNum < joinMonthValue) continue;
                    } else if (selectedYearInt == joinYear && monthNum < joinMonthValue) continue;

                    monthOptions.append("<option value='").append(allMonths[i]).append("'")
                            .append((selectedMonth != null && allMonths[i].equalsIgnoreCase(selectedMonth)) ? " selected" : "")
                            .append(">").append(allMonths[i]).append("</option>");
                }

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

                    // Fetch paid leaves from employee_leaves table
                    String leaveQuery = "SELECT SUM(num_leaves) AS paidleaves "
                            + "FROM employee_leaves WHERE empid=? AND leave_type='paid' "
                            + "AND MONTH(start_date)=? AND YEAR(start_date)=?";

                    PreparedStatement ps2 = con.prepareStatement(leaveQuery);
                    ps2.setString(1, empIdParam);
                    ps2.setInt(2, getMonthNumber(selectedMonth));
                    ps2.setInt(3, Integer.parseInt(selectedYear));

                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        paidLeaves = rs2.getInt("paidleaves");
                        leaveDeduction = (gross / 30) * paidLeaves;
                        net -= leaveDeduction;
                        deductions += leaveDeduction;
                    }

                    // Extra hours calculation
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

                    extraEarnings = extraHours * hourlyRate;
                    net += extraEarnings;
                }

                // Dropdown menu
                out.println("<form method='get' action='find'>"
                        + "<input type='hidden' name='eid' value='" + eid + "'/>"
                        + "<label>Month:</label><select name='month'>" + monthOptions + "</select>"
                        + "<label>Year:</label><select name='year'>" + yearOptions + "</select>"
                        + "<button type='submit' class='btn'>View Payslip</button></form>");

                // Employee Details
                out.println("<div class='employee-details'><h2>Employee Details</h2>"
                        + "<p><strong>Employee ID:</strong> " + rs.getInt("eid") + "</p>"
                        + "<p><strong>Name:</strong> " + rs.getString("name") + "</p>"
                        + "<p><strong>Designation:</strong> " + rs.getString("designation") + "</p>"
                        + "<p><strong>Date of Joining:</strong> " + rs.getDate("dateofjoining") + "</p>"
                        + "<p><strong>Address:</strong> " + rs.getString("address") + "</p>"
                        + "<p><strong>Total Extra Hours:</strong> " + extraHours + "</p></div>");

                // Salary Tables
                out.printf("<div class='tables'>"
                        + "<table><tr><th colspan='2'>Earnings</th></tr>"
                        + "<tr><td>Basic Pay</td><td>₹%.2f</td></tr>"
                        + "<tr><td>HRA</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Special Allowance</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Transport Allowance</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Extra Hours</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Total Earnings</td><td>₹%.2f</td></tr></table>"
                        + "<table><tr><th colspan='2'>Deductions</th></tr>"
                        + "<tr><td>PF</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Tax</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Leave Deduction (%d Paid Leaves)</td><td>₹%.2f</td></tr>"
                        + "<tr><td>Total Deductions</td><td>₹%.2f</td></tr>"
                        + "<tr><td><b>Net Pay</b></td><td><b>₹%.2f</b></td></tr></table></div>",
                        basic, hra, special, transport, extraEarnings, gross + extraEarnings,
                        pf, tax, paidLeaves, leaveDeduction, deductions, net);

            } else {
                out.println("<h3 style='color:red;text-align:center;'>No record found for Employee ID: " + eid + "</h3>");
            }

        } catch (Exception e) {
            out.println("<p style='color:red;text-align:center;'>Error: " + e.getMessage() + "</p>");
        }

        out.println("</div></body></html>");
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
 
