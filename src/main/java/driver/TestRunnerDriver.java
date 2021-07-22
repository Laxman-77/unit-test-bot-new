package driver;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import tests.CalculatorTestSuite1;
import tests.CalculatorTestSuite2;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * @author laxman.goliya
 * @date 20/07/2021
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CalculatorTestSuite1.class,
        CalculatorTestSuite2.class
})
public class TestRunnerDriver {
    private static final String TEST_DIR = "src/main/";  // todo - update TEST_DIR to your current folder after excluding packages
    private static final String FILE_PREFIX = TEST_DIR + "java/";

    private static HashMap<String, String> fullClassName = new HashMap<>();

    private static long startTime, endTime;

    public static void timeFrameSetup(String timeFrame) {
        /**
         * Setting the startTime and endTime according to the given timeFrame
         * LastWeek : Last Monday to Last Sunday
         * ThisWeek : This Monday to Now
         * LastSevenDays : 7 Days before today to Today
         * ThisMonth : First Day of Month to Now
         * LifeTime : Till Now
         */

        switch (timeFrame) {
            case "LastWeek": {
                LocalDate localDate = LocalDate.now();
                LocalDateTime time =
                        localDate.atStartOfDay().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY)).with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
                startTime = time.toEpochSecond(ZoneOffset.UTC);

                localDate = LocalDate.now();
                time = localDate.atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                endTime = time.toEpochSecond(ZoneOffset.UTC);
                break;
            }
            case "ThisWeek": {
                LocalDate localDate = LocalDate.now();
                LocalDateTime time = localDate.atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                startTime = time.toEpochSecond(ZoneOffset.UTC);

                endTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                break;
            }
            case "LastSevenDays": {
                startTime = new DateTime().withTimeAtStartOfDay().minusDays(6).getMillis() / 1000;
                endTime = new DateTime().getMillis() / 1000;
                break;
            }
            case "ThisMonth": {
                LocalDate localDate = LocalDate.now();
                LocalDateTime time = localDate.atStartOfDay().with(TemporalAdjusters.firstDayOfMonth());
                startTime = time.toEpochSecond(ZoneOffset.UTC);

                endTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                break;
            }
            case "LifeTime":{
                startTime = 0;
                endTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            }

            default:
                return;
        }
    }

    public static HashMap<String,String> getAuthorMap() throws ClassNotFoundException, IOException {

        Class currentClass = new Object(){}.getClass().getEnclosingClass(); // TestRunnerWithTimeFrame.class

        HashMap<String,String > authorMap = new HashMap<>(); // map for @Test method() --> AuthorName
        fullClassName = new HashMap<>(); // map for class.simpleName to class.fullName (class name with package)

        Suite.SuiteClasses testSuiteClasses = (Suite.SuiteClasses) currentClass.getAnnotation(Suite.SuiteClasses.class);
        Class<?>[] allTestSuitesClasses = testSuiteClasses.value();

        ArrayList<String> testSuites = new ArrayList<>(); // Extracting All TestSuite classes in the @SuiteClasses annotations with this class
        for(Class suiteName: allTestSuitesClasses){
            testSuites.add(suiteName.getName());
        }

        for(String testSuite: testSuites) {
            System.out.println("Processing started for "+ testSuite);
            Suite.SuiteClasses suiteClasses = Class.forName(testSuite).getAnnotation(Suite.SuiteClasses.class);

            Class<?>[] classesInSuite = suiteClasses.value(); // Extracting all test classes in the @SuiteClasses annotations of respective testSuite classes

            for (Class className : classesInSuite) {

                fullClassName.put(className.getSimpleName(),className.getName());
                String fileName = FILE_PREFIX + className.getName().replace(".", "/") + ".java";
                System.out.println("Parsing File : " + fileName);
                // fileName = src/main/java/com/example/demo/xyz.java

                try {
                    BufferedReader buf = new BufferedReader(new FileReader(fileName));
                    LineNumberReader rdr = new LineNumberReader(buf);

                    long fileTimeStamp = getFileTimeStamp(fileName); // get last commit time of the File
                    if( fileTimeStamp < startTime) continue; // skip this file
                    try {
                        String line;
                        while ((line = rdr.readLine()) != null) {

                            /**
                             *  We have to map all the @Test methods to its author.
                             *  So we are reading the file line by line and Whenever we get @Test annotation, We know that our test name will be in the next line.
                             *  So we are extracting the author of that line from the Git Metadata of the file via git blame command.
                             */
                            if (line.contains("@Test")) {
                                line = rdr.readLine(); // this line contains the method name
                                String gitBlameForLine = findGitBlameForLine(fileName, rdr.getLineNumber()); // returns the <author_name> <last_committed_time>

                                if(gitBlameForLine.split(" ").length<=1) continue;

                                String authorName = gitBlameForLine.split(" ")[0];
                                String commitTime = gitBlameForLine.split(" ")[1];

                                long lastCommitTime = endTime;
                                if(isNumeric(commitTime)) lastCommitTime = Integer.parseInt(commitTime);

                                if(lastCommitTime < startTime || lastCommitTime > endTime) continue; // There is no change in this file during the Given TimeFrame

                                String methodName = getMethodName(line);
                                authorMap.put(className.getSimpleName() + ". " + methodName, authorName); // ". " added explicitly

                                //Mapping test_name (MongoPersistentPropertyCacheTest. testMongoPersistentPropertyCache_index_created) to author_name
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        buf.close();
                        rdr.close();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return authorMap;

    }

    public static HashMap<String,String> getFullClassName(){
        return fullClassName;   // Returns the map which contains the package name of the classes
    }



    /******************************   PRIVATE METHODS       ******************************/

    private static long getFileTimeStamp(String fileName){
        long fileTimeStamp = 0;
        Process pr;
        try {
            // File last commit check for early abort
            String[] logCmd = {
                    "/bin/sh",
                    "-c",
                    "git log -1 --format=%ct " + fileName
            };
            pr = Runtime.getRuntime().exec(logCmd);

            InputStream is = pr.getInputStream();
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));

            String line = buf.readLine();
            if(line != null){
                fileTimeStamp = Integer.parseInt(line);
            }

            is.close();
            buf.close();
        }
        catch (Throwable throwable) {
            System.out.println("Exception while running git log for " + fileName);
            throwable.printStackTrace();
        }
        return fileTimeStamp;
    }

    private static String findGitBlameForLine(String fileName,int lineNumber) throws IOException {
        String[] blameCmd = {
                "/bin/sh",
                "-c",
                "git blame -L "+lineNumber+",+1 " +" --line-porcelain " + fileName + " | egrep \"author-mail|committer-time\"",
        };


        Process pr;
        String line = null;
        String ret = "";
        try {
            pr = Runtime.getRuntime().exec(blameCmd);
            /**
             * author-mail <laxman.goliya>
             * committer-time 1626922714
             */

            BufferedReader buf  = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            if( (line = buf.readLine()) != null){  // reading the result of git blame command
                String commit = buf.readLine();
                if(commit == null) return "NIL";
                String authorMail = getAuthorMailFromGitBlame(line);
                long committerTime = endTime;
                try{
                    committerTime = Integer.parseInt(commit.split(" ")[1]);
                }
                catch (Exception e){
                    committerTime = endTime;
                }
                ret = authorMail+" "+committerTime;
            }
            buf.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        if(line == null) line = "NIL"; // if no line readed from git blame,then line == null
        else line = ret;
        return line;
    }

    private static String getAuthorMailFromGitBlame(String line){
        /**
         * line as "author_mail <laxman.goliya@sprinklr.com>"
         * we have to extract laxman.goliya from it.
         */

        StringBuilder builder1 = new StringBuilder(line);
        if(builder1.indexOf("@") != -1) {
            builder1.delete(builder1.indexOf("@"),builder1.length()); // remove after @
            builder1.delete(0,builder1.indexOf("<")+1); // remove before <
        }
        else builder1 = new StringBuilder("not.committed.yet");

        return builder1.toString(); // laxman.goliya
    }

    private static String getMethodName(String line){
        // line is a method definition line ex. public String fun1(){
        // remove line after (
        StringBuilder builder = new StringBuilder(line);
        String methodName = "No_Method_Found";
        if(builder.indexOf("(")!=-1) {
            builder.delete(builder.indexOf("("), line.length());
            line = builder.toString();

            String[] words = line.split(" "); // tmp = {"public","String","fun1"}
            methodName = words[words.length - 1]; // fun1
        }
        else{
            String[] words = line.split(" ");
            if(words.length>0) methodName = words[words.length-1];
        }

        return methodName;
    }

}
