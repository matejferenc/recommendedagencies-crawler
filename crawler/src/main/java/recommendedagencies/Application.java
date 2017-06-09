package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application {

    private static final String BASE_URL = "http://search.recommendedagencies.com/api/v1/search?services[]=digital/web-development/&offset=";

    private static final List<String> subPages = Arrays.asList("", "clients", "staff", "finances", "rate-card", "contact");

    public static void main(String[] args) throws IOException {
        createModel();
    }

    private static void createModel() throws IOException {
        HashMap map = readMainData();
        File companyDirectory = new File("companies");
        companyDirectory.mkdir();

        List<Company> companies = new ArrayList<>();

        List<HashMap> responseList = (List<HashMap>) map.get("response");
//        int size = responseList.size();
        int size = 9;
        for (int i = 0; i < size; i++) {
            HashMap company = responseList.get(i);
            String id = ((String) company.get("id"));
            String companyTitle = (String) company.get("title");
            System.out.println("Company " + companyTitle + " started");
            File companyFolder = new File(companyDirectory, id);

            Company c = new Company();
            companies.add(c);

            subPages.forEach(subPage -> {
                File subPageSourceCodeFile = new File(companyFolder, subPage + ".html");
                String pageSourceCode = null;
                try {
                    byte[] bytes = Files.readAllBytes(subPageSourceCodeFile.toPath());
                    pageSourceCode = new String(bytes, "UTF-8").replaceAll("&amp;", "&");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                parse(subPage, pageSourceCode, c);
                System.out.println("Page " + subPage + " parsed");
            });
            System.out.println("Company " + companyTitle + " finished");
        }
    }

    private static void parse(String subPage, String pageContent, Company c) {
        switch (subPage) {
            case "":
                parseOverview(pageContent, c);
                break;
            case "clients":
                parseClients(pageContent, c);
                break;
            case "staff":
                parseStaff(pageContent, c);
                break;
            case "finances":
                parseFinances(pageContent, c);
                break;
            case "contact":
                parseContact(pageContent, c);
                break;
            case "rate-card":
                break;
            default:
                throw new IllegalArgumentException(subPage);
        }
    }

    private static void parseContact(String pageContent, Company c) {
        String website = parseContactField(pageContent, "Website");
        String contactEmail = parseContactField(pageContent, "Contact email");
        String twitter = parseTwitter(pageContent);
        String facebook = parseContactField(pageContent, "Facebook");
        String linkedIn = parseContactField(pageContent, "LinkedIn");

        Contact contact = new Contact();
        c.contact = contact;

        contact.website = website;
        contact.contactEmail = contactEmail;
        contact.twitter = twitter;
        contact.facebook = facebook;
        contact.linkedIn = linkedIn;

        List<ContactAddress> addresses = parseAddresses(pageContent);

        contact.contactAddresses = addresses;
    }

    private static List<ContactAddress> parseAddresses(String pageContent) {
        List<ContactAddress> addresses = new ArrayList<>();
        int start = 0;
        while (true) {
            int adIndex = pageContent.indexOf("<strong>Address</strong>:", start);
            if (adIndex < 0) {
                break;
            }
            int ceIndex = pageContent.indexOf("<strong>Contact email</strong>:", adIndex + 25);
            if (ceIndex < 0) {
                throw new IllegalStateException("Contact email not found after index " + adIndex + 25);
            }
            String addressUnparsed = pageContent.substring(adIndex + 25, ceIndex);
            int pnIndex = pageContent.indexOf("<strong>Phone number</strong>:", ceIndex + 31);
            if (pnIndex < 0) {
                throw new IllegalStateException("Phone number not found after index " + ceIndex + 31);
            }
            String contactEmailUnparsed = pageContent.substring(ceIndex + 31, pnIndex);
            int endTdIndex = pageContent.indexOf("</td>", pnIndex + 30);
            if (endTdIndex < 0) {
                throw new IllegalStateException("</td> not found after index " + pnIndex + 30);
            }
            String phoneNumberUnparsed = pageContent.substring(pnIndex + 30, endTdIndex);

            String a1 = Pattern.compile("</td>.*", Pattern.DOTALL).matcher(addressUnparsed).replaceAll("");
            String a2 = Pattern.compile("\\s*<br/>\\s*", Pattern.DOTALL).matcher(a1).replaceFirst("");
            String[] addressParts = a2.split("\\s*<br/>\\s*");
            String address = String.join(" ", addressParts);
            System.out.println(address);

            String contactEmail = null;
            Pattern p = Pattern.compile("mailto:([^\"]*)\"");
            Matcher m = p.matcher(contactEmailUnparsed);
            if (m.find()) {
                contactEmail = m.group(1).trim();
                System.out.println(contactEmail);
            }

            String phoneNumber = Pattern.compile("\\s*<br/>\\s*", Pattern.DOTALL).matcher(phoneNumberUnparsed).replaceAll("").trim();
            System.out.println(phoneNumber);
            start = endTdIndex;

            ContactAddress contactAddress = new ContactAddress();
            contactAddress.address = address;
            contactAddress.contactEmail = contactEmail;
            contactAddress.phoneNumber = phoneNumber;
            addresses.add(contactAddress);
        }
        return addresses;
    }

    private static void parseFinances(String pageContent, Company c) {

    }

    private static String parseContactField(String pageContent, String fieldName) {
        Pattern pattern = Pattern.compile("<strong>" + fieldName + "</strong>\\s*:\\s*<a href[^>]*>\\s*([^\\s]*)\\s*</a>");
        Matcher matcher = pattern.matcher(pageContent);
        if (matcher.find()) {
            String field = matcher.group(1);
            System.out.println(field);
            return field;
        }
        return null;
    }

    private static String parseTwitter(String pageContent) {
        Pattern pattern = Pattern.compile("<strong>Twitter</strong>\\s*\":\\s*\"\\s*<a href=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(pageContent);
        if (matcher.find()) {
            String twitter = matcher.group(1);
            System.out.println(twitter);
            return twitter;
        }
        return null;
    }

    private static void parseStaff(String pageContent, Company c) {
        List<Staff> staffs = new ArrayList<>();
        c.staff = staffs;

        Pattern pattern = Pattern.compile("<div class=\"item half[^>]*>\\s*<div[^>]*>\\s*<a[^>]*>\\s*<img[^>]*>\\s*</a>\\s*</div>\\s*<h2>\\s*<a href=[^>]+>([^<]+)</a>\\s*</h2>\\s*<h3>\\s*([^<]*)\\s*</h3>\\s*<p>([^<]*)</p>");
        Matcher matcher = pattern.matcher(pageContent);
        while (matcher.find()) {
            String name = matcher.group(1);
            System.out.println(name);
            String position = matcher.group(2).trim();
            System.out.println(position);
            String tel = matcher.group(3);
            System.out.println(tel);
            Staff staff = new Staff();
            staff.name = name;
            staff.position = position;
            staff.contact = tel;
            staffs.add(staff);
        }
    }

    private static void parseClients(String pageContent, Company c) {

    }

    private static void parseOverview(String pageContent, Company c) {
        String location = parseOverviewField(pageContent, "Location");
        String totalStaff = parseOverviewField(pageContent, "Total staff");
        String yearEstablished = parseOverviewField(pageContent, "Year established");

        Overview overview = new Overview();
        c.overview = overview;

        overview.location = location;
        overview.totalStaff = totalStaff;
        overview.yearEstablished = yearEstablished;
    }

    private static String parseOverviewField(String pageContent, String title) {
        Pattern pattern = Pattern.compile("<td class=\"profile_table_left_column\">" + title + "</td>\\s*<td>([^<]+)</td>");
        Matcher matcher = pattern.matcher(pageContent);
        if (matcher.find()) {
            String field = matcher.group(1);
            System.out.println(field);
            return field;
        }
        return null;
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
