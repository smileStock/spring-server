package com.example.smilestock.service;

import com.example.smilestock.entity.AnalysisEntity;
import com.example.smilestock.entity.StockEntity;
import com.example.smilestock.repository.AnalysisRepository;
import com.example.smilestock.repository.StockRepository;
import io.github.flashvayne.chatgpt.service.ChatgptService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ChatService {
    private final ChatgptService chatgptService;
    private final StockRepository stockRepository;
    private final AnalysisRepository analysisRepository;

    @Autowired
    public ChatService(ChatgptService chatgptService, StockRepository stockRepository, AnalysisRepository analysisRepository) {
        this.chatgptService = chatgptService;
        this.stockRepository = stockRepository;
        this.analysisRepository = analysisRepository;
    }

    @Value("${dart.api-key}")
    private String dartApiKey;


    public String getChatResponse(String prompt) {
        // ChatGPT 에게 질문을 던집니다.
        return chatgptService.sendMessage(prompt);
    }

    // 종목 코드 및 기업 코드 가져오기(DB 저장)
    @Transactional
    public ResponseEntity<?> getCorpInfo() {
        try {
            // url로 요청 후 binary 데이터 가져오기
            String url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + dartApiKey;
            RestTemplate restTemplate = new RestTemplate();
            byte[] zipBytes = restTemplate.getForObject(url, byte[].class);

            // 압축 해제
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry zipEntry = zis.getNextEntry();

                if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".xml")) {
                    // XML 파일 파싱
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(new InputSource(zis));
                    doc.getDocumentElement().normalize();

                    // list태그를 찾아 해당 노드들 순회
                    NodeList nList = doc.getElementsByTagName("list");
                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Node node = nList.item(temp);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;

                            String corpCode = element.getElementsByTagName("corp_code").item(0).getTextContent();
                            String stockCode = element.getElementsByTagName("stock_code").item(0).getTextContent().trim();

                            if (!stockCode.isEmpty()) {
                                // 주식 코드가 이미 데이터베이스에 존재하는지 확인
                                Optional<StockEntity> existingStock = stockRepository.findByStockCode(stockCode);
                                if (existingStock.isEmpty()) {
                                    StockEntity stockEntity = new StockEntity();
                                    stockEntity.setCorpCode(corpCode);
                                    stockEntity.setStockCode(stockCode);
                                    StockEntity savedStockEntity = stockRepository.save(stockEntity);

                                    AnalysisEntity analysisEntity = new AnalysisEntity();
                                    analysisEntity.setStockEntity(savedStockEntity);
                                    analysisRepository.save(analysisEntity);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error occurred while processing corp info: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("오류가 발생했습니다.");
        }
        return ResponseEntity.status(200).body("정상적으로 저장되었습니다.");
    }


    // 재무 정보 받아와 DB 저장하기
    public ResponseEntity<?> analysis() {

        // DB에서 고유 번호 가져오기
//        List<AnalysisEntity> analysisEntityList = analysisRepository.findAll();
        AnalysisEntity analysisEntity = analysisRepository.findById(2332L).get();

//        for ( AnalysisEntity analysisEntity : analysisEntityList){
//            // 정보 가져오기
//            String corp_code = analysisEntity.getStockEntity().getCorpCode();
//            String bsns_year = analysisEntity.getYear();
//            String reprt_code = analysisEntity.getReportCode();
//
//
//        }
        String corp_code = analysisEntity.getStockEntity().getCorpCode();
        Integer bsns_year = analysisEntity.getYear();
        Integer reprt_code = analysisEntity.getReportCode();

        if (bsns_year == null) {
            bsns_year = 2023;
        }
        if (reprt_code == null) {
            reprt_code = 11014;
        }

        // dart에 재무정보 요청
        requestDart(corp_code, bsns_year, reprt_code);

        // GPT에 분석 요청
        // 분석결과 DB에 저장

        return ResponseEntity.status(200).body("정상적으로 저장되었습니다.");
    }

    // dart에 재무정보 요청
    private void requestDart(String corp_code, Integer bsns_year, Integer reprt_code) {
        String requestURL = String.format(
                "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=%s",
                dartApiKey, corp_code, bsns_year, reprt_code);

        // dart로 요청
        try {
            URL url = new URL(requestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // GET 요청을 위한 설정
            conn.setRequestMethod("GET");

            // 응답 코드 확인 및 응답 내용 읽기
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 요청 성공
                // 응답 내용을 처리하는 로직을 여기에 작성하세요

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // 응답 내용을 JSON 객체로 변환
                JSONObject jsonResponse = new JSONObject(response.toString());

                // JSON에서 데이터 추출
                String status = jsonResponse.getString("status");
                String message = jsonResponse.getString("message");

                if ("000".equals(status)) {
                    JSONArray list = jsonResponse.getJSONArray("list");
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject item = list.getJSONObject(i);
                        // 필요한 정보 추출 예시
                        String accountNm = item.getString("account_nm");
                        String thstrmAmount = item.getString("thstrm_amount");
                        // 추가적인 필드 추출 및 처리...

                        System.out.println("Account Name: " + accountNm);
                        System.out.println("This Term Amount: " + thstrmAmount);
                        // 추출한 데이터 처리 로직...
                    }
                } else {
                    System.out.println("API 요청에 문제가 발생했습니다: " + message);
                }

            } else {
                // 요청 실패
                System.out.println("Request did not work: " + responseCode);
                System.out.println("Response Message : " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            System.out.println("Error occurred while processing corp info: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
