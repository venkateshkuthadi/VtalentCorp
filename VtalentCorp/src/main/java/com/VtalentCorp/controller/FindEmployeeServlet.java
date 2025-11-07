package com.VtalentCorp.controller;

import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.time.LocalDate;
import java.time.Month;

public class FindEmployeeServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        int eid = Integer.parseInt(request.getParameter("eid"));
        String selectedMonth = request.getParameter("month");
        String selectedYear = request.getParameter("year");

        out.println("""
            <html>
            <head>
                <title>Employee Payslip</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Arial, sans-serif;
                        background-color: #f4f7fc;
                        margin: 0;
                        padding: 0;
                        position: relative;
                    }

                    /* Logo at top-left */
                    .logo {
                        position: absolute;
                        top: 0px;
                        left: 25px;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    .logo img {
                        width: 150px;
                        height: 100px;
                    }
                    .logo h2 {
                        color: #2c3e50;
                        margin: 0;
                        font-size: 20px;
                    }

                    .container {
                        width: 900px;
                        margin: 120px auto 40px auto;
                        background: #fff;
                        padding: 30px 50px;
                        box-shadow: 0 5px 20px rgba(0,0,0,0.1);
                        border-radius: 12px;
                    }

                    h1, h2 { text-align: center; color: #2c3e50; }
                    .employee-details { margin-bottom: 20px; font-size: 16px; color: #333; }
                    .employee-details p { margin: 6px 0; }
                    .tables { display: flex; justify-content: space-between; margin-top: 25px; }
                    table { width: 48%; border-collapse: collapse; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 15px; }
                    th { background-color: #3498db; color: white; }
                    tr:nth-child(even) { background-color: #f9f9f9; }
                    .total { font-weight: bold; color: #2c3e50; }
                    .actions { text-align: center; margin-top: 20px; }
                    .btn {
                        display: inline-block;
                        background: #3498db;
                        color: white;
                        padding: 10px 18px;
                        text-decoration: none;
                        border-radius: 6px;
                        margin: 10px;
                    }
                    .btn:hover { background: #2980b9; }
                    form { text-align: center; margin: 20px; }
                    select { padding: 5px; margin: 0 10px; }

                    /* Footer styles */
                    .footer {
                        background-color: #2c3e50;
                        color: #fff;
                        margin-top: 40px;
                        padding: 40px 20px 10px;
                        font-size: 14px;
                    }
                    .footer-container {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                        gap: 20px;
                    }
                    .footer-logo { width: 100px; margin-bottom: 10px; }
                    .footer h3 { color: #3498db; margin-bottom: 10px; }
                    .footer a { color: #ecf0f1; text-decoration: none; }
                    .footer a:hover { color: #3498db; }
                    .footer ul { list-style: none; padding: 0; }
                    .footer ul li { margin: 6px 0; }
                    .footer-bottom {
                        text-align: center;
                        border-top: 1px solid #444;
                        margin-top: 20px;
                        padding-top: 10px;
                        font-size: 13px;
                    }
                </style>
                <script>function printPayslip(){window.print();}</script>
            </head>
            <body>

            <!-- Logo at top-left -->
            <div class='logo'>
                <img src='logo1.jpeg' alt='Optimus Logo'>
            </div>

            <div class='container'>
                <h1>Employee Payslip</h1>
        """);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/vignesh", "root", "root");

            String query = """
                SELECT e.eid, e.name, e.designation, e.dateofjoining, e.address,
                       p.basicpay, p.houserentallowances, p.specialallowances,
                       p.transport, p.pf, p.tax
                FROM employee e
                JOIN payroll p ON e.eid = p.eid
                WHERE e.eid = ?
                """;

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

                java.sql.Date doj = rs.getDate("dateofjoining");
                LocalDate joiningDate = doj.toLocalDate();
                int joinYear = joiningDate.getYear();
                Month joinMonth = joiningDate.getMonth();
                int joinMonthValue = joinMonth.getValue();
                int currentYear = LocalDate.now().getYear();

                String[] allMonths = {"january","february","march","april","may","june","july",
                                      "august","september","october","november","december"};
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
                if (selectedMonth != null && selectedYear != null) {
                    String leaveQuery = "SELECT paidleaves FROM attendance WHERE eid=? AND month=? AND year=?";
                    PreparedStatement ps2 = con.prepareStatement(leaveQuery);
                    ps2.setInt(1, eid);
                    ps2.setString(2, selectedMonth.toLowerCase());
                    ps2.setInt(3, Integer.parseInt(selectedYear));
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        paidLeaves = rs2.getInt("paidleaves");
                        double leaveDeduction = (gross / 30) * paidLeaves;
                        net -= leaveDeduction;
                        deductions += leaveDeduction;
                    }
                }

                out.printf("""
                    <form method='get' action='find'>
                        <input type='hidden' name='eid' value='%d'/>
                        <label>Month:</label>
                        <select name='month'>%s</select>
                        <label>Year:</label>
                        <select name='year' onchange='this.form.submit()'>%s</select>
                        <button type='submit' class='btn'>View Payslip</button>
                    </form>

                    <div class='employee-details'>
                        <h2>Employee Details</h2>
                        <p><strong>Employee ID:</strong> %d</p>
                        <p><strong>Name:</strong> %s</p>
                        <p><strong>Designation:</strong> %s</p>
                        <p><strong>Date of Joining:</strong> %s</p>
                        <p><strong>Address:</strong> %s</p>
                    </div>

                    <div class='tables'>
                        <table>
                            <tr><th colspan='2'>Earnings</th></tr>
                            <tr><td>Basic Pay</td><td>₹%.2f</td></tr>
                            <tr><td>House Rent Allowance</td><td>₹%.2f</td></tr>
                            <tr><td>Special Allowance</td><td>₹%.2f</td></tr>
                            <tr><td>Transport Allowance</td><td>₹%.2f</td></tr>
                            <tr class='total'><td>Total Earnings</td><td>₹%.2f</td></tr>
                        </table>

                        <table>
                            <tr><th colspan='2'>Deductions</th></tr>
                            <tr><td>PF</td><td>₹%.2f</td></tr>
                            <tr><td>Tax</td><td>₹%.2f</td></tr>
                            <tr><td>Leave Deduction (%d Paid Leaves)</td><td>₹%.2f</td></tr>
                            <tr class='total'><td>Total Deductions</td><td>₹%.2f</td></tr>
                            <tr class='total'><td>Net Pay</td><td><b>₹%.2f</b></td></tr>
                        </table>
                    </div>
                    """, eid, monthOptions, yearOptions,
                    rs.getInt("eid"), rs.getString("name"), rs.getString("designation"),
                    rs.getDate("dateofjoining"), rs.getString("address"),
                    basic, hra, special, transport, gross,
                    pf, tax, paidLeaves, (gross/30)*paidLeaves, deductions, net);
            } else {
                out.println("<h3 style='color:red;text-align:center;'>No record found for Employee ID: " + eid + "</h3>");
            }

        } catch (Exception e) {
            out.println("<p style='color:red;text-align:center;'>Error: " + e.getMessage() + "</p>");
        }

        // Close container BEFORE footer
        out.println("""
            <div class='actions'>
                <a href='payslip.html' class='btn'>Back</a>
                <a href='#' class='btn' onclick='printPayslip()'>Print Payslip</a>
            </div>
            </div> <!-- End container -->

            <footer class='footer'>
                <div class='footer-container'>
                    <div class='footer-about'>
                        <p>Vtalent solutions is an IT consultancy firm that takes pride in solving your toughest challenges.
                        Our team of experienced consultants are dedicated to delivering the best possible outcomes,
                        whether it's finding the perfect technological solution or providing the right advice.</p>
                    </div>

                    <div class='footer-links'>
                        <h3>Useful Links</h3>
                        <ul>
                            <li><a href='#'>Home</a></li>
                            <li><a href='#'>About Us</a></li>
                            <li><a href='#'>Services</a></li>
                            <li><a href='#'>Industries</a></li>
                            <li><a href='#'>Careers</a></li>
                            <li><a href='#'>Privacy Policy</a></li>
                        </ul>
                    </div>

                    <div class='footer-contact'>
                        <h3>Contact Us</h3>
                        <p>Unit 5, The Freehold Industrial Centre<br>
                        Amberly Way, Hounslow<br>
                        Middlesex TW4 6BX</p>
                        <p>📞 +44 20 7993 5143<br>
                        ✉️ <a href='mailto:vtalent@solutions.co.uk'>admin@vtalentsolutions.co.uk</a></p>
                    </div>

                    <div class='footer-news'>
                        <h3>Our Newsletter</h3>
                        <p>Administrative tasks and project management are very difficult to do on Excel
                        when the team grows larger than 10–15 employees. We believe in providing SMEs
                        affordable tools that can save time and keep all information in one place so
                        they can focus on their business growth.</p>
                    </div>
                </div>
                <div class='footer-bottom'>
                    <p>© Copyright Vtalent Solutions LTD. All Rights Reserved.</p>
                </div>
            </footer>
            </body>
            </html>
        """);
    }
}
