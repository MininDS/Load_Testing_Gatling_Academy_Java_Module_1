//Announced package with appropriate load testing classes and materials named "computerdatabase"
package computerdatabase;



//Announced some imports with gatling additional libraries
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;



//Created public class named "MyComputerSimulation" with load testing Simulation design - variables, methods, etc
public class MyComputerSimulation extends Simulation {


    //Created private class named "HttpProtocolBuilder" with field "httpProtocol" and value "http", some methods
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://computer-database.gatling.io")
            .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*detectportal\\.firefox\\.com.*"))
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .upgradeInsecureRequestsHeader("1")
            .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");


    //Created class-interface FeederBuilder.Batchable with field "searchFeeder" and its function "csv("data/search.csv").random()"
    FeederBuilder.Batchable searchFeeder =
            //Selects strings from search.csv file randomly
            csv("data/search.csv").random();


    //Created class-interface FeederBuilder.Batchable with field "computerFeeder" and its function "csv("data/computers.csv").circular();"
    FeederBuilder.Batchable computerFeeder =
            //Selects strings from computers.csv circularly from first to final and repeats if necessary
            csv("data/computers.csv").circular();



    //Functional action (method) - "searchForComputer"
    ChainBuilder searchForComputer =
            //Use searchFeeder above in this functional scenario
            feed(searchFeeder)

            //Step 1 - Load home page with search options for system user
            .exec(http("LoadHomePage")
                .get("/computers")

                //Check that http status-code is 200 in response
                .check(status().is(200)))
                .pause(2)

            //Step 2 - Search computer by "searchCriterion" from searchFeeder-csv-file
            .exec(http("SearchComputers_#{searchCriterion}")
                .get("/computers?f=#{searchCriterion}")

                //Check that http status-code is 200 in response
                .check(status().is(200))

                //Check that response has "searchComputerName" value from csv in href
                .check(css("a:contains('#{searchComputerName}')", "href")

                //Save response URL-address as "computerURL" local-variable
                .saveAs("computerURL")))
                .pause(2)

            //Step 3 - Open needed computer named from searchFeeder-csv-file (value of searchComputerName) with URL (computerURL) from step 2
            .exec(http("LoadComputerDetails_#{searchComputerName}")
                .get("#{computerURL}")

                //Check that http status-code is 200 in response
                .check(status().is(200)))
                .pause(2);


    //Functional action (method) - "browse"
    ChainBuilder browse =
            //Repeat 5 times (as variable n value) step in brackets
            repeat(5, "n").on(

                    //Step 1 - Get page number n
                    exec(http("Page #{n}")
                    .get("/computers?p=#{n}")

                    //Check that http status-code is 200 in response
                    .check(status().is(200)))
                    .pause(2)
            );


    //Functional actoion (method) - "createComputer"
    ChainBuilder createComputer =
            //Use "computerFeeder" above in this functional scenario
            feed(computerFeeder)

            //Step 1 - Load page for computer creation
            .exec(http("LoadCreateComputerPage")
                .get("/computers/new")

                //Check that http status-code is 200 in response
                .check(status().is(200)))
                .pause(2)

            //Step 2 - Create new custom computer with keys and values from computerFeeder-csv-file
            .exec(http("CreateNewComputer_#{computerName}")
                //Use POST-http-method for operation
                .post("/computers")

                //Use keys and its values below in request-body (keys and values from computerFeeder-csv-file)
                .formParam("name", "#{computerName}")
                .formParam("introduced", "#{introduced}")
                .formParam("discontinued", "#{discontinued}")
                .formParam("company", "#{companyId}")

                //Check that http status-code is 200 in response
                .check(status().is(200))
            );



    //Created class-ScenarioBuilder with field "admins" includes scenario-variable named "Admins" for typical admin's actions in the system
    private ScenarioBuilder admins = scenario("Admins")
            //Scenario "Admins" executes typical actions "searchForComputer", "browse", "createComputer" which were announced above
            .exec(searchForComputer, browse, createComputer);


    //Created class-ScenarioBuilder with field "users" includes scenario-variable named "Users" for typical user's actions in the system
    private ScenarioBuilder users = scenario ("Users")
            //Scenario "Users" executes typical actions "searchForComputer", "browse" which were announced above
            .exec(searchForComputer, browse);


    //Started MyComputerSimulation load testing with needed loading model
    {
        setUp(
                admins.injectOpen(atOnceUsers(1)),

                users.injectOpen(
                        nothingFor(5),
                        atOnceUsers(1),
                        rampUsers(5).during(10),
                        constantUsersPerSec(2).during(20)
                        ))
                .protocols(httpProtocol);
    }
}
