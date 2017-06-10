package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
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
    private static XSSFCreationHelper helper;

    public static void main(String[] args) throws IOException, InvalidFormatException {
        List<Company> model = createModel();
        writeToExcel(model);
    }

    private static void writeToExcel(List<Company> model) throws IOException, InvalidFormatException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        helper = new XSSFCreationHelper(workbook);
        XSSFSheet globalSheet = workbook.createSheet("Company");
        XSSFRow globalHeaderRow = globalSheet.createRow(0);
        writeGlobalHeaders(globalHeaderRow, globalSheet);

        XSSFSheet staffSheet = workbook.createSheet("Staff");
        XSSFRow staffHeaderRow = staffSheet.createRow(0);
        writeStaffHeaders(staffHeaderRow, staffSheet);

        XSSFSheet addressSheet = workbook.createSheet("Addresses");
        XSSFRow addressHeaderRow = addressSheet.createRow(0);
        writeAddressHeaders(addressHeaderRow, addressSheet);

        int staffRowIndex = 1;

        int contactAddressRowIndex = 1;

        for (int i = 0; i < model.size(); i++) {
            Company company = model.get(i);
            XSSFRow row = globalSheet.createRow(i + 1);

            int columnIndex = 0;
            writeCell(row, columnIndex++, company.name);
            XSSFCell companyCell = row.getCell(0);

            int firstContactAddressRowIndex = contactAddressRowIndex;
            for (ContactAddress contactAddress : company.contact.contactAddresses) {
                int addressColumnIndex = 0;
                XSSFRow addressSheetRow = addressSheet.createRow(contactAddressRowIndex++);
//                writeCell(addressSheetRow, addressColumnIndex++, company.name);
                writeCompanyLink(addressSheetRow, addressColumnIndex++, company.name, companyCell);
                writeCell(addressSheetRow, addressColumnIndex++, contactAddress.address);
                writeCell(addressSheetRow, addressColumnIndex++, contactAddress.contactEmail);
                writeCell(addressSheetRow, addressColumnIndex++, contactAddress.phoneNumber);
            }
            XSSFCell firstContactAddressCell = null;
            if (company.contact.contactAddresses!= null && !company.contact.contactAddresses.isEmpty()) {
                firstContactAddressCell = addressSheet.getRow(firstContactAddressRowIndex).getCell(0);
            }

            writeAddressLink(row, columnIndex++, company.overview.location, firstContactAddressCell);

            int firstStaffRowIndex = staffRowIndex;
            for (Staff staff : company.staff) {
                int staffColumnIndex = 0;
                XSSFRow staffSheetRow = staffSheet.createRow(staffRowIndex++);
                writeCompanyLink(staffSheetRow, staffColumnIndex++, company.name, companyCell);
                writeCell(staffSheetRow, staffColumnIndex++, staff.name);
                writeCell(staffSheetRow, staffColumnIndex++, staff.position);
                writeCell(staffSheetRow, staffColumnIndex++, staff.contact);
            }
            XSSFCell firstStaffCell = null;
            if (company.staff != null && !company.staff.isEmpty()) {
                firstStaffCell = staffSheet.getRow(firstStaffRowIndex).getCell(0);
            }

            writeStaffLink(row, columnIndex++, company.overview.totalStaff, firstStaffCell);
            writeCell(row, columnIndex++, company.overview.yearEstablished);
            writeLink(row, columnIndex++, company.contact.website);
            writeCell(row, columnIndex++, company.contact.contactEmail);
            writeLink(row, columnIndex++, company.contact.twitter);
            writeLink(row, columnIndex++, company.contact.facebook);
            writeLink(row, columnIndex++, company.contact.linkedIn);


        }
        OutputStream stream = new FileOutputStream("output.xlsx");
        workbook.write(stream);
    }

    private static void writeAddressLink(XSSFRow row, int i, String location, XSSFCell firstContactAddressCell) {
        if (firstContactAddressCell == null) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(location);
        } else {
            XSSFCell cell = row.createCell(i);
            Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.DOCUMENT);
            hyperlink.setAddress("'" + firstContactAddressCell.getSheet().getSheetName() + "'!" + firstContactAddressCell.getAddress().toString());

            if (location == null || location.isEmpty()) {
                location = "N/A";
            }

            hyperlink.setLabel(location);
            cell.setHyperlink(hyperlink);
            cell.setCellValue(location);
        }
    }

    private static void writeAddressHeaders(XSSFRow headerRow, XSSFSheet staffSheet) {
        int columnIndex = 0;
        writeCell(headerRow, columnIndex++, "Company");
        writeCell(headerRow, columnIndex++, "Address");
        writeCell(headerRow, columnIndex++, "Contact email");
        writeCell(headerRow, columnIndex++, "Phone number");

        staffSheet.setColumnWidth(0, 256 * 20);
        staffSheet.setColumnWidth(1, 256 * 100);
        staffSheet.setColumnWidth(2, 256 * 30);
        staffSheet.setColumnWidth(3, 256 * 20);
    }

    private static void writeStaffLink(XSSFRow row, int i, String totalStaff, XSSFCell firstStaffCell) {
        if (firstStaffCell == null) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(totalStaff);
        } else {
            XSSFCell cell = row.createCell(i);
            Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.DOCUMENT);
            hyperlink.setAddress("'" + firstStaffCell.getSheet().getSheetName() + "'!" + firstStaffCell.getAddress().toString());

            if (totalStaff == null || totalStaff.isEmpty()) {
                totalStaff = "N/A";
            }

            hyperlink.setLabel(totalStaff);
            cell.setHyperlink(hyperlink);
            cell.setCellValue(totalStaff);
        }
    }

    private static void writeCompanyLink(XSSFRow row, int i, String value, XSSFCell companyCell) {
        XSSFCell cell = row.createCell(i);
        Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.DOCUMENT);
        hyperlink.setAddress("'" + companyCell.getSheet().getSheetName() + "'!" + companyCell.getAddress().toString());
        hyperlink.setLabel(value);
        cell.setHyperlink(hyperlink);
        cell.setCellValue(value);
    }

    private static void writeStaffHeaders(XSSFRow headerRow, XSSFSheet staffSheet) {
        int columnIndex = 0;
        writeCell(headerRow, columnIndex++, "Company");
        writeCell(headerRow, columnIndex++, "Name");
        writeCell(headerRow, columnIndex++, "Position");
        writeCell(headerRow, columnIndex++, "Contact");

        staffSheet.setColumnWidth(0, 256 * 20);
        staffSheet.setColumnWidth(1, 256 * 20);
        staffSheet.setColumnWidth(2, 256 * 30);
        staffSheet.setColumnWidth(3, 256 * 20);
    }

    private static void writeGlobalHeaders(XSSFRow headerRow, XSSFSheet globalSheet) {
        int columnIndex = 0;
        writeCell(headerRow, columnIndex++, "Name");
        writeCell(headerRow, columnIndex++, "Location");
        writeCell(headerRow, columnIndex++, "Total staff");
        writeCell(headerRow, columnIndex++, "Year established");
        writeCell(headerRow, columnIndex++, "Website");
        writeCell(headerRow, columnIndex++, "Contact email");
        writeCell(headerRow, columnIndex++, "Twitter");
        writeCell(headerRow, columnIndex++, "Facebook");
        writeCell(headerRow, columnIndex++, "LinkedIn");
//        for (int i = 0; i < columnIndex; i++) {
//            globalSheet.autoSizeColumn(i);
//        }
        globalSheet.setColumnWidth(0, 256 * 20);
        globalSheet.setColumnWidth(1, 256 * 20);
        globalSheet.setColumnWidth(2, 256 * 10);
        globalSheet.setColumnWidth(3, 256 * 15);
        globalSheet.setColumnWidth(4, 256 * 30);
        globalSheet.setColumnWidth(5, 256 * 30);
        globalSheet.setColumnWidth(6, 256 * 30);
        globalSheet.setColumnWidth(7, 256 * 30);
        globalSheet.setColumnWidth(8, 256 * 30);
        globalSheet.setColumnWidth(9, 256 * 30);
    }

    private static void writeCell(XSSFRow row, int i, String value) {
        XSSFCell cell = row.createCell(i);
        cell.setCellValue(value);
    }

    private static void writeLink(XSSFRow row, int i, String value) {
        if (value == null) {
            value = "";
        }
        XSSFCell cell = row.createCell(i);
        Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(value);
        hyperlink.setLabel(value);
        cell.setHyperlink(hyperlink);
        cell.setCellValue(value);
    }

    private static List<Company> createModel() throws IOException {
        HashMap map = readMainData();
        File companyDirectory = new File("companies");
        companyDirectory.mkdir();

        List<Company> companies = new ArrayList<>();

        List<HashMap> responseList = (List<HashMap>) map.get("response");
//        int size = responseList.size();
        int size = 10;
        for (int i = 0; i < size; i++) {
            HashMap company = responseList.get(i);
            String id = ((String) company.get("id"));
            String companyTitle = (String) company.get("title");
            System.out.println("Company " + companyTitle + " started");
            File companyFolder = new File(companyDirectory, id);

            Company c = new Company();
            c.name = companyTitle;
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
        return companies;
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

        contact.website = fixIllegalCharacters(fixWww(prependProtocol(website)));
        contact.contactEmail = contactEmail;
        contact.twitter = fixIllegalCharacters(fixWww(prependProtocol(twitter)));
        contact.facebook = fixIllegalCharacters(fixWww(prependProtocol(facebook)));
        contact.linkedIn = fixIllegalCharacters(fixWww(prependProtocol(linkedIn)));

        List<ContactAddress> addresses = parseAddresses(pageContent);

        contact.contactAddresses = addresses;
    }

    private static String fixIllegalCharacters(String url) {
        if (url != null && !url.isEmpty() && url.contains(",")) {
            return url.replace(",", ".");
        } else {
            return url;
        }
    }

    private static String fixWww(String url) {
        if (url != null && !url.isEmpty() && url.matches(".*https?://[^w].*")) {
            return url.replace("://", "://www.");
        } else {
            return url;
        }
    }

    private static String prependProtocol(String url) {
        if (url != null && url.trim().equals("http://")) {
            return null;
        }
        if (url != null && !url.isEmpty() && !url.startsWith("http")) {
            return "http://" + url;
        } else {
            return url;
        }
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
            String address = String.join(" ", addressParts).trim();
            System.out.println(address);

            String contactEmail = null;
            Pattern p = Pattern.compile("mailto:([^\"]*)\"");
            Matcher m = p.matcher(contactEmailUnparsed);
            if (m.find()) {
                contactEmail = m.group(1).trim();
                System.out.println(contactEmail);
            }

            String phoneNumber = Pattern.compile("\\s*<br/>\\s*", Pattern.DOTALL).matcher(phoneNumberUnparsed).replaceAll("").trim();
            if (phoneNumber.contains("The agency has not filled in this phone number.")) {
                phoneNumber = null;
            }
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
            return field.trim();
        }
        return null;
    }

    private static String parseTwitter(String pageContent) {
        Pattern pattern = Pattern.compile("<strong>Twitter</strong>\\s*:\\s*<a href=\"([^\"]*)\"");
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
            if (tel.startsWith("T: ")){
                tel = tel.replace("T: ", "");
            }
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

    private static HashMap readMainData() throws IOException {
        File file = new File("main_data.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, HashMap.class);
    }

}
