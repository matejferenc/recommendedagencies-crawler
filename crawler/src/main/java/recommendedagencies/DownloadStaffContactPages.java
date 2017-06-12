package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DownloadStaffContactPages {

    private static final List<String> subPages = Arrays.asList("", "clients", "staff", "finances", "rate-card", "contact");

    public static void main(String[] args) throws IOException, InvalidFormatException {
        downloadPages();
    }

    private static void downloadPages() throws IOException {
        HashMap map = readMainData();
        File companiesDirectory = new File("companies");

        List<HashMap> responseList = (List<HashMap>) map.get("response");
        for (int i = 0; i < responseList.size(); i++) {
            HashMap company = responseList.get(i);
            String id = ((String) company.get("id"));
            String companyUrl = (String) company.get("url");
            String companyTitle = (String) company.get("title");
            System.out.println("Page " + companyTitle + " started");
            File companyFolder = new File(companiesDirectory, id);

            String subPage = "staff";
            File subPageSourceCodeFile = new File(companyFolder, subPage + ".html");

            String pageSourceCode;
            try {
                byte[] bytes = Files.readAllBytes(subPageSourceCodeFile.toPath());
                pageSourceCode = new String(bytes, "UTF-8").replaceAll("&amp;", "&");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }


            Pattern pattern = Pattern.compile("<div class=\"item half[^>]*>\\s*<div[^>]*>\\s*<a[^>]*>\\s*<img[^>]*>\\s*</a>\\s*</div>\\s*<h2>\\s*<a href=\"([^>]+)\">");
            Matcher matcher = pattern.matcher(pageSourceCode);
            while (matcher.find()) {
                File staffFolder = new File(companyFolder, "staff");
                staffFolder.mkdir();

                String url = matcher.group(1);
                System.out.println(url);
                Pattern pat = Pattern.compile("http://www.recommendedagencies.com/([^/]+)/staff/(\\d+)/");
                Matcher mat = pat.matcher(url);
                if (mat.find()) {
                    String staffId = mat.group(2);
                    String staffPageContent = HttpUtils.get(url);
                    try {
                        PrintWriter writer = new PrintWriter(new File(staffFolder, staffId + ".html"));
                        writer.print(staffPageContent);
                        System.out.println("Page " + url + " downloaded");
                    } catch (FileNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    throw new IllegalStateException("id not found in url " + url);
                }
            }
            System.out.println("Page " + companyTitle + " finished");
        }

    }

    private static HashMap readMainData() throws IOException {
        File file = new File("main_data.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, HashMap.class);
    }

}
