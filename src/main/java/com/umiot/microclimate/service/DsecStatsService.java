package com.umiot.microclimate.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DsecStatsService {

    private static final String WS_URL = "https://www.dsec.gov.mo/TimeSeriesDatabase.asmx";
    private static final String NS = "http://www.dsec.gov.mo/";

    private static final int GDP_ID = 1129;
    private static final int GDP_CHANGE_ID = 1153;
    private static final int VISITOR_ID = 68929;
    private static final int ELEC_ID = 55184;
    private static final int POPULATION_ID = 21001;
    private static final double MACAU_LAND_AREA_KM2 = 33.3;

    private final RestTemplate rest = new RestTemplate();

    public Map<String, Object> fetchCityInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // GDP (季度, MOP百万)
        try { info.putAll(gdp()); } catch (Exception e) { info.put("gdp", null); }

        // 入境旅客 (月度)
        try { info.putAll(visitor()); } catch (Exception e) { info.put("visitor", null); }

        // 用电量 (年度人均)
        try { info.putAll(electricity()); } catch (Exception e) { info.put("electricity", null); }

        // 人口
        try { info.putAll(population()); } catch (Exception e) { info.put("population", null); }

        info.put("updateTime", java.time.LocalDateTime.now().toString());
        return info;
    }

    private Map<String, Object> gdp() throws Exception {
        Map<String, String> v = call(GDP_ID, "Quarterly");
        Map<String, String> chg = call(GDP_CHANGE_ID, "Quarterly");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gdp", v.get("value"));
        m.put("gdpPeriod", v.get("period"));
        m.put("gdpUnit", v.get("unit"));
        m.put("gdpChange", chg.get("value"));
        return m;
    }

    private Map<String, Object> visitor() throws Exception {
        Map<String, String> v = call(VISITOR_ID, "Monthly");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visitor", v.get("value"));
        m.put("visitorPeriod", v.get("period"));
        return m;
    }

    private Map<String, Object> electricity() throws Exception {
        Map<String, String> v = call(ELEC_ID, "Yearly");
        Map<String, Object> m = new LinkedHashMap<>();
        // DSEC 返回单位 10³ kWh，换算为 kWh
        double val = Double.parseDouble(v.get("value")) * 1000;
        m.put("electricity", Math.round(val));
        m.put("electricityPeriod", v.get("period"));
        m.put("electricityUnit", "kWh/年");
        return m;
    }

    private Map<String, Object> population() throws Exception {
        Map<String, String> v = call(POPULATION_ID, "Yearly");
        Map<String, Object> m = new LinkedHashMap<>();
        // DSEC 返回单位 '000/km²，换算为 人/km²
        double density = Double.parseDouble(v.get("value"));
        m.put("population", Math.round(density * 1000));
        m.put("populationUnit", "人/km²");
        // 推算常住人口: 密度(千人/km²) × 面积(km²) × 1000
        long residentPop = Math.round(density * MACAU_LAND_AREA_KM2 * 1000);
        m.put("residentPopulation", residentPop);
        return m;
    }

    private Map<String, String> call(int indicatorId, String period) throws Exception {
        String soapXml = String.format(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>" +
            "<getIndicatorLatestValue xmlns=\"%s\">" +
            "<iIndicatorID>%d</iIndicatorID>" +
            "<vLanguageType>English</vLanguageType>" +
            "<vFunctionType>VAL</vFunctionType>" +
            "<vDataPeriodType>%s</vDataPeriodType>" +
            "</getIndicatorLatestValue>" +
            "</soap:Body>" +
            "</soap:Envelope>", NS, indicatorId, period);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", NS + "getIndicatorLatestValue");

        ResponseEntity<String> resp = rest.exchange(
            WS_URL, HttpMethod.POST, new HttpEntity<>(soapXml, headers), String.class);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new ByteArrayInputStream(resp.getBody().getBytes(StandardCharsets.UTF_8)));

        Map<String, String> result = new LinkedHashMap<>();
        result.put("value", getText(doc, "IndicatorValue"));
        result.put("period", getText(doc, "ReferencePeriod"));
        result.put("unit", getText(doc, "UnitLabel"));
        return result;
    }

    public String searchIndicator(String keyword, String lang) throws Exception {
        String soapXml = String.format(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
            "<soap:Body>" +
            "<getIndicatorID xmlns=\"%s\">" +
            "<strIndicatorDescription>%s</strIndicatorDescription>" +
            "<vLanguageType>%s</vLanguageType>" +
            "</getIndicatorID>" +
            "</soap:Body>" +
            "</soap:Envelope>", NS, keyword, lang);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", NS + "getIndicatorID");

        ResponseEntity<String> resp = rest.exchange(
            WS_URL, HttpMethod.POST, new HttpEntity<>(soapXml, headers), String.class);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder()
            .parse(new ByteArrayInputStream(resp.getBody().getBytes(StandardCharsets.UTF_8)));

        StringBuilder sb = new StringBuilder();
        NodeList indicators = doc.getElementsByTagNameNS(NS, "DSECIndicator");
        if (indicators.getLength() == 0) {
            // fallback: try without namespace
            indicators = doc.getElementsByTagName("DSECIndicator");
        }
        for (int i = 0; i < indicators.getLength(); i++) {
            Element el = (Element) indicators.item(i);
            NodeList idNodes = el.getElementsByTagNameNS(NS, "IndicatorID");
            if (idNodes.getLength() == 0) idNodes = el.getElementsByTagName("IndicatorID");
            NodeList descNodes = el.getElementsByTagNameNS(NS, "Description");
            if (descNodes.getLength() == 0) descNodes = el.getElementsByTagName("Description");
            String id = idNodes.getLength() > 0 ? idNodes.item(0).getTextContent() : "";
            String desc = descNodes.getLength() > 0 ? descNodes.item(0).getTextContent() : "";
            sb.append(id).append(": ").append(desc).append("\n");
        }
        return sb.toString();
    }

    private String getText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagNameNS(NS, tagName);
        if (nodes.getLength() > 0) return nodes.item(0).getTextContent();
        // 尝试不带 namespace
        nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) return nodes.item(0).getTextContent();
        return null;
    }
}
