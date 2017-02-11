package farm.bsg.pages;

import java.util.List;
import farm.bsg.Security.Permission;
import farm.bsg.data.RawObject;
import farm.bsg.data.Value;
import farm.bsg.html.Block;
import farm.bsg.html.Html;
import farm.bsg.html.HtmlPump;
import farm.bsg.html.Input;
import farm.bsg.html.Table;
import farm.bsg.html.shit.ObjectModelForm;
import farm.bsg.models.PayrollEntry;
import farm.bsg.models.Person;
import farm.bsg.ops.CounterCodeGen;
import farm.bsg.pages.common.SessionPage;
import farm.bsg.route.RoutingTable;
import farm.bsg.route.SessionRequest;

public class Payroll extends SessionPage {

    public Payroll(SessionRequest session) {
        super(session, "/payroll");
    }

    public static HtmlPump payrollTable(List<PayrollEntry> entries, boolean sessionEdit, boolean summary) {
        Table table = Html.table("Date", "Hours", "Mileage", "Benefits", "Taxes", "Owed", "Actions");
        double totalHours = 0.0;
        double totalMileage = 0.0;
        double totalBenefits = 0.0;
        double totalTaxes = 0.0;
        double totalOwed = 0.0;
        for (PayrollEntry entry : entries) {
            HtmlPump actions = null;
            if (entry.isOutstanding() && sessionEdit) {
                actions = Html.link("/payroll-wizard?id=" + entry.getId(), "revisit").btn_secondary();
            }

            double hours = entry.getAsDouble("hours_worked") + entry.getAsDouble("sick_leave_used") + entry.getAsDouble("pto_used");
            totalHours += hours;
            totalMileage += entry.getAsDouble("mileage");
            totalBenefits += entry.getAsDouble("benefits");
            totalTaxes += entry.getAsDouble("taxes");
            totalOwed += entry.getAsDouble("owed");

            table.row( //
                    entry.get("fiscal_day"), //
                    hours, //
                    entry.get("mileage"), //
                    entry.get("benefits"), //
                    entry.get("taxes"), //
                    entry.get("owed"), //
                    actions);
        }

        if (summary && entries.size() > 0) {
            table.footer("*", totalHours, totalMileage, totalBenefits, totalTaxes, totalOwed, "");
        }
        

        return table;
    }

    public HtmlPump getReportPayrollLink(Person person) {
        String fiscalDay = person().getCurrentDay();
        String payrollIdRoot = person().getId() + ";" + fiscalDay;
        String payrollId = payrollIdRoot;
        String label = "Report";
        int attempt = 1;
        while (true) {
            PayrollEntry entry = session.engine.payrollentry_by_id(payrollId, false);
            if (entry == null) {
                return Html.link("/payroll-wizard?id=" + payrollId, label + " Payroll (" + fiscalDay + ")").btn_primary();
            } else {
                if (entry.isOutstanding()) {
                    return Html.link("/payroll-wizard?id=" + payrollId, "Update Payroll (" + fiscalDay + ")").btn_primary();
                } else {
                    label = "Amend";
                    payrollId = payrollIdRoot + "_" + attempt;
                    attempt++;
                }
            }
        }
    }

    public String payroll() {
        Block page = Html.block();

        page.add(Html.wrapped().h5().wrap("Unpaid Work"));
        List<PayrollEntry> unpaid = session.engine.select_payrollentry() //
                .where_unpaid_eq(person().getId()) //
                .to_list() //
                .inline_order_lexographically_asc_by("reported") //
                .done();
        page.add(payrollTable(unpaid, true, true));

        page.add(Html.wrapped().h5().wrap("Actions"));
        page.add(Html.wrapped().ul() //
                .wrap(Html.wrapped().li().wrap(getReportPayrollLink(person()))) //
        );
        return finish_pump(page);
    }

    private PayrollEntry pullPayroll() {
        PayrollEntry payroll = query().payrollentry_by_id(session.getParam("id"), true);
        payroll.importValuesFromReqeust(session, "");
        return payroll;
    }

    public String full_edit() {
        PayrollEntry payroll = pullPayroll();
        StringBuilder sb = new StringBuilder();
        sb.append("<h5>Edit: " + payroll.getId() + "</h5>");
        sb.append("<form method=\"post\" action=\"/commit-payroll-edit\">");
        sb.append(ObjectModelForm.htmlOf(payroll));
        sb.append("<hr /><input type=\"submit\">");
        sb.append("</form>");
        return sb.toString();
    }

    private void lockMonthlyBenefits() {
        String id = person().getId() + ";benefits_for_" + person().getCurrentMonth();
        String key = "payroll/" + id;
        Value v = session.engine.storage.get(key);
        if (v == null) {
            PayrollEntry payroll = new PayrollEntry();
            payroll.set("id", id);
            if (payroll.executeBenefits(person())) {
                if (payroll.getOwed() > 0) {
                    session.engine.save(payroll);
                }
            }
        }
    }

    public String commit() {
        PayrollEntry payroll = pullAndCorrectPayroleForReporter();

        if (!payroll.isOutstanding()) {
            return "Not sure how this happened, but this record has been committed to a check... so that's awkward";
        }

        lockMonthlyBenefits();

        // fix up the payroll
        if (payroll.get("person").equals(person().getId())) {
            payroll.normalize(person());
        }

        Value commitValue = null;
        if (payroll.getOwed() > 0.01) {
            commitValue = new Value(payroll.toJson());
        }
        session.engine.storage.put("payroll/" + payroll.get("id"), commitValue);
        redirect("/payroll");
        return null;

    }

    public PayrollEntry pullAndCorrectPayroleForReporter() {
        PayrollEntry payroll = pullPayroll();
        if (payroll.get("reported") == null) {
            payroll.set("reported", RawObject.isoTimestamp());
            payroll.set("fiscal_day", person().getCurrentDay());
        }
        payroll.normalize(person());
        return payroll;
    }

    public String wizard() {
        String stage = session.getParam("stage");
        if (stage == null) {
            stage = "start";
        }

        if (stage.equals("done")) {
            return commit();
        }

        StringBuilder sb = new StringBuilder();
        PayrollEntry payroll = pullAndCorrectPayroleForReporter();
        sb.append("\n\n<form class=\"form-small\" method=\"post\" action=\"/payroll-wizard\">");
        sb.append(Html.input("id").value(payroll.getId()));
        sb.append("<h2 class=\"form-small-heading\">Report Work</h2>");
        sb.append("\n<label for=\"hours_worked\">Hours Worked</label>\n");
        sb.append(new Input("hours_worked").text().id_from_name().autofocus().value(payroll.get("hours_worked")).placeholder("hours...").clazz("form-control").toHtml());

        if (stage.equals("more")) {
            
            // correct for mileage
            if (payroll.getAsDouble("mileage") <= 0.01) {
                payroll.set("mileage", person().get("default_mileage"));
            }
            
            sb.append("\n<label for=\"mileage\">Mileage</label>\n");
            sb.append(new Input("mileage").text().id_from_name().value(payroll.get("mileage")).placeholder("miles...").clazz("form-control").toHtml());

            sb.append("\n<label for=\"pto_used\">PTO</label>\n");
            sb.append(new Input("pto_used").text().id_from_name().value(payroll.get("pto_used")).placeholder("hours...").clazz("form-control").toHtml());

            sb.append("\n<label for=\"sick_leave_used\">Sick Leave</label>\n");
            sb.append(new Input("sick_leave_used").text().id_from_name().value(payroll.get("sick_leave_used")).placeholder("hours...").clazz("form-control").toHtml());
        }

        if (stage.equals("start")) {
            sb.append("<input class=\"btn btn-secondary\" type=\"submit\" name=\"stage\" value=\"more\">");
        }
        sb.append("<input class=\"btn btn-primary\" type=\"submit\" name=\"stage\" value=\"done\">");
        sb.append("</form>\n");

        return formalize_html(sb.toString());
    }

    public static void link(RoutingTable routing) {
        routing.navbar("/payroll", "Payroll", Permission.SeePayrollTab);
        routing.get_or_post("/payroll", (session) -> new Payroll(session).payroll());

        routing.get_or_post("/payroll-edit", (session) -> new Payroll(session).full_edit());

        routing.get_or_post("/payroll-wizard", (session) -> new Payroll(session).wizard());
        routing.get_or_post("/commit-payroll-edit", (session) -> new Payroll(session).commit());
    }

    public static void link(CounterCodeGen c) {
        c.section("Page: Payroll");
    }

}