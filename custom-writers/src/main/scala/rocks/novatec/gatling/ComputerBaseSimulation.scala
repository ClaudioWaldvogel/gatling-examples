package rocks.novatec.gatling

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

class ComputerBaseSimulation extends Simulation {

  private val httpConf = http
    .baseURL("http://computer-database.gatling.io")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .disableWarmUp
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")


  private val scn = scenario("Awesome Writer Example")
    .group("Computer Database Test") {
      feed(csv("searchFeeder.csv").circular)
        .exec(http("Home Page")
          .get("/"))
        .group("Search Notebooks") {
          exec(http("Search ${searchCriterion}")
            .get("/computers?f=${searchCriterion}")
            .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL"))
            .extraInfoExtractor(extraInfo => {
              List(s"extra__${extraInfo.session.attributes("searchComputerName")}")
            }))
        }
        .group("Select Notebook") {
          exec(http("Select ${searchComputerName}")
            .get("${computerURL}"))
        }
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)

}
