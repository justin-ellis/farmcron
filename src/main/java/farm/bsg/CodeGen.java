package farm.bsg;

import java.io.File;
import java.nio.file.Files;

import farm.bsg.data.SchemaCodeGenerator;
import farm.bsg.models.Check;
import farm.bsg.models.Chore;
import farm.bsg.models.Event;
import farm.bsg.models.Habit;
import farm.bsg.models.PayrollEntry;
import farm.bsg.models.Person;
import farm.bsg.models.SiteProperties;
import farm.bsg.models.Subscriber;
import farm.bsg.models.Subscription;
import farm.bsg.ops.CounterCodeGen;
import farm.bsg.pages.Checks;
import farm.bsg.pages.Chores;
import farm.bsg.pages.Dashboard;
import farm.bsg.pages.Events;
import farm.bsg.pages.Habits;
import farm.bsg.pages.Payroll;
import farm.bsg.pages.People;
import farm.bsg.pages.SignIn;
import farm.bsg.pages.Site;
import farm.bsg.pages.Subscriptions;
import farm.bsg.pages.You;

public class CodeGen {
    private final String path;

    public CodeGen(String path) {
        this.path = path;
    }

    private void buildQueryEngine() throws Exception {
        SchemaCodeGenerator codegen = new SchemaCodeGenerator("farm.bsg", "QueryEngine");

        codegen.addSample(new Check());
        codegen.addSample(new Chore());
        codegen.addSample(new Event());
        codegen.addSample(new Habit());
        codegen.addSample(new PayrollEntry());
        codegen.addSample(new Person());
        codegen.addSample(new SiteProperties());
        codegen.addSample(new Subscriber());
        codegen.addSample(new Subscription());
        String java = codegen.java();

        System.out.println(java);
        File f = new File(path + "QueryEngine.java");
        Files.write(f.toPath(), java.getBytes());
    }

    private void FacebookMessenger(CounterCodeGen c) {
        c.section("Facebook Messenger");
        c.counter("fb_has_invalid_host", "Facebook is setting a host header that is wrong");
        c.counter("fb_subscribe_begin", "Facebook is attempting to subscribe to the page");
        c.counter("fb_token_given_correct", "Facebook gave us the right token");
        c.counter("fb_token_given_wrong", "Facebook gave us the wrong token");
        c.counter("fb_message", "Facebook is sending us a message");
        c.counter("fb_message_valid", "We were able to parse the message");
        c.counter("fb_message_invalid", "We failed to parse the message");
        c.counter("fb_attempt_response", "The engine gave a response back from a given message");
        c.counter("fb_send_failed_no_fb_token", "We are unable to send messages since the site lacks a token");
        c.counter("fb_send_out_on_wire", "We attempted to actuall send the message");
        c.counter("fb_send_ok", "We sent a message well enough");
        c.counter("fb_send_failed_send", "We were unable to send the message via HTTP");
    }

    private void Data(CounterCodeGen c) {
        Check.link(c);
        Chore.link(c);
        Event.link(c);
        Habit.link(c);
        PayrollEntry.link(c);
        Person.link(c);
        SiteProperties.link(c);
        Subscriber.link(c);
        Subscription.link(c);
    }

    private void Pages(CounterCodeGen c) {
        Checks.link(c);
        Chores.link(c);
        Dashboard.link(c);
        Events.link(c);
        Habits.link(c);
        Payroll.link(c);
        People.link(c);
        SignIn.link(c);
        Site.link(c);
        Subscriptions.link(c);
        You.link(c);
    }

    private void buildCounters() throws Exception {
        CounterCodeGen c = new CounterCodeGen();
        FacebookMessenger(c);
        Pages(c);
        Data(c);

        File f = new File(path + "BsgCounters.java");
        Files.write(f.toPath(), c.java("bsg.farm", "BsgCounters").getBytes());
    }

    public static void main(String[] args) throws Exception {
        CodeGen codegen = new CodeGen("/home/jeffrey/projects/bsg-farm-core/src/main/java/farm/bsg/");
        codegen.buildQueryEngine();
        codegen.buildCounters();
    }
}