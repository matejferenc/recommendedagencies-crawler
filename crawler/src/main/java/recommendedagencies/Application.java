package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Application {

    private static final String BASE_URL = "http://search.recommendedagencies.com/api/v1/search?services[]=digital/web-development/&offset=";

    private static final List<String> subPages = Arrays.asList("", "clients", "staff", "finances", "rate-card", "contact");

    public static void main(String[] args) throws IOException {
        downloadPages();
    }

    private static void parsePages() throws IOException {
        HashMap map = readMainData();
        File companyDirectory = new File("companies");
        companyDirectory.mkdir();

        List<HashMap> responseList = (List<HashMap>) map.get("response");
        for (int i = 0; i < responseList.size(); i++) {
            HashMap company = responseList.get(i);
            String id = ((String) company.get("id"));
            String companyUrl = (String) company.get("url");
            String companyTitle = (String) company.get("title");
            System.out.println("Page " + companyTitle + " started");
            File companyFolder = new File(companyDirectory, id);
            companyFolder.mkdir();
            subPages.forEach(subPage -> {
                String pageUrl = companyUrl + subPage;
                String pageSourceCode = HttpUtils.get(pageUrl);
                File subPageSourceCodeFile = new File(companyFolder, subPage + ".html");
                try {
                    PrintWriter writer = new PrintWriter(subPageSourceCodeFile);
                    writer.print(pageSourceCode);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
                System.out.println("Page " + subPage + " downloaded");
            });
            System.out.println("Page " + companyTitle + " finished");
        }
    }

    private static void downloadPages() throws IOException {
        HashMap map = readMainData();
        File companyDirectory = new File("companies");
        companyDirectory.mkdir();

        List<HashMap> responseList = (List<HashMap>) map.get("response");
        for (int i = 0; i < responseList.size(); i++) {
            HashMap company = responseList.get(i);
            String id = ((String) company.get("id"));
            String companyUrl = (String) company.get("url");
            String companyTitle = (String) company.get("title");
            System.out.println("Page " + companyTitle + " started");
            File companyFolder = new File(companyDirectory, id);
            companyFolder.mkdir();
            subPages.forEach(subPage -> {
                String pageUrl = companyUrl + subPage;
                String pageSourceCode = HttpUtils.get(pageUrl);
                File subPageSourceCodeFile = new File(companyFolder, subPage + ".html");
                try {
                    PrintWriter writer = new PrintWriter(subPageSourceCodeFile);
                    writer.print(pageSourceCode);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
                System.out.println("Page " + subPage + " downloaded");
            });
            System.out.println("Page " + companyTitle + " finished");
        }
        boolean stop = true;
    }

    private static HashMap readMainData() throws IOException {
        File file = new File("main_data.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, HashMap.class);
    }

}
