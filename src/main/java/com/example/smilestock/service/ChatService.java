package com.example.smilestock.service;

import com.example.smilestock.dto.AnalysisDto;
import com.example.smilestock.entity.AnalysisEntity;
import com.example.smilestock.entity.StockEntity;
import com.example.smilestock.repository.AnalysisRepository;
import com.example.smilestock.repository.StockRepository;
import io.github.flashvayne.chatgpt.service.ChatgptService;
import lombok.extern.slf4j.Slf4j;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class ChatService {
    private final ChatgptService chatgptService;
    private final StockRepository stockRepository;
    private final AnalysisRepository analysisRepository;

    private final List<Integer> reprtCodes = Arrays.asList(11013, 11012, 11014, 11011);

    @Autowired
    public ChatService(ChatgptService chatgptService, StockRepository stockRepository, AnalysisRepository analysisRepository) {
        this.chatgptService = chatgptService;
        this.stockRepository = stockRepository;
        this.analysisRepository = analysisRepository;
    }

    @Value("${dart.api-key}")
    private String dartApiKey;
    @Value("${chatgpt.api-key}")
    private String chatgptApiKey;

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

    // 분석결과 DB에 저장

    // 재무 정보 받아와 DB 저장하기
    public ResponseEntity<AnalysisDto> analysis(String stock_code) {
        AnalysisDto analysisDto = new AnalysisDto();

        // DB에서 고유 번호 가져오기
        Optional<StockEntity> optionalStockEntity = stockRepository.findByStockCode(stock_code);

        if (!optionalStockEntity.isPresent()) {
            return ResponseEntity.status(204).body(analysisDto);
        }

        StockEntity stockEntity = optionalStockEntity.get();
        Optional<AnalysisEntity> optionalAnalysisEntity = analysisRepository.findByStockEntity(stockEntity);

        AnalysisEntity analysisEntity;
        if (optionalAnalysisEntity.isPresent()) {
            analysisEntity = optionalAnalysisEntity.get();
            if (analysisEntity.getAnalysisResult().equals("데이터 없음")) {
                log.info("DB에는 저장되어 있지만 데이터 없어서 바로 반환");
                analysisDto.setResult(analysisEntity.getAnalysisResult());
                return ResponseEntity.status(200).body(analysisDto);
            }
            int nextIndex = reprtCodes.indexOf(analysisEntity.getReportCode()) + 1;

            if (nextIndex >= reprtCodes.size()) {
                analysisEntity.setYear(analysisEntity.getYear() + 1);
                nextIndex = 0;  // 새로운 연도의 첫 번째 분기로 설정
            }

            analysisEntity.setReportCode(reprtCodes.get(nextIndex));
        } else {
            // DB에 데이터가 없으면 현재 년도의 작년 1분기부터 시작
            int lastYear = Year.now().getValue() - 1;
            analysisEntity = new AnalysisEntity();
            analysisEntity.setStockEntity(stockEntity);
            analysisEntity.setYear(lastYear);
            analysisEntity.setReportCode(11013); // 1분기 코드
            analysisRepository.save(analysisEntity);
        }

        // reportCode와 year을 requestDart에 전달
        JSONArray jsonArray = requestDart(analysisEntity, stockEntity.getCorpCode(), analysisEntity.getYear(), analysisEntity.getReportCode());

        // GPT에 분석 요청
        if (jsonArray.length() > 0) {
            // JSONArray에 내용이 있을 경우
            log.info(String.valueOf(jsonArray));
            String result = requestChat(jsonArray);
            analysisEntity.setAnalysisResult(result);
            analysisRepository.save(analysisEntity);
        } else {
            // JSONArray가 비어 있을 경우
            log.info("JSONArray is empty.");
        }
        analysisDto.setResult(analysisEntity.getAnalysisResult());
        return ResponseEntity.status(200).body(analysisDto);
    }

    // dart에 재무정보 요청
    private JSONArray requestDart(AnalysisEntity analysisEntity, String corp_code, Integer bsns_year, Integer reprt_code) {

        boolean dataFound = false;
        JSONArray jsonArray = new JSONArray();

        int index = reprtCodes.indexOf(reprt_code);
        for (; index < reprtCodes.size(); index++) {
            int currentReprtCode = reprtCodes.get(index);
            String requestURL = String.format(
                    "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=%s",
                    dartApiKey, corp_code, bsns_year, currentReprtCode);
            log.info("년도 = " + bsns_year + ", 분기 = " + (index + 1)+"분기 요청");

            try {
                URL url = new URL(requestURL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if ("000".equals(jsonResponse.optString("status"))) {
                        dataFound = true;
                        analysisEntity.setYear(bsns_year);
                        analysisEntity.setReportCode(currentReprtCode);
//                        analysisEntity.setAnalysisResult("양호");
                        analysisRepository.save(analysisEntity);
                        jsonArray = jsonResponse.getJSONArray("list");
                    } else if ("013".equals(jsonResponse.optString("status"))) {
                        log.info("해당 데이터가 없음: {}", currentReprtCode);
                        break;
                    }
                } else {
                    log.error("Request did not work: " + responseCode + ", " + conn.getResponseMessage());
                }
            } catch (Exception e) {
                log.error("Error occurred while processing corp info: " + e.getMessage());
            }
        }

        if (!dataFound && reprt_code == 11013) {
            // 처음 요청한 분기가 1분기이고 데이터를 찾지 못했다면 "데이터 없음" 처리
            analysisEntity.setYear(bsns_year);
            analysisEntity.setReportCode(reprt_code);
            analysisEntity.setAnalysisResult("데이터 없음");
            analysisRepository.save(analysisEntity);
        }

        return jsonArray;
    }

    // chat GPT에 값 요청
    private String requestChat(JSONArray jsonArray) {
        String requestURL = "https://api.openai.com/v1/chat/completions";

        try {
            URL url = new URL(requestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + chatgptApiKey);
            conn.setDoOutput(true);

            JSONObject requestBody = createRequestBody(jsonArray);  // 사용자 메시지를 인자로 전달
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                } catch (IOException e) {
                    log.error("Error occurred while reading the response: " + e.getMessage());
                }

                String responseStr = response.toString();
                JSONObject responseObject = new JSONObject(responseStr);
                JSONObject firstChoice = responseObject.getJSONArray("choices").getJSONObject(0);
                String chatbotMessage = firstChoice.getJSONObject("message").getString("content");

                log.info(String.valueOf(responseObject));
                log.info("chatGPT 응답: " + chatbotMessage);
                return chatbotMessage;
            } else {
                log.error("Request did not work: " + responseCode + ", " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            log.error("Error occurred while processing corp info: " + e.getMessage());
        }
        return null;
    }

    // GPT에 요청할 바디 만들기
    private JSONObject createRequestBody(JSONArray jsonArray) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "아래 제공된 재무 데이터를 바탕으로 회사의 재무 상태를 '양호', '우려', '잠식' 중 하나로 분류하고, 해당하는 단어만을 답변으로 반환해주세요.");
        messages.put(systemMessage);

        // 재무 정보 추출
        double currentAssets = 0.0, currentLiabilities = 0.0, totalAssets = 0.0, totalLiabilities = 0.0;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String accountName = item.getString("account_nm");
            double amount = Double.parseDouble(item.getString("thstrm_amount").replaceAll(",", ""));

            switch (accountName) {
                case "유동자산":
                    currentAssets = amount;
                    break;
                case "유동부채":
                    currentLiabilities = amount;
                    break;
                case "자산총계":
                    totalAssets = amount;
                    break;
                case "부채총계":
                    totalLiabilities = amount;
                    break;
            }
        }

        // 질문 구성
        String userMessage = String.format(
                "Given the following financial information: 유동자산: %f, 유동부채: %f, 자산총계: %f, 부채총계: %f. " +
                        "재정 상태가 양호, 우려, 또는 잠식인지 분석해줘.",
                currentAssets, currentLiabilities, totalAssets, totalLiabilities
        );

        JSONObject userMessageObject = new JSONObject();
        userMessageObject.put("role", "user");
        userMessageObject.put("content", userMessage);
        messages.put(userMessageObject);

        requestBody.put("messages", messages);

        return requestBody;
    }
}
