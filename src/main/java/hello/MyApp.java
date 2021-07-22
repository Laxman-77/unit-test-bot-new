package hello;

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import driver.TestRunnerDriver;
import services.JenkinsParser;
import services.MapUtils;

import java.io.IOException;
import java.util.*;


/**
 * @author laxman.goliya
 * @date 20/07/2021
 */

public class MyApp {
    public static void main(String[] args) throws Exception {
        // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)

        App app = new App();


        final Set<String> ALLOWED_CHANNELS = Set.of("unit-test-bot","paid-backend","random");
        final Set<String> ALLOWED_DOMAINS = Set.of("unit-test-bot","sprinklr");

        app.command("/help", (req,ctx) -> {
            SlashCommandPayload payload = req.getPayload();

            if(!ALLOWED_DOMAINS.contains(payload.getTeamDomain())) return ctx.ack("Your TeamDomain is not authorized to use this bot.");
            if(!ALLOWED_CHANNELS.contains(payload.getChannelName())) return ctx.ack("This channel is not authorized to use this bot.");

            return ctx.ack(" ```      All commands:\n" +
                    "      1. /get_all_failures <build-no> : Get all failures in the given build With their authors\n" +
                    "      2. /get_test_count <time-frame> : Get map of author-test_counts added in given time-frame\n" +
                    "      3. /get_tests_added_by_author <author> <time-frame> : Get count of new tests added by author\n" +
                    "      4. /get_tests_failed_by_author <author> <time-frame> <build-no> : Get failed tests written by author in given timeframe\n" +
                    "      5. /help : Get list of commands\n" +
                    "     \n" +
                    "        Supported Time Frames : ThisWeek , LastWeek , LastSevenDays , ThisMonth , LifeTime ```");
        });


        app.command("/get_all_failures",(req,ctx) -> {
            SlashCommandPayload payload = req.getPayload();
            System.out.println(payload.getTeamDomain());
            System.out.println(req.getPayload().getChannelName());
            System.out.println(req.getPayload().getTeamDomain());

            if(!ALLOWED_DOMAINS.contains(payload.getTeamDomain())) return ctx.ack("Your TeamDomain is not authorized to use this bot.");
            if(!ALLOWED_CHANNELS.contains(payload.getChannelName())) return ctx.ack("This channel is not authorized to use this bot.");

            String text = payload.getText();
            String[] temp = text.split(" ");
            if(temp.length!=1){
                return ctx.ack("Invalid Command. Write build_number after the command.");
            }

            int buildNr = Integer.parseInt(temp[0]);

            Thread thread = new Thread(()-> {
                try {
                    TestRunnerDriver.timeFrameSetup("LifeTime");
                    HashMap<String, String> authorMap = TestRunnerDriver.getAuthorMap();
                    String[] headings = {"Test Name","Author Name"};
                    String mapTable = MapUtils.getMapAsTableString(authorMap,headings);
                    List<String> allFailedTests = JenkinsParser.getFailuresList(buildNr);
                    HashMap<String, String> fullClassName = TestRunnerDriver.getFullClassName();

                    LinkedHashMap<String, String> failuresByAuthor = new LinkedHashMap<>();

                    int cnt=0;
                    for (String test : allFailedTests) {
                        failuresByAuthor.put(test, authorMap.get(test));
                        cnt++;
                        if(cnt>10) break;
                    }


                    String failureTestAuthorMapTable = MapUtils.getMapAsTableString(failuresByAuthor,headings);

                    String JENKINS_PREFIX = "https://qa4-automation-jenkins-reports.sprinklr.com/CI_Test/builds/";
                    String JENKINS_SUFFIX = "/htmlreports/Reports/index.html";
                    String JENKINS_URL = JENKINS_PREFIX + buildNr + JENKINS_SUFFIX ;
                    String message = "Here are all the failed tests in build "+ buildNr+":\n" +mapTable+ "\n"; // todo - change it back to failureTestAuthorMapTable
                    message = "```" + message + "<" + JENKINS_URL + "|Show More> ```";

                    System.out.println(failureTestAuthorMapTable);

                    final String response = message;
                   // if(allFailedTests.size()>0)
                    ctx.respond( res -> res.responseType("in_channel").text(response));
                   // else ctx.respond("Jenkins report not found.");

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
            return ctx.ack("Thanks for your request, we'll process it and get back to you.");
        });


        app.command("/get_test_count",(req,ctx) -> {
            SlashCommandPayload payload = req.getPayload();
            System.out.println(payload.getTeamDomain());
            System.out.println(req.getPayload().getChannelName());
            System.out.println(req.getPayload().getTeamDomain());

            if(!ALLOWED_DOMAINS.contains(payload.getTeamDomain())) return ctx.ack("Your TeamDomain is not authorized to use this bot.");
            if(!ALLOWED_CHANNELS.contains(payload.getChannelName())) return ctx.ack("This channel is not authorized to use this bot.");

            String text = payload.getText();
            if(text.length() ==0) text = "LifeTime";
            String[] temp = text.split(" ");
            String timeFrame = temp[0];

            Set<String> ALLOWED_TIMEFRAMES = Set.of("ThisWeek","LastWeek","LastSevenDays","ThisMonth","LifeTime");
            if(!ALLOWED_TIMEFRAMES.contains(timeFrame)) return ctx.ack("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");


            Thread thread = new Thread(()-> {
                try {
                    TestRunnerDriver.timeFrameSetup(timeFrame);
                    HashMap<String, String> authorMap = TestRunnerDriver.getAuthorMap();

                    HashMap<String,Integer> counts = new HashMap<>();
                    for(Map.Entry entry:authorMap.entrySet()){
                        int prev = 0;
                        if(counts.get(entry.getValue().toString()) != null){
                            prev = counts.get(entry.getValue().toString());
                        };
                        counts.put(entry.getValue().toString(),prev+1);
                    }
                    String[] headings = {"Author Name","Test Count"};

                    //System.out.println(MapUtils.getMapAsTableString(authorMap,headings));
                    HashMap<String,String> testCount = new HashMap<>();
                    for(Map.Entry entry : counts.entrySet()){
                        testCount.put(entry.getKey().toString(),entry.getValue().toString());
                    }

                    String mapTable = MapUtils.getMapAsTableString(testCount,headings);
                    String message  = "Test added by developers in "+ timeFrame + " : \n";
                    message += mapTable;
                    message = "```" + message + "```";
                    System.out.println(message);

                    final String response = message;
                    ctx.respond( res -> res.responseType("in_channel").text(response));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
            return ctx.ack("Thanks for your request, we'll process it and get back to you.");
        });


        app.command("/get_tests_added_by_author",(req,ctx) -> {
            SlashCommandPayload payload = req.getPayload();
            System.out.println(payload.getTeamDomain());
            System.out.println(req.getPayload().getChannelName());
            System.out.println(req.getPayload().getTeamDomain());

            if(!ALLOWED_DOMAINS.contains(payload.getTeamDomain())) return ctx.ack("Your TeamDomain is not authorized to use this bot.");
            if(!ALLOWED_CHANNELS.contains(payload.getChannelName())) return ctx.ack("This channel is not authorized to use this bot.");

            String text = payload.getText();
            String[] temp = text.split(" ");
            if(temp.length!=2){
                return ctx.ack("Invalid Command. \n there should be Author name and TimeFrame after the command.");
            }
            String author = temp[0];
            String timeFrame = temp[1];

            Set<String> ALLOWED_TIMEFRAMES = Set.of("ThisWeek","LastWeek","LastSevenDays","ThisMonth","LifeTime");
            if(!ALLOWED_TIMEFRAMES.contains(timeFrame)) return ctx.ack("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");


            Thread thread = new Thread(()-> {
                try {
                    TestRunnerDriver.timeFrameSetup(timeFrame);
                    HashMap<String, String> authorMap = TestRunnerDriver.getAuthorMap();

                    List<String> testsByAuthor = MapUtils.getAllTestsOfAuthor(authorMap,author);

                    String message  = "Test added by "+ author +" in "+ timeFrame + " : " + testsByAuthor.size();
                    message = "```" + message + " ```";
                    System.out.println(message);

                    ctx.respond(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
            return ctx.ack("Thanks for your request, we'll process it and get back to you.");
        });


        app.command("/get_tests_failed_by_author",(req,ctx) -> {
            SlashCommandPayload payload = req.getPayload();
            System.out.println(payload.getTeamDomain());
            System.out.println(req.getPayload().getChannelName());
            System.out.println(req.getPayload().getTeamDomain());

            if(!ALLOWED_DOMAINS.contains(payload.getTeamDomain())) return ctx.ack("Your TeamDomain is not authorized to use this bot.");
            if(!ALLOWED_CHANNELS.contains(payload.getChannelName())) return ctx.ack("This channel is not authorized to use this bot.");

            String text = payload.getText();
            String[] temp = text.split(" ");
            if(temp.length!=3){
                return ctx.ack("Invalid Command. \n there should be AuthorName ,TimeFrame and BuildNumber after the command.");
            }
            String author = temp[0];
            String timeFrame = temp[1];
            int buildNr = Integer.parseInt(temp[2]);

            Set<String> ALLOWED_TIMEFRAMES = Set.of("ThisWeek","LastWeek","LastSevenDays","ThisMonth","LifeTime");
            if(!ALLOWED_TIMEFRAMES.contains(timeFrame)) return ctx.ack("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");


            Thread thread = new Thread(()-> {
                try {
                    TestRunnerDriver.timeFrameSetup(timeFrame);
                    HashMap<String, String> authorMap = TestRunnerDriver.getAuthorMap();
                    HashMap<String,String> fullClassName = TestRunnerDriver.getFullClassName();

                    List<String> testsByAuthor = MapUtils.getAllTestsOfAuthor(authorMap,author);

                    List<String> allFailedTests = JenkinsParser.getFailuresList(buildNr);
                    List<String> failedTestsByAuthor = MapUtils.getAllFailedTestsByAuthor(testsByAuthor,allFailedTests,author);

                    String embeddedListTable = MapUtils.getListAsTableString(failedTestsByAuthor,fullClassName,buildNr);

                    System.out.println(embeddedListTable);



                    String message = null;
                    if(allFailedTests.size()>0) {
                        message = "Test failed by " + author + " in build " + buildNr + " in " + timeFrame + " : " + failedTestsByAuthor.size();
                        if (failedTestsByAuthor.size() > 0)
                            message += "\nHere are the failed tests with their logs :\n" + embeddedListTable;
                        message = "```" + message + " ```";
                    }
                    else message = "Jenkins report with build number " + buildNr + " not found.";

                    ctx.respond(message);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });

            thread.start();
            return ctx.ack("Thanks for your request, we'll process it and get back to you.");
        });


        app.command("/hello", (req, ctx) -> {

            if(!ALLOWED_CHANNELS.contains(req.getPayload().getChannelName())) return ctx.ack("You are not allowed to use this command in this channel.");

            String result = ":wave: hello "+ req.getPayload().getUserName();

            System.out.println(req.toString());
            return ctx.ack(result);
        });


        SlackAppServer server = new SlackAppServer(app);
        server.start(); // http://localhost:3000/slack/events
    }
}