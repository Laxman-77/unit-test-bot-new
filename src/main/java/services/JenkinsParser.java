package services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;

public class JenkinsParser {

    public static ArrayList<String> getFailuresList( int buildNr) throws IOException {
        System.out.println("Parsing of Jenkins Report Started.");
        ArrayList<String> failedTests = new ArrayList<>();

        try{

            String JENKINS_URL = "https://qa4-automation-jenkins-reports.sprinklr.com/CI_Test/builds/"+ buildNr +"/htmlreports/Reports/index.html";

            String _oauth2_proxy = "bGF4bWFuLmdvbGl5YUBzcHJpbmtsci5jb20=|1626660983|hVrIxXsfJq-Z8qxo_3Jm-ZsOVgw=";

            /**
             * To get your _oauth2_proxy,
             * 1. go to the jenkins report url
             * 2. open the inspect page and go to network
             * 3. reload the tab, You will see 4 html-css files in your network
             * 4. click on the index.html
             * 5. go to the cookies
             * 6. find the value of _oauth2_proxy and copy it.
             * // If you are not able to copy it, click on the >> after memory tab and go to applications and in cookie you will find this value
             * 7. You can paste it here but if your code is public, then store it in some secure files.
             *
             */

            Document doc = Jsoup.connect(JENKINS_URL)
                    .userAgent("Chrome")
                    .cookie("_oauth2_proxy",_oauth2_proxy)
                    .get();

            ArrayList<String> list = new ArrayList<>();
            doc.select(".linkList li a").forEach(el -> list.add(el.text()));

            for(int i=0; i<list.size(); i+=2){
                failedTests.add(list.get(i)+". "+list.get(i+1)); // first line class name and second line test name
            }
            System.out.println("Number of total failed tests in report: "+ failedTests.size());

        }catch(IOException ioe){
            System.out.println("Exception: Exception in parsing of Jenkins Report. \nPlease check your internet connection and VPN.\nIt is also possible that the Jenkins File with this build number doesn't exist." + ioe);
        }
        finally {
            return failedTests;
        }
    }
}
