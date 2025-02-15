package computerdatabase;


import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class MyComputerSimulation extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://computer-database.gatling.io")
            .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*detectportal\\.firefox\\.com.*"))
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .upgradeInsecureRequestsHeader("1")
            .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");

    FeederBuilder.Batchable searchFeeder =
            csv("data/search.csv").random();


    ChainBuilder searchForComputer =
            exec(http("LoadHomePage")
                    .get("/computers"))
                    .pause(2)
            .feed(searchFeeder)


            .exec(http("SearchComputers_#{searchCriterion}")
                    .get("/computers?f=#{searchCriterion}")
                    .check(css("a:contains('#{searchComputerName}')", "href")
                            .saveAs("computerURL")))
                    .pause(2)
            .exec(http("LoadComputerDetails_#{searchComputerName}")
                    .get("#{computerURL}"))
                    .pause(2);



    ChainBuilder browse =
            repeat(5, "n").on(
                    exec(http("Page #{n}")
                            .get("/computers?p=#{n}"))
                            .pause(2)
            );

    FeederBuilder.Batchable computerFeeder =
            csv("data/computers.csv").circular();

    ChainBuilder createComputer =

            exec(http("LoadCreateComputerPage")
                    .get("/computers/new"))
                    .pause(2)
                    .feed(computerFeeder)


            .exec(http("CreateNewComputer_#{computerName}")
                            .post("/computers")
                            .formParam("name", "#{computerName}")
                            .formParam("introduced", "#{introduced}")
                            .formParam("discontinued", "#{discontinued}")
                            .formParam("company", "#{companyId}")
                    .check(status().is(200))
            );



    private ScenarioBuilder admins = scenario("Admins")
            .exec(searchForComputer, browse, createComputer);

    private ScenarioBuilder users = scenario ("Users")
            .exec(searchForComputer, browse);


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
