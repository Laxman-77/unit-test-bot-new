package services;

import components.SlackResponse;
import driver.TestRunnerDriver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestHandler {
    public static SlackResponse getAllFailures(String responseUrl, String text) throws IOException, ClassNotFoundException {
        String[] temp = text.split(" ");
        if(temp.length!=1){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid Command. \n there should be a Build Number after the command.");
        }

        int buildNr = Integer.parseInt(text);

        Thread thread = new Thread(()-> {
            try {
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
                String payload = "Here are all the failured tests in build "+ buildNr+":\n" +failureTestAuthorMapTable+ "\n";
                payload = "```" + payload + "<" + JENKINS_URL + "|Show More> ```";

                System.out.println(failureTestAuthorMapTable);


                // Sending HTTP Post request with the processed payload to Slack channel
                URL url = new URL(responseUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");

                String data = "{\n  \"status\":\"200\",\"mrkdwn\":\"true\",\"response_type\":\"in_channel\",\n  \"text\":\""+payload+" \"\n}";
                System.out.println(data);

                byte[] out = data.getBytes(StandardCharsets.UTF_8);

                OutputStream stream = http.getOutputStream();
                stream.write(out);

                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                http.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        return new SlackResponse().setText("Thanks for your request, we'll process it and get back to you.");

    }

    public static SlackResponse getTestCount(String responseUrl) throws IOException, ClassNotFoundException {

        Thread thread = new Thread(()-> {
            try {
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

                System.out.println(MapUtils.getMapAsTableString(authorMap,headings));
                HashMap<String,String> testCount = new HashMap<>();
                for(Map.Entry entry : counts.entrySet()){
                    testCount.put(entry.getKey().toString(),entry.getValue().toString());
                }

                String mapTable = MapUtils.getMapAsTableString(testCount,headings);

                String payload = "Test added by authors: \n";
                payload += mapTable ;
                payload = "```" + payload + "```";
                System.out.println(payload);
                //String payload = "```Here are all the failured tests in build "+ buildNr+":\n" +failureTestAuthorMapTable+ " ```";

                //System.out.println(failureTestAuthorMapTable);


                // Sending HTTP Post request with the processed payload to Slack channel
                URL url = new URL(responseUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");

                String data = "{\n  \"status\":\"200\",\"mrkdwn\":\"true\",\"response_type\":\"in_channel\",\n  \"text\":\""+payload+" \"\n}";
                System.out.println(data);

                byte[] out = data.getBytes(StandardCharsets.UTF_8);

                OutputStream stream = http.getOutputStream();
                stream.write(out);

                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                http.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        return new SlackResponse().setText("Thanks for your request, we'll process it and get back to you.");

    }

    public static SlackResponse getTestCountInTimeFrame(String responseUrl, String text) throws IOException, ClassNotFoundException {
        Set<String> allowedTimeFrames = Set.of("LastWeek","LastSevenDays","ThisWeek","ThisMonth","LifeTime");

        String[] temp = text.split(" ");
        if(temp.length!=1){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid Command. \n there should be TimeFrame after the command.");
        }
        String timeFrame = temp[0];
        //System.out.println(temp[0] + " : " + temp[1] + " : " + temp[2]);

        if(!allowedTimeFrames.contains(timeFrame)){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");
        }

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

                System.out.println(MapUtils.getMapAsTableString(authorMap,headings));
                HashMap<String,String> testCount = new HashMap<>();
                for(Map.Entry entry : counts.entrySet()){
                    testCount.put(entry.getKey().toString(),entry.getValue().toString());
                }

                String mapTable = MapUtils.getMapAsTableString(testCount,headings);

                String payload = "Test added by authors in "+ timeFrame +" : \n";
                payload += mapTable ;
                payload = "```" + payload + "```";
                System.out.println(payload);
                //String payload = "```Here are all the failured tests in build "+ buildNr+":\n" +failureTestAuthorMapTable+ " ```";

                //System.out.println(failureTestAuthorMapTable);


                // Sending HTTP Post request with the processed payload to Slack channel
                URL url = new URL(responseUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");

                String data = "{\n  \"status\":\"200\",\"mrkdwn\":\"true\",\"response_type\":\"in_channel\",\n  \"text\":\""+payload+" \"\n}";
                System.out.println(data);

                byte[] out = data.getBytes(StandardCharsets.UTF_8);

                OutputStream stream = http.getOutputStream();
                stream.write(out);

                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                http.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        thread.start();
        return new SlackResponse().setText("Thanks for your request, we'll process it and get back to you.");

    }

    public static SlackResponse getTestAddedByAuthorInTimeframe(String responseUrl,String text) throws IOException, ClassNotFoundException {
        Set<String> allowedTimeFrames = Set.of("LastWeek","LastSevenDays","ThisWeek","ThisMonth","LifeTime");

        String[] temp = text.split(" ");
        if(temp.length!=2){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid Command. \n there should be Author name and TimeFrame after the command.");
        }
        String author = temp[0];
        String timeFrame = temp[1];
        //System.out.println(temp[0] + " : " + temp[1] + " : " + temp[2]);
        //int buildNr = Integer.parseInt(temp[2]);

        if(!allowedTimeFrames.contains(timeFrame)){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");
        }

        Thread thread = new Thread(()-> {
            try{
                TestRunnerDriver.timeFrameSetup(timeFrame);
                HashMap<String,String> authorMap = TestRunnerDriver.getAuthorMap();
                HashMap<String,String> fullClassName = TestRunnerDriver.getFullClassName();

                List<String> testsByAuthor = MapUtils.getAllTestsOfAuthor(authorMap,author);

                //List<String> allFailedTests = JenkinsParser.getFailuresList(buildNr);
                //List<String> failedTestsByAuthor = MapUtils.getAllFailedTestsByAuthor(testsByAuthor,allFailedTests,author);

                //String embeddedListTable = MapUtils.getListAsTableString(failedTestsByAuthor,fullClassName,buildNr);

                //System.out.println(embeddedListTable);


                String payload = "Tests added by " + author + " in " + timeFrame + " : " + testsByAuthor.size();
                payload = "```" + payload + " ```";


                // Sending HTTP Post request with the processed payload to Slack channel
                URL url = new URL(responseUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");

                String data = "{\n  \"status\":\"200\",\"response_type\":\"in_channel\",\n  \"text\":\""+payload+"\"\n}";
                System.out.println(data);

                byte[] out = data.getBytes(StandardCharsets.UTF_8);

                OutputStream stream = http.getOutputStream();
                stream.write(out);

                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                http.disconnect();

            } catch (ProtocolException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (MalformedURLException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
        System.out.println("Thread Starting");
        thread.start();

        return new SlackResponse()
                .setResponseType("ephemeral")
                .setText("Thanks for your request, we'll process it and get back to you.");

    }

    public static SlackResponse getTestFailedByAuthorInTimeframe(String responseUrl,String text) throws IOException, ClassNotFoundException {
        Set<String> allowedTimeFrames = Set.of("LastWeek","LastSevenDays","ThisWeek","ThisMonth","LifeTime");

        String[] temp = text.split(" ");
        if(temp.length!=3){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid Command. \n there should be Author name, TimeFrame and Build Number after the command.");
        }
        String author = temp[0];
        String timeFrame = temp[1];
        System.out.println(temp[0] + " : " + temp[1] + " : " + temp[2]);
        int buildNr = Integer.parseInt(temp[2]);

        if(!allowedTimeFrames.contains(timeFrame)){
            return new SlackResponse()
                    .setResponseType("ephemeral")
                    .setText("Invalid TimeFrame.\nThese are allowed timeFrames: LastWeek, LastSevenDays,ThisWeek,ThisMonth, LifeTime.\n");
        }

        Thread thread = new Thread(()-> {
            try{
                TestRunnerDriver.timeFrameSetup(timeFrame);
                HashMap<String,String> authorMap = TestRunnerDriver.getAuthorMap();
                HashMap<String,String> fullClassName = TestRunnerDriver.getFullClassName();

                List<String> testsByAuthor = MapUtils.getAllTestsOfAuthor(authorMap,author);

                List<String> allFailedTests = JenkinsParser.getFailuresList(buildNr);
                List<String> failedTestsByAuthor = MapUtils.getAllFailedTestsByAuthor(testsByAuthor,allFailedTests,author);

                String embeddedListTable = MapUtils.getListAsTableString(failedTestsByAuthor,fullClassName,buildNr);

                System.out.println(embeddedListTable);

                String payload = null;
                if(allFailedTests.size()>0) {
                    payload = "Tests failed by " + author + " in given build "+buildNr+ " written in " + timeFrame + " : " + failedTestsByAuthor.size();
                    if(failedTestsByAuthor.size() != 0) {
                        payload += "\nHere are the failed tests :\n ";
                        payload += embeddedListTable;
                    }
                    else payload += "\n No failed tests to show.\n ";
                    payload = "```" + payload + " ```";
                }
                else payload="Jenkins File with build number "+buildNr+" is not accessible. ";


                // Sending HTTP Post request with the processed payload to Slack channel
                URL url = new URL(responseUrl);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Content-Type", "application/json");

                String data = "{\n  \"status\":\"200\",\"response_type\":\"in_channel\",\n  \"text\":\""+payload+"\"\n}";
                System.out.println(data);

                byte[] out = data.getBytes(StandardCharsets.UTF_8);

                OutputStream stream = http.getOutputStream();
                stream.write(out);

                System.out.println(http.getResponseCode() + " " + http.getResponseMessage());
                http.disconnect();

            } catch (ProtocolException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (MalformedURLException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
        System.out.println("Thread Starting");
        thread.start();

        return new SlackResponse()
                .setResponseType("ephemeral")
                .setText("Thanks for your request, we'll process it and get back to you.");

    }
}
